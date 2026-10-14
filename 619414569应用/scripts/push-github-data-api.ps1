param(
    [string]$Repository = '619414569/ai-project',
    [string]$Branch = 'main'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function New-GitProcess([string[]]$Arguments) {
    $gitPath = (Get-Command git -ErrorAction Stop).Source
    $startInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = $gitPath
    $startInfo.UseShellExecute = $false
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $startInfo.StandardOutputEncoding = [System.Text.UTF8Encoding]::new($false)
    $startInfo.StandardErrorEncoding = [System.Text.UTF8Encoding]::new($false)
    foreach ($argument in $Arguments) {
        $startInfo.ArgumentList.Add($argument)
    }
    return [System.Diagnostics.Process]::new() | ForEach-Object {
        $_.StartInfo = $startInfo
        $_
    }
}

function Invoke-GitText([string[]]$Arguments) {
    $process = New-GitProcess $Arguments
    $null = $process.Start()
    $stdout = $process.StandardOutput.ReadToEnd()
    $stderr = $process.StandardError.ReadToEnd()
    $process.WaitForExit()
    if ($process.ExitCode -ne 0) {
        throw "git $($Arguments -join ' ') failed: $stderr"
    }
    return $stdout
}

function Invoke-GitBytes([string[]]$Arguments) {
    $process = New-GitProcess $Arguments
    $null = $process.Start()
    $memory = [System.IO.MemoryStream]::new()
    $stderrTask = $process.StandardError.ReadToEndAsync()
    $process.StandardOutput.BaseStream.CopyTo($memory)
    $process.WaitForExit()
    $stderr = $stderrTask.GetAwaiter().GetResult()
    if ($process.ExitCode -ne 0) {
        throw "git $($Arguments -join ' ') failed: $stderr"
    }
    return $memory.ToArray()
}

function Get-GitHubCredential {
    $credentialOutput = "protocol=https`nhost=github.com`n`n" | git credential fill
    if ($LASTEXITCODE -ne 0) {
        throw 'Git credential helper did not return a GitHub credential'
    }
    $credential = @{}
    foreach ($line in $credentialOutput) {
        $separator = $line.IndexOf('=')
        if ($separator -gt 0) {
            $credential[$line.Substring(0, $separator)] = $line.Substring($separator + 1)
        }
    }
    if (-not $credential.ContainsKey('password') -or [string]::IsNullOrWhiteSpace($credential.password)) {
        throw 'GitHub token is missing from the Git credential helper'
    }
    return $credential
}

$credential = Get-GitHubCredential
$headers = @{
    Accept = 'application/vnd.github+json'
    Authorization = "Bearer $($credential.password)"
    'X-GitHub-Api-Version' = '2022-11-28'
    'User-Agent' = 'ai-project-release'
}

function Invoke-GitHubApi {
    param(
        [Parameter(Mandatory)] [string]$Method,
        [Parameter(Mandatory)] [string]$Path,
        [object]$Body
    )

    $parameters = @{
        Method = $Method
        Uri = "https://api.github.com/repos/$Repository$Path"
        Headers = $headers
        NoProxy = $true
    }
    if ($null -ne $Body) {
        $parameters.ContentType = 'application/json; charset=utf-8'
        $parameters.Body = $Body | ConvertTo-Json -Depth 30 -Compress
    }
    return Invoke-RestMethod @parameters
}

$localHead = (Invoke-GitText -Arguments @('rev-parse', 'HEAD')).Trim()
$remoteRef = Invoke-GitHubApi -Method GET -Path "/git/ref/heads/$Branch"
$remoteHead = [string]$remoteRef.object.sha

if ($remoteHead -eq $localHead) {
    Write-Output "GitHub $Branch already points to $localHead"
    exit 0
}

$null = Invoke-GitText -Arguments @('merge-base', '--is-ancestor', $remoteHead, $localHead)
$commitShas = (Invoke-GitText -Arguments @(
    'rev-list', '--reverse', "$remoteHead..$localHead"
)).Split("`n", [System.StringSplitOptions]::RemoveEmptyEntries)

$objectLines = (Invoke-GitText -Arguments @(
    'rev-list', '--objects', "$remoteHead..$localHead"
)).Split("`n", [System.StringSplitOptions]::RemoveEmptyEntries)
$blobShas = [System.Collections.Generic.HashSet[string]]::new()
foreach ($line in $objectLines) {
    $sha = $line.Substring(0, 40)
    $type = (Invoke-GitText -Arguments @('cat-file', '-t', $sha)).Trim()
    if ($type -eq 'blob') {
        $null = $blobShas.Add($sha)
    }
}

$blobPayloads = foreach ($blobSha in $blobShas) {
    [pscustomobject]@{
        Sha = $blobSha
        Body = (@{
            content = [Convert]::ToBase64String((Invoke-GitBytes -Arguments @('cat-file', 'blob', $blobSha)))
            encoding = 'base64'
        } | ConvertTo-Json -Compress)
    }
}
$apiBase = "https://api.github.com/repos/$Repository"
$createdBlobs = $blobPayloads | ForEach-Object -Parallel {
    $response = Invoke-RestMethod -NoProxy -Method POST `
        -Uri "$using:apiBase/git/blobs" `
        -Headers $using:headers `
        -ContentType 'application/json; charset=utf-8' `
        -Body $_.Body
    [pscustomobject]@{ Expected = $_.Sha; Actual = [string]$response.sha }
} -ThrottleLimit 8
foreach ($createdBlob in $createdBlobs) {
    if ($createdBlob.Actual -ne $createdBlob.Expected) {
        throw "Blob SHA mismatch: local $($createdBlob.Expected), GitHub $($createdBlob.Actual)"
    }
}
Write-Output "Verified $($blobShas.Count) blobs"

$expectedParent = $remoteHead
foreach ($commitSha in $commitShas) {
    $metadata = (Invoke-GitText -Arguments @(
        'show', '-s', '--format=%an%x09%ae%x09%aI%x09%cn%x09%ce%x09%cI%x09%T%x09%P', $commitSha
    )).TrimEnd("`r", "`n").Split("`t")
    if ($metadata.Count -ne 8) {
        throw "Unexpected commit metadata for $commitSha"
    }
    $treeSha = $metadata[6]
    $parents = $metadata[7].Split(' ', [System.StringSplitOptions]::RemoveEmptyEntries)
    if ($parents.Count -ne 1 -or $parents[0] -ne $expectedParent) {
        throw "Commit $commitSha is not a single-parent fast-forward from $expectedParent"
    }

    $treeEntries = @()
    $treeLines = (Invoke-GitText -Arguments @(
        '-c', 'core.quotePath=false', 'ls-tree', '-r', '--full-tree', $treeSha
    )).Split("`n", [System.StringSplitOptions]::RemoveEmptyEntries)
    foreach ($line in $treeLines) {
        $tab = $line.IndexOf("`t")
        if ($tab -lt 1) {
            throw "Unexpected tree entry in $treeSha"
        }
        $parts = $line.Substring(0, $tab).Split(' ')
        $treeEntries += @{
            path = $line.Substring($tab + 1)
            mode = $parts[0]
            type = $parts[1]
            sha = $parts[2]
        }
    }
    $createdTree = Invoke-GitHubApi -Method POST -Path '/git/trees' -Body @{ tree = $treeEntries }
    if ([string]$createdTree.sha -ne $treeSha) {
        throw "Tree SHA mismatch: local $treeSha, GitHub $($createdTree.sha)"
    }

    $rawCommit = Invoke-GitText -Arguments @('cat-file', 'commit', $commitSha)
    $messageSeparator = $rawCommit.IndexOf("`n`n")
    if ($messageSeparator -lt 0) {
        throw "Commit message separator was not found for $commitSha"
    }
    $message = $rawCommit.Substring($messageSeparator + 2)
    $createdCommit = Invoke-GitHubApi -Method POST -Path '/git/commits' -Body @{
        message = $message
        tree = $treeSha
        parents = @($expectedParent)
        author = @{
            name = $metadata[0]
            email = $metadata[1]
            date = $metadata[2]
        }
        committer = @{
            name = $metadata[3]
            email = $metadata[4]
            date = $metadata[5]
        }
    }
    if ([string]$createdCommit.sha -ne $commitSha) {
        throw "Commit SHA mismatch: local $commitSha, GitHub $($createdCommit.sha)"
    }
    $expectedParent = $commitSha
    Write-Output "Verified commit $commitSha"
}

$null = Invoke-GitHubApi -Method PATCH -Path "/git/refs/heads/$Branch" -Body @{
    sha = $localHead
    force = $false
}
$verifiedRef = Invoke-GitHubApi -Method GET -Path "/git/ref/heads/$Branch"
if ([string]$verifiedRef.object.sha -ne $localHead) {
    throw "Remote verification failed: expected $localHead, got $($verifiedRef.object.sha)"
}

$null = Invoke-GitText -Arguments @('update-ref', "refs/remotes/origin/$Branch", $localHead, $remoteHead)
Write-Output "GitHub $Branch advanced from $remoteHead to $localHead ($($commitShas.Count) commits, $($blobShas.Count) blobs verified)"

import { access, readFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import path from 'node:path'

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url))
const logDirectory = path.resolve(scriptDirectory, '..', '提交日志')
const entries = JSON.parse(await readFile(path.join(logDirectory, 'index.json'), 'utf8'))
const versionPattern = /^V(\d+)\.(\d+)$/
const fileNamePattern = /^\d{4}-\d{2}-\d{2}_V\d+\.\d+\.md$/
const errors = []
const keys = new Set()

if (!Array.isArray(entries) || entries.length === 0) {
  errors.push('index.json 必须包含至少一条版本记录')
}

for (const [index, entry] of entries.entries()) {
  const versionMatch = versionPattern.exec(entry.version ?? '')
  if (!entry.author || !entry.fileName || !entry.releasedAt || !entry.summary || !entry.releaseType) {
    errors.push(`第 ${index + 1} 条记录字段不完整`)
    continue
  }
  if (!versionMatch) errors.push(`${entry.fileName}: 版本号必须使用 V<major>.<minor>`)
  if (!fileNamePattern.test(entry.fileName)) errors.push(`${entry.fileName}: 文件名格式不正确`)
  if (!new Set(['major', 'minor']).has(entry.releaseType)) {
    errors.push(`${entry.fileName}: releaseType 必须是 major 或 minor`)
  }
  const key = `${entry.author}/${entry.fileName}`
  if (keys.has(key)) errors.push(`${key}: 索引重复`)
  keys.add(key)
  try {
    await access(path.join(logDirectory, entry.author, entry.fileName))
  } catch {
    errors.push(`${key}: Markdown 文件不存在`)
  }
}

const displayedFileNames = entries.map((entry) => entry.fileName)
const descendingFileNames = [...displayedFileNames].sort((left, right) => right.localeCompare(left))
if (displayedFileNames.some((fileName, index) => fileName !== descendingFileNames[index])) {
  errors.push('index.json 必须按文件名倒序排列')
}

const chronological = [...entries]
  .map((entry) => {
    const [, major, minor] = versionPattern.exec(entry.version) ?? []
    return { ...entry, major: Number(major), minor: Number(minor) }
  })
  .sort((left, right) => left.releasedAt.localeCompare(right.releasedAt))

if (chronological[0]?.version !== 'V1.0') errors.push('版本序列必须从 V1.0 开始')
for (let index = 1; index < chronological.length; index += 1) {
  const previous = chronological[index - 1]
  const current = chronological[index]
  const validMinor = current.major === previous.major
    && current.minor === previous.minor + 1
    && current.releaseType === 'minor'
  const validMajor = current.major === previous.major + 1
    && current.minor === 0
    && current.releaseType === 'major'
  if (!validMinor && !validMajor) {
    errors.push(`${previous.version} -> ${current.version}: 版本递增不符合规则`)
  }
}

if (errors.length > 0) {
  console.error(errors.join('\n'))
  process.exit(1)
}

console.log(`版本日志校验通过：${entries.length} 个版本，最新版本 ${entries[0].version}`)

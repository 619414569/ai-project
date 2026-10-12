import { readdir, readFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import path from 'node:path'

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url))
const notebookDirectory = path.resolve(scriptDirectory, '错题本')
const releaseCheck = process.argv.includes('--release')
const requiredFields = ['id', 'date', 'severity', 'status', 'summary', 'regression']
const allowedStatuses = new Set(['open', 'resolved'])

const files = (await readdir(notebookDirectory))
  .filter((file) => file.endsWith('.md') && file !== 'README.md')
  .sort()

if (files.length === 0) {
  throw new Error('错题本至少需要一条问题记录')
}

const seenIds = new Set()
const errors = []
let openCount = 0

for (const file of files) {
  const content = await readFile(path.join(notebookDirectory, file), 'utf8')
  const frontMatter = content.match(/^---\r?\n([\s\S]*?)\r?\n---/)
  if (!frontMatter) {
    errors.push(`${file}: 缺少 YAML 元数据`)
    continue
  }

  const metadata = Object.fromEntries(
    frontMatter[1]
      .split(/\r?\n/)
      .filter(Boolean)
      .map((line) => {
        const separator = line.indexOf(':')
        return separator < 1
          ? [line.trim(), '']
          : [line.slice(0, separator).trim(), line.slice(separator + 1).trim()]
      }),
  )

  for (const field of requiredFields) {
    if (!metadata[field]) errors.push(`${file}: 缺少字段 ${field}`)
  }
  if (metadata.id && seenIds.has(metadata.id)) errors.push(`${file}: 问题编号 ${metadata.id} 重复`)
  if (metadata.id) seenIds.add(metadata.id)
  if (metadata.status && !allowedStatuses.has(metadata.status)) {
    errors.push(`${file}: status 必须是 open 或 resolved`)
  }
  if (metadata.status === 'open') openCount += 1
}

if (releaseCheck && openCount > 0) {
  errors.push(`发布检查失败：错题本仍有 ${openCount} 条 open 记录`)
}

if (errors.length > 0) {
  console.error(errors.join('\n'))
  process.exit(1)
}

console.log(`错题本校验通过：${files.length} 条记录，${openCount} 条待解决${releaseCheck ? '，发布门禁已通过' : ''}`)

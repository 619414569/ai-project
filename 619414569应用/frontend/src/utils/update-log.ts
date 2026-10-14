import type { UpdateLogSummary } from '@/types/workspace'

export function filterAndSortUpdateLogs(
  logs: UpdateLogSummary[],
  selectedAuthor: string,
  keyword: string,
): UpdateLogSummary[] {
  const normalizedKeyword = keyword.trim().toLowerCase()
  return logs
    .filter((log) => {
      const matchesAuthor = selectedAuthor === 'all' || log.author === selectedAuthor
      const searchable = `${log.author}${log.fileName}${log.version}${log.summary}`.toLowerCase()
      return matchesAuthor && searchable.includes(normalizedKeyword)
    })
    .sort((left, right) => right.fileName.localeCompare(left.fileName))
}

import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import type { UpdateLogSummary } from '../src/types/workspace.ts'
import { filterAndSortUpdateLogs } from '../src/utils/update-log.ts'

const logs: UpdateLogSummary[] = [
  {
    author: 'yf',
    fileName: '2026-10-12_V1.0.md',
    version: 'V1.0',
    releaseType: 'major',
    releasedAt: '2026-10-12',
    summary: '初始版本',
  },
  {
    author: '619414569',
    fileName: '2026-10-14_V1.1.md',
    version: 'V1.1',
    releaseType: 'minor',
    releasedAt: '2026-10-14',
    summary: '在线预览',
  },
]

describe('update log list', () => {
  it('sorts file names in descending order', () => {
    assert.deepEqual(
      filterAndSortUpdateLogs(logs, 'all', '').map((log) => log.fileName),
      ['2026-10-14_V1.1.md', '2026-10-12_V1.0.md'],
    )
  })

  it('filters by author and searchable content', () => {
    assert.deepEqual(filterAndSortUpdateLogs(logs, '619414569', '预览'), [logs[1]])
    assert.deepEqual(filterAndSortUpdateLogs(logs, 'yf', 'V1.1'), [])
  })
})

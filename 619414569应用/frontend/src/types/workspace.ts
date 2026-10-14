export type ApplicationScope = 'personal' | 'collaboration'
export type ApplicationVisibility = 'shared' | 'exclusive'
export type ApplicationStatus = 'running' | 'paused'

export interface WorkspaceApplication {
  id: string
  name: string
  description: string
  category: string
  scope: ApplicationScope
  visibility: ApplicationVisibility
  status: ApplicationStatus
  owner: string
  collaborators: string[]
  applicationUrl?: string
  updatedAt: string
}

export interface CreateApplicationInput {
  name: string
  description: string
  category: string
  scope: ApplicationScope
  visibility: ApplicationVisibility
  owner: string
  collaborators: string[]
  applicationUrl?: string
}

export interface SubmissionLog {
  id: string
  username: string
  committedAt: string
  commit: string
  summary: string
  area: string
}

export type UpdateReleaseType = 'major' | 'minor'

export interface UpdateLogSummary {
  author: string
  fileName: string
  version: string
  releaseType: UpdateReleaseType
  releasedAt: string
  summary: string
}

export interface UpdateLogDocument extends UpdateLogSummary {
  markdown: string
}

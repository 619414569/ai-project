import { request } from '@/api/http'
import type { CreateApplicationInput, SubmissionLog, WorkspaceApplication } from '@/types/workspace'

type CreateApplicationRequest = Omit<CreateApplicationInput, 'owner'>

export const workspaceApi = {
  listApplications() {
    return request<WorkspaceApplication[]>({ method: 'GET', url: '/v1/workspace/applications' })
  },
  createApplication(data: CreateApplicationRequest) {
    return request<WorkspaceApplication>({ method: 'POST', url: '/v1/workspace/applications', data })
  },
  updateApplication(id: string, data: Partial<WorkspaceApplication>) {
    return request<WorkspaceApplication>({ method: 'PATCH', url: `/v1/workspace/applications/${id}`, data })
  },
  removeApplication(id: string) {
    return request<void>({ method: 'DELETE', url: `/v1/workspace/applications/${id}` })
  },
  listUsers() {
    return request<string[]>({ method: 'GET', url: '/v1/users' })
  },
  listSubmissionLogs() {
    return request<SubmissionLog[]>({ method: 'GET', url: '/v1/workspace/submission-logs' })
  },
}

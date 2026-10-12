import { request } from '@/api/http'
import type {
  CacheValue,
  MessageReceipt,
  PingResponse,
  ProcessStarted,
  WorkflowTask,
} from '@/types/api'

export const demoApi = {
  ping() {
    return request<PingResponse>({ method: 'GET', url: '/v1/public/ping' })
  },
  putCache(key: string, value: string, ttlSeconds: number) {
    return request<CacheValue>({
      method: 'PUT',
      url: `/v1/demo/cache/${encodeURIComponent(key)}`,
      data: { value, ttlSeconds },
    })
  },
  getCache(key: string) {
    return request<CacheValue>({
      method: 'GET',
      url: `/v1/demo/cache/${encodeURIComponent(key)}`,
    })
  },
  publishMessage(body: string) {
    return request<MessageReceipt>({ method: 'POST', url: '/v1/demo/messages', data: { body } })
  },
  startApproval(data: { title: string; businessKey: string; approver: string }) {
    return request<ProcessStarted>({ method: 'POST', url: '/v1/demo/workflows/approval', data })
  },
  listTasks(assignee: string) {
    return request<WorkflowTask[]>({
      method: 'GET',
      url: '/v1/demo/workflows/tasks',
      params: { assignee },
    })
  },
  completeTask(taskId: string) {
    return request<void>({ method: 'POST', url: `/v1/demo/workflows/tasks/${taskId}/complete` })
  },
}

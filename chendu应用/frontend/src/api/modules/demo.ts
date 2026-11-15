import { request } from '@/api/http'
import type { PingResponse } from '@/types/api'

export const demoApi = {
  ping() {
    return request<PingResponse>({ method: 'GET', url: '/v1/public/ping' })
  },
}

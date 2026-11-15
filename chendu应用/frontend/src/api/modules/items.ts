import { request } from '@/api/http'
import type { Item, ItemRequest } from '@/types/api'

export const itemsApi = {
  list(deleted = false) {
    return request<Item[]>({ method: 'GET', url: '/v1/items', params: { deleted } })
  },
  get(id: string) {
    return request<Item>({ method: 'GET', url: `/v1/items/${encodeURIComponent(id)}` })
  },
  create(data: ItemRequest) {
    return request<Item>({ method: 'POST', url: '/v1/items', data })
  },
  update(id: string, data: ItemRequest) {
    return request<Item>({ method: 'PUT', url: `/v1/items/${encodeURIComponent(id)}`, data })
  },
  trash(id: string) {
    return request<Item>({ method: 'DELETE', url: `/v1/items/${encodeURIComponent(id)}` })
  },
  restore(id: string) {
    return request<Item>({ method: 'POST', url: `/v1/items/${encodeURIComponent(id)}/restore` })
  },
  purge(id: string) {
    return request<void>({ method: 'DELETE', url: `/v1/items/${encodeURIComponent(id)}/permanent` })
  },
}

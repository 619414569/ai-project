export interface ApiResponse<T> {
  code: string
  message: string
  data: T
  traceId: string
  timestamp: string
}

export interface PingResponse {
  application: string
  status: string
  timestamp: string
}

export interface Item {
  id: string
  title: string
  content: string | null
  createdAt: string
  updatedAt: string
  deleted: boolean
}

export interface ItemRequest {
  title: string
  content: string
}

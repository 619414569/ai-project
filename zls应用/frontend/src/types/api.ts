export interface ApiResponse<T> {
  code: string
  message: string
  data: T
  traceId: string
  timestamp: string
}

export interface LoginRequest {
  username: string
  password: string
}

export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
}

export interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
}

export interface UserProfile {
  username: string
  roles: string[]
  passwordChangeRequired: boolean
}

export interface PingResponse {
  application: string
  status: string
  timestamp: string
}

export interface CacheValue {
  key: string
  value: string
}

export interface MessageReceipt {
  messageId: string
  destination: string
}

export interface ProcessStarted {
  processInstanceId: string
  processDefinitionId: string
  businessKey: string
}

export interface WorkflowTask {
  id: string
  name: string
  assignee: string
  processInstanceId: string
  createdAt: string | null
}

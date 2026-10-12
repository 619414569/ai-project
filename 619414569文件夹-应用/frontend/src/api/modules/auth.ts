import { request } from '@/api/http'
import type { ChangePasswordRequest, LoginRequest, LoginResponse, UserProfile } from '@/types/api'

export const authApi = {
  login(data: LoginRequest) {
    return request<LoginResponse>({ method: 'POST', url: '/v1/auth/login', data })
  },
  me() {
    return request<UserProfile>({ method: 'GET', url: '/v1/auth/me' })
  },
  changePassword(data: ChangePasswordRequest) {
    return request<void>({ method: 'POST', url: '/v1/auth/password', data })
  },
}

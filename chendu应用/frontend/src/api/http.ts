import axios, { AxiosError, type AxiosRequestConfig } from 'axios'
import type { ApiResponse } from '@/types/api'

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

client.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiResponse<unknown>>) => {
    const message = error.response?.data?.message || error.message || '请求失败'
    return Promise.reject(new Error(message))
  },
)

export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await client.request<ApiResponse<T>>(config)
  return response.data.data
}

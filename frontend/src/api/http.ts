import axios from 'axios'
import type { ApiResponse } from './types'
import { ApiCode } from './types'

const http = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
  timeout: 15000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  async (error) => {
    const code = error?.response?.data?.code
    if (code === ApiCode.UNAUTHORIZED) {
      localStorage.removeItem('token')
      localStorage.removeItem('user_info')
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export async function unwrapResponse<T>(promise: Promise<{ data: ApiResponse<T> }>): Promise<T> {
  const response = await promise
  if (response.data.code !== ApiCode.SUCCESS) {
    throw new Error(response.data.message || 'Request failed')
  }
  return response.data.data
}

export default http

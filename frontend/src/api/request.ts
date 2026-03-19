import http from './http'

export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  traceId?: string
  timestamp?: number
}

const request = {
  async get<T>(url: string, params?: any): Promise<ApiResponse<T>> {
    const response = await http.get(url, { params })
    return response.data
  },

  async post<T>(url: string, data?: any): Promise<ApiResponse<T>> {
    const response = await http.post(url, data)
    return response.data
  },

  async put<T>(url: string, data?: any): Promise<ApiResponse<T>> {
    const response = await http.put(url, data)
    return response.data
  },

  async patch<T>(url: string, data?: any): Promise<ApiResponse<T>> {
    const response = await http.patch(url, data)
    return response.data
  },

  async delete<T>(url: string): Promise<ApiResponse<T>> {
    const response = await http.delete(url)
    return response.data
  }
}

export default request

import http, { unwrapResponse } from './http'

export interface RegisterRequest {
  username: string
  phone: string
  password: string
}

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  username: string
}

export interface UserInfo {
  id: number
  username: string
  nickname: string
  avatarUrl?: string
  email?: string
  phone?: string
  bio?: string
}

/**
 * 用户注册
 */
export const register = (data: RegisterRequest) => {
  return unwrapResponse<void>(http.post('/auth/register', data))
}

/**
 * 用户登录
 */
export const login = (data: LoginRequest) => {
  return unwrapResponse<LoginResponse>(http.post('/auth/login', data))
}

/**
 * 获取当前用户信息
 */
export const getCurrentUser = () => {
  return unwrapResponse<UserInfo>(http.get('/users/me'))
}

import request from './request'
import type { UserInfo } from './auth'

export interface UpdateProfileRequest {
  nickname?: string
  bio?: string
  phone?: string
}

export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}

/**
 * 获取当前用户详情（含bio、phone明文）
 */
export const getMyProfile = () => {
  return request.get<UserInfo>('/users/me')
}

/**
 * 更新个人资料
 */
export const updateProfile = (data: UpdateProfileRequest) => {
  return request.put<void>('/users/me/profile', data)
}

/**
 * 上传头像（multipart/form-data）
 * 注意：不要手动设置 Content-Type，让 Axios 自动处理 boundary
 */
export const uploadAvatar = (file: File) => {
  const form = new FormData()
  form.append('file', file)
  return request.post<{ avatarUrl: string }>('/users/me/avatar', form)
}

/**
 * 修改密码
 */
export const changePassword = (data: ChangePasswordRequest) => {
  return request.post<void>('/users/me/change-password', data)
}

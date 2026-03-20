import request from './request'
import http from './http'

export interface WorkshopAuthor {
  id: number
  username: string
  nickname: string
  avatarUrl: string
}

export interface WorkshopProject {
  id: number
  projectNo: string
  title: string
  description: string
  coverUrl: string
  category: string
  tags: string
  version: string
  fileUrl: string
  fileSize: string
  visibility: number
  status: number
  likeCount: number
  favoriteCount: number
  downloadCount: number
  rating: number
  ratingCount: number
  author: WorkshopAuthor
  createdAt: string
  updatedAt: string
}

export interface WorkshopListResponse {
  items: WorkshopProject[]
  total: number
  page: number
  pageSize: number
}

export interface InteractionStatus {
  liked: boolean
  favorited: boolean
  subscribed: boolean
  myScore: number
}

/**
 * 获取工坊项目列表（公开）
 */
export const getWorkshopProjects = (params?: {
  category?: string
  keyword?: string
  sortBy?: string
  page?: number
  pageSize?: number
}) => {
  return request.get<WorkshopListResponse>('/workshop/projects', params)
}

/**
 * 获取项目详情
 */
export const getWorkshopProject = (id: number) => {
  return request.get<WorkshopProject>(`/workshop/projects/${id}`)
}

/**
 * 获取我的工坊项目
 */
export const getMyWorkshopProjects = (params?: { page?: number; pageSize?: number }) => {
  return request.get<WorkshopListResponse>('/workshop/my-projects', params)
}

/**
 * 创建工坊项目
 */
export const createWorkshopProject = (data: {
  title: string
  description?: string
  coverUrl?: string
  tags?: string
  version?: string
  fileUrl?: string
  fileSize?: string
  category?: string
}) => {
  return request.post<WorkshopProject>('/workshop/projects', data)
}

/**
 * 更新工坊项目
 */
export const updateWorkshopProject = (id: number, data: {
  title?: string
  description?: string
  coverUrl?: string
  tags?: string
  version?: string
  fileUrl?: string
  fileSize?: string
  category?: string
}) => {
  return request.put<WorkshopProject>(`/workshop/projects/${id}`, data)
}

/**
 * 删除工坊项目
 */
export const deleteWorkshopProject = (id: number) => {
  return request.delete<void>(`/workshop/projects/${id}`)
}

/**
 * 订阅/取消订阅
 */
export const toggleSubscription = (id: number) => {
  return request.post<{ subscribed: boolean }>(`/workshop/projects/${id}/subscribe`)
}

/**
 * 点赞/取消点赞
 */
export const toggleLike = (id: number) => {
  return request.post<{ liked: boolean }>(`/workshop/projects/${id}/like`)
}

/**
 * 收藏/取消收藏
 */
export const toggleFavorite = (id: number) => {
  return request.post<{ favorited: boolean }>(`/workshop/projects/${id}/favorite`)
}

/**
 * 评分
 */
export const rateProject = (id: number, score: number) => {
  return request.post<{ rating: number; ratingCount: number; myScore: number }>(
    `/workshop/projects/${id}/rate`, { score }
  )
}

/**
 * 获取用户对项目的交互状态
 */
export const getInteractionStatus = (id: number) => {
  return request.get<InteractionStatus>(`/workshop/projects/${id}/interaction`)
}

/**
 * 上传项目封面图
 */
export const uploadWorkshopCover = (file: File) => {
  const form = new FormData()
  form.append('file', file)
  return http.post<{ data: { url: string } }>('/workshop/upload-cover', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

import http from './http'
import request from './request'

export interface UserCreation {
  id: number
  title: string
  description: string
  fileUrl: string
  coverUrl: string
  fileType: 'image' | 'video' | 'audio' | 'other'
  fileSize: number
  visibility: 0 | 1  // 0=私密 1=公开
  createdAt: string
}

/**
 * 获取我的创作列表
 */
export const getMyCreations = () => {
  return request.get<UserCreation[]>('/creations')
}

/**
 * 上传新创作
 */
export const uploadCreation = (
  file: File,
  options?: { title?: string; description?: string; visibility?: 0 | 1 }
) => {
  const form = new FormData()
  form.append('file', file)
  if (options?.title) form.append('title', options.title)
  if (options?.description) form.append('description', options.description)
  form.append('visibility', String(options?.visibility ?? 0))
  return http.post<{ data: UserCreation }>('/creations', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 切换创作可见性
 */
export const updateCreationVisibility = (id: number, visibility: 0 | 1) => {
  return request.put<void>(`/creations/${id}/visibility`, { visibility })
}

/**
 * 删除创作
 */
export const deleteCreation = (id: number) => {
  return request.delete<void>(`/creations/${id}`)
}

/**
 * 获取创作下载信息（通过 Authorization header 鉴权）
 * 后端返回 JSON { fileUrl, fileName, fileType, fileSize }
 * 使用 fetch 鉴权后从 fileUrl 拉取实际文件 blob 触发下载
 */
export const downloadCreation = async (id: number, filename?: string) => {
  const token = localStorage.getItem('token')
  // 1. 鉴权获取文件信息
  const infoRes = await fetch(`http://localhost:8080/api/v1/creations/${id}/download`, {
    headers: { Authorization: `Bearer ${token}` }
  })
  if (!infoRes.ok) throw new Error('下载失败')
  const json = await infoRes.json()
  const fileUrl: string | undefined = json.data?.fileUrl
  const fileName: string = filename || json.data?.fileName || `creation-${id}`
  if (!fileUrl) throw new Error('获取下载链接失败')

  // 2. 拉取实际文件 blob（本地文件无需额外 auth；OSS presigned URL 也是公开的）
  const fileRes = await fetch(fileUrl)
  if (!fileRes.ok) throw new Error('文件下载失败')
  const blob = await fileRes.blob()
  const blobUrl = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = blobUrl
  a.download = fileName
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(blobUrl)
}

/**
 * 获取创作下载直链（用于在新标签页打开预览）
 * @deprecated 推荐使用 downloadCreation
 */
export const getDownloadUrl = (id: number) => {
  const token = localStorage.getItem('token')
  return `http://localhost:8080/api/v1/creations/${id}/download?token=${token}`
}

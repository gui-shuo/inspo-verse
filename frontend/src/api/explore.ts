import http, { unwrapResponse } from './http'

// ==================== 类型定义 ====================

export interface ExploreAuthor {
  id: number
  username: string
  nickname: string
  avatarUrl: string
  bio: string
}

export interface ExploreItem {
  id: number
  contentNo: string
  title: string
  description: string
  image: string
  images: string | null
  category: string
  tag: string
  likes: number
  comments: number
  views: number
  isLiked: boolean
  isFollowed?: boolean
  createdAt: string
  author: ExploreAuthor
}

export interface ExploreListResponse {
  items: ExploreItem[]
  total: number
  page: number
  pageSize: number
}

export interface ExploreComment {
  id: number
  contentId: number
  userId: number
  content: string
  parentCommentId: number
  replyToUserId: number | null
  likeCount: number
  createdAt: string
  author: ExploreAuthor
}

export interface CommentListResponse {
  comments: ExploreComment[]
  total: number
  page: number
  pageSize: number
}

// ==================== API 方法 ====================

export const exploreApi = {
  /** 获取发现内容列表（支持分类/搜索/排序/分页） */
  getList(params: {
    category?: string
    keyword?: string
    sortBy?: string
    page?: number
    pageSize?: number
  }): Promise<ExploreListResponse> {
    return unwrapResponse(http.get('/content/explore', { params }))
  },

  /** 获取内容详情 */
  getDetail(id: number): Promise<ExploreItem> {
    return unwrapResponse(http.get(`/content/explore/${id}`))
  },

  /** 点赞/取消点赞 */
  toggleLike(id: number): Promise<{ isLiked: boolean; likes: number }> {
    return unwrapResponse(http.post(`/content/explore/${id}/like`))
  },

  /** 获取评论列表 */
  getComments(id: number, page = 1, pageSize = 50): Promise<CommentListResponse> {
    return unwrapResponse(http.get(`/content/explore/${id}/comments`, { params: { page, pageSize } }))
  },

  /** 发布评论 */
  addComment(id: number, data: {
    content: string
    parentCommentId?: number
    replyToUserId?: number
  }): Promise<{ commentId: number; commentCount: number; author: ExploreAuthor }> {
    return unwrapResponse(http.post(`/content/explore/${id}/comments`, data))
  },

  /** 关注/取消关注作者 */
  toggleFollow(id: number): Promise<{ isFollowed: boolean }> {
    return unwrapResponse(http.post(`/content/explore/${id}/follow`))
  },

  /** 发布新内容 */
  publish(data: {
    category: string
    title: string
    description?: string
    coverUrl?: string
    images?: string
    tag?: string
  }): Promise<{ id: number; contentNo: string }> {
    return unwrapResponse(http.post('/content/explore', data))
  },

  /** 上传图片 */
  uploadImage(file: File): Promise<{ url: string }> {
    const formData = new FormData()
    formData.append('file', file)
    return unwrapResponse(http.post('/content/upload-image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }))
  },
}

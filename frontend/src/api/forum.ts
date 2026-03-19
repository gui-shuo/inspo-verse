import http, { unwrapResponse } from './http'

export interface ForumAuthor {
  username: string
  nickname: string
  avatarUrl: string
}

export interface ForumPost {
  id: number
  postNo: string
  userId: number
  category: string
  title: string
  content: string
  tags: string | null
  viewCount: number
  likeCount: number
  commentCount: number
  isTop: number
  isEssence: number
  createdAt: string
  author: ForumAuthor
}

export interface ForumComment {
  id: number
  postId: number
  userId: number
  content: string
  parentCommentId: number
  replyToUserId: number | null
  likeCount: number
  createdAt: string
  author: ForumAuthor
}

export interface PostListResponse {
  posts: ForumPost[]
  total: number
  page: number
  pageSize: number
}

export interface CommunityStats {
  todayPosts: number
  onlineUsers: number
}

export const forumApi = {
  /** 获取帖子分页列表 */
  getPosts(params: {
    category?: string
    keyword?: string
    sortBy?: string
    page?: number
    pageSize?: number
  }): Promise<PostListResponse> {
    return unwrapResponse(http.get('/forum/posts', { params }))
  },

  /** 获取帖子详情（同时增加浏览量） */
  getPost(id: number): Promise<ForumPost> {
    return unwrapResponse(http.get(`/forum/posts/${id}`))
  },

  /** 发布新帖 */
  createPost(data: {
    category: string
    title: string
    content: string
    tags?: string
  }): Promise<{ postId: number; postNo: string }> {
    return unwrapResponse(http.post('/forum/posts', data))
  },

  /** 删除帖子 */
  deletePost(id: number): Promise<void> {
    return unwrapResponse(http.delete(`/forum/posts/${id}`))
  },

  /** 获取帖子评论列表 */
  getComments(
    postId: number,
    page = 1,
    pageSize = 50
  ): Promise<{ comments: ForumComment[]; total: number; page: number; pageSize: number }> {
    return unwrapResponse(http.get('/forum/comments', { params: { postId, page, pageSize } }))
  },

  /** 发布评论 */
  createComment(data: {
    postId: number
    content: string
    parentCommentId?: number
    replyToUserId?: number
  }): Promise<{ commentId: number }> {
    return unwrapResponse(http.post('/forum/comments', data))
  },

  /** 点赞/收藏互动（幂等，重复调用则取消） */
  interact(data: {
    targetType: string
    targetId: number
    actionType: string
  }): Promise<void> {
    return unwrapResponse(http.post('/forum/interactions', data))
  },

  /** 检查是否已互动 */
  checkInteraction(
    targetType: string,
    targetId: number,
    actionType: string
  ): Promise<{ hasInteracted: boolean }> {
    return unwrapResponse(
      http.get('/forum/interactions/check', { params: { targetType, targetId, actionType } })
    )
  },

  /** 获取社区统计（今日发帖、在线用户） */
  getStats(): Promise<CommunityStats> {
    return unwrapResponse(http.get('/forum/stats'))
  },

  /** 上传帖子图片，返回图片URL */
  uploadImage(file: File): Promise<{ url: string }> {
    const formData = new FormData()
    formData.append('file', file)
    return unwrapResponse(
      http.post('/forum/upload-image', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
    )
  },
}

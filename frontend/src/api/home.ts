import http, { unwrapResponse } from './http'

export interface TopCreator {
  userId: number
  nickname: string
  avatarUrl: string
  bio: string
  totalLikes: number
  followerCount: number
}

export interface SubscribeResponse {
  subscribed: boolean
  message: string
}

export const homeApi = {
  /** 获取本周热门创作者 */
  getTopCreators(limit = 8): Promise<TopCreator[]> {
    return unwrapResponse(http.get('/public/top-creators', { params: { limit } }))
  },

  /** 邮箱订阅 */
  subscribe(email: string): Promise<SubscribeResponse> {
    return unwrapResponse(http.post('/public/subscribe', { email }))
  },

  /** 取消邮箱订阅 */
  unsubscribe(email: string): Promise<SubscribeResponse> {
    return unwrapResponse(http.post('/public/unsubscribe', { email }))
  },
}

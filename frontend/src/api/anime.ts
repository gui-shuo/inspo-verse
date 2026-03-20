import request from './request'

// ── 类型定义 ──────────────────────────────────────────────────────────────────

export interface AnimeItem {
  id: number
  seriesNo: string
  title: string
  description: string
  coverUrl: string
  heroUrl: string
  authorName: string
  userId: number
  score: number
  scheduleDay: number
  updateTime: string
  currentEpisode: string
  status: 'ONGOING' | 'COMPLETED' | 'UPCOMING' | 'AIRING'
  isPaid: boolean
  freeEpisodes: number
  priceCents: number
  price: number
  totalEpisodes: number
  viewCount: number
  subscribeCount: number
  createdAt: string
  subscribed?: boolean
  purchased?: boolean
}

export interface AnimePayOrderResp {
  orderNo: string
  animeId: number
  animeTitle: string
  amountCents: number
  amount: number
  payMethod: string
  payUrl: string
  qrCode: string
  expiredAt: string
  mockMode: boolean
}

export interface AnimePayStatusResp {
  orderNo: string
  status: 'PENDING' | 'PAID' | 'EXPIRED' | 'FAILED'
  animeId: number
  amountCents: number
  paidAt?: string
}

export interface AnimeOrderItem {
  id: number
  orderNo: string
  orderType: 'ANIME'
  animeId: number
  animeTitle: string
  amountCents: number
  amount: number
  payMethod: 'ALIPAY' | 'WECHAT'
  status: 'PENDING' | 'PAID' | 'EXPIRED' | 'FAILED'
  createdAt: string
  paidAt?: string
}

export interface CreateAnimeForm {
  title: string
  description?: string
  coverUrl?: string
  heroUrl?: string
  score?: number
  scheduleDay: number
  updateTime?: string
  currentEpisode?: string
  status?: string
  isPaid?: boolean
  freeEpisodes?: number
  priceCents?: number
  totalEpisodes?: number
}

// ── 番剧列表 ──────────────────────────────────────────────────────────────────

/** 按星期获取番剧列表（公开） */
export const getAnimeSchedule = (day: number) => {
  return request.get<AnimeItem[]>('/anime/schedule', { day })
}

/** 获取热门番剧（公开） */
export const getHotAnime = (limit = 20) => {
  return request.get<AnimeItem[]>('/anime/hot', { limit })
}

/** 获取番剧详情（公开，登录可获取追番/购买状态） */
export const getAnimeDetail = (id: number) => {
  return request.get<AnimeItem>(`/anime/${id}`)
}

// ── 番剧 CRUD ─────────────────────────────────────────────────────────────────

/** 发布番剧 */
export const createAnime = (data: CreateAnimeForm) => {
  return request.post<AnimeItem>('/anime', data)
}

/** 编辑番剧 */
export const updateAnime = (id: number, data: Partial<CreateAnimeForm>) => {
  return request.put<AnimeItem>(`/anime/${id}`, data)
}

/** 删除番剧 */
export const deleteAnime = (id: number) => {
  return request.delete<string>(`/anime/${id}`)
}

// ── 追番 ──────────────────────────────────────────────────────────────────────

/** 追番 */
export const subscribeAnime = (id: number) => {
  return request.post<string>(`/anime/${id}/subscribe`)
}

/** 取消追番 */
export const unsubscribeAnime = (id: number) => {
  return request.delete<string>(`/anime/${id}/subscribe`)
}

/** 我的追番列表 */
export const getMySubscriptions = () => {
  return request.get<AnimeItem[]>('/anime/my-subscriptions')
}

// ── 番剧支付 ──────────────────────────────────────────────────────────────────

/** 创建番剧支付订单 */
export const createAnimePayOrder = (animeId: number, payMethod: 'ALIPAY' | 'WECHAT') => {
  return request.post<AnimePayOrderResp>(`/anime/${animeId}/pay`, { payMethod })
}

/** 查询番剧支付状态（轮询） */
export const queryAnimePayStatus = (orderNo: string) => {
  return request.get<AnimePayStatusResp>(`/anime/order/status/${orderNo}`)
}

/** Mock 确认番剧支付 */
export const mockConfirmAnimePay = (orderNo: string) => {
  return request.post<string>(`/anime/order/mock-confirm/${orderNo}`)
}

/** 我的番剧订单 */
export const getMyAnimeOrders = () => {
  return request.get<AnimeOrderItem[]>('/anime/my-orders')
}

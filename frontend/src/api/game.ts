import request from './request'

// ==================== 类型定义 ====================

export interface GameItem {
  id: number
  gameNo: string
  title: string
  genre: string
  description: string
  cover: string
  hero: string
  gameUrl: string
  tags: string
  developer: string
  releaseDate: string
  rating: number
  ratingCount: number
  playCount: number
  favoriteCount: number
  isPaid: number
  priceCents: number
  trialMinutes: number
  isFavorited: boolean
  isPurchased?: boolean
  createdAt: string
  author: {
    id: number
    nickname: string
    avatar: string
  }
}

export interface GameListResp {
  items: GameItem[]
  total: number
  page: number
  pageSize: number
}

export interface GameOrderItem {
  id: number
  orderNo: string
  orderType: 'GAME'
  gameId: number
  gameTitle: string
  amountCents: number
  amount: number
  payMethod: 'ALIPAY' | 'WECHAT'
  status: 'PENDING' | 'PAID' | 'EXPIRED' | 'FAILED'
  createdAt: string
  paidAt?: string
}

export interface GamePurchaseResp {
  orderNo: string
  gameId: number
  gameTitle: string
  amountCents: number
  amount: number
  payMethod: string
  qrCode: string
  expiredAt: string
  mockMode: boolean
}

export interface GamePayStatusResp {
  orderNo: string
  status: 'PENDING' | 'PAID' | 'EXPIRED' | 'FAILED'
  gameId: number
  amountCents: number
  paidAt?: string
}

// ==================== 游戏列表（匿名可访问） ====================

export const getGameList = (params?: {
  genre?: string
  keyword?: string
  sortBy?: string
  page?: number
  pageSize?: number
}) => {
  return request.get<GameListResp>('/games', params)
}

// ==================== 游戏详情（匿名可访问） ====================

export const getGameDetail = (id: number) => {
  return request.get<GameItem>(`/games/${id}`)
}

// ==================== 收藏/取消收藏（需登录） ====================

export const toggleGameFavorite = (id: number) => {
  return request.post<{ isFavorited: boolean; favoriteCount: number }>(`/games/${id}/favorite`)
}

// ==================== 发布游戏（需登录） ====================

export interface PublishGameData {
  title: string
  genre: string
  description?: string
  coverUrl?: string
  heroUrl?: string
  gameUrl?: string
  tags?: string
  developer?: string
  releaseDate?: string
  isPaid?: number
  priceCents?: number
  trialMinutes?: number
}

export const publishGame = (data: PublishGameData) => {
  return request.post<{ id: number; gameNo: string }>('/games', data)
}

// ==================== 编辑游戏（需登录 + 本人） ====================

export const updateGame = (id: number, data: Partial<PublishGameData>) => {
  return request.put<string>(`/games/${id}`, data)
}

// ==================== 删除游戏（需登录 + 本人） ====================

export const deleteGame = (id: number) => {
  return request.delete<string>(`/games/${id}`)
}

// ==================== 游戏购买支付 ====================

export const createGamePurchase = (gameId: number, payMethod: 'ALIPAY' | 'WECHAT') => {
  return request.post<GamePurchaseResp>(`/games/${gameId}/purchase`, { payMethod })
}

export const queryGameOrderStatus = (orderNo: string) => {
  return request.get<GamePayStatusResp>(`/games/orders/${orderNo}/status`)
}

export const mockConfirmGameOrder = (orderNo: string) => {
  return request.post<string>(`/games/orders/${orderNo}/mock-confirm`)
}

// ==================== 我的游戏订单 ====================

export const getMyGameOrders = () => {
  return request.get<GameOrderItem[]>('/games/orders/my')
}

// ==================== 我发布的游戏 ====================

export const getMyGames = () => {
  return request.get<GameItem[]>('/games/my')
}

// ==================== 上传游戏图片 ====================

export const uploadGameImage = async (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  const { default: http } = await import('./http')
  const response = await http.post('/games/upload-image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return response.data as { code: number; message: string; data: { url: string } }
}

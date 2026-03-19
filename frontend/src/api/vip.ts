import request from './request'

export interface VipPlan {
  id: number
  planCode: string
  planName: string
  price: number
  priceCents: number
  durationDays: number
  level: number
  levelName: string
}

export interface VipOrder {
  id: number
  orderNo: string
  amount: number
  payStatus: number
  payStatusText: string
  payChannel?: string
  paidAt?: string
  createdAt: string
}

export interface VipMembership {
  isVip: boolean
  level: number
  levelName: string
  startTime?: string
  endTime?: string
  daysRemaining?: number
}

/**
 * 获取VIP套餐列表
 */
export const getVipPlans = () => {
  return request.get<VipPlan[]>('/vip/plans')
}

/**
 * 创建VIP订单
 */
export const createVipOrder = (planId: number) => {
  return request.post<{ orderId: number; orderNo: string; amount: number; payStatus: number }>('/vip/orders', { planId })
}

/**
 * 获取我的订单列表
 */
export const getMyOrders = () => {
  return request.get<VipOrder[]>('/vip/orders')
}

/**
 * 模拟支付
 */
export const payOrder = (orderId: number) => {
  return request.post<void>(`/vip/orders/${orderId}/pay`)
}

/**
 * 获取我的会员信息
 */
export const getMyMembership = () => {
  return request.get<VipMembership>('/vip/membership')
}

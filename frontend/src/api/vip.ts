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
  itemName?: string
  planLevel?: number
}

export interface VipMembership {
  isVip: boolean
  vipLevel: number
  vipLevelName: string
  startTime?: string
  endTime?: string
  daysRemaining?: number
  // 经验等级信息
  level: number
  levelName: string
  expPoints: number
  currentLevelExp: number
  nextLevelExp: number
  nextLevelName: string
  progressInLevel: number
  expNeeded: number
  isMaxLevel: boolean
}

export interface GrowthPoint {
  month: string
  monthKey: string
  exp: number
}

export interface DailyTaskItem {
  taskCode: string
  taskName: string
  description: string
  rewardPoints: number
  rewardExp: number
  dailyLimit: number
  taskType: string
  routePath: string
  progress: number
  completed: boolean
  rewarded: boolean
  statusText: string
}

export interface VipPrivilege {
  icon: string
  title: string
  desc: string
  unlocked: boolean
}

export interface VipOrderPaymentResult {
  orderId: number
  orderNo: string
  amount: number
  payStatus: number
  planName: string
  payMethod: string
  payUrl: string
  qrCode: string
  paymentOrderNo: string
  expiredAt: string
  mockMode: boolean
}

// ─── VIP 套餐 ──────────────────────────────────────────────────────────────
export const getVipPlans = () => {
  return request.get<VipPlan[]>('/vip/plans')
}

// ─── 创建VIP订单（含支付二维码） ────────────────────────────────────────────
export const createVipOrder = (planId: number, payMethod: 'ALIPAY' | 'WECHAT') => {
  return request.post<VipOrderPaymentResult>('/vip/orders', { planId, payMethod })
}

// ─── 查询VIP订单支付状态 ────────────────────────────────────────────────────
export const queryVipPayStatus = (orderNo: string) => {
  return request.get<{ orderNo: string; payStatus: number; payStatusText: string; paidAt?: string }>(`/vip/orders/${orderNo}/status`)
}

// ─── Mock确认VIP支付 ────────────────────────────────────────────────────────
export const mockConfirmVipPay = (orderNo: string) => {
  return request.post<string>(`/vip/orders/${orderNo}/mock-confirm`)
}

// ─── 获取我的VIP订单列表 ────────────────────────────────────────────────────
export const getMyOrders = () => {
  return request.get<VipOrder[]>('/vip/orders')
}

// ─── 旧版模拟支付（兼容） ───────────────────────────────────────────────────
export const payOrder = (orderId: number) => {
  return request.post<void>(`/vip/orders/${orderId}/pay`)
}

// ─── 会员信息（含经验等级） ─────────────────────────────────────────────────
export const getMyMembership = () => {
  return request.get<VipMembership>('/vip/membership')
}

// ─── 成长轨迹 ──────────────────────────────────────────────────────────────
export const getGrowthTrajectory = () => {
  return request.get<GrowthPoint[]>('/vip/growth')
}

// ─── 每日任务 ──────────────────────────────────────────────────────────────
export const getDailyTasks = () => {
  return request.get<DailyTaskItem[]>('/vip/tasks')
}

export const signIn = () => {
  return request.post<{ pointsEarned: number; expEarned: number; taskCode: string }>('/vip/tasks/signin')
}

export const claimTaskReward = (taskCode: string) => {
  return request.post<{ pointsEarned: number; expEarned: number; taskCode: string }>(`/vip/tasks/${taskCode}/claim`)
}

// ─── 会员权益 ──────────────────────────────────────────────────────────────
export const getVipPrivileges = () => {
  return request.get<VipPrivilege[]>('/vip/privileges')
}

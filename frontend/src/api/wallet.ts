import request from './request'

export interface WalletInfo {
  balance: number
  totalEarned: number
  totalSpent: number
  monthlySpent: number
  monthlyThreshold: number
  monthlyProgress: number
  monthlyRemaining: number
}

export interface PointTransaction {
  id: number
  amount: number
  type: string
  description: string
  balanceAfter: number
  refId?: string
  createdAt: string
}

/**
 * 获取钱包基本信息
 */
export const getWallet = () => {
  return request.get<WalletInfo>('/wallet')
}

/**
 * 获取收支明细
 */
export const getTransactions = (limit = 20) => {
  return request.get<PointTransaction[]>('/wallet/transactions', { limit })
}

/**
 * 每日签到（+50点）
 */
export const dailySignIn = () => {
  return request.post<{ balance: number; earned: number; message: string }>('/wallet/sign-in')
}

/**
 * 充值点数（开发模式直接充值，生产需先走支付）
 * @deprecated 改用 createPaymentOrder / mockConfirmPayment 走完整支付流程
 */
export const rechargePoints = (points: number) => {
  return request.post<{ balance: number; recharged: number; orderNo: string }>('/wallet/recharge', { points })
}

// ──────────────────────── 支付相关 ────────────────────────────────────────────

export interface PaymentPackage {
  id: string
  pts: number
  price: number
  label: string
  save?: string
  hot?: boolean
}

/** 前端固定套餐（与后端 PaymentService.PACKAGES 对应） */
export const RECHARGE_PACKAGES: PaymentPackage[] = [
  { id: 'p100',  pts: 100,  price: 6,   label: '入门包' },
  { id: 'p500',  pts: 500,  price: 28,  label: '标准包',  save: '节省7%'  },
  { id: 'p1500', pts: 1500, price: 68,  label: '超值包',  save: '节省24%', hot: true },
  { id: 'p5000', pts: 5000, price: 188, label: '豪华包',  save: '节省37%' },
]

export interface CreateOrderResp {
  orderNo: string
  points: number
  amount: number
  payMethod: string
  payUrl: string
  qrCode: string        // Base64 data URI
  expiredAt: string
  mockMode: boolean
}

export interface PayStatusResp {
  orderNo: string
  status: 'PENDING' | 'PAID' | 'EXPIRED' | 'FAILED'
  points: number
  amount: number
  paidAt?: string
}

/**
 * 创建支付订单
 */
export const createPaymentOrder = (packageId: string, payMethod: 'ALIPAY' | 'WECHAT') => {
  return request.post<CreateOrderResp>('/payment/create-order', { packageId, payMethod })
}

/**
 * 查询支付状态（轮询用）
 */
export const queryPaymentStatus = (orderNo: string) => {
  return request.get<PayStatusResp>(`/payment/status/${orderNo}`)
}

/**
 * Mock 模式：模拟扫码支付成功
 */
export const mockConfirmPayment = (orderNo: string) => {
  return request.post<string>(`/payment/mock-confirm/${orderNo}`)
}

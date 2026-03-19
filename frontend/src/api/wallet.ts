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
 */
export const rechargePoints = (points: number) => {
  return request.post<{ balance: number; recharged: number; orderNo: string }>('/wallet/recharge', { points })
}

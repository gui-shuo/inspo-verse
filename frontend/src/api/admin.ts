import http, { unwrapResponse } from './http'

// ========================= 仪表盘 =========================

export const getDashboardStats = () =>
  unwrapResponse<Record<string, any>>(http.get('/admin/dashboard/stats'))

export const getUserGrowthTrend = (days = 7) =>
  unwrapResponse<Array<{ date: string; newUsers: number }>>(
    http.get('/admin/dashboard/user-growth', { params: { days } })
  )

export const getRevenueComposition = () =>
  unwrapResponse<Array<{ name: string; value: number }>>(
    http.get('/admin/dashboard/revenue-composition')
  )

// ========================= 用户管理 =========================

export interface AdminUserListParams {
  page?: number
  size?: number
  keyword?: string
  status?: string
  role?: string
}

export const getAdminUsers = (params: AdminUserListParams = {}) =>
  unwrapResponse<any>(http.get('/admin/users', { params }))

export const toggleUserStatus = (userId: number) =>
  unwrapResponse<void>(http.put(`/admin/users/${userId}/toggle-status`))

export const updateUserRole = (userId: number, roleCode: string) =>
  unwrapResponse<void>(http.put(`/admin/users/${userId}/role`, { roleCode }))

export const resetUserPassword = (userId: number, newPassword: string) =>
  unwrapResponse<void>(http.put(`/admin/users/${userId}/reset-password`, { newPassword }))

// ========================= 帖子管理 =========================

export interface AdminPostListParams {
  page?: number
  size?: number
  keyword?: string
  category?: string
  status?: string
}

export const getAdminPosts = (params: AdminPostListParams = {}) =>
  unwrapResponse<any>(http.get('/admin/posts', { params }))

export const togglePostTop = (postId: number) =>
  unwrapResponse<void>(http.put(`/admin/posts/${postId}/toggle-top`))

export const togglePostEssence = (postId: number) =>
  unwrapResponse<void>(http.put(`/admin/posts/${postId}/toggle-essence`))

export const updatePostStatus = (postId: number, status: number) =>
  unwrapResponse<void>(http.put(`/admin/posts/${postId}/status`, { status }))

export const deletePost = (postId: number) =>
  unwrapResponse<void>(http.delete(`/admin/posts/${postId}`))

// ========================= 订单管理 =========================

export interface AdminOrderListParams {
  page?: number
  size?: number
  keyword?: string
  status?: string
  bizType?: string
}

export const getAdminOrders = (params: AdminOrderListParams = {}) =>
  unwrapResponse<any>(http.get('/admin/orders', { params }))

// ========================= AI 监控 =========================

export const getAiStats = () =>
  unwrapResponse<Record<string, any>>(http.get('/admin/ai/stats'))

export const getAiMessageLogs = (params: { page?: number; size?: number; safetyFilter?: string } = {}) =>
  unwrapResponse<any>(http.get('/admin/ai/messages', { params }))

// ========================= 内容管理（发现） =========================

export interface AdminContentListParams {
  page?: number
  size?: number
  keyword?: string
  category?: string
  status?: string
}

export const getAdminContent = (params: AdminContentListParams = {}) =>
  unwrapResponse<any>(http.get('/admin/content', { params }))

export const updateContentStatus = (contentId: number, status: number) =>
  unwrapResponse<void>(http.put(`/admin/content/${contentId}/status`, { status }))

export const deleteContent = (contentId: number) =>
  unwrapResponse<void>(http.delete(`/admin/content/${contentId}`))

// ========================= 番剧管理 =========================

export const getAdminAnime = (params: { page?: number; size?: number; keyword?: string; status?: string } = {}) =>
  unwrapResponse<any>(http.get('/admin/anime', { params }))

export const updateAnimeStatus = (animeId: number, status: string) =>
  unwrapResponse<void>(http.put(`/admin/anime/${animeId}/status`, { status }))

export const deleteAnime = (animeId: number) =>
  unwrapResponse<void>(http.delete(`/admin/anime/${animeId}`))

// ========================= 游戏管理 =========================

export const getAdminGames = (params: { page?: number; size?: number; keyword?: string; genre?: string } = {}) =>
  unwrapResponse<any>(http.get('/admin/games', { params }))

export const updateGameStatus = (gameId: number, status: number) =>
  unwrapResponse<void>(http.put(`/admin/games/${gameId}/status`, { status }))

export const deleteGame = (gameId: number) =>
  unwrapResponse<void>(http.delete(`/admin/games/${gameId}`))

// ========================= 工坊管理 =========================

export const getAdminWorkshop = (params: { page?: number; size?: number; keyword?: string; category?: string } = {}) =>
  unwrapResponse<any>(http.get('/admin/workshop', { params }))

export const updateWorkshopStatus = (projectId: number, status: number) =>
  unwrapResponse<void>(http.put(`/admin/workshop/${projectId}/status`, { status }))

export const deleteWorkshopProject = (projectId: number) =>
  unwrapResponse<void>(http.delete(`/admin/workshop/${projectId}`))

// ========================= 系统配置 =========================

export const getAllConfigs = () =>
  unwrapResponse<Array<{ id: number; configKey: string; configValue: string; valueType: string; remark: string }>>(
    http.get('/admin/settings')
  )

export const batchUpdateConfigs = (configs: Record<string, string>) =>
  unwrapResponse<void>(http.put('/admin/settings', configs))

export const updateConfig = (key: string, value: string, valueType?: string, remark?: string) =>
  unwrapResponse<void>(http.put(`/admin/settings/${key}`, { value, valueType, remark }))

// ========================= VIP 管理 =========================

export const getVipMembers = (params: { page?: number; size?: number } = {}) =>
  unwrapResponse<any>(http.get('/admin/vip/members', { params }))

export const getVipPlans = () =>
  unwrapResponse<any[]>(http.get('/admin/vip/plans'))

export const updateVipPlan = (planId: number, data: Record<string, any>) =>
  unwrapResponse<void>(http.put(`/admin/vip/plans/${planId}`, data))

// ========================= 每日任务管理 =========================

export const getDailyTasks = () =>
  unwrapResponse<any[]>(http.get('/admin/tasks'))

export const updateDailyTask = (taskId: number, data: Record<string, any>) =>
  unwrapResponse<void>(http.put(`/admin/tasks/${taskId}`, data))

// ========================= 邮件订阅管理 =========================

export const getEmailSubscriptions = (params: { page?: number; size?: number } = {}) =>
  unwrapResponse<any>(http.get('/admin/email-subscriptions', { params }))

// ========================= 权限检查 =========================

export const checkAdminAccess = () =>
  unwrapResponse<{ isAdmin: boolean; roles: string[] }>(http.get('/admin/check'))

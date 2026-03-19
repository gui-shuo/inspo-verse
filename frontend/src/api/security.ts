import request from './request'

export interface OAuthBinding {
  provider: string
  providerUsername: string
  createdAt: string
}

/**
 * 获取第三方绑定列表
 */
export const getOAuthBindings = () => {
  return request.get<OAuthBinding[]>('/oauth/bindings')
}

/**
 * 获取OAuth授权跳转URL
 */
export const getOAuthAuthUrl = (provider: 'github' | 'discord') => {
  return request.get<{ authUrl: string; state: string }>(`/oauth/authorize/${provider}`)
}

/**
 * OAuth回调绑定（前端拿到code后调此接口）
 */
export const oauthCallback = (provider: string, code: string) => {
  return request.post<{ provider: string; providerUsername: string }>(`/oauth/callback/${provider}`, { code })
}

/**
 * 解绑第三方账号
 */
export const unbindOAuth = (provider: string) => {
  return request.delete<void>(`/oauth/bindings/${provider}`)
}

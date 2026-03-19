import request from './request'

export interface AISession {
  id: number
  sessionNo: string
  title: string
  modelName: string
  scene: string
  tokenUsed: number
  createdAt: string
  updatedAt: string
}

export interface AIMessage {
  id: number
  role: 'user' | 'assistant'
  content: string
  tokens: number
  latencyMs?: number
  createdAt: string
}

/**
 * 创建AI会话
 */
export const createAISession = (scene?: string, modelName?: string) => {
  return request.post<{ sessionId: number; sessionNo: string; title: string; modelName: string }>('/ai/sessions', {
    scene: scene || 'general',
    modelName: modelName || 'gpt-3.5-turbo'
  })
}

/**
 * 获取会话列表
 */
export const getAISessions = () => {
  return request.get<AISession[]>('/ai/sessions')
}

/**
 * 获取会话消息历史
 */
export const getSessionMessages = (sessionId: number) => {
  return request.get<AIMessage[]>(`/ai/sessions/${sessionId}/messages`)
}

/**
 * 发送消息
 */
export const sendAIMessage = (sessionId: number, content: string) => {
  return request.post<AIMessage>('/ai/chat', { sessionId, content })
}

/**
 * 删除会话
 */
export const deleteAISession = (sessionId: number) => {
  return request.delete<void>(`/ai/sessions/${sessionId}`)
}

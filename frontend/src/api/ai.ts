import request from './request'
import http from './http'

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
    modelName: modelName || 'creative'
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
 * 发送消息（非流式，兼容旧接口）
 */
export const sendAIMessage = (sessionId: number, content: string) => {
  return request.post<AIMessage>('/ai/chat', { sessionId, content })
}

/**
 * 发送消息 — SSE 流式回复
 * 返回一个 EventSource 控制对象
 */
export function sendAIMessageStream(
  sessionId: number,
  content: string,
  callbacks: {
    onStart?: (data: { status: string; model: string }) => void
    onToken?: (data: { delta: string; tokenIndex: number }) => void
    onEnd?: (data: { finishReason: string; totalTokens: number; fullContent: string }) => void
    onError?: (message: string) => void
  }
): AbortController {
  const abortController = new AbortController()
  const token = localStorage.getItem('token')

  fetch('http://localhost:8080/api/v1/ai/chat/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
    body: JSON.stringify({ sessionId, content }),
    signal: abortController.signal,
  })
    .then(async (response) => {
      if (!response.ok) {
        const text = await response.text()
        callbacks.onError?.(`请求失败: ${response.status} ${text}`)
        return
      }

      const reader = response.body?.getReader()
      if (!reader) {
        callbacks.onError?.('无法读取响应流')
        return
      }

      const decoder = new TextDecoder()
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })

        // Parse SSE events from buffer
        const lines = buffer.split('\n')
        buffer = lines.pop() || '' // Keep incomplete line in buffer

        let currentEvent = ''
        for (const line of lines) {
          if (line.startsWith('event: ')) {
            currentEvent = line.substring(7).trim()
          } else if (line.startsWith('data: ')) {
            const data = line.substring(6)
            try {
              const parsed = JSON.parse(data)
              switch (currentEvent) {
                case 'start':
                  callbacks.onStart?.(parsed)
                  break
                case 'token':
                  callbacks.onToken?.(parsed)
                  break
                case 'end':
                  callbacks.onEnd?.(parsed)
                  break
                case 'error':
                  callbacks.onError?.(parsed.message || 'AI 服务异常')
                  break
              }
            } catch {
              // Ignore malformed JSON
            }
          }
        }
      }
    })
    .catch((err) => {
      if (err.name !== 'AbortError') {
        callbacks.onError?.(err.message || '网络错误')
      }
    })

  return abortController
}

/**
 * 上传文件并提取内容
 */
export const uploadFileForExtraction = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return http.post('/ai/upload-file', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 30000,
  }).then(res => res.data as { code: number; data: { text: string; fileType: string; fileName: string; charCount: number } })
}

/**
 * 语音转文字
 */
export const voiceToText = (audioBlob: Blob, filename: string = 'audio.webm') => {
  const formData = new FormData()
  formData.append('file', audioBlob, filename)
  return http.post('/ai/voice-to-text', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000,
  }).then(res => res.data as { code: number; data: { text: string; durationMs?: number } })
}

/**
 * 更新会话模型
 */
export const updateSessionModel = (sessionId: number, modelName: string) => {
  return request.patch<void>(`/ai/sessions/${sessionId}/model`, { modelName })
}

/**
 * 删除会话
 */
export const deleteAISession = (sessionId: number) => {
  return request.delete<void>(`/ai/sessions/${sessionId}`)
}

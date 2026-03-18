export interface StreamMessage {
  role: 'user' | 'assistant' | 'system'
  content: string
}

export interface StreamRequest {
  sessionId: string
  model: string
  messages: StreamMessage[]
}

export interface StreamCallbacks {
  onToken: (delta: string) => void
  onError: (error: Error) => void
  onDone: () => void
}

function parseSSEChunk(chunk: string, onEvent: (event: string, data: string) => void) {
  const events = chunk.split('\n\n')
  for (const item of events) {
    const lines = item.trim().split('\n')
    if (lines.length === 0) continue
    let eventName = 'message'
    let dataPayload = ''
    for (const line of lines) {
      if (line.startsWith('event:')) {
        eventName = line.slice(6).trim()
      } else if (line.startsWith('data:')) {
        dataPayload += line.slice(5).trim()
      }
    }
    if (dataPayload) onEvent(eventName, dataPayload)
  }
}

export async function streamAIChat(payload: StreamRequest, callbacks: StreamCallbacks): Promise<void> {
  const token = localStorage.getItem('token')
  const internalSign = import.meta.env.VITE_AI_INTERNAL_SIGN
  const response = await fetch(import.meta.env.VITE_AI_STREAM_URL || '/ai-stream/v1/chat/completions', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(internalSign ? { 'x-internal-sign': internalSign } : {})
    },
    body: JSON.stringify(payload)
  })

  if (!response.ok || !response.body) {
    callbacks.onError(new Error(`Streaming failed: ${response.status} ${response.statusText}`))
    return
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })

    const boundary = buffer.lastIndexOf('\n\n')
    if (boundary === -1) continue

    const complete = buffer.slice(0, boundary)
    buffer = buffer.slice(boundary + 2)

    parseSSEChunk(complete, (eventName, dataText) => {
      try {
        const data = JSON.parse(dataText)
        if (eventName === 'token' && typeof data.delta === 'string') {
          callbacks.onToken(data.delta)
        } else if (eventName === 'end') {
          callbacks.onDone()
        }
      } catch (error) {
        callbacks.onError(error instanceof Error ? error : new Error('Invalid stream payload'))
      }
    })
  }
}

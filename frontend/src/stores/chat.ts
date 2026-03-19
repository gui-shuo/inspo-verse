import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  createAISession,
  getAISessions,
  getSessionMessages,
  sendAIMessageStream,
  deleteAISession,
  uploadFileForExtraction,
  voiceToText,
  updateSessionModel,
} from '@/api/ai'

export interface Message {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: number
  isError?: boolean
  isStreaming?: boolean
  attachmentName?: string
}

export interface Conversation {
  id: string
  title: string
  modelName: string
  messages: Message[]
  updatedAt: number
  messagesLoaded: boolean
}

export const useChatStore = defineStore('chat', () => {
  const conversations = ref<Conversation[]>([])
  const currentConversationId = ref<string | null>(null)
  const isGenerating = ref(false)
  const currentModel = ref('creative')
  const loading = ref(false)
  const currentAbortController = ref<AbortController | null>(null)

  // ── 初始化：加载会话列表 ──────────────────────────────────────
  async function initChat() {
    try {
      loading.value = true
      const res = await getAISessions()
      conversations.value = res.data.map(session => ({
        id: session.id.toString(),
        title: session.title,
        modelName: session.modelName || 'creative',
        messages: [],
        updatedAt: new Date(session.updatedAt).getTime(),
        messagesLoaded: false,
      }))

      if (conversations.value.length === 0) {
        await createNewConversation()
      } else {
        currentConversationId.value = conversations.value[0].id
        currentModel.value = conversations.value[0].modelName || 'creative'
        // Load messages for first conversation
        await loadConversationMessages(conversations.value[0].id)
      }
    } catch (error: any) {
      console.error('加载会话失败', error.message)
    } finally {
      loading.value = false
    }
  }

  // ── 创建新对话 ────────────────────────────────────────────────
  async function createNewConversation() {
    try {
      loading.value = true
      const res = await createAISession('general', currentModel.value)
      const newConv: Conversation = {
        id: res.data.sessionId.toString(),
        title: res.data.title,
        modelName: res.data.modelName || currentModel.value,
        messages: [],
        updatedAt: Date.now(),
        messagesLoaded: true,
      }
      conversations.value.unshift(newConv)
      currentConversationId.value = newConv.id
    } catch (error: any) {
      console.error('创建会话失败', error.message)
    } finally {
      loading.value = false
    }
  }

  // ── 切换对话 & 加载历史消息 ───────────────────────────────────
  async function selectConversation(id: string) {
    currentConversationId.value = id
    const conv = conversations.value.find(c => c.id === id)
    if (conv) {
      currentModel.value = conv.modelName || 'creative'
      if (!conv.messagesLoaded) {
        await loadConversationMessages(id)
      }
    }
  }

  async function loadConversationMessages(id: string) {
    const conv = conversations.value.find(c => c.id === id)
    if (!conv) return

    try {
      const res = await getSessionMessages(parseInt(id))
      conv.messages = res.data.map(msg => ({
        id: msg.id.toString(),
        role: msg.role,
        content: msg.content,
        timestamp: new Date(msg.createdAt).getTime(),
      }))
      conv.messagesLoaded = true
    } catch (error: any) {
      console.error('加载消息历史失败', error.message)
    }
  }

  // ── 发送消息（SSE 流式回复）────────────────────────────────────
  async function sendMessage(content: string, attachmentName?: string) {
    if (!currentConversationId.value) return

    const conversation = conversations.value.find(c => c.id === currentConversationId.value)
    if (!conversation) return

    // Add user message
    const userMsg: Message = {
      id: Date.now().toString(),
      role: 'user',
      content,
      timestamp: Date.now(),
      attachmentName,
    }
    conversation.messages.push(userMsg)

    // Add placeholder AI message for streaming
    const aiMsgId = (Date.now() + 1).toString()
    const aiMsg: Message = {
      id: aiMsgId,
      role: 'assistant',
      content: '',
      timestamp: Date.now(),
      isStreaming: true,
    }
    conversation.messages.push(aiMsg)

    isGenerating.value = true

    // SSE streaming call
    const abortController = sendAIMessageStream(
      parseInt(currentConversationId.value),
      content,
      {
        onStart: (_data) => {
          // Stream started
        },
        onToken: (data) => {
          // Append token to AI message
          const msg = conversation.messages.find(m => m.id === aiMsgId)
          if (msg) {
            msg.content += data.delta
          }
        },
        onEnd: (data) => {
          // Stream completed
          const msg = conversation.messages.find(m => m.id === aiMsgId)
          if (msg) {
            msg.isStreaming = false
            if (data.fullContent) {
              msg.content = data.fullContent
            }
          }
          isGenerating.value = false
          currentAbortController.value = null
          conversation.updatedAt = Date.now()

          // Update conversation title if it's "新对话"
          if (conversation.title === '新对话' && content) {
            conversation.title = content.length > 20 ? content.substring(0, 20) + '...' : content
          }
        },
        onError: (message) => {
          const msg = conversation.messages.find(m => m.id === aiMsgId)
          if (msg) {
            msg.content = message || '抱歉，AI 服务暂时无法响应，请稍后重试。'
            msg.isError = true
            msg.isStreaming = false
          }
          isGenerating.value = false
          currentAbortController.value = null
        },
      }
    )

    currentAbortController.value = abortController
  }

  // ── 停止生成 ──────────────────────────────────────────────────
  function stopGenerating() {
    if (currentAbortController.value) {
      currentAbortController.value.abort()
      currentAbortController.value = null
    }
    isGenerating.value = false

    // Mark streaming message as complete
    const conversation = conversations.value.find(c => c.id === currentConversationId.value)
    if (conversation) {
      const streamingMsg = conversation.messages.find(m => m.isStreaming)
      if (streamingMsg) {
        streamingMsg.isStreaming = false
        if (!streamingMsg.content) {
          streamingMsg.content = '（已停止生成）'
        }
      }
    }
  }

  // ── 上传文件并作为上下文发送 ──────────────────────────────────
  async function uploadAndSendFile(file: File) {
    try {
      loading.value = true
      const res = await uploadFileForExtraction(file)
      if (res.code === 0 && res.data.text) {
        const prompt = `请帮我分析以下文件内容：\n\n📎 文件: ${res.data.fileName} (${res.data.fileType})\n\n---\n${res.data.text.substring(0, 8000)}\n---\n\n请进行分析和总结。`
        await sendMessage(prompt, res.data.fileName)
      } else {
        throw new Error('文件内容提取失败')
      }
    } catch (error: any) {
      console.error('文件上传失败', error.message)
      // Show error in chat
      const conversation = conversations.value.find(c => c.id === currentConversationId.value)
      if (conversation) {
        conversation.messages.push({
          id: Date.now().toString(),
          role: 'assistant',
          content: `文件上传失败: ${error.message}`,
          timestamp: Date.now(),
          isError: true,
        })
      }
    } finally {
      loading.value = false
    }
  }

  // ── 语音识别 ──────────────────────────────────────────────────
  async function transcribeVoice(audioBlob: Blob): Promise<string> {
    try {
      loading.value = true
      const res = await voiceToText(audioBlob)
      if (res.code === 0 && res.data.text) {
        return res.data.text
      }
      throw new Error('语音识别失败')
    } catch (error: any) {
      console.error('语音识别失败', error.message)
      throw error
    } finally {
      loading.value = false
    }
  }

  // ── 切换模型 ──────────────────────────────────────────────────
  async function switchModel(modelId: string) {
    currentModel.value = modelId

    // Update current session model in backend
    if (currentConversationId.value) {
      const conv = conversations.value.find(c => c.id === currentConversationId.value)
      if (conv) {
        conv.modelName = modelId
      }
      try {
        await updateSessionModel(parseInt(currentConversationId.value), modelId)
      } catch (error: any) {
        console.error('更新模型失败', error.message)
      }
    }
  }

  // ── 删除对话 ──────────────────────────────────────────────────
  async function deleteConversation(id: string) {
    try {
      await deleteAISession(parseInt(id))
      conversations.value = conversations.value.filter(c => c.id !== id)
      if (currentConversationId.value === id) {
        if (conversations.value.length > 0) {
          await selectConversation(conversations.value[0].id)
        } else {
          currentConversationId.value = null
          await createNewConversation()
        }
      }
    } catch (error: any) {
      console.error('删除失败', error.message)
    }
  }

  return {
    conversations,
    currentConversationId,
    isGenerating,
    currentModel,
    loading,
    initChat,
    createNewConversation,
    selectConversation,
    loadConversationMessages,
    sendMessage,
    stopGenerating,
    uploadAndSendFile,
    transcribeVoice,
    switchModel,
    deleteConversation,
  }
})

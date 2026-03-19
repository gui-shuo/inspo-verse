import { defineStore } from 'pinia'
import { ref } from 'vue'
import { createAISession, getAISessions, sendAIMessage, deleteAISession } from '@/api/ai'

export interface Message {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: number
  isError?: boolean
}

export interface Conversation {
  id: string
  title: string
  messages: Message[]
  updatedAt: number
}

export const useChatStore = defineStore('chat', () => {
  const conversations = ref<Conversation[]>([])
  const currentConversationId = ref<string | null>(null)
  const isGenerating = ref(false)
  const currentModel = ref('gpt-3.5-turbo')
  const loading = ref(false)

  // 初始化：加载真实会话列表
  async function initChat() {
    try {
      loading.value = true
      const res = await getAISessions()
      conversations.value = res.data.map(session => ({
        id: session.id.toString(),
        title: session.title,
        messages: [],
        updatedAt: new Date(session.updatedAt).getTime()
      }))

      // 如果没有会话，创建一个新的
      if (conversations.value.length === 0) {
        await createNewConversation()
      } else {
        currentConversationId.value = conversations.value[0].id
      }
    } catch (error: any) {
      console.error('加载会话失败', error.message)
    } finally {
      loading.value = false
    }
  }

  async function createNewConversation() {
    try {
      loading.value = true
      const res = await createAISession('general', currentModel.value)
      const newConv: Conversation = {
        id: res.data.sessionId.toString(),
        title: res.data.title,
        messages: [],
        updatedAt: Date.now()
      }
      conversations.value.unshift(newConv)
      currentConversationId.value = newConv.id
    } catch (error: any) {
      console.error('创建会话失败', error.message)
    } finally {
      loading.value = false
    }
  }

  // 发送消息
  async function sendMessage(content: string) {
    if (!currentConversationId.value) return

    const conversation = conversations.value.find(c => c.id === currentConversationId.value)
    if (!conversation) return

    // 添加用户消息
    conversation.messages.push({
      id: Date.now().toString(),
      role: 'user',
      content,
      timestamp: Date.now()
    })

    try {
      isGenerating.value = true

      // 调用真实 API
      const res = await sendAIMessage(parseInt(currentConversationId.value), content)

      // 添加 AI 回复
      conversation.messages.push({
        id: res.data.id.toString(),
        role: 'assistant',
        content: res.data.content,
        timestamp: new Date(res.data.createdAt).getTime()
      })

      conversation.updatedAt = Date.now()
    } catch (error: any) {
      console.error('发送消息失败', error.message)
      // 添加错误消息
      conversation.messages.push({
        id: Date.now().toString(),
        role: 'assistant',
        content: '抱歉，消息发送失败。请稍后重试。',
        timestamp: Date.now(),
        isError: true
      })
    } finally {
      isGenerating.value = false
    }
  }

  async function deleteConversation(id: string) {
    try {
      await deleteAISession(parseInt(id))
      conversations.value = conversations.value.filter(c => c.id !== id)
      if (currentConversationId.value === id) {
        currentConversationId.value = conversations.value[0]?.id || null
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
    sendMessage,
    deleteConversation
  }
})

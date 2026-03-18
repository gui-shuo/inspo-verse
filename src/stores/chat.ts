import { defineStore } from 'pinia'
import { ref } from 'vue'
import dayjs from 'dayjs'
import { streamAIChat } from '@/api/ai'

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
  const currentModel = ref('creative') // creative, precise, coding

  // 初始化默认会话
  function initChat() {
    if (conversations.value.length === 0) {
      // 预置一些"历史"对话
      conversations.value = [
        {
          id: 'mock-1',
          title: '赛博朋克游戏文案',
          messages: [
            { id: 'm1-1', role: 'user', content: '帮我写一段赛博朋克风格的游戏开场白，要有雨夜和霓虹灯的元素。', timestamp: Date.now() - 86400000 },
            { id: 'm1-2', role: 'assistant', content: '雨水像廉价的机油一样冲刷着夜之城的脊梁。霓虹灯管在积水中破碎成千万片光斑，映照出你义眼里的疲惫。\n\n"又是一个如果不插上电极就无法入睡的夜晚。" 你点燃了一支合成烟草，看着烟雾在全息广告牌的蓝光中盘旋上升。', timestamp: Date.now() - 86390000 },
          ],
          updatedAt: Date.now() - 86390000
        },
        {
          id: 'mock-2',
          title: 'Vue 3 组件通信疑问',
          messages: [
            { id: 'm2-1', role: 'user', content: 'Provide/Inject 和 Vuex/Pinia 有什么区别？什么时候用哪个？', timestamp: Date.now() - 172800000 },
            { id: 'm2-2', role: 'assistant', content: '这是一个很好的架构问题。\n\n### Provide/Inject\n* **适用场景**: 深度嵌套的组件树（如 UI 库开发），不需要全局共享，只需父子孙之间通信。\n* **优点**: 既然耦合，又不需要引入额外库。\n\n### Pinia\n* **适用场景**: 全局状态（用户信息、购物车、主题配置），跨路由组件通信。\n* **优点**: DevTools 支持好，TypeScript 类型推断完美，支持 SSR。\n\n**总结**: 写业务逻辑优先用 Pinia，写通用组件优先用 Provide/Inject。', timestamp: Date.now() - 172700000 },
          ],
          updatedAt: Date.now() - 172700000
        }
      ]
      createNewConversation() // Add a fresh one on top
    }
  }

  function createNewConversation() {
    const id = Date.now().toString()
    const newConv: Conversation = {
      id,
      title: '新对话 ' + dayjs().format('HH:mm'),
      messages: [
        {
          id: 'welcome',
          role: 'assistant',
          content: getWelcomeMessage(currentModel.value),
          timestamp: Date.now()
        }
      ],
      updatedAt: Date.now()
    }
    conversations.value.unshift(newConv)
    currentConversationId.value = id
  }

  function getWelcomeMessage(model: string) {
    if (model === 'coding') return '我是 Inspo 代码助手。请粘贴您的代码或描述遇到的 Bug，我将为您提供精确的调试建议。'
    if (model === 'precise') return '我是精确模式助手。我将基于事实提供简明扼要的回答，不进行发散性创作。'
    return '你好！我是 Inspo 创意助手。我可以帮你生成游戏攻略、润色文章或提供灵感。请问有什么可以帮你的？'
  }

  // 模拟流式发送消息
  async function sendMessage(content: string) {
    if (!currentConversationId.value) return

    const conversation = conversations.value.find(c => c.id === currentConversationId.value)
    if (!conversation) return

    // 1. 添加用户消息
    conversation.messages.push({
      id: Date.now().toString(),
      role: 'user',
      content,
      timestamp: Date.now()
    })

    // 2. 准备 AI 响应容器
    isGenerating.value = true
    const aiMsgId = (Date.now() + 1).toString()
    conversation.messages.push({
      id: aiMsgId,
      role: 'assistant',
      content: '', // Start empty
      timestamp: Date.now()
    })

    const aiMsgIndex = conversation.messages.length - 1

    const fallback = async () => {
      const responseText = generateMockResponse(content, currentModel.value)
      for (const char of responseText.split('')) {
        await new Promise(r => setTimeout(r, 20 + Math.random() * 30))
        conversation.messages[aiMsgIndex].content += char
      }
      isGenerating.value = false
      conversation.updatedAt = Date.now()
    }

    // 3. 优先调用真实流式接口，失败时降级到本地模拟
    try {
      let streamFailed = false
      let streamErrorMessage = ''
      await streamAIChat(
        {
          sessionId: conversation.id,
          model: currentModel.value,
          messages: conversation.messages.map((message) => ({
            role: message.role,
            content: message.content
          }))
        },
        {
          onToken: (delta: string) => {
            conversation.messages[aiMsgIndex].content += delta
          },
          onError: (error: Error) => {
            streamFailed = true
            streamErrorMessage = error.message
          },
          onDone: () => {
            isGenerating.value = false
            conversation.updatedAt = Date.now()
          }
        }
      )
      if (streamFailed) {
        console.warn(`Falling back to mock response for session ${conversation.id} due to streaming error: ${streamErrorMessage || 'unknown error'}`)
        await fallback()
        return
      }
      // Some gateways may close chunked responses without emitting an explicit `end` event.
      if (isGenerating.value) {
        isGenerating.value = false
        conversation.updatedAt = Date.now()
      }
    } catch (error) {
      console.warn('AI stream request error, fallback to mock response', error)
      await fallback()
    }
  }

  function generateMockResponse(input: string, model: string) {
    if (model === 'coding') {
      return `\`\`\`typescript\n// 基于 "${input}" 的代码实现\nfunction solution() {\n  console.log("Optimization complete.");\n  return true;\n}\n\`\`\`\n\n建议检查边界条件处理。`
    }
    if (model === 'precise') {
      return `关于 **${input}**：\n\n1. 核心定义：这是一个...概念。\n2. 关键数据：增长率约为 15%。\n3. 结论：建议采取保守策略。`
    }
    // Creative (Default)
    const responses = [
      `关于 **${input}**，这是一个非常有趣的话题！\n\n根据我的数据库：\n1. *这一点很重要*\n2. *那一点也很有趣*\n\n希望这些灵感能帮到你！`,
      `收到！正在为您生成关于 "${input}" 的创意方案...\n\n> 灵感就像海绵里的水，只要愿挤，总还是有的。\n\n建议您可以尝试：\n- 结合赛博朋克风格\n- 增加互动性叙事`,
    ]
    return responses[Math.floor(Math.random() * responses.length)]
  }

  function deleteConversation(id: string) {
    conversations.value = conversations.value.filter(c => c.id !== id)
    if (currentConversationId.value === id) {
      currentConversationId.value = conversations.value[0]?.id || null
    }
  }

  return { 
    conversations, 
    currentConversationId, 
    isGenerating,
    currentModel,
    initChat,
    createNewConversation, 
    sendMessage,
    deleteConversation
  }
})

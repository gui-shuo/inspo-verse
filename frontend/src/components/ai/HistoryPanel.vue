<script setup lang="ts">
import { useChatStore } from '@/stores/chat'
import { useModalStore } from '@/stores/modal'
import { Plus, MessageSquare, Trash2, MoreHorizontal } from 'lucide-vue-next'
import { storeToRefs } from 'pinia'

const chatStore = useChatStore()
const modalStore = useModalStore()
const { conversations, currentConversationId } = storeToRefs(chatStore)

const handleNewChat = () => {
  chatStore.createNewConversation()
}

const selectChat = (id: string) => {
  currentConversationId.value = id
}

const deleteChat = async (e: Event, id: string) => {
  e.stopPropagation()
  const confirmed = await modalStore.open({
    title: '删除对话',
    content: '确定要删除这条对话记录吗？此操作无法撤销。',
    type: 'warning',
    confirmText: '确认删除'
  })
  
  if (confirmed) {
    chatStore.deleteConversation(id)
  }
}
</script>

<template>
  <div class="flex flex-col h-full py-4">
    <!-- New Chat Button -->
    <div class="px-4 mb-4">
      <button 
        @click="handleNewChat"
        class="w-full flex items-center gap-3 px-4 py-3 bg-slate-700/50 hover:bg-slate-700 border border-white/5 rounded-xl transition-all group"
      >
        <div class="p-1 bg-neon-blue/20 rounded text-neon-blue group-hover:bg-neon-blue group-hover:text-slate-900 transition-colors">
          <Plus class="w-4 h-4" />
        </div>
        <span class="text-sm font-medium text-gray-200">新对话</span>
      </button>
    </div>

    <!-- History List -->
    <div class="flex-1 overflow-y-auto px-2 space-y-1 custom-scrollbar">
      <div v-if="conversations.length === 0" class="text-center text-gray-500 text-xs mt-10">
        暂无历史记录
      </div>

      <div 
        v-for="chat in conversations" 
        :key="chat.id"
        @click="selectChat(chat.id)"
        class="group relative flex items-center gap-3 px-3 py-3 rounded-lg cursor-pointer transition-colors"
        :class="currentConversationId === chat.id ? 'bg-slate-700 text-white' : 'text-gray-400 hover:bg-slate-700/30 hover:text-gray-200'"
      >
        <MessageSquare class="w-4 h-4 flex-shrink-0" />
        <span class="text-sm truncate pr-6">{{ chat.title }}</span>
        
        <!-- Delete Action (Visible on hover or active) -->
        <button 
          @click="(e) => deleteChat(e, chat.id)"
          class="absolute right-2 p-1 rounded hover:bg-red-500/20 hover:text-red-400 opacity-0 group-hover:opacity-100 transition-opacity"
          title="删除"
        >
          <Trash2 class="w-3 h-3" />
        </button>
      </div>
    </div>
    
    <!-- User Info (Bottom) -->
    <div class="mt-auto px-4 pt-4 border-t border-white/5">
      <div class="flex items-center gap-3">
        <div class="w-8 h-8 rounded-full bg-gradient-to-tr from-neon-blue to-purple-500"></div>
        <div class="flex-1 overflow-hidden">
          <p class="text-sm font-medium text-white truncate">User Name</p>
          <p class="text-xs text-neon-yellow truncate">黄金会员</p>
        </div>
        <button class="text-gray-400 hover:text-white">
          <MoreHorizontal class="w-4 h-4" />
        </button>
      </div>
    </div>
  </div>
</template>

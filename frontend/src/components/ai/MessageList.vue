<script setup lang="ts">
import { computed, ref, nextTick, watch } from 'vue'
import { useChatStore } from '@/stores/chat'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { Bot, User, Paperclip, AlertCircle } from 'lucide-vue-next'

const chatStore = useChatStore()
const scrollRef = ref<HTMLElement | null>(null)

const currentMessages = computed(() => {
  const current = chatStore.conversations.find(c => c.id === chatStore.currentConversationId)
  return current ? current.messages : []
})

// Markdown Parser
const renderMarkdown = (content: string) => {
  if (!content) return ''
  const rawHtml = marked.parse(content) as string
  return DOMPurify.sanitize(rawHtml)
}

// Auto scroll on new messages and streaming content
watch(() => currentMessages.value.length, () => {
  scrollToBottom()
}, { flush: 'post' })

watch(() => currentMessages.value[currentMessages.value.length - 1]?.content, () => {
  scrollToBottom()
}, { flush: 'post' })

const scrollToBottom = async () => {
  await nextTick()
  if (scrollRef.value) {
    scrollRef.value.scrollTop = scrollRef.value.scrollHeight
  }
}
</script>

<template>
  <div ref="scrollRef" class="h-full overflow-y-auto px-4 py-4 md:px-8 space-y-6 scroll-smooth custom-scrollbar">
    <div v-if="currentMessages.length === 0" class="flex flex-col items-center justify-center h-full text-gray-500">
      <Bot class="w-16 h-16 mb-4 text-gray-600" />
      <p>开始一个新的对话吧...</p>
    </div>

    <div
      v-for="msg in currentMessages"
      :key="msg.id"
      class="flex gap-4 max-w-4xl mx-auto animate__animated animate__fadeIn"
      :class="msg.role === 'user' ? 'flex-row-reverse' : ''"
    >
      <!-- Avatar -->
      <div
        class="flex-shrink-0 w-10 h-10 rounded-full flex items-center justify-center"
        :class="msg.role === 'user' ? 'bg-neon-blue text-slate-900' : 'bg-neon-purple text-white'"
      >
        <component :is="msg.role === 'user' ? User : Bot" class="w-6 h-6" />
      </div>

      <!-- Content Bubble -->
      <div
        class="flex flex-col max-w-[85%]"
        :class="msg.role === 'user' ? 'items-end' : 'items-start'"
      >
        <!-- Attachment indicator for user messages -->
        <div v-if="msg.attachmentName" class="flex items-center gap-1 text-xs text-neon-blue mb-1">
          <Paperclip class="w-3 h-3" />
          <span>{{ msg.attachmentName }}</span>
        </div>

        <div
          class="px-5 py-3.5 rounded-2xl shadow-md text-sm md:text-base leading-relaxed break-words"
          :class="[
            msg.role === 'user'
              ? 'bg-neon-blue/10 border border-neon-blue/20 text-gray-100 rounded-tr-sm'
              : msg.isError
                ? 'bg-red-900/20 border border-red-500/20 text-red-300 rounded-tl-sm'
                : 'bg-slate-800 border border-white/5 text-gray-200 rounded-tl-sm'
          ]"
        >
          <!-- Error indicator -->
          <div v-if="msg.isError" class="flex items-center gap-2 mb-2 text-red-400 text-xs">
            <AlertCircle class="w-4 h-4" />
            <span>响应异常</span>
          </div>

          <!-- Markdown Content for assistant -->
          <div v-if="msg.role === 'assistant' && msg.content" class="markdown-body" v-html="renderMarkdown(msg.content)"></div>

          <!-- Streaming cursor -->
          <div v-else-if="msg.role === 'assistant' && msg.isStreaming && !msg.content" class="flex items-center gap-1">
            <span class="w-2 h-2 bg-neon-blue rounded-full animate-bounce" style="animation-delay: 0ms;"></span>
            <span class="w-2 h-2 bg-neon-blue rounded-full animate-bounce" style="animation-delay: 150ms;"></span>
            <span class="w-2 h-2 bg-neon-blue rounded-full animate-bounce" style="animation-delay: 300ms;"></span>
          </div>

          <!-- User message (plain text) -->
          <div v-else>{{ msg.content }}</div>

          <!-- Streaming indicator -->
          <span v-if="msg.isStreaming && msg.content" class="inline-block w-1.5 h-4 bg-neon-blue/80 animate-pulse ml-0.5 align-text-bottom"></span>
        </div>

        <!-- Timestamp -->
        <span class="text-xs text-gray-500 mt-1 px-1">
          {{ new Date(msg.timestamp).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'}) }}
        </span>
      </div>
    </div>
  </div>
</template>

<style>
/* Markdown Styles override for dark mode */
.markdown-body {
  @apply text-gray-200;
}
.markdown-body p {
  @apply mb-2 last:mb-0;
}
.markdown-body pre {
  @apply bg-slate-900 p-3 rounded-lg my-2 overflow-x-auto border border-white/10;
}
.markdown-body code {
  @apply font-mono text-xs bg-black/30 px-1 py-0.5 rounded text-neon-pink;
}
.markdown-body pre code {
  @apply bg-transparent p-0 text-gray-300;
}
.markdown-body ul {
  @apply list-disc list-inside mb-2;
}
.markdown-body ol {
  @apply list-decimal list-inside mb-2;
}
.markdown-body h1, .markdown-body h2, .markdown-body h3 {
  @apply font-bold text-white mt-4 mb-2;
}
.markdown-body a {
  @apply text-neon-blue hover:underline;
}
.markdown-body blockquote {
  @apply border-l-4 border-neon-purple pl-4 italic text-gray-400 my-2;
}

/* Custom Scrollbar for this container */
.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 3px;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.2);
}
</style>

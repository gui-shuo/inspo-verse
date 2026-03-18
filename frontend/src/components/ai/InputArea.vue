<script setup lang="ts">
import { ref } from 'vue'
import { useChatStore } from '@/stores/chat'
import { Send, Paperclip, Mic, Sparkles, Code, CheckCircle2 } from 'lucide-vue-next'

const chatStore = useChatStore()
const inputContent = ref('')

const models = [
  { id: 'creative', name: '创意助手', icon: Sparkles },
  { id: 'coding', name: '代码大师', icon: Code },
  { id: 'precise', name: '严谨模式', icon: CheckCircle2 },
]

const handleSend = () => {
  if (!inputContent.value.trim() || chatStore.isGenerating) return
  
  chatStore.sendMessage(inputContent.value)
  inputContent.value = ''
}

const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

const handleFileUpload = () => {
  alert('仅支持 TXT/PDF 文件上传 (模拟功能)')
}
</script>

<template>
  <div class="space-y-3">
    <!-- Model Selector (New Feature) -->
    <div class="flex gap-2 justify-center md:justify-start">
      <button 
        v-for="model in models" 
        :key="model.id"
        @click="chatStore.currentModel = model.id"
        class="flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-medium border transition-all"
        :class="chatStore.currentModel === model.id 
          ? 'bg-neon-blue/20 text-neon-blue border-neon-blue/50' 
          : 'bg-slate-800 text-gray-400 border-white/5 hover:bg-slate-700'"
      >
        <component :is="model.icon" class="w-3 h-3" />
        {{ model.name }}
      </button>
    </div>

    <!-- Input Box -->
    <div class="relative bg-slate-800 border border-white/10 rounded-xl shadow-lg transition-all focus-within:border-neon-blue/50 focus-within:ring-1 focus-within:ring-neon-blue/20">
      <textarea 
        v-model="inputContent"
        @keydown="handleKeydown"
        placeholder="输入您的问题，探索无限灵感... (Enter 发送)"
        class="w-full bg-transparent text-white px-4 py-3 min-h-[60px] max-h-[200px] resize-none focus:outline-none scrollbar-hide text-sm md:text-base"
        rows="2"
      ></textarea>

      <div class="flex justify-between items-center px-2 pb-2">
        <!-- Tools -->
        <div class="flex gap-1">
          <button 
            @click="handleFileUpload"
            class="p-2 text-gray-400 hover:text-white hover:bg-white/10 rounded-lg transition-colors"
            title="上传文件"
          >
            <Paperclip class="w-5 h-5" />
          </button>
          <button 
            class="p-2 text-gray-400 hover:text-white hover:bg-white/10 rounded-lg transition-colors"
            title="语音输入"
          >
            <Mic class="w-5 h-5" />
          </button>
        </div>

        <!-- Send Button -->
        <button 
          @click="handleSend"
          :disabled="!inputContent.trim() || chatStore.isGenerating"
          class="p-2 bg-neon-blue text-slate-900 rounded-lg disabled:opacity-50 disabled:cursor-not-allowed hover:bg-neon-blue/90 transition-all transform active:scale-95"
        >
          <Send class="w-5 h-5" />
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.scrollbar-hide::-webkit-scrollbar {
    display: none;
}
</style>

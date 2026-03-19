<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useChatStore } from '@/stores/chat'
import MessageList from '@/components/ai/MessageList.vue'
import InputArea from '@/components/ai/InputArea.vue'
import HistoryPanel from '@/components/ai/HistoryPanel.vue'
import { Menu, X, Sparkles, Target, Terminal, ChevronDown } from 'lucide-vue-next'

const chatStore = useChatStore()
const showSidebar = ref(true)
const showModelDropdown = ref(false)

const models = [
  { id: 'creative', name: '灵感创作 V4', icon: Sparkles, desc: '发散思维，适合写作与创意' },
  { id: 'precise', name: '精确问答 Pro', icon: Target, desc: '基于事实，逻辑严密' },
  { id: 'coding', name: '代码编程 Max', icon: Terminal, desc: '代码生成、调试与重构' },
]

const currentModelInfo = computed(() => 
  models.find(m => m.id === chatStore.currentModel) || models[0]
)

const selectModel = (id: string) => {
  chatStore.switchModel(id)
  showModelDropdown.value = false
}

onMounted(() => {
  chatStore.initChat()
})
</script>

<template>
  <div class="flex h-[calc(100vh-80px)] overflow-hidden bg-slate-900">
    <!-- Mobile Sidebar Toggle -->
    <div class="md:hidden fixed z-30 bottom-4 right-4">
      <button @click="showSidebar = !showSidebar" class="p-3 bg-neon-blue rounded-full shadow-lg text-slate-900">
        <component :is="showSidebar ? X : Menu" class="w-6 h-6" />
      </button>
    </div>

    <!-- History Panel (Sidebar) -->
    <aside 
      class="fixed md:relative z-20 w-64 h-full bg-slate-800/50 backdrop-blur border-r border-white/5 transition-transform duration-300 transform md:translate-x-0"
      :class="showSidebar ? 'translate-x-0' : '-translate-x-full'"
    >
      <HistoryPanel />
    </aside>

    <!-- Chat Area -->
    <main class="flex-1 flex flex-col relative w-full h-full bg-gradient-to-b from-slate-900 to-slate-800">
      
      <!-- Model Selector Header -->
      <header class="absolute top-0 left-0 right-0 z-10 p-4 flex justify-center pointer-events-none">
        <div class="relative pointer-events-auto">
          <button 
            @click="showModelDropdown = !showModelDropdown"
            class="flex items-center gap-2 px-4 py-2 rounded-full bg-slate-800/80 backdrop-blur border border-white/10 hover:border-neon-blue/50 transition-all shadow-lg group"
          >
            <component :is="currentModelInfo.icon" class="w-4 h-4 text-neon-blue" />
            <span class="text-sm font-bold text-white">{{ currentModelInfo.name }}</span>
            <ChevronDown 
              class="w-4 h-4 text-gray-500 transition-transform group-hover:text-white" 
              :class="showModelDropdown ? 'rotate-180' : ''"
            />
          </button>

          <!-- Dropdown -->
          <transition
            enter-active-class="transition duration-200 ease-out"
            enter-from-class="opacity-0 translate-y-2"
            enter-to-class="opacity-100 translate-y-0"
            leave-active-class="transition duration-150 ease-in"
            leave-from-class="opacity-100 translate-y-0"
            leave-to-class="opacity-0 translate-y-2"
          >
            <div 
              v-if="showModelDropdown"
              class="absolute top-full left-1/2 -translate-x-1/2 mt-2 w-72 bg-slate-900/95 backdrop-blur border border-white/10 rounded-xl shadow-2xl p-2 overflow-hidden"
            >
              <div class="px-3 py-2 text-xs text-gray-500 font-bold tracking-wider uppercase">Select Model</div>
              <button 
                v-for="model in models"
                :key="model.id"
                @click="selectModel(model.id)"
                class="w-full text-left p-3 rounded-lg flex items-start gap-3 transition-colors relative group"
                :class="chatStore.currentModel === model.id ? 'bg-white/5' : 'hover:bg-white/5'"
              >
                <div 
                  class="p-2 rounded-lg"
                  :class="chatStore.currentModel === model.id ? 'bg-neon-blue/20 text-neon-blue' : 'bg-slate-800 text-gray-400 group-hover:text-white'"
                >
                  <component :is="model.icon" class="w-5 h-5" />
                </div>
                <div>
                  <div class="font-bold text-sm text-white flex items-center gap-2">
                    {{ model.name }}
                    <span v-if="chatStore.currentModel === model.id" class="w-1.5 h-1.5 rounded-full bg-neon-blue"></span>
                  </div>
                  <div class="text-xs text-gray-400 mt-0.5">{{ model.desc }}</div>
                </div>
              </button>
            </div>
          </transition>
          
          <!-- Click Outside Overlay -->
          <div v-if="showModelDropdown" @click="showModelDropdown = false" class="fixed inset-0 -z-10"></div>
        </div>
      </header>

      <!-- Messages -->
      <div class="flex-1 overflow-hidden relative pt-16">
        <MessageList />
      </div>

      <!-- Input -->
      <div class="p-4 md:p-6 pb-8">
        <div class="max-w-4xl mx-auto">
          <InputArea />
          <p class="text-xs text-gray-500 text-center mt-2">
            AI 生成内容仅供参考，请核实重要信息。
          </p>
        </div>
      </div>
    </main>
  </div>
</template>

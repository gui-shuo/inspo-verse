<script setup lang="ts">
import { Terminal } from 'lucide-vue-next'

const rules = [
  { id: '0x01', title: '尊重与包容', content: '禁止任何形式的歧视、骚扰或仇恨言论。社区是多元化的避风港。' },
  { id: '0x02', title: '原创与版权', content: '尊重知识产权。转载请注明出处，禁止发布盗版资源。' },
  { id: '0x03', title: 'AI 使用规范', content: '使用 AI 生成的内容必须打上 #AI 标签。禁止生成暴力、色情或误导性内容。' },
  { id: '0x04', title: '友善交流', content: '理性讨论，拒绝引战。遇到分歧请使用决斗...哦不，请使用举报功能。' },
]
</script>

<template>
  <div class="container mx-auto px-4 py-20 min-h-screen flex items-center justify-center relative">
    <!-- Background Image -->
    <div class="absolute inset-0 z-0 opacity-20 bg-[url('https://images.unsplash.com/photo-1550751827-4bd374c3f58b?w=1600&q=80')] bg-cover bg-center mix-blend-overlay"></div>
    
    <div class="w-full max-w-4xl bg-black/80 backdrop-blur-xl border border-green-500/30 rounded-lg shadow-[0_0_50px_rgba(34,197,94,0.15)] font-mono overflow-hidden relative z-10">
      <!-- Terminal Header -->
      <div class="bg-gray-900 px-4 py-2 flex items-center gap-2 border-b border-green-500/20">
        <div class="w-3 h-3 rounded-full bg-red-500"></div>
        <div class="w-3 h-3 rounded-full bg-yellow-500"></div>
        <div class="w-3 h-3 rounded-full bg-green-500"></div>
        <div class="ml-4 text-xs text-green-500 opacity-70 flex items-center gap-2">
          <Terminal class="w-3 h-3" /> system@inspo-verse:~/rules
        </div>
      </div>

      <!-- Terminal Body -->
      <div class="p-8 text-green-500 space-y-6 relative">
        <!-- Holographic Icon -->
        <div class="absolute top-4 right-8 opacity-20 pointer-events-none">
           <img src="https://api.iconify.design/game-icons:locked-fortress.svg?color=%2322c55e" class="w-32 h-32" />
        </div>

        <div class="mb-8">
          <p class="animate-typing overflow-hidden whitespace-nowrap border-r-2 border-green-500 pr-1">
            > cat community_guidelines.txt
          </p>
        </div>

        <div v-for="rule in rules" :key="rule.id" class="border-l-2 border-green-500/20 pl-4 hover:border-green-500 transition-colors">
          <h3 class="font-bold mb-1 opacity-90">[{{ rule.id }}] {{ rule.title }}</h3>
          <p class="opacity-70 text-sm leading-relaxed">{{ rule.content }}</p>
        </div>

        <div class="mt-8 pt-4 border-t border-green-500/20 opacity-50 text-xs">
          > Last login: {{ new Date().toDateString() }} from 127.0.0.1
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.animate-typing {
  animation: typing 2s steps(30, end), blink-caret .75s step-end infinite;
}

@keyframes typing {
  from { width: 0 }
  to { width: 100% }
}

@keyframes blink-caret {
  from, to { border-color: transparent }
  50% { border-color: #22c55e; }
}
</style>

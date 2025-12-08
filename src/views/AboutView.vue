<script setup lang="ts">
import GlitchText from '@/components/ui/GlitchText.vue'
</script>

<template>
  <div class="min-h-screen">
    <!-- Hero -->
    <div class="h-[80vh] flex flex-col items-center justify-center text-center px-4 relative overflow-hidden">
      <div class="absolute inset-0 bg-[url('https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=1600&q=80')] bg-cover bg-center opacity-20"></div>
      <div class="absolute inset-0 bg-gradient-to-b from-transparent to-slate-900"></div>
      
      <h1 class="text-6xl md:text-9xl font-black mb-8 relative z-10"><GlitchText text="ABOUT US" /></h1>
      <p class="text-xl md:text-3xl text-gray-300 max-w-3xl leading-relaxed relative z-10">
        我们是数字荒原上的<span class="text-neon-blue font-bold">造梦者</span>。<br>
        致力于通过 AI 技术，打破创意与实现的壁垒。
      </p>
    </div>

    <!-- Content -->
    <div class="container mx-auto px-4 py-20">
      <div class="grid grid-cols-1 md:grid-cols-2 gap-20 items-center mb-32">
        <div class="space-y-6">
          <h2 class="text-4xl font-bold text-white">我们的愿景</h2>
          <div class="w-20 h-1 bg-neon-purple"></div>
          <p class="text-gray-400 text-lg leading-loose">
            在 Inspo-Verse，我们相信每个人都是潜在的艺术家。无论你是游戏开发者、小说家还是插画师，我们的 AI 工具链都将成为你最得力的助手。我们不创造创意，我们只是灵感的<strong class="text-white">加速器</strong>。
          </p>
        </div>
        <div class="relative">
          <div class="absolute inset-0 bg-neon-purple blur-[100px] opacity-20"></div>
          <img src="https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=800&q=80" class="relative rounded-2xl border border-white/10 shadow-2xl transform rotate-3 hover:rotate-0 transition-transform duration-500" />
        </div>
      </div>

      <div class="text-center relative">
        <!-- Team Background -->
        <div class="absolute inset-0 bg-gradient-to-r from-neon-blue/5 to-neon-purple/5 blur-3xl rounded-full pointer-events-none"></div>
        
        <h2 class="text-4xl font-bold text-white mb-16 relative z-10">核心团队</h2>
        <div class="grid grid-cols-1 md:grid-cols-3 gap-8 relative z-10">
          <div v-for="i in 3" :key="i" class="p-8 bg-slate-800/50 rounded-2xl border border-white/5 hover:border-neon-blue/30 transition-all hover:-translate-y-2 group">
            <div class="w-24 h-24 mx-auto bg-slate-700 rounded-full mb-6 overflow-hidden border-2 border-white/10 group-hover:border-neon-blue group-hover:shadow-[0_0_20px_rgba(0,243,255,0.3)] transition-all">
              <img :src="`https://api.dicebear.com/7.x/avataaars/svg?seed=Team${i}`" class="w-full h-full object-cover" />
            </div>
            <h3 class="text-xl font-bold text-white group-hover:text-neon-blue transition-colors">Developer {{ i }}</h3>
            <p class="text-neon-blue text-sm">Full Stack Engineer</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

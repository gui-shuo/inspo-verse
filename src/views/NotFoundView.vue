<script setup lang="ts">
import { RouterLink } from 'vue-router'
import GlitchText from '@/components/ui/GlitchText.vue'
</script>

<template>
  <div class="h-[calc(100vh-80px)] flex flex-col items-center justify-center text-center p-4">
    <div class="text-[120px] font-black leading-none mb-4 text-white opacity-20 select-none">
      404
    </div>
    
    <h1 class="text-4xl md:text-6xl font-bold mb-6">
      <GlitchText text="LOST IN VOID" />
    </h1>
    
    <p class="text-xl text-gray-400 mb-12 max-w-md">
      您寻找的页面似乎已迷失在数据流的缝隙中。
    </p>

    <RouterLink 
      to="/" 
      class="px-8 py-3 bg-neon-blue text-slate-900 font-bold rounded-full hover:shadow-[0_0_20px_rgba(0,243,255,0.5)] transition-all"
    >
      返回安全地带
    </RouterLink>
  </div>
</template>

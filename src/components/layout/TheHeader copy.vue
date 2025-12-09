<script setup lang="ts">
import { computed } from 'vue'
import { useWindowScroll } from '@vueuse/core'
import { RouterLink } from 'vue-router'

const { y } = useWindowScroll()

// 核心考核点：导航栏滚动时的样式变化
// 超过 50px 滚动时，背景变深，增加模糊效果
const headerClass = computed(() => {
  return y.value > 50 
    ? 'bg-slate-900/80 backdrop-blur-md shadow-lg border-b border-white/10 py-3' 
    : 'bg-transparent py-5'
})
</script>

<template>
  <header class="fixed top-0 left-0 w-full z-50 transition-all duration-300 ease-in-out" :class="headerClass">
    <div class="container mx-auto px-4 flex justify-between items-center">
      <!-- Logo Area -->
      <RouterLink to="/" class="flex items-center gap-2 group">
        <div class="w-10 h-10 rounded-lg bg-gradient-to-br from-neon-blue to-neon-purple flex items-center justify-center text-white font-bold text-xl group-hover:rotate-12 transition-transform">
          I
        </div>
        <span class="text-2xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-white to-gray-400">
          Inspo-Verse
        </span>
      </RouterLink>

      <!-- Navigation -->
      <nav class="hidden md:flex items-center gap-8">
        <RouterLink to="/" class="text-gray-300 hover:text-neon-blue transition-colors font-medium relative after:content-[''] after:absolute after:bottom-[-4px] after:left-0 after:w-0 after:h-[2px] after:bg-neon-blue after:transition-all hover:after:w-full">
          首页
        </RouterLink>
        <RouterLink to="/explore" class="text-gray-300 hover:text-neon-blue transition-colors font-medium">
          发现
        </RouterLink>
        <RouterLink to="/forum" class="text-gray-300 hover:text-neon-blue transition-colors font-medium">
          论坛
        </RouterLink>
        <RouterLink to="/ai-chat" class="text-gray-300 hover:text-neon-blue transition-colors font-medium">
          AI 助手
        </RouterLink>
        <RouterLink to="/vip" class="text-gray-300 hover:text-neon-purple transition-colors font-medium">
          会员中心
        </RouterLink>
      </nav>

      <!-- User Actions -->
      <div class="flex items-center gap-4">
        <RouterLink to="/login" class="px-5 py-2 rounded-full border border-white/20 hover:bg-white/10 hover:border-white/40 transition-all text-sm font-medium">
          登录
        </RouterLink>
        <button class="px-5 py-2 rounded-full bg-neon-blue/10 text-neon-blue hover:bg-neon-blue/20 transition-all text-sm font-medium">
          注册
        </button>
      </div>
    </div>
  </header>
</template>

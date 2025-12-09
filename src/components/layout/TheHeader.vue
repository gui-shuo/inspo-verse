<script setup lang="ts">
import { computed } from 'vue'
import { useWindowScroll } from '@vueuse/core'
import { RouterLink } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import { User, LogOut, ChevronDown } from 'lucide-vue-next'

const { y } = useWindowScroll()
const authStore = useAuthStore()
// Use storeToRefs to maintain reactivity when destructuring
const { isAuthenticated, user } = storeToRefs(authStore)

const headerClass = computed(() => {
  return y.value > 50 
    ? 'bg-slate-900/80 backdrop-blur-md shadow-lg border-b border-white/10 py-3' 
    : 'bg-transparent py-5'
})

const handleLogout = () => {
  authStore.logout()
}
</script>

<template>
  <header class="fixed top-0 left-0 w-full z-50 transition-all duration-300 ease-in-out" :class="headerClass">
    <div class="container mx-auto px-4 flex justify-between items-center">
      <RouterLink to="/" class="flex items-center gap-2 group">
        <div class="w-10 h-10 rounded-lg bg-gradient-to-br from-neon-blue to-neon-purple flex items-center justify-center text-white font-bold text-xl group-hover:rotate-12 transition-transform">
          I
        </div>
        <span class="text-2xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-white to-gray-400">
          Inspo-Verse
        </span>
      </RouterLink>

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

      <div class="flex items-center gap-4">
        <template v-if="!isAuthenticated">
          <RouterLink to="/login" class="px-5 py-2 rounded-full border border-white/20 hover:bg-white/10 hover:border-white/40 transition-all text-sm font-medium">
            登录
          </RouterLink>
          <RouterLink to="/login" class="px-5 py-2 rounded-full bg-neon-blue/10 text-neon-blue hover:bg-neon-blue/20 transition-all text-sm font-medium">
            注册
          </RouterLink>
        </template>

        <div v-else class="relative group">
          <button class="flex items-center gap-2 outline-none py-1">
            <div class="w-8 h-8 rounded-full bg-gradient-to-tr from-neon-blue to-neon-purple p-[1px]">
              <img :src="user?.avatar" class="w-full h-full rounded-full object-cover bg-slate-900" alt="Avatar" />
            </div>
            <span class="text-sm font-medium text-white max-w-[100px] truncate hidden sm:block">{{ user?.nickname }}</span>
            <ChevronDown class="w-4 h-4 text-gray-400 group-hover:rotate-180 transition-transform" />
          </button>

          <div class="absolute right-0 top-full mt-2 w-48 bg-slate-800 border border-white/10 rounded-xl shadow-xl overflow-hidden opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all transform origin-top-right scale-95 group-hover:scale-100 z-50">
            <div class="p-2 space-y-1">
              <RouterLink to="/user" class="flex items-center gap-2 px-3 py-2 text-sm text-gray-300 hover:bg-white/5 hover:text-white rounded-lg transition-colors">
                <User class="w-4 h-4" /> 个人中心
              </RouterLink>
              <button @click="handleLogout" class="w-full flex items-center gap-2 px-3 py-2 text-sm text-red-400 hover:bg-red-500/10 rounded-lg transition-colors text-left">
                <LogOut class="w-4 h-4" /> 退出登录
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </header>
</template>
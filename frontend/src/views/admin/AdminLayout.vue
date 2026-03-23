<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { RouterView, RouterLink, useRoute, useRouter } from 'vue-router'
import { LayoutDashboard, Users, ShoppingBag, Settings, LogOut, Menu, ChevronRight, MessageSquare, Image, Bot, Film, Gamepad2, Wrench, Crown } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { checkAdminAccess } from '@/api/admin'

const isCollapsed = ref(false)
const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const loading = ref(true)

const menuItems = [
  { name: '仪表盘', icon: LayoutDashboard, path: '/admin' },
  { name: '用户管理', icon: Users, path: '/admin/users' },
  { name: '帖子管理', icon: MessageSquare, path: '/admin/posts' },
  { name: '内容管理', icon: Image, path: '/admin/explore' },
  { name: 'AI 监控', icon: Bot, path: '/admin/ai-monitor' },
  { name: '订单管理', icon: ShoppingBag, path: '/admin/orders' },
  { name: '番剧管理', icon: Film, path: '/admin/anime' },
  { name: '游戏管理', icon: Gamepad2, path: '/admin/games' },
  { name: '工坊管理', icon: Wrench, path: '/admin/workshop' },
  { name: 'VIP管理', icon: Crown, path: '/admin/vip' },
  { name: '系统设置', icon: Settings, path: '/admin/settings' },
]

const handleLogout = () => {
  authStore.logout()
}

onMounted(async () => {
  try {
    const result = await checkAdminAccess()
    if (!result.isAdmin) {
      router.replace('/')
      return
    }
  } catch {
    router.replace('/login')
    return
  }
  loading.value = false
})
</script>

<template>
  <div v-if="loading" class="flex h-screen bg-slate-900 items-center justify-center">
    <div class="text-center">
      <div class="w-12 h-12 border-4 border-neon-blue border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
      <p class="text-gray-400">验证管理员权限...</p>
    </div>
  </div>
  <div v-else class="flex h-screen bg-slate-900 text-white overflow-hidden">
    <!-- Sidebar -->
    <aside 
      class="bg-slate-800 border-r border-white/5 transition-all duration-300 flex flex-col"
      :class="isCollapsed ? 'w-20' : 'w-64'"
    >
      <div class="h-16 flex items-center justify-center border-b border-white/5">
        <span class="font-bold text-xl bg-clip-text text-transparent bg-gradient-to-r from-neon-blue to-neon-purple" v-if="!isCollapsed">
          Inspo Admin
        </span>
        <span v-else class="font-bold text-xl text-neon-blue">IA</span>
      </div>

      <nav class="flex-1 py-4 space-y-2 px-3">
        <RouterLink 
          v-for="item in menuItems" 
          :key="item.path"
          :to="item.path"
          class="flex items-center gap-3 px-3 py-3 rounded-lg transition-colors hover:bg-white/5 text-gray-400 hover:text-white"
          :class="{ 'bg-neon-blue/10 text-neon-blue': route.path === item.path }"
        >
          <component :is="item.icon" class="w-5 h-5" />
          <span v-if="!isCollapsed" class="text-sm">{{ item.name }}</span>
        </RouterLink>
      </nav>

      <div class="p-4 border-t border-white/5">
        <button 
          @click="handleLogout"
          class="flex items-center gap-3 w-full px-3 py-2 text-red-400 hover:bg-red-500/10 rounded-lg transition-colors"
        >
          <LogOut class="w-5 h-5" />
          <span v-if="!isCollapsed" class="text-sm">退出登录</span>
        </button>
      </div>
    </aside>

    <!-- Main Content -->
    <div class="flex-1 flex flex-col min-w-0">
      <!-- Header -->
      <header class="h-16 bg-slate-800 border-b border-white/5 flex items-center justify-between px-6">
        <div class="flex items-center gap-4">
          <button @click="isCollapsed = !isCollapsed" class="p-2 hover:bg-white/5 rounded-lg">
            <Menu class="w-5 h-5 text-gray-400" />
          </button>
          
          <!-- Breadcrumb -->
          <div class="flex items-center text-sm text-gray-400">
            <span>首页</span>
            <ChevronRight class="w-4 h-4 mx-2" />
            <span class="text-white">后台管理</span>
            <ChevronRight class="w-4 h-4 mx-2" />
            <span class="text-neon-blue">{{ route.meta.title || '当前页面' }}</span>
          </div>
        </div>

        <div class="flex items-center gap-4">
          <div class="text-right hidden md:block">
            <p class="text-sm font-medium text-white">{{ authStore.user?.nickname || 'Admin' }}</p>
            <p class="text-xs text-gray-400">超级管理员</p>
          </div>
          <div class="w-10 h-10 rounded-full bg-slate-700 overflow-hidden">
            <img v-if="authStore.user?.avatar" :src="authStore.user.avatar" class="w-full h-full object-cover" alt="Avatar" />
          </div>
        </div>
      </header>

      <!-- Content Area -->
      <main class="flex-1 overflow-y-auto p-6 bg-slate-900">
        <RouterView />
      </main>
    </div>
  </div>
</template>

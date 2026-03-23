<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { Search, Shield, ShieldAlert, KeyRound, ChevronLeft, ChevronRight } from 'lucide-vue-next'
import { useToastStore } from '@/stores/toast'
import { useModalStore } from '@/stores/modal'
import { getAdminUsers, toggleUserStatus, updateUserRole, resetUserPassword } from '@/api/admin'

const toast = useToastStore()
const modalStore = useModalStore()

const users = ref<any[]>([])
const loading = ref(false)
const searchQuery = ref('')
const currentPage = ref(1)
const pageSize = ref(15)
const total = ref(0)

let searchTimer: ReturnType<typeof setTimeout> | null = null

const loadUsers = async () => {
  loading.value = true
  try {
    const res = await getAdminUsers({ page: currentPage.value, size: pageSize.value, keyword: searchQuery.value })
    users.value = res.records || []
    total.value = res.total || 0
  } catch (e) {
    toast.error('加载用户失败')
  } finally {
    loading.value = false
  }
}

watch(searchQuery, () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => { currentPage.value = 1; loadUsers() }, 400)
})

const totalPages = () => Math.max(1, Math.ceil(total.value / pageSize.value))

const prevPage = () => { if (currentPage.value > 1) { currentPage.value--; loadUsers() } }
const nextPage = () => { if (currentPage.value < totalPages()) { currentPage.value++; loadUsers() } }

const handleToggleStatus = async (user: any) => {
  const isActive = user.status === 1
  const confirmed = await modalStore.open({
    title: isActive ? '封禁用户' : '解封用户',
    content: isActive ? `确定要封禁用户 ${user.nickname || user.username} 吗？` : `确定要解封用户 ${user.nickname || user.username} 吗？`,
    type: isActive ? 'error' : 'info',
    confirmText: isActive ? '确认封禁' : '确认解封'
  })
  if (confirmed) {
    try {
      await toggleUserStatus(user.id)
      toast.success(isActive ? '已封禁' : '已解封')
      loadUsers()
    } catch { toast.error('操作失败') }
  }
}

const handleResetPassword = async (user: any) => {
  const confirmed = await modalStore.open({
    title: '重置密码',
    content: `将用户 ${user.nickname || user.username} 的密码重置为 inspo123456？`,
    type: 'warning',
    confirmText: '确认重置'
  })
  if (confirmed) {
    try {
      await resetUserPassword(user.id, 'inspo123456')
      toast.success('密码已重置')
    } catch { toast.error('重置失败') }
  }
}

const handleRoleChange = async (user: any) => {
  const hasAdmin = (user.roles || []).includes('ROLE_ADMIN')
  const newRole = hasAdmin ? 'ROLE_USER' : 'ROLE_ADMIN'
  const confirmed = await modalStore.open({
    title: '更改角色',
    content: hasAdmin ? `移除 ${user.nickname || user.username} 的管理员权限？` : `赋予 ${user.nickname || user.username} 管理员权限？`,
    type: 'warning',
    confirmText: '确认'
  })
  if (confirmed) {
    try {
      await updateUserRole(user.id, newRole)
      toast.success('角色已更新')
      loadUsers()
    } catch { toast.error('操作失败') }
  }
}

const getRoleLabel = (roles: string[]) => {
  if (!roles || roles.length === 0) return 'USER'
  if (roles.includes('ROLE_ADMIN')) return 'ADMIN'
  if (roles.includes('ROLE_VIP')) return 'VIP'
  return 'USER'
}

const getRoleClass = (roles: string[]) => {
  const label = getRoleLabel(roles)
  if (label === 'ADMIN') return 'bg-neon-blue/10 text-neon-blue border-neon-blue/20'
  if (label === 'VIP') return 'bg-neon-purple/10 text-neon-purple border-neon-purple/20'
  return 'bg-slate-700 text-gray-300 border-slate-600'
}

onMounted(loadUsers)
</script>

<template>
  <div class="space-y-6 animate__animated animate__fadeIn">
    <!-- Header -->
    <div class="flex flex-col md:flex-row justify-between items-center gap-4">
      <h2 class="text-2xl font-bold text-white">用户管理</h2>
      <div class="relative w-full md:w-64">
        <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
        <input 
          v-model="searchQuery"
          type="text" 
          placeholder="搜索用户名或邮箱..." 
          class="w-full bg-slate-800 border border-slate-700 rounded-lg pl-10 pr-4 py-2 text-sm text-white focus:border-neon-blue focus:outline-none focus:ring-1 focus:ring-neon-blue transition-all"
        >
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex items-center justify-center h-32">
      <div class="w-8 h-8 border-4 border-neon-blue border-t-transparent rounded-full animate-spin"></div>
    </div>

    <!-- Table -->
    <div v-else class="bg-slate-800 rounded-xl border border-white/5 overflow-hidden">
      <div class="overflow-x-auto">
        <table class="w-full text-left border-collapse">
          <thead>
            <tr class="border-b border-white/5 bg-slate-900/50">
              <th class="p-4 text-xs font-medium text-gray-400 uppercase">用户</th>
              <th class="p-4 text-xs font-medium text-gray-400 uppercase">角色</th>
              <th class="p-4 text-xs font-medium text-gray-400 uppercase">状态</th>
              <th class="p-4 text-xs font-medium text-gray-400 uppercase">注册时间</th>
              <th class="p-4 text-xs font-medium text-gray-400 uppercase text-right">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-white/5">
            <tr v-for="user in users" :key="user.id" class="hover:bg-white/5 transition-colors">
              <td class="p-4">
                <div class="flex items-center gap-3">
                  <img v-if="user.avatarUrl" :src="user.avatarUrl" class="w-8 h-8 rounded-full object-cover" />
                  <div v-else class="w-8 h-8 rounded-full bg-gradient-to-tr from-slate-700 to-slate-600 flex items-center justify-center text-xs font-bold text-white">
                    {{ (user.nickname || user.username || '?').charAt(0) }}
                  </div>
                  <div>
                    <p class="text-sm font-medium text-white">{{ user.nickname || user.username }}</p>
                    <p class="text-xs text-gray-500">{{ user.email }}</p>
                  </div>
                </div>
              </td>
              <td class="p-4">
                <span class="px-2 py-1 rounded text-xs font-medium border" :class="getRoleClass(user.roles)">
                  {{ getRoleLabel(user.roles) }}
                </span>
              </td>
              <td class="p-4">
                <div class="flex items-center gap-2">
                  <div class="w-2 h-2 rounded-full" :class="user.status === 1 ? 'bg-green-500' : 'bg-red-500'"></div>
                  <span class="text-sm text-gray-300">{{ user.status === 1 ? '正常' : '已封禁' }}</span>
                </div>
              </td>
              <td class="p-4 text-sm text-gray-400">{{ user.createdAt?.substring(0, 10) }}</td>
              <td class="p-4 text-right space-x-1">
                <button @click="handleToggleStatus(user)" class="p-2 rounded hover:bg-white/10 text-gray-400 hover:text-white transition-colors" :title="user.status === 1 ? '封禁' : '解封'">
                  <component :is="user.status === 1 ? ShieldAlert : Shield" class="w-4 h-4" />
                </button>
                <button @click="handleResetPassword(user)" class="p-2 rounded hover:bg-white/10 text-gray-400 hover:text-yellow-400 transition-colors" title="重置密码">
                  <KeyRound class="w-4 h-4" />
                </button>
                <button @click="handleRoleChange(user)" class="p-2 rounded hover:bg-white/10 text-gray-400 hover:text-neon-blue transition-colors" title="切换管理员">
                  <Shield class="w-4 h-4" />
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      
      <!-- Empty State -->
      <div v-if="users.length === 0" class="p-8 text-center text-gray-500">
        未找到匹配用户
      </div>

      <!-- Pagination -->
      <div v-if="total > pageSize" class="flex items-center justify-between p-4 border-t border-white/5">
        <span class="text-sm text-gray-400">共 {{ total }} 条</span>
        <div class="flex items-center gap-2">
          <button @click="prevPage" :disabled="currentPage <= 1" class="p-1.5 rounded hover:bg-white/10 text-gray-400 disabled:opacity-30"><ChevronLeft class="w-4 h-4" /></button>
          <span class="text-sm text-gray-300">{{ currentPage }} / {{ totalPages() }}</span>
          <button @click="nextPage" :disabled="currentPage >= totalPages()" class="p-1.5 rounded hover:bg-white/10 text-gray-400 disabled:opacity-30"><ChevronRight class="w-4 h-4" /></button>
        </div>
      </div>
    </div>
  </div>
</template>

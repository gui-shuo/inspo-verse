<script setup lang="ts">
import { ref, computed } from 'vue'
import { Search, Shield, ShieldAlert } from 'lucide-vue-next'
import { useToastStore } from '@/stores/toast'
import { useModalStore } from '@/stores/modal'

const toast = useToastStore()
const modalStore = useModalStore()

// Mock Data
const users = ref([
  { id: 1, name: 'CyberPunk_2077', email: 'neo@matrix.com', role: 'admin', status: 'active', joinDate: '2025-11-01' },
  { id: 2, name: 'Alice_Wonder', email: 'alice@inspo.verse', role: 'user', status: 'active', joinDate: '2025-11-05' },
  { id: 3, name: 'Glitch_Hunter', email: 'hunter@darkweb.net', role: 'user', status: 'banned', joinDate: '2025-11-12' },
  { id: 4, name: 'Code_Ninja', email: 'ninja@vuejs.org', role: 'vip', status: 'active', joinDate: '2025-11-15' },
  { id: 5, name: 'Pixel_Artist', email: 'art@design.io', role: 'user', status: 'active', joinDate: '2025-11-20' },
])

const searchQuery = ref('')

const filteredUsers = computed(() => {
  return users.value.filter(user => 
    user.name.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
    user.email.toLowerCase().includes(searchQuery.value.toLowerCase())
  )
})

const toggleStatus = async (user: any) => {
  if (user.status === 'active') {
    const confirmed = await modalStore.open({
      title: '封禁用户',
      content: `确定要封禁用户 ${user.name} 吗？此操作将限制其登录。`,
      type: 'error',
      confirmText: '确认封禁'
    })
    
    if(confirmed) {
      user.status = 'banned'
      toast.warning(`用户 ${user.name} 已被封禁`)
    }
  } else {
    user.status = 'active'
    toast.success(`用户 ${user.name} 已解封`)
  }
}
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

    <!-- Table -->
    <div class="bg-slate-800 rounded-xl border border-white/5 overflow-hidden">
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
            <tr v-for="user in filteredUsers" :key="user.id" class="hover:bg-white/5 transition-colors group">
              <td class="p-4">
                <div class="flex items-center gap-3">
                  <div class="w-8 h-8 rounded-full bg-gradient-to-tr from-slate-700 to-slate-600 flex items-center justify-center text-xs font-bold text-white">
                    {{ user.name.charAt(0) }}
                  </div>
                  <div>
                    <p class="text-sm font-medium text-white">{{ user.name }}</p>
                    <p class="text-xs text-gray-500">{{ user.email }}</p>
                  </div>
                </div>
              </td>
              <td class="p-4">
                <span 
                  class="px-2 py-1 rounded text-xs font-medium border"
                  :class="{
                    'bg-neon-blue/10 text-neon-blue border-neon-blue/20': user.role === 'admin',
                    'bg-neon-purple/10 text-neon-purple border-neon-purple/20': user.role === 'vip',
                    'bg-slate-700 text-gray-300 border-slate-600': user.role === 'user'
                  }"
                >
                  {{ user.role.toUpperCase() }}
                </span>
              </td>
              <td class="p-4">
                <div class="flex items-center gap-2">
                  <div class="w-2 h-2 rounded-full" :class="user.status === 'active' ? 'bg-green-500' : 'bg-red-500'"></div>
                  <span class="text-sm text-gray-300">{{ user.status === 'active' ? '正常' : '已封禁' }}</span>
                </div>
              </td>
              <td class="p-4 text-sm text-gray-400">{{ user.joinDate }}</td>
              <td class="p-4 text-right">
                <button 
                  @click="toggleStatus(user)"
                  class="p-2 rounded hover:bg-white/10 text-gray-400 hover:text-white transition-colors"
                  :title="user.status === 'active' ? '封禁用户' : '解封用户'"
                >
                  <component :is="user.status === 'active' ? ShieldAlert : Shield" class="w-4 h-4" />
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      
      <!-- Empty State -->
      <div v-if="filteredUsers.length === 0" class="p-8 text-center text-gray-500">
        未找到匹配用户
      </div>
    </div>
  </div>
</template>

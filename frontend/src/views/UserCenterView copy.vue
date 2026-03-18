<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useModalStore } from '@/stores/modal'
import { useToastStore } from '@/stores/toast'
import { User, Package, Settings, Save } from 'lucide-vue-next'
import { debounce } from 'lodash-es'

const authStore = useAuthStore()
const modalStore = useModalStore()
const toast = useToastStore()
const activeTab = ref('profile')
const isLoading = ref(true)
const isSaving = ref(false)

// Mock Data State
const orders = ref<any[]>([])
const formData = reactive({
  nickname: '',
  phone: '',
  bio: ''
})

// Concurrent Data Fetching
const fetchData = async () => {
  isLoading.value = true
  try {
    // 模拟并发请求：用户信息 + 订单列表
    const [userRes, ordersRes] = await Promise.all([
      new Promise(r => setTimeout(() => r(authStore.user), 500)),
      new Promise(r => setTimeout(() => r([
        { id: 'ORD-20251201-001', item: '黄金会员年卡 (12个月)', amount: 299, status: 'completed', date: '2025-12-01 14:20' },
        { id: 'ORD-20251120-056', item: 'AI 绘画点数包 (500点)', amount: 49, status: 'completed', date: '2025-11-20 09:15' },
        { id: 'ORD-20251115-102', item: '赛博朋克 2077 周边T恤 (L码)', amount: 129, status: 'pending', date: '2025-11-15 18:30' },
        { id: 'ORD-20251110-003', item: 'Inspo 创作者大会门票', amount: 699, status: 'cancelled', date: '2025-11-10 10:00' },
        { id: 'ORD-20251001-088', item: 'Python 数据分析课程', amount: 199, status: 'completed', date: '2025-10-01 22:45' },
        { id: 'ORD-20250920-012', item: '白银会员月卡', amount: 29, status: 'completed', date: '2025-09-20 11:20' },
      ]), 800))
    ])
    
    // 初始化表单
    if (userRes) {
      formData.nickname = (userRes as any).nickname || 'Inspo_User'
      formData.phone = '138****8888' // 脱敏展示
      formData.bio = '热爱技术，热爱生活。在代码的世界里寻找诗意，在 AI 的浪潮中探索未来。\n\nTech Stack: Vue 3, TypeScript, WebGL'
    }
    orders.value = ordersRes as any[]
  } finally {
    isLoading.value = false
  }
}

// Debounced Input Validation
const handleInput = debounce((e: Event) => {
  console.log('Real-time validation check:', (e.target as HTMLInputElement).value)
}, 500)

// Save Profile
const handleSave = async () => {
  // 敏感操作二次确认
  const confirmed = await modalStore.open({
    title: '保存修改',
    content: '确定要保存修改吗？涉及敏感信息修改可能需要重新登录。',
    type: 'info',
    confirmText: '确认保存'
  })

  if (!confirmed) return

  isSaving.value = true
  await new Promise(r => setTimeout(r, 1000))
  // Update store (mock)
  if (authStore.user) {
    authStore.user.nickname = formData.nickname
  }
  isSaving.value = false
  toast.success('个人资料已更新')
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="container mx-auto px-4 py-8">
    <div class="grid grid-cols-1 md:grid-cols-12 gap-8">
      <!-- Left Profile Card -->
      <div class="md:col-span-4 lg:col-span-3">
        <div class="bg-slate-800 rounded-2xl p-6 border border-white/5 text-center sticky top-24">
          <div class="w-24 h-24 mx-auto rounded-full bg-gradient-to-br from-neon-blue to-neon-purple p-1 mb-4">
            <img :src="authStore.user?.avatar" class="w-full h-full rounded-full bg-slate-900 object-cover" />
          </div>
          <h2 class="text-xl font-bold text-white">{{ authStore.user?.nickname }}</h2>
          <p class="text-neon-yellow text-sm mt-1 uppercase">{{ authStore.user?.level }} MEMBER</p>
          
          <div class="mt-8 flex flex-col gap-2">
            <button 
              @click="activeTab = 'profile'"
              class="flex items-center gap-3 px-4 py-3 rounded-xl transition-all"
              :class="activeTab === 'profile' ? 'bg-neon-blue text-slate-900 font-bold' : 'hover:bg-white/5 text-gray-400'"
            >
              <User class="w-5 h-5" /> 个人资料
            </button>
            <button 
              @click="activeTab = 'orders'"
              class="flex items-center gap-3 px-4 py-3 rounded-xl transition-all"
              :class="activeTab === 'orders' ? 'bg-neon-blue text-slate-900 font-bold' : 'hover:bg-white/5 text-gray-400'"
            >
              <Package class="w-5 h-5" /> 我的订单
            </button>
          </div>
        </div>
      </div>

      <!-- Right Content Area -->
      <div class="md:col-span-8 lg:col-span-9">
        <div v-if="isLoading" class="flex justify-center py-20">
          <div class="animate-spin w-8 h-8 border-2 border-neon-blue border-t-transparent rounded-full"></div>
        </div>

        <!-- Profile Edit Form -->
        <div v-else-if="activeTab === 'profile'" class="bg-slate-800 rounded-2xl p-8 border border-white/5 animate__animated animate__fadeIn">
          <h3 class="text-xl font-bold text-white mb-6 flex items-center gap-2">
            <Settings class="w-5 h-5 text-neon-blue" /> 编辑资料
          </h3>
          
          <div class="space-y-6 max-w-2xl">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div class="space-y-2">
                <label class="text-gray-400 text-sm">昵称</label>
                <input 
                  v-model="formData.nickname" 
                  @input="handleInput"
                  type="text" 
                  class="w-full bg-slate-900 border border-slate-700 rounded-lg px-4 py-3 text-white focus:border-neon-blue focus:outline-none"
                >
              </div>
              <div class="space-y-2">
                <label class="text-gray-400 text-sm">手机号</label>
                <input 
                  v-model="formData.phone" 
                  type="text" 
                  disabled
                  class="w-full bg-slate-900/50 border border-slate-700 rounded-lg px-4 py-3 text-gray-500 cursor-not-allowed"
                >
              </div>
            </div>
            
            <div class="space-y-2">
              <label class="text-gray-400 text-sm">个人简介</label>
              <textarea 
                v-model="formData.bio"
                rows="4"
                class="w-full bg-slate-900 border border-slate-700 rounded-lg px-4 py-3 text-white focus:border-neon-blue focus:outline-none resize-none"
              ></textarea>
            </div>

            <div class="pt-4 border-t border-white/5 flex justify-end">
              <button 
                @click="handleSave"
                :disabled="isSaving"
                class="flex items-center gap-2 px-6 py-3 bg-neon-blue text-slate-900 font-bold rounded-lg hover:shadow-[0_0_15px_rgba(0,243,255,0.3)] transition-all disabled:opacity-50"
              >
                <Save class="w-4 h-4" />
                {{ isSaving ? '保存中...' : '保存修改' }}
              </button>
            </div>
          </div>
        </div>

        <!-- Order List -->
        <div v-else class="bg-slate-800 rounded-2xl p-8 border border-white/5 animate__animated animate__fadeIn">
          <h3 class="text-xl font-bold text-white mb-6 flex items-center gap-2">
            <Package class="w-5 h-5 text-neon-purple" /> 订单记录
          </h3>
          
          <div class="space-y-4">
            <div v-for="order in orders" :key="order.id" class="flex items-center justify-between p-4 bg-slate-900 rounded-xl border border-white/5 hover:border-white/10 transition-colors">
              <div class="flex items-center gap-4">
                <div class="w-12 h-12 rounded-lg bg-slate-800 flex items-center justify-center">
                  <Package class="w-6 h-6 text-gray-400" />
                </div>
                <div>
                  <h4 class="font-bold text-white">{{ order.item }}</h4>
                  <p class="text-xs text-gray-500">{{ order.id }} • {{ order.date }}</p>
                </div>
              </div>
              <div class="text-right">
                <p class="font-bold text-neon-blue">¥ {{ order.amount }}</p>
                <span 
                  class="text-xs px-2 py-0.5 rounded-full"
                  :class="order.status === 'completed' ? 'bg-green-500/20 text-green-400' : 'bg-yellow-500/20 text-yellow-400'"
                >
                  {{ order.status === 'completed' ? '已完成' : '处理中' }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

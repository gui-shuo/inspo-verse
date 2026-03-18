<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useModalStore } from '@/stores/modal'
import { useToastStore } from '@/stores/toast'
// 引入所有需要的图标
import { 
  User, Package, Settings, Save, 
  Image, CreditCard, ShieldCheck, Download, Trash2, 
  Plus, Zap, ArrowUpRight, ArrowDownLeft, Gamepad2, Github 
} from 'lucide-vue-next'
import { debounce } from 'lodash-es'

const authStore = useAuthStore()
const modalStore = useModalStore()
const toast = useToastStore()

// 定义 Tab 类型
type TabType = 'profile' | 'orders' | 'creations' | 'wallet' | 'security'
const activeTab = ref<TabType>('profile')

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
    // 模拟并发请求
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
  const confirmed = await modalStore.open({
    title: '保存修改',
    content: '确定要保存修改吗？涉及敏感信息修改可能需要重新登录。',
    type: 'info',
    confirmText: '确认保存'
  })

  if (!confirmed) return

  isSaving.value = true
  await new Promise(r => setTimeout(r, 1000))
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
            <button 
              @click="activeTab = 'creations'"
              class="flex items-center gap-3 px-4 py-3 rounded-xl transition-all"
              :class="activeTab === 'creations' ? 'bg-neon-blue text-slate-900 font-bold' : 'hover:bg-white/5 text-gray-400'"
            >
              <Image class="w-5 h-5" /> 我的创作
            </button>
            <button 
              @click="activeTab = 'wallet'"
              class="flex items-center gap-3 px-4 py-3 rounded-xl transition-all"
              :class="activeTab === 'wallet' ? 'bg-neon-blue text-slate-900 font-bold' : 'hover:bg-white/5 text-gray-400'"
            >
              <CreditCard class="w-5 h-5" /> 数字钱包
            </button>
            <button 
              @click="activeTab = 'security'"
              class="flex items-center gap-3 px-4 py-3 rounded-xl transition-all"
              :class="activeTab === 'security' ? 'bg-neon-blue text-slate-900 font-bold' : 'hover:bg-white/5 text-gray-400'"
            >
              <ShieldCheck class="w-5 h-5" /> 安全设置
            </button>
          </div>
        </div>
      </div>

      <div class="md:col-span-8 lg:col-span-9">
        
        <div v-if="isLoading" class="flex justify-center py-20">
          <div class="animate-spin w-8 h-8 border-2 border-neon-blue border-t-transparent rounded-full"></div>
        </div>

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

        <div v-else-if="activeTab === 'orders'" class="bg-slate-800 rounded-2xl p-8 border border-white/5 animate__animated animate__fadeIn">
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

        <div v-else-if="activeTab === 'creations'" class="bg-slate-800 rounded-2xl p-8 border border-white/5 animate__animated animate__fadeIn">
          <div class="flex justify-between items-center mb-6">
            <h3 class="text-xl font-bold text-white flex items-center gap-2">
              <Image class="w-5 h-5 text-neon-purple" /> 创作档案
            </h3>
            <button class="text-sm text-neon-blue hover:underline">管理分类</button>
          </div>

          <div class="grid grid-cols-2 md:grid-cols-3 gap-4">
            <div v-for="i in 6" :key="i" class="group relative aspect-square bg-slate-900 rounded-xl overflow-hidden border border-white/5 cursor-pointer">
              <img :src="`https://picsum.photos/seed/${i + 100}/400/400`" class="w-full h-full object-cover transition-transform group-hover:scale-110" />
              
              <div class="absolute inset-0 bg-black/60 opacity-0 group-hover:opacity-100 transition-opacity flex flex-col justify-center items-center gap-2">
                <span class="text-xs font-mono text-neon-green">Created: 2025.12.0{{i}}</span>
                <div class="flex gap-2">
                  <button class="p-2 bg-white/10 rounded-full hover:bg-neon-blue hover:text-slate-900 transition-colors" title="下载">
                    <Download class="w-4 h-4" />
                  </button>
                  <button class="p-2 bg-white/10 rounded-full hover:bg-red-500 hover:text-white transition-colors" title="删除">
                    <Trash2 class="w-4 h-4" />
                  </button>
                </div>
              </div>
              
              <div class="absolute top-2 right-2 px-2 py-0.5 bg-black/50 backdrop-blur rounded text-[10px] text-gray-300 border border-white/10">
                {{ i % 2 === 0 ? '公开' : '私密' }}
              </div>
            </div>
            
            <div class="aspect-square rounded-xl border-2 border-dashed border-white/10 flex flex-col items-center justify-center text-gray-500 hover:border-neon-blue/50 hover:text-neon-blue transition-colors cursor-pointer">
              <Plus class="w-8 h-8 mb-2" />
              <span class="text-xs font-bold">新建创作</span>
            </div>
          </div>
        </div>

        <div v-else-if="activeTab === 'wallet'" class="space-y-6 animate__animated animate__fadeIn">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div class="bg-gradient-to-br from-indigo-900 to-slate-900 rounded-2xl p-6 border border-white/10 relative overflow-hidden">
              <div class="absolute top-0 right-0 p-4 opacity-10">
                <Zap class="w-32 h-32 text-neon-yellow" />
              </div>
              <p class="text-gray-400 text-sm mb-2">灵感点数 (Inspo Points)</p>
              <div class="text-4xl font-mono font-bold text-neon-yellow mb-4">2,450</div>
              <div class="flex gap-3">
                <button class="px-4 py-2 bg-neon-yellow text-slate-900 font-bold rounded-lg text-sm hover:shadow-lg transition-all">
                  立即充值
                </button>
                <button class="px-4 py-2 bg-white/10 text-white rounded-lg text-sm hover:bg-white/20 transition-all">
                  兑换权益
                </button>
              </div>
            </div>
            
            <div class="bg-slate-800 rounded-2xl p-6 border border-white/5 flex flex-col justify-center">
              <div class="flex justify-between items-center mb-4">
                <span class="text-gray-400 text-sm">本月消耗</span>
                <span class="text-white font-bold">-450 pts</span>
              </div>
              <div class="w-full h-2 bg-slate-700 rounded-full overflow-hidden mb-2">
                <div class="h-full bg-neon-blue w-[35%]"></div>
              </div>
              <p class="text-xs text-gray-500">距离下个月度礼包还需消耗 550 pts</p>
            </div>
          </div>

          <div class="bg-slate-800 rounded-2xl p-8 border border-white/5">
            <h3 class="text-xl font-bold text-white mb-6">收支明细</h3>
            <div class="space-y-0 divide-y divide-white/5">
              <div v-for="i in 5" :key="i" class="flex justify-between items-center py-4">
                <div class="flex items-center gap-4">
                  <div class="w-10 h-10 rounded-full flex items-center justify-center" 
                       :class="i % 2 === 0 ? 'bg-red-500/10 text-red-400' : 'bg-green-500/10 text-green-400'">
                    <component :is="i % 2 === 0 ? ArrowUpRight : ArrowDownLeft" class="w-5 h-5" />
                  </div>
                  <div>
                    <p class="text-white font-medium">{{ i % 2 === 0 ? 'AI 绘图消耗 (Fast Mode)' : '每日签到奖励' }}</p>
                    <p class="text-xs text-gray-500">2025-12-0{{i}} 14:20</p>
                  </div>
                </div>
                <span class="font-mono font-bold" :class="i % 2 === 0 ? 'text-white' : 'text-neon-yellow'">
                  {{ i % 2 === 0 ? '-15' : '+50' }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <div v-else-if="activeTab === 'security'" class="bg-slate-800 rounded-2xl p-8 border border-white/5 animate__animated animate__fadeIn">
          <h3 class="text-xl font-bold text-white mb-6 flex items-center gap-2">
            <ShieldCheck class="w-5 h-5 text-green-400" /> 账号安全
          </h3>

          <div class="space-y-8 max-w-2xl">
            <div class="space-y-4">
              <h4 class="text-white font-bold border-l-4 border-neon-blue pl-3">修改密码</h4>
              <div class="grid gap-4">
                <input type="password" placeholder="当前密码" class="w-full bg-slate-900 border border-slate-700 rounded-lg px-4 py-3 text-white focus:border-neon-blue focus:outline-none transition-all">
                <input type="password" placeholder="新密码 (8位以上含大小写字母)" class="w-full bg-slate-900 border border-slate-700 rounded-lg px-4 py-3 text-white focus:border-neon-blue focus:outline-none transition-all">
                <input type="password" placeholder="确认新密码" class="w-full bg-slate-900 border border-slate-700 rounded-lg px-4 py-3 text-white focus:border-neon-blue focus:outline-none transition-all">
              </div>
              <button class="px-6 py-2 bg-white/5 hover:bg-white/10 text-white rounded-lg border border-white/10 transition-all">
                更新密码
              </button>
            </div>

            <div class="w-full h-px bg-white/5"></div>

            <div class="space-y-4">
              <h4 class="text-white font-bold border-l-4 border-neon-purple pl-3">第三方绑定</h4>
              
              <div class="flex items-center justify-between p-4 bg-slate-900 rounded-xl border border-white/5">
                <div class="flex items-center gap-3">
                  <div class="w-8 h-8 rounded-full bg-[#5865F2] flex items-center justify-center text-white">
                    <Gamepad2 class="w-5 h-5" />
                  </div>
                  <div>
                    <p class="text-sm font-bold text-white">Discord</p>
                    <p class="text-xs text-green-400">已连接: CyberPlayer#9527</p>
                  </div>
                </div>
                <button class="text-xs text-gray-400 hover:text-white border border-white/10 px-3 py-1 rounded">解绑</button>
              </div>

              <div class="flex items-center justify-between p-4 bg-slate-900 rounded-xl border border-white/5">
                <div class="flex items-center gap-3">
                  <div class="w-8 h-8 rounded-full bg-white flex items-center justify-center text-black">
                    <Github class="w-5 h-5" />
                  </div>
                  <div>
                    <p class="text-sm font-bold text-white">GitHub</p>
                    <p class="text-xs text-gray-500">未连接</p>
                  </div>
                </div>
                <button class="text-xs text-neon-blue hover:text-white border border-neon-blue/30 px-3 py-1 rounded">连接</button>
              </div>
            </div>
          </div>
        </div>

      </div>
    </div>
  </div>
</template>

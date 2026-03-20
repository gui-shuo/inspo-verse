<script setup lang="ts">
import { ref, onMounted, reactive, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useModalStore } from '@/stores/modal'
import { useToastStore } from '@/stores/toast'
import {
  User, Package, Settings, Save,
  Image, CreditCard, ShieldCheck, Download, Trash2,
  Plus, Zap, ArrowUpRight, ArrowDownLeft, Gamepad2, Github,
  Camera, Eye, EyeOff, Link, Unlink, RefreshCw
} from 'lucide-vue-next'

// API imports
import { getMyProfile, updateProfile, uploadAvatar, changePassword } from '@/api/user'
import { getMyOrders } from '@/api/vip'
import { getMyPaymentOrders } from '@/api/wallet'
import { getMyCreations, uploadCreation, deleteCreation, updateCreationVisibility, downloadCreation, type UserCreation } from '@/api/creation'
import { getWallet, getTransactions, dailySignIn, type WalletInfo, type PointTransaction } from '@/api/wallet'
import { getOAuthBindings, getOAuthAuthUrl, unbindOAuth, type OAuthBinding } from '@/api/security'
import PaymentModal from '@/components/wallet/PaymentModal.vue'

const authStore = useAuthStore()
const modalStore = useModalStore()
const toast = useToastStore()

type TabType = 'profile' | 'orders' | 'creations' | 'wallet' | 'security'
const activeTab = ref<TabType>('profile')
const isLoading = ref(true)

// ========================= 个人资料 =========================
const isSaving = ref(false)
const isUploadingAvatar = ref(false)
const avatarPreview = ref<string>('')
const avatarFile = ref<File | null>(null)
const formData = reactive({ nickname: '', phone: '', bio: '' })

const loadProfile = async () => {
  try {
    const res = await getMyProfile()
    if (res.code === 0 && res.data) {
      const d = res.data
      formData.nickname = d.nickname || ''
      formData.phone = d.phone || ''
      formData.bio = d.bio || ''
      avatarPreview.value = d.avatarUrl || ''
    }
  } catch {
    formData.nickname = authStore.user?.nickname || ''
  }
}

const onAvatarChange = (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (file.size > 5 * 1024 * 1024) { toast.error('头像不能超过5MB'); return }
  avatarFile.value = file
  avatarPreview.value = URL.createObjectURL(file)
}

const handleSaveProfile = async () => {
  const confirmed = await modalStore.open({
    title: '保存修改',
    content: '确定要保存修改吗？',
    type: 'info',
    confirmText: '确认保存'
  })
  if (!confirmed) return
  isSaving.value = true
  try {
    if (avatarFile.value) {
      isUploadingAvatar.value = true
      const uploadRes = await uploadAvatar(avatarFile.value)
      if (uploadRes.code !== 0) {
        isUploadingAvatar.value = false
        throw new Error(uploadRes.message || '头像上传失败')
      }
      const newAvatarUrl = uploadRes.data?.avatarUrl
      if (newAvatarUrl) {
        // 用服务器返回的真实 URL 替换 ObjectURL，避免 ObjectURL 被回收后预览消失
        avatarPreview.value = newAvatarUrl
        if (authStore.user) authStore.user.avatar = newAvatarUrl
      }
      avatarFile.value = null
      isUploadingAvatar.value = false
    }
    const phoneToSend = /\*/.test(formData.phone) ? undefined : formData.phone || undefined
    await updateProfile({ nickname: formData.nickname, bio: formData.bio, phone: phoneToSend })
    // 从后端重新拉取最新用户信息，同步 store 和 localStorage（确保右上角头像实时更新）
    await authStore.fetchUserInfo()
    // 同步 avatarPreview 确保 UserCenter 内也显示最新头像
    if (authStore.user?.avatar) avatarPreview.value = authStore.user.avatar
    toast.success('个人资料已更新')
  } catch (err: any) {
    toast.error(err?.message || '保存失败，请稍后重试')
  } finally {
    isSaving.value = false
  }
}

// ========================= 我的订单 =========================
// 统一展示格式：type RECHARGE=充值, VIP=会员订单
interface UnifiedOrder {
  id: number
  orderType: 'RECHARGE' | 'VIP'
  orderNo: string
  itemName: string
  amount: number
  points?: number
  payMethod?: string
  statusCode: 'PENDING' | 'PAID' | 'EXPIRED' | 'FAILED'
  statusText: string
  createdAt: string
  paidAt?: string
}

const orders = ref<UnifiedOrder[]>([])
const ordersLoading = ref(false)

const normalizeVipOrder = (o: any): UnifiedOrder => {
  const statusMap: Record<number, 'PENDING' | 'PAID' | 'FAILED'> = { 0: 'PENDING', 1: 'PAID', 2: 'FAILED', 3: 'FAILED' }
  const textMap: Record<number, string> = { 0: '待支付', 1: '已完成', 2: '已失败', 3: '已退款' }
  return {
    id: o.id,
    orderType: 'VIP',
    orderNo: o.orderNo,
    itemName: o.itemName || 'VIP套餐',
    amount: o.amount,
    payMethod: o.payChannel,
    statusCode: statusMap[o.payStatus] ?? 'PENDING',
    statusText: textMap[o.payStatus] ?? '处理中',
    createdAt: o.createdAt,
    paidAt: o.paidAt,
  }
}

const normalizePaymentOrder = (o: any): UnifiedOrder => {
  const textMap: Record<string, string> = { PENDING: '待支付', PAID: '已完成', EXPIRED: '已超时', FAILED: '支付失败' }
  const methodLabel = o.payMethod === 'WECHAT' ? '微信支付' : '支付宝'
  return {
    id: o.id,
    orderType: 'RECHARGE',
    orderNo: o.orderNo,
    itemName: `充値 ${(o.points ?? 0).toLocaleString()} 灵感点数`,
    amount: o.amount,
    points: o.points,
    payMethod: methodLabel,
    statusCode: o.status as UnifiedOrder['statusCode'],
    statusText: textMap[o.status] ?? '未知',
    createdAt: o.createdAt,
    paidAt: o.paidAt,
  }
}

const loadOrders = async () => {
  ordersLoading.value = true
  try {
    const [vipRes, payRes] = await Promise.all([getMyOrders(), getMyPaymentOrders()])
    const vipOrders = (vipRes.code === 0 ? vipRes.data || [] : []).map(normalizeVipOrder)
    const payOrders = (payRes.code === 0 ? payRes.data || [] : []).map(normalizePaymentOrder)
    // 合并并按创建时间倒序排列
    orders.value = [...vipOrders, ...payOrders].sort(
      (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    )
  } catch { toast.error('订单加载失败') }
  finally { ordersLoading.value = false }
}

const orderStatusClass = (code: string) => {
  if (code === 'PAID')    return 'bg-green-500/20 text-green-400 border-green-500/20'
  if (code === 'PENDING') return 'bg-yellow-500/20 text-yellow-400 border-yellow-500/20'
  if (code === 'EXPIRED') return 'bg-gray-500/20 text-gray-400 border-gray-500/20'
  if (code === 'FAILED')  return 'bg-red-500/20 text-red-400 border-red-500/20'
  return 'bg-slate-600/20 text-slate-400 border-slate-500/20'
}
const formatDate = (d: string) => d ? d.replace('T', ' ').substring(0, 16) : ''

// ========================= 我的创作 =========================
const creations = ref<UserCreation[]>([])
const creationsLoading = ref(false)
const isUploading = ref(false)

const loadCreations = async () => {
  creationsLoading.value = true
  try {
    const res = await getMyCreations()
    if (res.code === 0) creations.value = res.data || []
  } catch { toast.error('创作加载失败') }
  finally { creationsLoading.value = false }
}

const handleDeleteCreation = async (id: number) => {
  const ok = await modalStore.open({ title: '删除创作', content: '此操作不可恢复，确定删除？', type: 'error', confirmText: '确认删除' })
  if (!ok) return
  try {
    await deleteCreation(id)
    creations.value = creations.value.filter(c => c.id !== id)
    toast.success('已删除')
  } catch (err: any) { toast.error(err?.message || '删除失败') }
}

const handleToggleVisibility = async (c: UserCreation) => {
  const nv = c.visibility === 1 ? 0 : 1
  try {
    await updateCreationVisibility(c.id, nv as 0 | 1)
    c.visibility = nv as 0 | 1
    toast.success(nv === 1 ? '已设为公开' : '已设为私密')
  } catch (err: any) { toast.error(err?.message || '操作失败') }
}

const openDownload = async (id: number, title?: string) => {
  try {
    await downloadCreation(id, title)
  } catch (err: any) {
    toast.error(err?.message || '下载失败')
  }
}

const triggerUpload = () => {  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*,video/*,audio/*'
  input.onchange = async (e: Event) => {
    const file = (e.target as HTMLInputElement).files?.[0]
    if (!file) return
    isUploading.value = true
    try {
      const res = await uploadCreation(file, { visibility: 0 })
      const creation = (res.data as any)?.data
      if (creation) { creations.value.unshift(creation); toast.success('上传成功') }
    } catch (err: any) { toast.error(err?.message || '上传失败') }
    finally { isUploading.value = false }
  }
  input.click()
}

// ========================= 数字钱包 =========================
const wallet = ref<WalletInfo | null>(null)
const transactions = ref<PointTransaction[]>([])
const walletLoading = ref(false)
const isSigningIn = ref(false)
const showPaymentModal = ref(false)

const loadWallet = async () => {
  walletLoading.value = true
  try {
    const [wr, tr] = await Promise.all([getWallet(), getTransactions(20)])
    if (wr.code === 0) wallet.value = wr.data
    if (tr.code === 0) transactions.value = tr.data || []
  } catch { toast.error('钱包数据加载失败') }
  finally { walletLoading.value = false }
}

const handleSignIn = async () => {
  isSigningIn.value = true
  try {
    const res = await dailySignIn()
    if (res.code === 0) { toast.success(res.data?.message || '签到成功'); await loadWallet() }
    else toast.warning(res.message || '今日已签到')
  } catch (err: any) { toast.warning(err?.message || '今日已签到') }
  finally { isSigningIn.value = false }
}

const handlePaymentPaid = async (pts: number) => {
  toast.success(`充值成功！+${pts.toLocaleString()} 灵感点数已到账`)
  // 同时刷新钱包余额和订单列表（无论当前在哪个 tab 都同步刷新）
  await Promise.all([loadWallet(), loadOrders()])
}

const formattedBalance = computed(() => wallet.value ? wallet.value.balance.toLocaleString() : '0')

// ========================= 安全设置 =========================
const pwdForm = reactive({ current: '', newPwd: '', confirm: '' })
const pwdLoading = ref(false)
const showPwd = reactive({ current: false, newPwd: false, confirm: false })
const oauthBindings = ref<OAuthBinding[]>([])

const loadSecurityData = async () => {
  try {
    const res = await getOAuthBindings()
    if (res.code === 0) oauthBindings.value = res.data || []
  } catch { /* ignore */ }
}

const getBinding = (p: string) => oauthBindings.value.find(b => b.provider === p)

const handleChangePassword = async () => {
  if (!pwdForm.current || !pwdForm.newPwd || !pwdForm.confirm) { toast.error('请填写所有密码字段'); return }
  if (pwdForm.newPwd !== pwdForm.confirm) { toast.error('两次新密码不一致'); return }
  if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/.test(pwdForm.newPwd)) {
    toast.error('新密码需8位以上，包含大小写字母及数字'); return
  }
  pwdLoading.value = true
  try {
    const res = await changePassword({ currentPassword: pwdForm.current, newPassword: pwdForm.newPwd, confirmPassword: pwdForm.confirm })
    if (res.code === 0) {
      toast.success('密码修改成功，即将重新登录')
      pwdForm.current = pwdForm.newPwd = pwdForm.confirm = ''
      setTimeout(() => authStore.logout(), 2000)
    } else toast.error(res.message || '修改失败')
  } catch (err: any) { toast.error(err?.message || '修改密码失败') }
  finally { pwdLoading.value = false }
}

const handleOAuthBind = async (provider: 'github' | 'discord') => {
  try {
    const res = await getOAuthAuthUrl(provider)
    if (res.code === 0 && res.data?.authUrl) window.location.href = res.data.authUrl
    else toast.error(res.message || `${provider} OAuth未配置`)
  } catch (err: any) { toast.error(err?.message || 'OAuth服务暂不可用') }
}

const handleOAuthUnbind = async (provider: string) => {
  const ok = await modalStore.open({ title: '解除绑定', content: `确定解除 ${provider} 绑定？`, type: 'info', confirmText: '确认解绑' })
  if (!ok) return
  try {
    await unbindOAuth(provider)
    oauthBindings.value = oauthBindings.value.filter(b => b.provider !== provider)
    toast.success('已解除绑定')
  } catch (err: any) { toast.error(err?.message || '解绑失败') }
}

// ========================= Tab 切换 & 初始加载 =========================
const switchTab = async (tab: TabType) => {
  activeTab.value = tab
  if (tab === 'orders' && orders.value.length === 0) loadOrders()
  if (tab === 'creations' && creations.value.length === 0) loadCreations()
  if (tab === 'wallet' && !wallet.value) loadWallet()
  if (tab === 'security') loadSecurityData()
}

onMounted(async () => {
  isLoading.value = true
  await loadProfile()
  isLoading.value = false
})
</script>

<template>
  <div class="container mx-auto px-4 py-8">
    <div class="grid grid-cols-1 md:grid-cols-12 gap-8">

      <!-- 左侧导航 -->
      <div class="md:col-span-4 lg:col-span-3">
        <div class="bg-slate-800 rounded-2xl p-6 border border-white/5 text-center sticky top-24">
          <!-- 头像 -->
          <div class="relative w-24 h-24 mx-auto mb-4">
            <div class="w-24 h-24 rounded-full bg-gradient-to-br from-neon-blue to-neon-purple p-0.5">
              <img
                :src="avatarPreview || authStore.user?.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=default'"
                class="w-full h-full rounded-full bg-slate-900 object-cover"
                alt="avatar"
              />
            </div>
            <label
              v-if="activeTab === 'profile'"
              class="absolute bottom-0 right-0 w-7 h-7 bg-neon-blue rounded-full flex items-center justify-center cursor-pointer hover:scale-110 transition-transform z-10"
              title="更换头像"
            >
              <Camera class="w-3.5 h-3.5 text-slate-900" />
              <input type="file" class="hidden" accept="image/jpeg,image/png,image/gif,image/webp" @change="onAvatarChange" />
            </label>
            <div v-if="isUploadingAvatar" class="absolute inset-0 rounded-full bg-black/60 flex items-center justify-center">
              <div class="animate-spin w-5 h-5 border-2 border-neon-blue border-t-transparent rounded-full"></div>
            </div>
          </div>

          <h2 class="text-xl font-bold text-white">{{ authStore.user?.nickname }}</h2>
          <p class="text-neon-yellow text-sm mt-1 uppercase tracking-widest">{{ authStore.user?.level || 'MEMBER' }}</p>

          <div class="mt-8 flex flex-col gap-1">
            <button
              v-for="tab in [
                { key: 'profile', icon: User, label: '个人资料' },
                { key: 'orders', icon: Package, label: '我的订单' },
                { key: 'creations', icon: Image, label: '我的创作' },
                { key: 'wallet', icon: CreditCard, label: '数字钱包' },
                { key: 'security', icon: ShieldCheck, label: '安全设置' }
              ]"
              :key="tab.key"
              @click="switchTab(tab.key as TabType)"
              class="flex items-center gap-3 px-4 py-3 rounded-xl transition-all text-left"
              :class="activeTab === tab.key
                ? 'bg-neon-blue text-slate-900 font-bold shadow-[0_0_12px_rgba(0,243,255,0.2)]'
                : 'hover:bg-white/5 text-gray-400'"
            >
              <component :is="tab.icon" class="w-5 h-5 shrink-0" />{{ tab.label }}
            </button>
          </div>
        </div>
      </div>

      <!-- 右侧内容 -->
      <div class="md:col-span-8 lg:col-span-9">

        <div v-if="isLoading" class="flex justify-center py-20">
          <div class="animate-spin w-8 h-8 border-2 border-neon-blue border-t-transparent rounded-full"></div>
        </div>

        <!-- 个人资料 -->
        <div v-else-if="activeTab === 'profile'" class="bg-slate-800 rounded-2xl p-8 border border-white/5">
          <h3 class="text-xl font-bold text-white mb-6 flex items-center gap-2">
            <Settings class="w-5 h-5 text-neon-blue" /> 编辑资料
          </h3>
          <div class="space-y-6 max-w-2xl">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div class="space-y-2">
                <label class="text-gray-400 text-sm">昵称</label>
                <input v-model="formData.nickname" type="text" maxlength="32" placeholder="请输入昵称"
                  class="w-full bg-slate-900 border border-slate-700 rounded-lg px-4 py-3 text-white focus:border-neon-blue focus:outline-none transition-colors" />
              </div>
              <div class="space-y-2">
                <label class="text-gray-400 text-sm">手机号</label>
                <input v-model="formData.phone" type="text" placeholder="输入完整手机号可修改绑定"
                  class="w-full bg-slate-900 border border-slate-700 rounded-lg px-4 py-3 text-white focus:border-neon-blue focus:outline-none transition-colors" />
                <p class="text-xs text-gray-500">脱敏显示；填写完整11位手机号可更新绑定</p>
              </div>
            </div>
            <div class="space-y-2">
              <label class="text-gray-400 text-sm">个人简介</label>
              <textarea v-model="formData.bio" rows="4" maxlength="500" placeholder="介绍一下你自己..."
                class="w-full bg-slate-900 border border-slate-700 rounded-lg px-4 py-3 text-white focus:border-neon-blue focus:outline-none resize-none transition-colors"></textarea>
              <p class="text-xs text-gray-500 text-right">{{ formData.bio.length }}/500</p>
            </div>
            <div class="pt-4 border-t border-white/5 flex justify-end">
              <button @click="handleSaveProfile" :disabled="isSaving"
                class="flex items-center gap-2 px-6 py-3 bg-neon-blue text-slate-900 font-bold rounded-lg hover:shadow-[0_0_15px_rgba(0,243,255,0.3)] transition-all disabled:opacity-50 disabled:cursor-not-allowed">
                <Save class="w-4 h-4" />{{ isSaving ? '保存中...' : '保存修改' }}
              </button>
            </div>
          </div>
        </div>

        <!-- 我的订单 -->
        <div v-else-if="activeTab === 'orders'" class="space-y-4">
          <div class="flex items-center justify-between">
            <h3 class="text-xl font-bold text-white flex items-center gap-2">
              <Package class="w-5 h-5 text-neon-purple" /> 订单记录
            </h3>
            <button @click="loadOrders" class="text-sm text-gray-400 hover:text-neon-blue flex items-center gap-1 transition-colors">
              <RefreshCw class="w-4 h-4" /> 刷新
            </button>
          </div>
          <div v-if="ordersLoading" class="flex justify-center py-12">
            <div class="animate-spin w-6 h-6 border-2 border-neon-purple border-t-transparent rounded-full"></div>
          </div>
          <div v-else-if="orders.length === 0" class="py-16 text-center text-gray-500 bg-slate-800 rounded-2xl border border-white/5">
            <Package class="w-12 h-12 mx-auto mb-3 opacity-30" /><p>暂无订单记录</p>
          </div>
          <div v-else class="space-y-3">
            <div v-for="order in orders" :key="order.orderType + order.id"
              class="flex items-center justify-between p-4 bg-slate-800 rounded-2xl border border-white/5 hover:border-white/10 transition-colors gap-4">
              <!-- 图标 -->
              <div class="w-11 h-11 rounded-xl flex items-center justify-center shrink-0 text-lg"
                :class="order.orderType === 'RECHARGE'
                  ? (order.payMethod === '微信支付' ? 'bg-[#07C160]/15' : 'bg-[#1677FF]/15')
                  : 'bg-yellow-500/15'">
                <span v-if="order.orderType === 'RECHARGE'">
                  {{ order.payMethod === '微信支付' ? '💬' : '💳' }}
                </span>
                <span v-else>👑</span>
              </div>
              <!-- 主信息 -->
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-2 mb-0.5">
                  <h4 class="font-bold text-white text-sm truncate">{{ order.itemName }}</h4>
                  <!-- 类型小标 -->
                  <span class="shrink-0 text-[10px] px-1.5 py-0.5 rounded font-medium"
                    :class="order.orderType === 'RECHARGE' ? 'bg-neon-yellow/10 text-neon-yellow' : 'bg-yellow-500/10 text-yellow-400'">
                    {{ order.orderType === 'RECHARGE' ? '充値' : 'VIP' }}
                  </span>
                </div>
                <p class="text-xs text-gray-500 truncate">
                  {{ order.orderNo }}
                  <span v-if="order.payMethod" class="ml-1 text-gray-600">· {{ order.payMethod }}</span>
                </p>
                <p class="text-xs text-gray-600 mt-0.5">{{ formatDate(order.createdAt) }}</p>
              </div>
              <!-- 金额 + 状态 -->
              <div class="text-right shrink-0">
                <p class="font-bold font-mono"
                  :class="order.orderType === 'RECHARGE' ? 'text-neon-yellow' : 'text-neon-blue'">
                  {{ order.orderType === 'RECHARGE' ? '+' + (order.points ?? 0).toLocaleString() + ' pts' : '¥' + order.amount }}
                </p>
                <p class="text-xs text-gray-500 font-mono mb-1">
                  {{ order.orderType === 'RECHARGE' ? '¥' + order.amount : '' }}
                </p>
                <span class="text-xs px-2 py-0.5 rounded-full border" :class="orderStatusClass(order.statusCode)">
                  {{ order.statusText }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- 我的创作 -->
        <div v-else-if="activeTab === 'creations'" class="bg-slate-800 rounded-2xl p-8 border border-white/5">
          <div class="flex justify-between items-center mb-6">
            <h3 class="text-xl font-bold text-white flex items-center gap-2">
              <Image class="w-5 h-5 text-neon-purple" /> 创作档案
            </h3>
            <button @click="loadCreations" class="text-sm text-gray-400 hover:text-neon-blue flex items-center gap-1 transition-colors">
              <RefreshCw class="w-4 h-4" /> 刷新
            </button>
          </div>
          <div v-if="creationsLoading" class="flex justify-center py-12">
            <div class="animate-spin w-6 h-6 border-2 border-neon-purple border-t-transparent rounded-full"></div>
          </div>
          <div v-else class="grid grid-cols-2 md:grid-cols-3 gap-4">
            <div v-for="creation in creations" :key="creation.id"
              class="group relative aspect-square bg-slate-900 rounded-xl overflow-hidden border border-white/5 cursor-pointer">
              <img :src="creation.coverUrl || creation.fileUrl" class="w-full h-full object-cover transition-transform group-hover:scale-110" :alt="creation.title"
                @error="($event.target as HTMLImageElement).src = 'https://api.dicebear.com/7.x/shapes/svg?seed=' + creation.id" />
              <div class="absolute inset-0 bg-black/65 opacity-0 group-hover:opacity-100 transition-opacity flex flex-col justify-center items-center gap-3">
                <span class="text-xs font-mono text-neon-green">{{ creation.createdAt.substring(0,10).replace(/-/g, '.') }}</span>
                <div class="flex gap-2">
                  <button @click.stop="openDownload(creation.id, creation.title)"
                    class="p-2 bg-white/10 rounded-full hover:bg-neon-blue hover:text-slate-900 transition-colors" title="下载">
                    <Download class="w-4 h-4" />
                  </button>
                  <button @click.stop="handleToggleVisibility(creation)"
                    class="p-2 bg-white/10 rounded-full hover:bg-neon-yellow hover:text-slate-900 transition-colors"
                    :title="creation.visibility === 1 ? '设为私密' : '设为公开'">
                    <component :is="creation.visibility === 1 ? Eye : EyeOff" class="w-4 h-4" />
                  </button>
                  <button @click.stop="handleDeleteCreation(creation.id)"
                    class="p-2 bg-white/10 rounded-full hover:bg-red-500 hover:text-white transition-colors" title="删除">
                    <Trash2 class="w-4 h-4" />
                  </button>
                </div>
              </div>
              <div class="absolute top-2 right-2 px-2 py-0.5 rounded text-[10px] border border-white/10"
                :class="creation.visibility === 1 ? 'bg-neon-blue/20 text-neon-blue' : 'bg-black/50 text-gray-300 backdrop-blur'">
                {{ creation.visibility === 1 ? '公开' : '私密' }}
              </div>
            </div>
            <div @click="triggerUpload" :class="{ 'opacity-50 cursor-wait': isUploading }"
              class="aspect-square rounded-xl border-2 border-dashed border-white/10 flex flex-col items-center justify-center text-gray-500 hover:border-neon-blue/50 hover:text-neon-blue transition-colors cursor-pointer">
              <div v-if="isUploading" class="animate-spin w-8 h-8 border-2 border-neon-blue border-t-transparent rounded-full mb-2"></div>
              <Plus v-else class="w-8 h-8 mb-2" />
              <span class="text-xs font-bold">{{ isUploading ? '上传中...' : '新建创作' }}</span>
            </div>
          </div>
        </div>

        <!-- 数字钱包 -->
        <div v-else-if="activeTab === 'wallet'" class="space-y-6">
          <div v-if="walletLoading" class="flex justify-center py-20">
            <div class="animate-spin w-6 h-6 border-2 border-neon-yellow border-t-transparent rounded-full"></div>
          </div>
          <template v-else>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
              <!-- 余额卡片 -->
              <div class="bg-gradient-to-br from-indigo-900 via-slate-900 to-violet-900/50 rounded-2xl p-6 border border-white/10 relative overflow-hidden">
                <div class="absolute top-0 right-0 p-4 opacity-10 pointer-events-none"><Zap class="w-32 h-32 text-neon-yellow" /></div>
                <!-- 余额 -->
                <p class="text-gray-400 text-sm mb-1">灵感点数 (Inspo Points)</p>
                <div class="text-4xl font-mono font-bold text-neon-yellow mb-5 flex items-end gap-2">
                  {{ formattedBalance }}
                  <span class="text-sm text-gray-400 font-normal mb-1">pts</span>
                </div>
                <!-- 操作按钮 -->
                <div class="flex flex-col gap-2">
                  <!-- 签到 -->
                  <button @click="handleSignIn" :disabled="isSigningIn"
                    class="flex items-center gap-2 px-4 py-2 bg-white/8 text-white rounded-xl text-sm hover:bg-white/15 transition-all disabled:opacity-50 border border-white/10 hover:border-white/20 w-fit">
                    <Zap class="w-4 h-4 text-neon-yellow" />
                    {{ isSigningIn ? '签到中...' : '每日签到 +50 pts' }}
                  </button>
                  <!-- 充值按钮（美化） -->
                  <button
                    @click="showPaymentModal = true"
                    class="group relative overflow-hidden flex items-center gap-2.5 px-5 py-2.5 rounded-xl text-sm font-bold text-slate-900 transition-all duration-300 hover:scale-[1.02] active:scale-[0.98] w-fit"
                    style="background: linear-gradient(135deg, #facc15 0%, #f59e0b 50%, #10b981 100%); box-shadow: 0 4px 20px rgba(250,204,21,0.35);"
                  >
                    <!-- 闪光动画层 -->
                    <span class="absolute inset-0 bg-white/20 translate-x-[-100%] group-hover:translate-x-[100%] skew-x-[-20deg] transition-transform duration-700 pointer-events-none"></span>
                    <CreditCard class="w-4 h-4 relative z-10" />
                    <span class="relative z-10">立即充值</span>
                    <ArrowUpRight class="w-3.5 h-3.5 relative z-10 group-hover:translate-x-0.5 group-hover:-translate-y-0.5 transition-transform" />
                  </button>
                </div>
              </div>
              <!-- 月度消耗卡片 -->
              <div class="bg-slate-800 rounded-2xl p-6 border border-white/5 flex flex-col justify-center">
                <div class="flex justify-between items-center mb-4">
                  <span class="text-gray-400 text-sm">本月消耗</span>
                  <span class="text-white font-bold">-{{ wallet?.monthlySpent ?? 0 }} pts</span>
                </div>
                <div class="w-full h-2 bg-slate-700 rounded-full overflow-hidden mb-3">
                  <div class="h-full bg-neon-blue rounded-full transition-all duration-700" :style="{ width: (wallet?.monthlyProgress ?? 0) + '%' }"></div>
                </div>
                <p class="text-xs text-gray-500">
                  距下个月度礼包还需消耗 <span class="text-neon-blue font-bold">{{ wallet?.monthlyRemaining ?? 1000 }}</span> pts
                </p>
              </div>
            </div>
            <div class="bg-slate-800 rounded-2xl p-8 border border-white/5">
              <h3 class="text-xl font-bold text-white mb-6">收支明细</h3>
              <div v-if="transactions.length === 0" class="text-center text-gray-500 py-8">暂无记录</div>
              <div v-else class="divide-y divide-white/5">
                <div v-for="tx in transactions" :key="tx.id" class="flex justify-between items-center py-4">
                  <div class="flex items-center gap-4">
                    <div class="w-10 h-10 rounded-full flex items-center justify-center shrink-0"
                      :class="tx.amount > 0 ? 'bg-green-500/10 text-green-400' : 'bg-red-500/10 text-red-400'">
                      <component :is="tx.amount > 0 ? ArrowDownLeft : ArrowUpRight" class="w-5 h-5" />
                    </div>
                    <div>
                      <p class="text-white font-medium">{{ tx.description }}</p>
                      <p class="text-xs text-gray-500">{{ formatDate(tx.createdAt) }}</p>
                    </div>
                  </div>
                  <span class="font-mono font-bold shrink-0 ml-4" :class="tx.amount > 0 ? 'text-neon-yellow' : 'text-white'">
                    {{ tx.amount > 0 ? '+' : '' }}{{ tx.amount }}
                  </span>
                </div>
              </div>
            </div>
          </template>
        </div>

        <!-- 安全设置 -->
        <div v-else-if="activeTab === 'security'" class="bg-slate-800 rounded-2xl p-8 border border-white/5">
          <h3 class="text-xl font-bold text-white mb-6 flex items-center gap-2">
            <ShieldCheck class="w-5 h-5 text-green-400" /> 账号安全
          </h3>
          <div class="space-y-10 max-w-2xl">
            <div class="space-y-4">
              <h4 class="text-white font-bold border-l-4 border-neon-blue pl-3">修改密码</h4>
              <div class="grid gap-4">
                <div class="relative">
                  <input v-model="pwdForm.current" :type="showPwd.current ? 'text' : 'password'" placeholder="当前密码"
                    class="w-full bg-slate-900 border border-slate-700 rounded-lg px-4 py-3 pr-12 text-white focus:border-neon-blue focus:outline-none transition-colors" />
                  <button class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-white" @click="showPwd.current = !showPwd.current">
                    <component :is="showPwd.current ? EyeOff : Eye" class="w-5 h-5" />
                  </button>
                </div>
                <div class="relative">
                  <input v-model="pwdForm.newPwd" :type="showPwd.newPwd ? 'text' : 'password'" placeholder="新密码 (8位以上含大小写字母及数字)"
                    class="w-full bg-slate-900 border border-slate-700 rounded-lg px-4 py-3 pr-12 text-white focus:border-neon-blue focus:outline-none transition-colors" />
                  <button class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-white" @click="showPwd.newPwd = !showPwd.newPwd">
                    <component :is="showPwd.newPwd ? EyeOff : Eye" class="w-5 h-5" />
                  </button>
                </div>
                <div class="relative">
                  <input v-model="pwdForm.confirm" :type="showPwd.confirm ? 'text' : 'password'" placeholder="确认新密码"
                    class="w-full bg-slate-900 border border-slate-700 rounded-lg px-4 py-3 pr-12 text-white focus:border-neon-blue focus:outline-none transition-colors"
                    :class="{ 'border-red-500 focus:border-red-500': pwdForm.confirm && pwdForm.newPwd !== pwdForm.confirm }" />
                  <button class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-white" @click="showPwd.confirm = !showPwd.confirm">
                    <component :is="showPwd.confirm ? EyeOff : Eye" class="w-5 h-5" />
                  </button>
                </div>
                <p v-if="pwdForm.confirm && pwdForm.newPwd !== pwdForm.confirm" class="text-xs text-red-400 mt-1">两次密码不一致</p>
              </div>
              <button @click="handleChangePassword" :disabled="pwdLoading"
                class="px-6 py-2 bg-white/5 hover:bg-white/10 text-white rounded-lg border border-white/10 transition-all disabled:opacity-50 flex items-center gap-2">
                <Save class="w-4 h-4" />{{ pwdLoading ? '更新中...' : '更新密码' }}
              </button>
            </div>
            <div class="w-full h-px bg-white/5"></div>
            <div class="space-y-4">
              <h4 class="text-white font-bold border-l-4 border-neon-purple pl-3">第三方绑定</h4>
              <!-- Discord -->
              <div class="flex items-center justify-between p-4 bg-slate-900 rounded-xl border border-white/5">
                <div class="flex items-center gap-3">
                  <div class="w-9 h-9 rounded-full bg-[#5865F2] flex items-center justify-center text-white shrink-0"><Gamepad2 class="w-5 h-5" /></div>
                  <div>
                    <p class="text-sm font-bold text-white">Discord</p>
                    <p v-if="getBinding('discord')" class="text-xs text-green-400">已连接: {{ getBinding('discord')?.providerUsername }}</p>
                    <p v-else class="text-xs text-gray-500">未连接</p>
                  </div>
                </div>
                <button v-if="getBinding('discord')" @click="handleOAuthUnbind('discord')"
                  class="text-xs text-gray-400 hover:text-white border border-white/10 px-3 py-1.5 rounded flex items-center gap-1 transition-colors">
                  <Unlink class="w-3 h-3" /> 解绑
                </button>
                <button v-else @click="handleOAuthBind('discord')"
                  class="text-xs text-neon-blue hover:text-white border border-neon-blue/30 hover:border-neon-blue px-3 py-1.5 rounded flex items-center gap-1 transition-colors">
                  <Link class="w-3 h-3" /> 连接
                </button>
              </div>
              <!-- GitHub -->
              <div class="flex items-center justify-between p-4 bg-slate-900 rounded-xl border border-white/5">
                <div class="flex items-center gap-3">
                  <div class="w-9 h-9 rounded-full bg-white flex items-center justify-center text-black shrink-0"><Github class="w-5 h-5" /></div>
                  <div>
                    <p class="text-sm font-bold text-white">GitHub</p>
                    <p v-if="getBinding('github')" class="text-xs text-green-400">已连接: {{ getBinding('github')?.providerUsername }}</p>
                    <p v-else class="text-xs text-gray-500">未连接</p>
                  </div>
                </div>
                <button v-if="getBinding('github')" @click="handleOAuthUnbind('github')"
                  class="text-xs text-gray-400 hover:text-white border border-white/10 px-3 py-1.5 rounded flex items-center gap-1 transition-colors">
                  <Unlink class="w-3 h-3" /> 解绑
                </button>
                <button v-else @click="handleOAuthBind('github')"
                  class="text-xs text-neon-blue hover:text-white border border-neon-blue/30 hover:border-neon-blue px-3 py-1.5 rounded flex items-center gap-1 transition-colors">
                  <Link class="w-3 h-3" /> 连接
                </button>
              </div>
            </div>
          </div>
        </div>

      </div>
    </div>

    <!-- 充值支付弹窗（保持在根 div 内，避免 Fragment 破坏 transition） -->
    <!-- PaymentModal 内部使用 <Teleport to="body">，实际渲染在 body 层，不影响布局 -->
    <PaymentModal
      v-if="showPaymentModal"
      @close="showPaymentModal = false"
      @paid="handlePaymentPaid"
    />
  </div>
</template>

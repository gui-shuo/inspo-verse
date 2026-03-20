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
import { getMyCreations, uploadCreation, deleteCreation, updateCreationVisibility, downloadCreation, type UserCreation } from '@/api/creation'
import { getWallet, getTransactions, dailySignIn, rechargePoints, type WalletInfo, type PointTransaction } from '@/api/wallet'
import { getOAuthBindings, getOAuthAuthUrl, unbindOAuth, type OAuthBinding } from '@/api/security'

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
      const res = await uploadAvatar(avatarFile.value)
      const url = (res.data as any)?.data?.avatarUrl
      if (url && authStore.user) authStore.user.avatar = url
      avatarFile.value = null
      isUploadingAvatar.value = false
    }
    const phoneToSend = /\*/.test(formData.phone) ? undefined : formData.phone || undefined
    await updateProfile({ nickname: formData.nickname, bio: formData.bio, phone: phoneToSend })
    // 从后端重新拉取最新用户信息，同步 store 和 localStorage（确保右上角头像实时更新）
    await authStore.fetchUserInfo()
    toast.success('个人资料已更新')
  } catch (err: any) {
    toast.error(err?.message || '保存失败，请稍后重试')
  } finally {
    isSaving.value = false
  }
}

// ========================= 我的订单 =========================
const orders = ref<any[]>([])
const ordersLoading = ref(false)

const loadOrders = async () => {
  ordersLoading.value = true
  try {
    const res = await getMyOrders()
    if (res.code === 0) orders.value = (res.data || []) as any[]
  } catch { toast.error('订单加载失败') }
  finally { ordersLoading.value = false }
}

const orderStatusClass = (s: number) => {
  if (s === 1) return 'bg-green-500/20 text-green-400'
  if (s === 2) return 'bg-red-500/20 text-red-400'
  if (s === 3) return 'bg-gray-500/20 text-gray-400'
  return 'bg-yellow-500/20 text-yellow-400'
}
const orderStatusText = (s: number) => (['待支付','已完成','已失败','已退款'][s] ?? '处理中')
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
const isRecharging = ref(false)
const rechargeAmount = ref(100)

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

const handleRecharge = async () => {
  if (rechargeAmount.value < 10) { toast.error('最少充值10点数'); return }
  isRecharging.value = true
  try {
    const res = await rechargePoints(rechargeAmount.value)
    if (res.code === 0) {
      if (wallet.value && res.data) wallet.value.balance = res.data.balance
      toast.success(`充值成功，获得${rechargeAmount.value}灵感点数`)
      await loadWallet()
    }
  } catch (err: any) { toast.error(err?.message || '充值失败') }
  finally { isRecharging.value = false }
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
        <div v-else-if="activeTab === 'orders'" class="bg-slate-800 rounded-2xl p-8 border border-white/5">
          <h3 class="text-xl font-bold text-white mb-6 flex items-center gap-2">
            <Package class="w-5 h-5 text-neon-purple" /> 订单记录
          </h3>
          <div v-if="ordersLoading" class="flex justify-center py-12">
            <div class="animate-spin w-6 h-6 border-2 border-neon-purple border-t-transparent rounded-full"></div>
          </div>
          <div v-else-if="orders.length === 0" class="py-16 text-center text-gray-500">
            <Package class="w-12 h-12 mx-auto mb-3 opacity-30" /><p>暂无订单记录</p>
          </div>
          <div v-else class="space-y-3">
            <div v-for="order in orders" :key="order.id"
              class="flex items-center justify-between p-4 bg-slate-900 rounded-xl border border-white/5 hover:border-white/10 transition-colors">
              <div class="flex items-center gap-4">
                <div class="w-12 h-12 rounded-lg bg-slate-800 flex items-center justify-center shrink-0">
                  <Package class="w-6 h-6 text-gray-400" />
                </div>
                <div>
                  <h4 class="font-bold text-white">{{ order.itemName || '套餐订单' }}</h4>
                  <p class="text-xs text-gray-500">{{ order.orderNo }} • {{ formatDate(order.createdAt) }}</p>
                </div>
              </div>
              <div class="text-right shrink-0 ml-4">
                <p class="font-bold text-neon-blue">¥ {{ order.amount }}</p>
                <span class="text-xs px-2 py-0.5 rounded-full" :class="orderStatusClass(order.payStatus)">
                  {{ orderStatusText(order.payStatus) }}
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
              <div class="bg-gradient-to-br from-indigo-900 to-slate-900 rounded-2xl p-6 border border-white/10 relative overflow-hidden">
                <div class="absolute top-0 right-0 p-4 opacity-10 pointer-events-none"><Zap class="w-32 h-32 text-neon-yellow" /></div>
                <p class="text-gray-400 text-sm mb-2">灵感点数 (Inspo Points)</p>
                <div class="text-4xl font-mono font-bold text-neon-yellow mb-4">{{ formattedBalance }}</div>
                <div class="flex gap-3 flex-wrap">
                  <button @click="handleSignIn" :disabled="isSigningIn"
                    class="px-4 py-2 bg-white/10 text-white rounded-lg text-sm hover:bg-white/20 transition-all disabled:opacity-50 flex items-center gap-1">
                    <Zap class="w-4 h-4 text-neon-yellow" />{{ isSigningIn ? '签到中...' : '每日签到' }}
                  </button>
                  <div class="flex items-center gap-2">
                    <input v-model.number="rechargeAmount" type="number" min="10"
                      class="w-20 bg-slate-900/50 border border-white/20 rounded px-2 py-1.5 text-white text-sm text-center focus:outline-none focus:border-neon-yellow" placeholder="点数" />
                    <button @click="handleRecharge" :disabled="isRecharging"
                      class="px-4 py-2 bg-neon-yellow text-slate-900 font-bold rounded-lg text-sm hover:shadow-lg transition-all disabled:opacity-50">
                      {{ isRecharging ? '充值中...' : '立即充值' }}
                    </button>
                  </div>
                </div>
              </div>
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
  </div>
</template>

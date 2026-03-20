<script setup lang="ts">
import { ref, computed, onUnmounted } from 'vue'
import { X, Wallet, CheckCircle2, Clock, AlertCircle, ChevronRight, Smartphone } from 'lucide-vue-next'
import {
  RECHARGE_PACKAGES,
  createPaymentOrder,
  queryPaymentStatus,
  mockConfirmPayment,
  type PaymentPackage,
  type CreateOrderResp,
} from '@/api/wallet'

// ── Props / Emit ──────────────────────────────────────────────────────────────
const emit = defineEmits<{
  (e: 'close'): void
  (e: 'paid', points: number): void
}>()

// ── 步骤状态 ──────────────────────────────────────────────────────────────────
// step: 'select' → 选择套餐和支付方式
//       'qr'     → 显示二维码，等待扫码
//       'success'→ 支付成功
//       'failed' → 支付失败/超时
type Step = 'select' | 'qr' | 'success' | 'failed'
const step = ref<Step>('select')

// ── 选择态 ────────────────────────────────────────────────────────────────────
const selectedPkg  = ref<PaymentPackage>(RECHARGE_PACKAGES[2]) // 默认超值包
const selectedMethod = ref<'ALIPAY' | 'WECHAT'>('WECHAT')
const creating = ref(false)

// ── 订单态 ────────────────────────────────────────────────────────────────────
const order = ref<CreateOrderResp | null>(null)
const isMock = ref(false)
const mockConfirming = ref(false)

// ── 倒计时 ────────────────────────────────────────────────────────────────────
const remainSeconds = ref(300) // 5 分钟
let countdownTimer: ReturnType<typeof setInterval> | null = null

const countdownText = computed(() => {
  const m = Math.floor(remainSeconds.value / 60).toString().padStart(2, '0')
  const s = (remainSeconds.value % 60).toString().padStart(2, '0')
  return `${m}:${s}`
})

const startCountdown = () => {
  remainSeconds.value = 300
  countdownTimer = setInterval(() => {
    remainSeconds.value--
    if (remainSeconds.value <= 0) {
      stopCountdown()
      step.value = 'failed'
      stopPolling()
    }
  }, 1000)
}

const stopCountdown = () => {
  if (countdownTimer) { clearInterval(countdownTimer); countdownTimer = null }
}

// ── 状态轮询 ──────────────────────────────────────────────────────────────────
let pollingTimer: ReturnType<typeof setInterval> | null = null

const startPolling = (orderNo: string) => {
  pollingTimer = setInterval(async () => {
    try {
      const res = await queryPaymentStatus(orderNo)
      if (res.code !== 0) return
      if (res.data?.status === 'PAID') {
        stopPolling()
        stopCountdown()
        step.value = 'success'
        emit('paid', order.value?.points ?? 0)
      } else if (res.data?.status === 'EXPIRED' || res.data?.status === 'FAILED') {
        stopPolling()
        stopCountdown()
        step.value = 'failed'
      }
    } catch { /* 忽略轮询错误 */ }
  }, 2000)
}

const stopPolling = () => {
  if (pollingTimer) { clearInterval(pollingTimer); pollingTimer = null }
}

// ── 操作函数 ──────────────────────────────────────────────────────────────────
const handleCreate = async () => {
  creating.value = true
  try {
    const res = await createPaymentOrder(selectedPkg.value.id, selectedMethod.value)
    if (res.code !== 0) { alert(res.message || '创建订单失败'); return }
    order.value = res.data
    isMock.value = res.data?.mockMode ?? false
    step.value = 'qr'
    startCountdown()
    startPolling(res.data!.orderNo)
  } catch (e: any) {
    alert(e?.message || '网络错误，请重试')
  } finally {
    creating.value = false
  }
}

const handleMockConfirm = async () => {
  if (!order.value) return
  mockConfirming.value = true
  try {
    await mockConfirmPayment(order.value.orderNo)
    // 轮询会检测到 PAID 状态，不用在这里手动切换
  } catch (e: any) {
    alert(e?.message || '模拟支付失败')
  } finally {
    mockConfirming.value = false
  }
}

const handleClose = () => {
  stopCountdown()
  stopPolling()
  emit('close')
}

const handleRetry = () => {
  stopCountdown()
  stopPolling()
  order.value = null
  step.value = 'select'
}

onUnmounted(() => {
  stopCountdown()
  stopPolling()
})

// ── 颜色映射 ──────────────────────────────────────────────────────────────────
const methodConfig = {
  WECHAT: {
    label: '微信支付',
    icon: '微信',
    bg: 'bg-[#07C160]',
    border: 'border-[#07C160]',
    ring: 'ring-[#07C160]/40',
    text: 'text-[#07C160]',
    shadow: 'shadow-[0_0_20px_rgba(7,193,96,0.4)]',
    emoji: '💚',
  },
  ALIPAY: {
    label: '支付宝',
    icon: '支付宝',
    bg: 'bg-[#1677FF]',
    border: 'border-[#1677FF]',
    ring: 'ring-[#1677FF]/40',
    text: 'text-[#1677FF]',
    shadow: 'shadow-[0_0_20px_rgba(22,119,255,0.4)]',
    emoji: '💙',
  },
}

const activeMethod = computed(() => methodConfig[selectedMethod.value])
</script>

<template>
  <!-- 遮罩层 -->
  <Teleport to="body">
    <div
      class="fixed inset-0 z-[9000] flex items-center justify-center p-4"
      style="background: rgba(0,0,0,0.75); backdrop-filter: blur(8px);"
      @click.self="handleClose"
    >
      <div
        class="relative w-full max-w-lg bg-slate-900 border border-white/10 rounded-3xl shadow-2xl overflow-hidden"
        style="animation: modalIn .25s cubic-bezier(.4,0,.2,1)"
      >
        <!-- 顶部渐变装饰条 -->
        <div class="h-1 w-full bg-gradient-to-r from-violet-500 via-neon-yellow to-emerald-400"></div>

        <!-- 关闭按钮 -->
        <button
          @click="handleClose"
          class="absolute top-4 right-4 z-10 p-1.5 rounded-full text-gray-400 hover:text-white hover:bg-white/10 transition-all"
        >
          <X class="w-5 h-5" />
        </button>

        <!-- ════════════ STEP: 选择套餐 ════════════ -->
        <div v-if="step === 'select'" class="p-8">
          <!-- 标题 -->
          <div class="flex items-center gap-3 mb-8">
            <div class="w-10 h-10 rounded-2xl bg-gradient-to-br from-violet-500 to-neon-yellow flex items-center justify-center">
              <Wallet class="w-5 h-5 text-white" />
            </div>
            <div>
              <h2 class="text-xl font-bold text-white">灵感点数充值</h2>
              <p class="text-xs text-gray-400">选择套餐，为你的创作注入能量</p>
            </div>
          </div>

          <!-- 套餐列表 -->
          <p class="text-xs text-gray-500 uppercase tracking-widest mb-3">选择套餐</p>
          <div class="grid grid-cols-2 gap-3 mb-8">
            <button
              v-for="pkg in RECHARGE_PACKAGES"
              :key="pkg.id"
              @click="selectedPkg = pkg"
              class="relative rounded-2xl p-4 border-2 transition-all duration-200 text-left group"
              :class="selectedPkg.id === pkg.id
                ? 'border-neon-yellow bg-neon-yellow/5 shadow-[0_0_20px_rgba(250,204,21,0.15)]'
                : 'border-white/10 bg-slate-800/50 hover:border-white/25'"
            >
              <!-- 热门标签 -->
              <span
                v-if="pkg.hot"
                class="absolute -top-2.5 left-1/2 -translate-x-1/2 bg-gradient-to-r from-orange-500 to-pink-500 text-white text-[10px] font-bold px-2 py-0.5 rounded-full shadow-md whitespace-nowrap"
              >🔥 最受欢迎</span>

              <!-- 点数 -->
              <div class="flex items-end gap-1 mb-1">
                <span class="text-2xl font-bold font-mono" :class="selectedPkg.id === pkg.id ? 'text-neon-yellow' : 'text-white'">
                  {{ pkg.pts.toLocaleString() }}
                </span>
                <span class="text-xs text-gray-400 mb-0.5">pts</span>
              </div>

              <!-- 套餐名 + 节省 -->
              <div class="flex items-center justify-between">
                <span class="text-xs text-gray-400">{{ pkg.label }}</span>
                <span v-if="pkg.save" class="text-[10px] text-green-400 font-medium">{{ pkg.save }}</span>
              </div>

              <!-- 价格 -->
              <div class="mt-2 text-sm font-bold" :class="selectedPkg.id === pkg.id ? 'text-neon-yellow' : 'text-gray-300'">
                ¥{{ pkg.price }}
              </div>

              <!-- 选中圆点 -->
              <div
                v-if="selectedPkg.id === pkg.id"
                class="absolute top-3 right-3 w-4 h-4 rounded-full bg-neon-yellow flex items-center justify-center"
              >
                <div class="w-2 h-2 rounded-full bg-slate-900"></div>
              </div>
            </button>
          </div>

          <!-- 支付方式 -->
          <p class="text-xs text-gray-500 uppercase tracking-widest mb-3">支付方式</p>
          <div class="flex gap-3 mb-8">
            <button
              v-for="m in (['WECHAT', 'ALIPAY'] as const)"
              :key="m"
              @click="selectedMethod = m"
              class="flex-1 flex items-center gap-3 p-4 rounded-2xl border-2 transition-all duration-200"
              :class="selectedMethod === m
                ? `${methodConfig[m].border} ${methodConfig[m].bg}/10 ring-2 ${methodConfig[m].ring}`
                : 'border-white/10 bg-slate-800/50 hover:border-white/25'"
            >
              <span class="text-2xl">{{ methodConfig[m].emoji }}</span>
              <div class="text-left">
                <p class="text-sm font-bold text-white">{{ methodConfig[m].label }}</p>
                <p class="text-[10px] text-gray-400">扫码支付</p>
              </div>
              <div
                v-if="selectedMethod === m"
                class="ml-auto w-4 h-4 rounded-full flex items-center justify-center"
                :class="methodConfig[m].bg"
              >
                <div class="w-1.5 h-1.5 rounded-full bg-white"></div>
              </div>
            </button>
          </div>

          <!-- 确认按钮 -->
          <button
            @click="handleCreate"
            :disabled="creating"
            class="group w-full relative overflow-hidden py-4 rounded-2xl font-bold text-slate-900 text-base transition-all duration-300 disabled:opacity-60 disabled:cursor-not-allowed"
            :class="[activeMethod.bg, activeMethod.shadow, 'hover:brightness-110 active:scale-[0.98]']"
          >
            <span v-if="!creating" class="flex items-center justify-center gap-2">
              {{ activeMethod.emoji }} 立即支付 ¥{{ selectedPkg.price }}，获得 {{ selectedPkg.pts.toLocaleString() }} pts
              <ChevronRight class="w-4 h-4 group-hover:translate-x-1 transition-transform" />
            </span>
            <span v-else class="flex items-center justify-center gap-2">
              <span class="animate-spin w-4 h-4 border-2 border-slate-900/30 border-t-slate-900 rounded-full"></span>
              创建订单中...
            </span>
          </button>
        </div>

        <!-- ════════════ STEP: 扫码支付 ════════════ -->
        <div v-else-if="step === 'qr'" class="p-8">
          <!-- 顶部：返回 + 标题 -->
          <div class="flex items-center gap-3 mb-6">
            <button @click="handleRetry" class="text-gray-400 hover:text-white transition-colors">
              <ChevronRight class="w-5 h-5 rotate-180" />
            </button>
            <div>
              <h2 class="text-lg font-bold text-white">
                {{ activeMethod.emoji }} {{ activeMethod.label }}
              </h2>
              <p class="text-xs text-gray-400">请在 {{ countdownText }} 内完成支付</p>
            </div>
            <!-- 倒计时环 -->
            <div class="ml-auto flex items-center gap-1.5" :class="remainSeconds < 60 ? 'text-red-400' : 'text-gray-400'">
              <Clock class="w-4 h-4" />
              <span class="font-mono text-sm font-bold">{{ countdownText }}</span>
            </div>
          </div>

          <!-- 订单信息条 -->
          <div class="flex items-center justify-between bg-slate-800 rounded-xl px-4 py-3 mb-6 border border-white/5">
            <div>
              <p class="text-xs text-gray-400">本次充值</p>
              <p class="text-white font-bold font-mono">
                {{ order?.points.toLocaleString() }} <span class="text-xs text-gray-400">灵感点数</span>
              </p>
            </div>
            <div class="text-right">
              <p class="text-xs text-gray-400">支付金额</p>
              <p class="text-neon-yellow font-bold font-mono text-xl">¥{{ order?.amount }}</p>
            </div>
          </div>

          <!-- 二维码区域 -->
          <div class="flex flex-col items-center mb-6">
            <div
              class="relative p-4 rounded-2xl bg-white shadow-2xl mb-4"
              :class="activeMethod.shadow"
            >
              <img
                v-if="order?.qrCode"
                :src="order.qrCode"
                alt="支付二维码"
                class="w-48 h-48 block"
              />
              <div v-else class="w-48 h-48 flex items-center justify-center">
                <span class="animate-spin w-8 h-8 border-2 border-gray-200 border-t-gray-600 rounded-full"></span>
              </div>
              <!-- 方式角标 -->
              <div
                class="absolute -bottom-3 left-1/2 -translate-x-1/2 px-4 py-1 rounded-full text-white text-xs font-bold whitespace-nowrap"
                :class="activeMethod.bg"
              >{{ activeMethod.emoji }} {{ activeMethod.label }}</div>
            </div>

            <p class="text-sm text-gray-400 mt-5 flex items-center gap-2">
              <Smartphone class="w-4 h-4" />
              {{ selectedMethod === 'WECHAT' ? '打开微信 → 扫一扫' : '打开支付宝 → 扫一扫' }}
            </p>
          </div>

          <!-- Mock 确认按钮（仅开发模式） -->
          <div
            v-if="isMock"
            class="bg-amber-500/10 border border-amber-500/30 rounded-2xl p-4 text-center"
          >
            <p class="text-amber-400 text-xs mb-3 font-medium">🧪 开发模式 — 无真实支付</p>
            <button
              @click="handleMockConfirm"
              :disabled="mockConfirming"
              class="px-6 py-2 bg-amber-500 hover:bg-amber-400 text-slate-900 font-bold rounded-xl text-sm transition-all disabled:opacity-50 flex items-center gap-2 mx-auto"
            >
              <span v-if="mockConfirming" class="animate-spin w-3.5 h-3.5 border-2 border-slate-900/30 border-t-slate-900 rounded-full"></span>
              {{ mockConfirming ? '确认中...' : '模拟扫码支付成功' }}
            </button>
          </div>
        </div>

        <!-- ════════════ STEP: 支付成功 ════════════ -->
        <div v-else-if="step === 'success'" class="p-10 text-center">
          <div class="relative inline-flex mb-6">
            <div class="absolute inset-0 bg-green-400 rounded-full blur-xl opacity-30 animate-pulse"></div>
            <div class="relative w-20 h-20 rounded-full bg-green-500/20 border-2 border-green-400 flex items-center justify-center">
              <CheckCircle2 class="w-10 h-10 text-green-400" />
            </div>
          </div>
          <h2 class="text-2xl font-bold text-white mb-2">支付成功！</h2>
          <p class="text-gray-400 mb-2">
            <span class="text-neon-yellow font-bold font-mono text-xl">+{{ order?.points.toLocaleString() }}</span>
            灵感点数已到账
          </p>
          <p class="text-xs text-gray-500 mb-8">可在收支明细中查看本次充值记录</p>
          <button
            @click="handleClose"
            class="px-8 py-3 bg-green-500 hover:bg-green-400 text-slate-900 font-bold rounded-2xl transition-all"
          >
            完成
          </button>
        </div>

        <!-- ════════════ STEP: 失败 / 超时 ════════════ -->
        <div v-else-if="step === 'failed'" class="p-10 text-center">
          <div class="relative inline-flex mb-6">
            <div class="absolute inset-0 bg-red-400 rounded-full blur-xl opacity-20"></div>
            <div class="relative w-20 h-20 rounded-full bg-red-500/20 border-2 border-red-400 flex items-center justify-center">
              <AlertCircle class="w-10 h-10 text-red-400" />
            </div>
          </div>
          <h2 class="text-2xl font-bold text-white mb-2">订单已超时</h2>
          <p class="text-gray-400 mb-8">二维码已失效，请重新生成订单后支付</p>
          <div class="flex justify-center gap-3">
            <button
              @click="handleRetry"
              class="px-8 py-3 bg-neon-yellow hover:bg-yellow-300 text-slate-900 font-bold rounded-2xl transition-all"
            >
              重新充值
            </button>
            <button
              @click="handleClose"
              class="px-8 py-3 bg-white/10 hover:bg-white/20 text-white font-bold rounded-2xl transition-all"
            >
              关闭
            </button>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
@keyframes modalIn {
  from { opacity: 0; transform: scale(0.92) translateY(16px); }
  to   { opacity: 1; transform: scale(1)    translateY(0); }
}
</style>

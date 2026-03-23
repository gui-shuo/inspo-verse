<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, nextTick } from 'vue'
import * as echarts from 'echarts'
import { Crown, Zap, Gift, Shield, Check, Star, Trophy, Activity, ArrowUpRight, Lock, Unlock } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import {
  getVipPlans, createVipOrder, getMyMembership,
  getGrowthTrajectory, getDailyTasks, signIn, claimTaskReward,
  getVipPrivileges, queryVipPayStatus, mockConfirmVipPay,
  type VipPlan, type VipMembership, type GrowthData,
  type DailyTaskItem, type VipPrivilege, type VipOrderPaymentResult
} from '@/api/vip'

const router = useRouter()

const toast = (msg: string, type: 'success' | 'error' = 'success') => {
  const el = document.createElement('div')
  el.textContent = msg
  el.className = `fixed top-6 left-1/2 -translate-x-1/2 z-[9999] px-6 py-3 rounded-xl text-white text-sm font-medium shadow-lg transition-all ${type === 'success' ? 'bg-green-600' : 'bg-red-600'}`
  document.body.appendChild(el)
  setTimeout(() => el.remove(), 3000)
}

// ─── 状态 ────────────────────────────────────────────────────────────────────
const chartRef = ref<HTMLElement | null>(null)
let chart: echarts.ECharts | null = null

const vipPlans = ref<VipPlan[]>([])
const membership = ref<VipMembership | null>(null)
const growthData = ref<GrowthData | null>(null)
const tasks = ref<DailyTaskItem[]>([])
const privileges = ref<VipPrivilege[]>([])
const loading = ref(false)
const taskLoading = ref<Record<string, boolean>>({})

// ─── 支付弹窗状态 ────────────────────────────────────────────────────────────
const showPayModal = ref(false)
type PayStep = 'method' | 'qr' | 'success' | 'failed'
const payStep = ref<PayStep>('method')
const selectedPayMethod = ref<'ALIPAY' | 'WECHAT'>('WECHAT')
const selectedPlan = ref<VipPlan | null>(null)
const paymentResult = ref<VipOrderPaymentResult | null>(null)
const payCreating = ref(false)
const remainSeconds = ref(300)
let countdownTimer: ReturnType<typeof setInterval> | null = null
let pollingTimer: ReturnType<typeof setInterval> | null = null

const countdownText = computed(() => {
  const m = Math.floor(remainSeconds.value / 60).toString().padStart(2, '0')
  const s = (remainSeconds.value % 60).toString().padStart(2, '0')
  return `${m}:${s}`
})

// ─── 计算属性 ────────────────────────────────────────────────────────────────
const plans = computed(() => {
  return vipPlans.value.map(plan => ({
    id: plan.id,
    raw: plan,
    name: plan.planName,
    price: `¥${plan.price}`,
    duration: `${plan.durationDays}天`,
    color: plan.level === 1 ? 'text-gray-300' : 'text-yellow-400',
    border: plan.level === 1 ? 'border-gray-600' : 'border-yellow-500',
    isPopular: plan.planCode === 'GOLD_MONTH',
    features: plan.level === 1
      ? ['每日 100 次对话', '基础 AI 模型', '社区发帖权限', '标准响应速度']
      : ['每日 1000 次对话', '解锁 GPT-4 模型', '专属身份标识', '极速响应通道', '去广告体验', 'API 接口调用']
  }))
})

const membershipVipName = computed(() => membership.value?.vipLevelName || '普通用户')
const membershipExpire = computed(() => {
  if (!membership.value?.endTime) return '未开通'
  return membership.value.endTime.split('T')[0].replace(/-/g, '.')
})
const userLevel = computed(() => membership.value?.level ?? 1)
const userLevelName = computed(() => membership.value?.levelName ?? '灵感新手')
const userExp = computed(() => membership.value?.expPoints ?? 0)
const nextLevelExp = computed(() => membership.value?.nextLevelExp ?? 100)
const nextLevelName = computed(() => membership.value?.nextLevelName ?? '创意学徒')
const progressPercent = computed(() => {
  if (membership.value?.isMaxLevel) return 100
  const needed = membership.value?.expNeeded ?? 1
  const progress = membership.value?.progressInLevel ?? 0
  return Math.min(Math.round((progress / needed) * 100), 100)
})
const expRemaining = computed(() => {
  if (membership.value?.isMaxLevel) return 0
  return (membership.value?.expNeeded ?? 0) - (membership.value?.progressInLevel ?? 0)
})

// ─── 图标映射 ────────────────────────────────────────────────────────────────
const iconMap: Record<string, any> = {
  'zap': Zap,
  'crown': Crown,
  'gift': Gift,
  'shield': Shield,
  'activity': Activity,
  'arrow-up-right': ArrowUpRight,
}

// ─── 数据加载 ────────────────────────────────────────────────────────────────
const loadData = async () => {
  try {
    loading.value = true
    const [plansRes, membershipRes, growthRes, tasksRes, privilegesRes] = await Promise.all([
      getVipPlans(),
      getMyMembership(),
      getGrowthTrajectory(),
      getDailyTasks(),
      getVipPrivileges()
    ])
    vipPlans.value = plansRes.data || []
    membership.value = membershipRes.data || null
    growthData.value = growthRes.data || null
    tasks.value = tasksRes.data || []
    privileges.value = privilegesRes.data || []

    await nextTick()
    initChart()
  } catch (error: any) {
    toast(error.message || '加载数据失败', 'error')
  } finally {
    loading.value = false
  }
}

const refreshTasks = async () => {
  try {
    const res = await getDailyTasks()
    tasks.value = res.data || []
  } catch {}
}

const refreshMembership = async () => {
  try {
    const [res, growthRes] = await Promise.all([getMyMembership(), getGrowthTrajectory()])
    membership.value = res.data || null
    growthData.value = growthRes.data || null
    await nextTick()
    initChart()
  } catch {}
}

// ─── 签到 ────────────────────────────────────────────────────────────────────
const handleSignIn = async () => {
  taskLoading.value['DAILY_SIGNIN'] = true
  try {
    const res = await signIn()
    toast(`签到成功！积分 +${res.data.pointsEarned}，经验 +${res.data.expEarned}`)
    await Promise.all([refreshTasks(), refreshMembership()])
  } catch (error: any) {
    toast(error.message || '签到失败', 'error')
  } finally {
    taskLoading.value['DAILY_SIGNIN'] = false
  }
}

// ─── 领取任务奖励 ────────────────────────────────────────────────────────────
const handleClaimReward = async (taskCode: string) => {
  taskLoading.value[taskCode] = true
  try {
    const res = await claimTaskReward(taskCode)
    toast(`领取成功！积分 +${res.data.pointsEarned}，经验 +${res.data.expEarned}`)
    await Promise.all([refreshTasks(), refreshMembership()])
  } catch (error: any) {
    toast(error.message || '领取失败', 'error')
  } finally {
    taskLoading.value[taskCode] = false
  }
}

// ─── 任务按钮点击 ────────────────────────────────────────────────────────────
const handleTaskAction = (task: DailyTaskItem) => {
  if (task.taskCode === 'DAILY_SIGNIN') {
    if (!task.completed) {
      handleSignIn()
    }
    return
  }
  if (task.completed && !task.rewarded) {
    handleClaimReward(task.taskCode)
    return
  }
  if (!task.completed && task.routePath) {
    router.push(task.routePath)
  }
}

// ─── VIP购买 ─────────────────────────────────────────────────────────────────
const openPayModal = (plan: VipPlan) => {
  selectedPlan.value = plan
  selectedPayMethod.value = 'WECHAT'
  payStep.value = 'method'
  paymentResult.value = null
  showPayModal.value = true
}

const startPayment = async () => {
  if (!selectedPlan.value) return
  payCreating.value = true
  try {
    const res = await createVipOrder(selectedPlan.value.id, selectedPayMethod.value)
    paymentResult.value = res.data
    payStep.value = 'qr'
    startCountdown()
    startPolling(res.data.orderNo)
  } catch (error: any) {
    toast(error.message || '创建订单失败', 'error')
  } finally {
    payCreating.value = false
  }
}

const handleMockConfirm = async () => {
  if (!paymentResult.value) return
  try {
    await mockConfirmVipPay(paymentResult.value.paymentOrderNo)
    stopPolling()
    stopCountdown()
    payStep.value = 'success'
    await Promise.all([loadData()])
  } catch (error: any) {
    toast(error.message || 'Mock支付失败', 'error')
  }
}

const closePayModal = () => {
  showPayModal.value = false
  stopPolling()
  stopCountdown()
}

const startCountdown = () => {
  remainSeconds.value = 300
  countdownTimer = setInterval(() => {
    remainSeconds.value--
    if (remainSeconds.value <= 0) {
      stopCountdown()
      payStep.value = 'failed'
      stopPolling()
    }
  }, 1000)
}
const stopCountdown = () => { if (countdownTimer) { clearInterval(countdownTimer); countdownTimer = null } }

const startPolling = (orderNo: string) => {
  pollingTimer = setInterval(async () => {
    try {
      const res = await queryVipPayStatus(orderNo)
      if (res.data?.payStatus === 1) {
        stopPolling()
        stopCountdown()
        payStep.value = 'success'
        await loadData()
      }
    } catch {}
  }, 3000)
}
const stopPolling = () => { if (pollingTimer) { clearInterval(pollingTimer); pollingTimer = null } }

// ─── 成长轨迹图表 ───────────────────────────────────────────────────────────
const initChart = () => {
  if (!chartRef.value) return
  if (chart) chart.dispose()
  chart = echarts.init(chartRef.value)

  const pts = growthData.value?.points || []
  const nextLevelExp = growthData.value?.nextLevelExp ?? 100
  const currentLevelExp = growthData.value?.currentLevelExp ?? 0

  // X轴标签：跨年时显示年份
  const xLabels: string[] = []
  let prevYear = 0
  for (const p of pts) {
    if (p.year !== prevYear) {
      xLabels.push(`${String(p.year).slice(2)}年${p.month}月`)
    } else {
      xLabels.push(`${p.month}月`)
    }
    prevYear = p.year
  }
  const expValues = pts.map(p => p.exp)

  // Y轴最大值 = 下一等级经验阈值（至少比数据最大值大）
  const dataMax = expValues.length > 0 ? Math.max(...expValues) : 0
  const yMax = Math.max(nextLevelExp, dataMax)

  const option: echarts.EChartsOption = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const p = params[0]
        const idx = p.dataIndex
        const pt = pts[idx]
        if (!pt) return `${p.name}: ${p.value} 经验`
        return [
          `<b>${pt.year}年${pt.month}月</b>`,
          `累计经验：${pt.exp.toLocaleString()}`,
          `本月获得：+${pt.monthlyGain.toLocaleString()}`
        ].join('<br/>')
      }
    },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: xLabels.length > 0 ? xLabels : ['1月','2月','3月','4月','5月','6月','7月','8月','9月','10月','11月','12月'],
      axisLine: { lineStyle: { color: '#94a3b8' } },
      axisLabel: { color: '#94a3b8', fontSize: 11, interval: 0, rotate: xLabels.length > 8 ? 30 : 0 }
    },
    yAxis: {
      type: 'value',
      max: yMax > 0 ? yMax : undefined,
      min: 0,
      axisLine: { lineStyle: { color: '#94a3b8' } },
      axisLabel: {
        color: '#94a3b8',
        formatter: (v: number) => v >= 10000 ? (v / 10000).toFixed(1) + 'w' : v >= 1000 ? (v / 1000).toFixed(1) + 'k' : String(v)
      },
      splitLine: { lineStyle: { color: 'rgba(255,255,255,0.1)' } }
    },
    series: [
      {
        name: '累计经验',
        data: expValues.length > 0 ? expValues : [0,0,0,0,0,0,0,0,0,0,0,0],
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { color: '#FFD700', width: 3 },
        itemStyle: { color: '#FFD700', borderWidth: 2, borderColor: '#1e293b' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(255, 215, 0, 0.4)' },
            { offset: 1, color: 'rgba(255, 215, 0, 0)' }
          ])
        },
        markLine: currentLevelExp !== nextLevelExp ? {
          silent: true,
          symbol: 'none',
          lineStyle: { type: 'dashed', color: 'rgba(255,215,0,0.3)', width: 1 },
          label: { show: true, position: 'insideEndTop', color: '#FFD700', fontSize: 10 },
          data: [
            { yAxis: nextLevelExp, label: { formatter: `下一等级 ${nextLevelExp.toLocaleString()}` } }
          ]
        } : undefined
      }
    ]
  }
  chart.setOption(option)
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', () => chart?.resize())
})

onUnmounted(() => {
  chart?.dispose()
  stopCountdown()
  stopPolling()
})
</script>

<template>
  <div class="container mx-auto px-4 py-12 pb-24 min-h-screen">
    
    <!-- 顶部区域：会员卡 + 成长图 -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-8 mb-16">
      
      <!-- 会员卡 -->
      <div class="relative perspective-1000 h-full min-h-[300px]">
        <div class="relative z-10 h-full bg-gradient-to-br from-yellow-600 via-yellow-500 to-yellow-300 rounded-2xl p-8 shadow-[0_0_60px_rgba(234,179,8,0.4)] transform hover:scale-[1.02] transition-transform duration-500 overflow-hidden flex flex-col justify-between">
          <div class="absolute inset-0 opacity-10 bg-[url('https://www.transparenttextures.com/patterns/cubes.png')]"></div>
          
          <div class="relative flex justify-between items-start">
            <div>
              <div class="flex items-center gap-2 mb-2">
                <Crown class="w-6 h-6 text-yellow-900" />
                <span class="font-bold text-yellow-900 tracking-wider">INSPO VIP</span>
              </div>
              <h2 class="text-4xl font-bold text-white text-shadow">{{ membershipVipName }}</h2>
            </div>
            <div class="text-right">
              <p class="text-yellow-900 font-bold">有效期至</p>
              <p class="text-white font-mono text-xl">{{ membershipExpire }}</p>
            </div>
          </div>

          <div class="relative mt-8">
            <div class="flex justify-between items-end mb-2">
              <span class="text-yellow-900 font-bold">LV.{{ userLevel }} {{ userLevelName }}</span>
              <span class="text-yellow-900 font-mono">{{ userExp.toLocaleString() }} / {{ nextLevelExp.toLocaleString() }}</span>
            </div>
            <div class="w-full h-3 bg-black/20 rounded-full overflow-hidden">
              <div class="h-full bg-white shadow-[0_0_10px_white] transition-all duration-500" :style="{ width: progressPercent + '%' }"></div>
            </div>
            <p v-if="!membership?.isMaxLevel" class="text-xs text-yellow-900 mt-2 font-medium">
              再获得 {{ expRemaining.toLocaleString() }} 经验值即可升级为 [{{ nextLevelName }}]
            </p>
            <p v-else class="text-xs text-yellow-900 mt-2 font-medium">✨ 已达最高等级</p>
          </div>
        </div>
      </div>

      <!-- 成长轨迹图 -->
      <div class="bg-slate-800 rounded-2xl p-8 border border-white/5 relative overflow-hidden">
        <div class="absolute top-0 right-0 p-4 opacity-10">
          <Activity class="w-24 h-24 text-neon-blue" />
        </div>
        <h3 class="text-xl font-bold text-white mb-6 flex items-center gap-2">
          <Trophy class="w-5 h-5 text-yellow-400" /> 成长轨迹
        </h3>
        <div ref="chartRef" class="w-full h-[250px]"></div>
      </div>
    </div>

    <!-- 套餐选择 -->
    <div class="mb-20">
      <div class="text-center mb-12">
        <h2 class="text-3xl font-bold text-white mb-4">选择你的身份</h2>
        <p class="text-gray-400">升级会员，解锁更多赛博科技力量</p>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <div 
          v-for="plan in plans" 
          :key="plan.id"
          class="relative bg-slate-800 rounded-2xl p-8 border transition-all duration-300 hover:-translate-y-2"
          :class="[plan.border, plan.isPopular ? 'shadow-[0_0_30px_rgba(234,179,8,0.2)] border-2' : 'border-white/10 hover:border-white/30']"
        >
          <div v-if="plan.isPopular" class="absolute -top-4 left-1/2 -translate-x-1/2 bg-yellow-500 text-slate-900 text-xs font-bold px-3 py-1 rounded-full shadow-lg">
            MOST POPULAR
          </div>

          <h3 class="text-xl font-bold mb-2" :class="plan.color">{{ plan.name }}</h3>
          <div class="text-3xl font-bold text-white mb-1 font-mono">{{ plan.price }}</div>
          <div class="text-sm text-gray-400 mb-6">/ {{ plan.duration }}</div>

          <ul class="space-y-4 mb-8">
            <li v-for="feat in plan.features" :key="feat" class="flex items-start gap-3 text-gray-300">
              <Check class="w-5 h-5 text-green-400 flex-shrink-0" />
              <span class="text-sm">{{ feat }}</span>
            </li>
          </ul>

          <button
            class="w-full py-3 rounded-xl font-bold transition-all"
            :class="plan.isPopular ? 'bg-yellow-500 text-slate-900 hover:bg-yellow-400' : 'bg-white/10 text-white hover:bg-white/20'"
            @click="openPayModal(plan.raw)"
            :disabled="loading"
          >
            立即升级
          </button>
        </div>
      </div>
    </div>

    <!-- 底部区域：权益 + 任务 -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
      
      <!-- 会员权益概览 -->
      <div class="lg:col-span-2 bg-slate-800 rounded-2xl p-8 border border-white/5">
        <h3 class="text-xl font-bold text-white mb-6 flex items-center gap-2">
          <Star class="w-5 h-5 text-neon-purple" /> 会员权益概览
        </h3>
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div v-for="(item, index) in privileges" :key="index" class="flex items-center gap-4 p-4 rounded-xl transition-colors border"
            :class="item.unlocked ? 'bg-slate-700/30 hover:bg-slate-700/50 border-white/5 hover:border-white/10' : 'bg-slate-800/50 border-white/5 opacity-60'"
          >
            <div class="w-12 h-12 rounded-full flex items-center justify-center border"
              :class="item.unlocked ? 'bg-slate-800 border-white/10' : 'bg-slate-900 border-white/5'"
            >
              <component :is="iconMap[item.icon] || Zap" class="w-5 h-5" :class="item.unlocked ? 'text-neon-blue' : 'text-gray-600'" />
            </div>
            <div class="flex-1">
              <div class="flex items-center gap-2">
                <h4 class="font-bold text-sm" :class="item.unlocked ? 'text-white' : 'text-gray-500'">{{ item.title }}</h4>
                <Lock v-if="!item.unlocked" class="w-3 h-3 text-gray-600" />
                <Unlock v-else class="w-3 h-3 text-green-400" />
              </div>
              <p class="text-xs text-gray-400">{{ item.desc }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 每日任务 -->
      <div class="bg-slate-800 rounded-2xl p-8 border border-white/5">
        <h3 class="text-xl font-bold text-white mb-6 flex items-center gap-2">
          <Activity class="w-5 h-5 text-green-400" /> 每日任务
        </h3>
        <div class="space-y-6">
          <div v-for="task in tasks" :key="task.taskCode">
            <div class="flex justify-between items-center mb-2">
              <span class="text-gray-200 font-medium text-sm">{{ task.taskName }}</span>
              <span class="text-xs font-bold text-neon-blue">
                +{{ task.rewardPoints }} 积分
                <span v-if="task.rewardExp" class="text-neon-purple ml-1">+{{ task.rewardExp }} 经验</span>
              </span>
            </div>
            <div class="flex items-center gap-3">
              <div class="flex-1 h-2 bg-black/20 rounded-full overflow-hidden">
                <div 
                  class="h-full bg-gradient-to-r from-green-400 to-emerald-500 rounded-full transition-all duration-300" 
                  :style="{ width: (task.progress / task.dailyLimit) * 100 + '%' }"
                ></div>
              </div>
              <button 
                class="text-xs px-3 py-1 rounded-full border transition-colors min-w-[70px]"
                :class="[
                  task.completed && task.rewarded
                    ? 'text-green-400 border-green-400/30 cursor-default'
                    : task.completed && !task.rewarded
                    ? 'text-yellow-400 border-yellow-400/30 hover:bg-yellow-500/20 cursor-pointer'
                    : 'text-gray-400 border-white/10 hover:bg-white/10 cursor-pointer'
                ]"
                :disabled="(task.completed && task.rewarded) || !!taskLoading[task.taskCode]"
                @click="handleTaskAction(task)"
              >
                {{ taskLoading[task.taskCode] ? '...' : task.statusText }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- VIP支付弹窗 -->
    <Teleport to="body">
      <div v-if="showPayModal" class="fixed inset-0 z-[9998] flex items-center justify-center">
        <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" @click="closePayModal"></div>
        <div class="relative bg-slate-800 rounded-2xl p-8 w-full max-w-md border border-white/10 shadow-2xl z-10">
          
          <!-- Step 1: 选择支付方式 -->
          <template v-if="payStep === 'method'">
            <h3 class="text-xl font-bold text-white mb-2">选择支付方式</h3>
            <p class="text-gray-400 text-sm mb-6">
              {{ selectedPlan?.planName }} — ¥{{ selectedPlan?.price }}
            </p>

            <div class="space-y-3 mb-6">
              <label class="flex items-center gap-4 p-4 rounded-xl border cursor-pointer transition-all"
                :class="selectedPayMethod === 'WECHAT' ? 'border-green-400 bg-green-500/10' : 'border-white/10 hover:border-white/20'"
                @click="selectedPayMethod = 'WECHAT'"
              >
                <div class="w-10 h-10 rounded-full bg-green-500/20 flex items-center justify-center">
                  <span class="text-lg">💬</span>
                </div>
                <span class="text-white font-medium">微信支付</span>
                <div class="ml-auto w-5 h-5 rounded-full border-2 flex items-center justify-center"
                  :class="selectedPayMethod === 'WECHAT' ? 'border-green-400' : 'border-gray-500'">
                  <div v-if="selectedPayMethod === 'WECHAT'" class="w-3 h-3 rounded-full bg-green-400"></div>
                </div>
              </label>

              <label class="flex items-center gap-4 p-4 rounded-xl border cursor-pointer transition-all"
                :class="selectedPayMethod === 'ALIPAY' ? 'border-blue-400 bg-blue-500/10' : 'border-white/10 hover:border-white/20'"
                @click="selectedPayMethod = 'ALIPAY'"
              >
                <div class="w-10 h-10 rounded-full bg-blue-500/20 flex items-center justify-center">
                  <span class="text-lg">💰</span>
                </div>
                <span class="text-white font-medium">支付宝</span>
                <div class="ml-auto w-5 h-5 rounded-full border-2 flex items-center justify-center"
                  :class="selectedPayMethod === 'ALIPAY' ? 'border-blue-400' : 'border-gray-500'">
                  <div v-if="selectedPayMethod === 'ALIPAY'" class="w-3 h-3 rounded-full bg-blue-400"></div>
                </div>
              </label>
            </div>

            <div class="flex gap-3">
              <button class="flex-1 py-3 rounded-xl bg-white/10 text-white hover:bg-white/20 transition-all" @click="closePayModal">取消</button>
              <button class="flex-1 py-3 rounded-xl bg-yellow-500 text-slate-900 font-bold hover:bg-yellow-400 transition-all" 
                @click="startPayment" :disabled="payCreating">
                {{ payCreating ? '创建中...' : '确认支付' }}
              </button>
            </div>
          </template>

          <!-- Step 2: 扫码支付 -->
          <template v-if="payStep === 'qr'">
            <h3 class="text-xl font-bold text-white mb-2 text-center">扫码支付</h3>
            <p class="text-gray-400 text-sm mb-4 text-center">
              {{ selectedPlan?.planName }} — ¥{{ selectedPlan?.price }}
            </p>

            <div class="flex justify-center mb-4">
              <div class="bg-white rounded-xl p-3">
                <img v-if="paymentResult?.qrCode" :src="paymentResult.qrCode" alt="支付二维码" class="w-48 h-48" />
              </div>
            </div>

            <p class="text-center text-gray-400 text-sm mb-2">
              请使用{{ selectedPayMethod === 'WECHAT' ? '微信' : '支付宝' }}扫描二维码
            </p>
            <p class="text-center text-yellow-400 font-mono text-lg mb-4">{{ countdownText }}</p>

            <!-- Mock模式快速确认 -->
            <button v-if="paymentResult?.mockMode" 
              class="w-full py-3 rounded-xl bg-green-600 text-white font-bold hover:bg-green-500 transition-all mb-3"
              @click="handleMockConfirm"
            >
              🔧 开发模式：模拟支付成功
            </button>

            <button class="w-full py-2 rounded-xl bg-white/10 text-gray-400 hover:bg-white/20 transition-all" @click="closePayModal">
              取消支付
            </button>
          </template>

          <!-- Step 3: 支付成功 -->
          <template v-if="payStep === 'success'">
            <div class="text-center py-8">
              <div class="w-20 h-20 rounded-full bg-green-500/20 flex items-center justify-center mx-auto mb-4">
                <Check class="w-10 h-10 text-green-400" />
              </div>
              <h3 class="text-2xl font-bold text-white mb-2">支付成功！</h3>
              <p class="text-gray-400 mb-6">{{ selectedPlan?.planName }} 已激活</p>
              <button class="px-8 py-3 rounded-xl bg-yellow-500 text-slate-900 font-bold hover:bg-yellow-400 transition-all" @click="closePayModal">
                完成
              </button>
            </div>
          </template>

          <!-- Step 4: 支付失败/超时 -->
          <template v-if="payStep === 'failed'">
            <div class="text-center py-8">
              <div class="w-20 h-20 rounded-full bg-red-500/20 flex items-center justify-center mx-auto mb-4">
                <span class="text-4xl">⏳</span>
              </div>
              <h3 class="text-2xl font-bold text-white mb-2">支付超时</h3>
              <p class="text-gray-400 mb-6">订单已过期，请重新发起支付</p>
              <div class="flex gap-3 justify-center">
                <button class="px-6 py-3 rounded-xl bg-white/10 text-white hover:bg-white/20 transition-all" @click="closePayModal">关闭</button>
                <button class="px-6 py-3 rounded-xl bg-yellow-500 text-slate-900 font-bold hover:bg-yellow-400 transition-all" @click="payStep = 'method'">重新支付</button>
              </div>
            </div>
          </template>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.text-shadow {
  text-shadow: 0 2px 4px rgba(0,0,0,0.3);
}
</style>

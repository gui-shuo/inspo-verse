<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { Crown, Zap, Gift, Shield, Check, Star, Trophy, Activity, ArrowUpRight } from 'lucide-vue-next'

const chartRef = ref<HTMLElement | null>(null)
let chart: echarts.ECharts | null = null

const privileges = [
  { icon: Zap, title: 'AI 极速响应', desc: '优先调用高性能计算节点' },
  { icon: Crown, title: '专属模型', desc: '解锁 GPT-4 及 Claude 3 Opus' },
  { icon: Gift, title: '月度礼包', desc: '每月领取 1000 灵感点数' },
  { icon: Shield, title: '数据保险箱', desc: '企业级数据加密存储' },
  { icon: Activity, title: '无限上下文', desc: '支持 100k+ Token 对话' },
  { icon: ArrowUpRight, title: '多模态输入', desc: '支持图片/音频/视频识别' },
]

const plans = [
  {
    name: '白银公民',
    price: '免费',
    color: 'text-gray-300',
    border: 'border-gray-600',
    bg: 'bg-slate-800',
    features: ['每日 10 次对话', '基础 AI 模型', '社区发帖权限', '标准响应速度']
  },
  {
    name: '黄金极客',
    price: '¥29 / 月',
    color: 'text-yellow-400',
    border: 'border-yellow-500',
    bg: 'bg-slate-800',
    isPopular: true,
    features: ['每日 500 次对话', '解锁 GPT-4 模型', '专属身份标识', '极速响应通道', '去广告体验']
  },
  {
    name: '赛博领主',
    price: '¥99 / 月',
    color: 'text-neon-purple',
    border: 'border-neon-purple',
    bg: 'bg-slate-900',
    features: ['无限次对话', '全模型解锁 (含画图)', '专属客户经理', 'API 接口调用', '早期功能内测', '自定义 AI 角色']
  }
]

const tasks = [
  { title: '每日签到', reward: '+10 积分', progress: 1, total: 1, status: '已完成' },
  { title: '发布一条动态', reward: '+50 积分', progress: 0, total: 1, status: '去完成' },
  { title: '邀请新用户', reward: '+100 积分/人', progress: 3, total: 5, status: '3/5' },
  { title: '使用 AI 绘图', reward: '+20 积分', progress: 0, total: 3, status: '0/3' },
]

const initChart = () => {
  if (!chartRef.value) return
  chart = echarts.init(chartRef.value)
  
  const option = {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: ['1月', '2月', '3月', '4月', '5月', '6月'],
      axisLine: { lineStyle: { color: '#94a3b8' } }
    },
    yAxis: {
      type: 'value',
      axisLine: { lineStyle: { color: '#94a3b8' } },
      splitLine: { lineStyle: { color: 'rgba(255,255,255,0.1)' } }
    },
    series: [{
      data: [150, 230, 224, 218, 350, 470],
      type: 'line',
      smooth: true,
      lineStyle: { color: '#FFD700', width: 4 },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(255, 215, 0, 0.5)' },
          { offset: 1, color: 'rgba(255, 215, 0, 0)' }
        ])
      }
    }]
  }
  chart.setOption(option)
}

onMounted(() => {
  initChart()
  window.addEventListener('resize', () => chart?.resize())
})

onUnmounted(() => {
  chart?.dispose()
})
</script>

<template>
  <div class="container mx-auto px-4 py-12 pb-24 min-h-screen">
    
    <!-- Top Section: Status & Chart -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-8 mb-16">
      
      <!-- Membership Card -->
      <div class="relative perspective-1000 h-full min-h-[300px]">
        <div class="relative z-10 h-full bg-gradient-to-br from-yellow-600 via-yellow-500 to-yellow-300 rounded-2xl p-8 shadow-[0_0_60px_rgba(234,179,8,0.4)] transform hover:scale-[1.02] transition-transform duration-500 overflow-hidden flex flex-col justify-between">
          <div class="absolute inset-0 opacity-10 bg-[url('https://www.transparenttextures.com/patterns/cubes.png')]"></div>
          
          <div class="relative flex justify-between items-start">
            <div>
              <div class="flex items-center gap-2 mb-2">
                <Crown class="w-6 h-6 text-yellow-900" />
                <span class="font-bold text-yellow-900 tracking-wider">INSPO VIP</span>
              </div>
              <h2 class="text-4xl font-bold text-white text-shadow">GOLD MEMBER</h2>
            </div>
            <div class="text-right">
              <p class="text-yellow-900 font-bold">有效期至</p>
              <p class="text-white font-mono text-xl">2026.12.31</p>
            </div>
          </div>

          <div class="relative mt-8">
            <div class="flex justify-between items-end mb-2">
              <span class="text-yellow-900 font-bold">LV.5 赛博游侠</span>
              <span class="text-yellow-900 font-mono">4,850 / 5,000</span>
            </div>
            <div class="w-full h-3 bg-black/20 rounded-full overflow-hidden">
              <div class="h-full bg-white w-[97%] shadow-[0_0_10px_white]"></div>
            </div>
            <p class="text-xs text-yellow-900 mt-2 font-medium">再获得 150 经验值即可升级为 [赛博领主]</p>
          </div>
        </div>
      </div>

      <!-- Growth Chart -->
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

    <!-- Plans Section -->
    <div class="mb-20">
      <div class="text-center mb-12">
        <h2 class="text-3xl font-bold text-white mb-4">选择你的身份</h2>
        <p class="text-gray-400">升级会员，解锁更多赛博科技力量</p>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-8">
        <div 
          v-for="plan in plans" 
          :key="plan.name"
          class="relative bg-slate-800 rounded-2xl p-8 border transition-all duration-300 hover:-translate-y-2"
          :class="[plan.border, plan.isPopular ? 'shadow-[0_0_30px_rgba(234,179,8,0.2)] border-2' : 'border-white/10 hover:border-white/30']"
        >
          <div v-if="plan.isPopular" class="absolute -top-4 left-1/2 -translate-x-1/2 bg-yellow-500 text-slate-900 text-xs font-bold px-3 py-1 rounded-full shadow-lg">
            MOST POPULAR
          </div>

          <h3 class="text-xl font-bold mb-2" :class="plan.color">{{ plan.name }}</h3>
          <div class="text-3xl font-bold text-white mb-6 font-mono">{{ plan.price }}</div>

          <ul class="space-y-4 mb-8">
            <li v-for="feat in plan.features" :key="feat" class="flex items-start gap-3 text-gray-300">
              <Check class="w-5 h-5 text-green-400 flex-shrink-0" />
              <span class="text-sm">{{ feat }}</span>
            </li>
          </ul>

          <button 
            class="w-full py-3 rounded-xl font-bold transition-all"
            :class="plan.isPopular ? 'bg-yellow-500 text-slate-900 hover:bg-yellow-400' : 'bg-white/10 text-white hover:bg-white/20'"
          >
            {{ plan.isPopular ? '立即升级' : '当前版本' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Bottom Grid: Privileges & Tasks -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
      
      <!-- Privileges -->
      <div class="lg:col-span-2 bg-slate-800 rounded-2xl p-8 border border-white/5">
        <h3 class="text-xl font-bold text-white mb-6 flex items-center gap-2">
          <Star class="w-5 h-5 text-neon-purple" /> 会员权益概览
        </h3>
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div v-for="(item, index) in privileges" :key="index" class="flex items-center gap-4 p-4 rounded-xl bg-slate-700/30 hover:bg-slate-700/50 transition-colors border border-white/5 hover:border-white/10">
            <div class="w-12 h-12 rounded-full bg-slate-800 flex items-center justify-center border border-white/10">
              <component :is="item.icon" class="w-5 h-5 text-neon-blue" />
            </div>
            <div>
              <h4 class="font-bold text-white text-sm">{{ item.title }}</h4>
              <p class="text-xs text-gray-400">{{ item.desc }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Daily Tasks -->
      <div class="bg-slate-800 rounded-2xl p-8 border border-white/5">
        <h3 class="text-xl font-bold text-white mb-6 flex items-center gap-2">
          <Activity class="w-5 h-5 text-green-400" /> 每日任务
        </h3>
        <div class="space-y-6">
          <div v-for="(task, i) in tasks" :key="i">
            <div class="flex justify-between items-center mb-2">
              <span class="text-gray-200 font-medium text-sm">{{ task.title }}</span>
              <span class="text-xs font-bold text-neon-blue">{{ task.reward }}</span>
            </div>
            <div class="flex items-center gap-3">
              <div class="flex-1 h-2 bg-black/20 rounded-full overflow-hidden">
                <div 
                  class="h-full bg-gradient-to-r from-green-400 to-emerald-500 rounded-full" 
                  :style="{ width: (task.progress / task.total) * 100 + '%' }"
                ></div>
              </div>
              <button 
                class="text-xs px-3 py-1 rounded-full border border-white/10 hover:bg-white/10 transition-colors"
                :class="task.progress >= task.total ? 'text-green-400 border-green-400/30' : 'text-gray-400'"
              >
                {{ task.status }}
              </button>
            </div>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<style scoped>
.text-shadow {
  text-shadow: 0 2px 4px rgba(0,0,0,0.3);
}
</style>

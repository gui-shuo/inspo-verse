<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { Users, CreditCard, Activity, TrendingUp, MessageSquare, Bot, Film, Gamepad2 } from 'lucide-vue-next'
import { getDashboardStats, getUserGrowthTrend, getRevenueComposition } from '@/api/admin'

const growthChartRef = ref<HTMLElement | null>(null)
const pieChartRef = ref<HTMLElement | null>(null)
let growthChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null

const stats = ref<Record<string, any>>({})
const loading = ref(true)

const loadData = async () => {
  loading.value = true
  try {
    const [statsData, growthData, revenueData] = await Promise.all([
      getDashboardStats(),
      getUserGrowthTrend(14),
      getRevenueComposition()
    ])
    stats.value = statsData
    initGrowthChart(growthData)
    initPieChart(revenueData)
  } catch (e) {
    console.error('仪表盘数据加载失败', e)
  } finally {
    loading.value = false
  }
}

const initGrowthChart = (data: Array<{ date: string; newUsers: number }>) => {
  if (!growthChartRef.value) return
  growthChart = echarts.init(growthChartRef.value)
  
  const option = {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: data.map(d => d.date),
      axisLine: { lineStyle: { color: '#94a3b8' } }
    },
    yAxis: {
      type: 'value',
      axisLine: { lineStyle: { color: '#94a3b8' } },
      splitLine: { lineStyle: { color: 'rgba(255,255,255,0.1)' } }
    },
    series: [
      {
        name: '新增用户',
        type: 'line',
        smooth: true,
        lineStyle: { width: 3, color: '#00f3ff' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(0, 243, 255, 0.5)' },
            { offset: 1, color: 'rgba(0, 243, 255, 0)' }
          ])
        },
        data: data.map(d => d.newUsers)
      }
    ]
  }
  growthChart.setOption(option)
}

const initPieChart = (data: Array<{ name: string; value: number }>) => {
  if (!pieChartRef.value) return
  pieChart = echarts.init(pieChartRef.value)
  
  const colors = ['#00f3ff', '#bc13fe', '#ff00ff', '#0aff00', '#F59E0B']
  const option = {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
    legend: { bottom: '5%', left: 'center', textStyle: { color: '#94a3b8' } },
    series: [{
      name: '收入构成',
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 10, borderColor: '#1e293b', borderWidth: 2 },
      label: { show: false },
      labelLine: { show: false },
      data: data.map((d, i) => ({ value: d.value, name: d.name, itemStyle: { color: colors[i % colors.length] } }))
    }]
  }
  pieChart.setOption(option)
}

const handleResize = () => {
  growthChart?.resize()
  pieChart?.resize()
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  growthChart?.dispose()
  pieChart?.dispose()
})
</script>

<template>
  <div class="space-y-6 animate__animated animate__fadeIn">
    <div v-if="loading" class="flex items-center justify-center h-64">
      <div class="w-10 h-10 border-4 border-neon-blue border-t-transparent rounded-full animate-spin"></div>
    </div>
    <template v-else>
      <!-- Stats Cards -->
      <div class="grid grid-cols-1 md:grid-cols-4 gap-6">
        <div class="bg-slate-800 p-6 rounded-xl border border-white/5 hover:border-neon-blue/50 transition-colors">
          <div class="flex justify-between items-start">
            <div>
              <p class="text-sm text-gray-400">总用户数</p>
              <h3 class="text-2xl font-bold text-white mt-1">{{ stats.totalUsers?.toLocaleString() || 0 }}</h3>
            </div>
            <div class="p-2 bg-neon-blue/10 rounded-lg text-neon-blue"><Users class="w-5 h-5" /></div>
          </div>
          <p class="text-xs text-green-400 mt-4 flex items-center">
            <TrendingUp class="w-3 h-3 mr-1" /> 今日新增 {{ stats.todayNewUsers || 0 }}
          </p>
        </div>

        <div class="bg-slate-800 p-6 rounded-xl border border-white/5 hover:border-neon-purple/50 transition-colors">
          <div class="flex justify-between items-start">
            <div>
              <p class="text-sm text-gray-400">今日收入</p>
              <h3 class="text-2xl font-bold text-white mt-1">¥ {{ stats.todayRevenue || '0.00' }}</h3>
            </div>
            <div class="p-2 bg-neon-purple/10 rounded-lg text-neon-purple"><CreditCard class="w-5 h-5" /></div>
          </div>
          <p class="text-xs text-green-400 mt-4 flex items-center">
            <TrendingUp class="w-3 h-3 mr-1" /> 累计订单 {{ stats.totalPaidOrders || 0 }}
          </p>
        </div>
        
        <div class="bg-slate-800 p-6 rounded-xl border border-white/5 hover:border-pink-500/50 transition-colors">
          <div class="flex justify-between items-start">
            <div>
              <p class="text-sm text-gray-400">在线用户</p>
              <h3 class="text-2xl font-bold text-white mt-1">{{ stats.onlineUsers || 0 }}</h3>
            </div>
            <div class="p-2 bg-pink-500/10 rounded-lg text-pink-500"><Activity class="w-5 h-5" /></div>
          </div>
          <p class="text-xs text-gray-400 mt-4">活跃VIP {{ stats.totalActiveVip || 0 }}</p>
        </div>

        <div class="bg-slate-800 p-6 rounded-xl border border-white/5 hover:border-green-500/50 transition-colors">
          <div class="flex justify-between items-start">
            <div>
              <p class="text-sm text-gray-400">今日AI消息</p>
              <h3 class="text-2xl font-bold text-white mt-1">{{ stats.todayAiMessages?.toLocaleString() || 0 }}</h3>
            </div>
            <div class="p-2 bg-green-500/10 rounded-lg text-green-500"><Bot class="w-5 h-5" /></div>
          </div>
          <p class="text-xs text-gray-400 mt-4">Token消耗 {{ stats.todayTokenUsage?.toLocaleString() || 0 }}</p>
        </div>
      </div>

      <!-- Secondary Stats -->
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div class="bg-slate-800 p-4 rounded-xl border border-white/5">
          <div class="flex items-center gap-2 text-gray-400 text-xs mb-1"><MessageSquare class="w-3 h-3" /> 帖子总数</div>
          <div class="text-lg font-bold text-white">{{ stats.totalPosts?.toLocaleString() || 0 }}</div>
        </div>
        <div class="bg-slate-800 p-4 rounded-xl border border-white/5">
          <div class="flex items-center gap-2 text-gray-400 text-xs mb-1"><Film class="w-3 h-3" /> 番剧总数</div>
          <div class="text-lg font-bold text-white">{{ stats.totalAnime || 0 }}</div>
        </div>
        <div class="bg-slate-800 p-4 rounded-xl border border-white/5">
          <div class="flex items-center gap-2 text-gray-400 text-xs mb-1"><Gamepad2 class="w-3 h-3" /> 游戏总数</div>
          <div class="text-lg font-bold text-white">{{ stats.totalGames || 0 }}</div>
        </div>
        <div class="bg-slate-800 p-4 rounded-xl border border-white/5">
          <div class="flex items-center gap-2 text-gray-400 text-xs mb-1"><Activity class="w-3 h-3" /> 工坊项目</div>
          <div class="text-lg font-bold text-white">{{ stats.totalWorkshopProjects || 0 }}</div>
        </div>
      </div>

      <!-- Charts Section -->
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div class="lg:col-span-2 bg-slate-800 p-6 rounded-xl border border-white/5">
          <h3 class="text-lg font-bold text-white mb-6">用户增长趋势（近14天）</h3>
          <div ref="growthChartRef" class="w-full h-[350px]"></div>
        </div>
        <div class="bg-slate-800 p-6 rounded-xl border border-white/5">
          <h3 class="text-lg font-bold text-white mb-6">收入构成</h3>
          <div ref="pieChartRef" class="w-full h-[350px]"></div>
        </div>
      </div>
    </template>
  </div>
</template>

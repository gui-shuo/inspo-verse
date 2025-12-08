<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { Users, CreditCard, Activity, TrendingUp } from 'lucide-vue-next'

const growthChartRef = ref<HTMLElement | null>(null)
const pieChartRef = ref<HTMLElement | null>(null)
let growthChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null

const initGrowthChart = () => {
  if (!growthChartRef.value) return
  growthChart = echarts.init(growthChartRef.value)
  
  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
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
        data: [120, 132, 101, 134, 90, 230, 210, 240, 280, 250, 320, 380, 410, 450]
      },
      {
        name: '活跃用户',
        type: 'line',
        smooth: true,
        lineStyle: { width: 3, color: '#bc13fe' },
        data: [220, 182, 191, 234, 290, 330, 310, 350, 400, 380, 420, 480, 500, 550]
      }
    ]
  }
  growthChart.setOption(option)
}

const initPieChart = () => {
  if (!pieChartRef.value) return
  pieChart = echarts.init(pieChartRef.value)
  
  const option = {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'item' },
    legend: {
      bottom: '5%',
      left: 'center',
      textStyle: { color: '#94a3b8' }
    },
    series: [
      {
        name: '订单分布',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#1e293b',
          borderWidth: 2
        },
        label: { show: false },
        labelLine: { show: false },
        data: [
          { value: 1048, name: '游戏会员', itemStyle: { color: '#00f3ff' } },
          { value: 735, name: '周边商城', itemStyle: { color: '#bc13fe' } },
          { value: 580, name: '课程订阅', itemStyle: { color: '#ff00ff' } },
          { value: 484, name: '广告投放', itemStyle: { color: '#0aff00' } },
          { value: 300, name: '企业服务', itemStyle: { color: '#F59E0B' } },
        ]
      }
    ]
  }
  pieChart.setOption(option)
}

const handleResize = () => {
  growthChart?.resize()
  pieChart?.resize()
}

onMounted(() => {
  initGrowthChart()
  initPieChart()
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
    <!-- Stats Cards -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-6">
      <div class="bg-slate-800 p-6 rounded-xl border border-white/5 hover:border-neon-blue/50 transition-colors">
        <div class="flex justify-between items-start">
          <div>
            <p class="text-sm text-gray-400">总用户数</p>
            <h3 class="text-2xl font-bold text-white mt-1">12,345</h3>
          </div>
          <div class="p-2 bg-neon-blue/10 rounded-lg text-neon-blue">
            <Users class="w-5 h-5" />
          </div>
        </div>
        <p class="text-xs text-green-400 mt-4 flex items-center">
          <TrendingUp class="w-3 h-3 mr-1" /> +12% 较上周
        </p>
      </div>

      <div class="bg-slate-800 p-6 rounded-xl border border-white/5 hover:border-neon-purple/50 transition-colors">
        <div class="flex justify-between items-start">
          <div>
            <p class="text-sm text-gray-400">今日订单</p>
            <h3 class="text-2xl font-bold text-white mt-1">¥ 8,245</h3>
          </div>
          <div class="p-2 bg-neon-purple/10 rounded-lg text-neon-purple">
            <CreditCard class="w-5 h-5" />
          </div>
        </div>
        <p class="text-xs text-green-400 mt-4 flex items-center">
          <TrendingUp class="w-3 h-3 mr-1" /> +5% 较昨日
        </p>
      </div>
      
      <!-- More cards placeholders -->
      <div class="bg-slate-800 p-6 rounded-xl border border-white/5">
        <div class="flex justify-between items-start">
          <div>
            <p class="text-sm text-gray-400">活跃设备</p>
            <h3 class="text-2xl font-bold text-white mt-1">452</h3>
          </div>
          <div class="p-2 bg-pink-500/10 rounded-lg text-pink-500">
            <Activity class="w-5 h-5" />
          </div>
        </div>
      </div>
    </div>

    <!-- Charts Section -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <!-- Line Chart -->
      <div class="lg:col-span-2 bg-slate-800 p-6 rounded-xl border border-white/5">
        <h3 class="text-lg font-bold text-white mb-6">增长趋势</h3>
        <div ref="growthChartRef" class="w-full h-[350px]"></div>
      </div>

      <!-- Pie Chart -->
      <div class="bg-slate-800 p-6 rounded-xl border border-white/5">
        <h3 class="text-lg font-bold text-white mb-6">收入构成</h3>
        <div ref="pieChartRef" class="w-full h-[350px]"></div>
      </div>
    </div>
  </div>
</template>

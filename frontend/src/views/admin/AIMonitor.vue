<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { Activity, Bot, ChevronLeft, ChevronRight } from 'lucide-vue-next'
import { useToastStore } from '@/stores/toast'
import { getAiStats, getAiMessageLogs } from '@/api/admin'

const toast = useToastStore()

const stats = ref<Record<string, any>>({})
const logs = ref<any[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const safetyFilter = ref('')

const loadData = async () => {
  loading.value = true
  try {
    const [statsRes, logsRes] = await Promise.all([
      getAiStats(),
      getAiMessageLogs({ page: currentPage.value, size: pageSize.value, safetyFilter: safetyFilter.value || undefined })
    ])
    stats.value = statsRes
    logs.value = logsRes.records || []
    total.value = logsRes.total || 0
  } catch { toast.error('加载AI数据失败') }
  finally { loading.value = false }
}

watch(safetyFilter, () => { currentPage.value = 1; loadData() })

const totalPages = () => Math.max(1, Math.ceil(total.value / pageSize.value))
const prevPage = () => { if (currentPage.value > 1) { currentPage.value--; loadData() } }
const nextPage = () => { if (currentPage.value < totalPages()) { currentPage.value++; loadData() } }

onMounted(loadData)
</script>

<template>
  <div class="space-y-6 animate__animated animate__fadeIn">
    <div class="flex flex-col md:flex-row justify-between items-center gap-4">
      <h2 class="text-2xl font-bold text-white">AI 服务监控</h2>
      <select v-model="safetyFilter" class="bg-slate-800 border border-slate-700 text-sm text-white rounded-lg px-3 py-2 focus:border-neon-blue focus:outline-none">
        <option value="">全部消息</option>
        <option value="blocked">仅拦截</option>
      </select>
    </div>

    <!-- Stats Cards -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-6">
      <div class="bg-slate-800 p-6 rounded-xl border border-white/5">
        <div class="text-gray-400 text-sm mb-1">今日 Token 消耗</div>
        <div class="text-3xl font-bold text-neon-blue">{{ stats.todayTokenUsage?.toLocaleString() || 0 }}</div>
      </div>
      <div class="bg-slate-800 p-6 rounded-xl border border-white/5">
        <div class="text-gray-400 text-sm mb-1">今日消息数</div>
        <div class="text-3xl font-bold text-neon-purple">{{ stats.todayMessages?.toLocaleString() || 0 }}</div>
      </div>
      <div class="bg-slate-800 p-6 rounded-xl border border-white/5">
        <div class="text-gray-400 text-sm mb-1">总会话数</div>
        <div class="text-3xl font-bold text-green-400">{{ stats.totalSessions?.toLocaleString() || 0 }}</div>
      </div>
      <div class="bg-slate-800 p-6 rounded-xl border border-white/5">
        <div class="text-gray-400 text-sm mb-1">活跃用户</div>
        <div class="text-3xl font-bold text-yellow-400">{{ stats.activeAiUsers || 0 }}</div>
      </div>
    </div>

    <!-- Logs Table -->
    <div v-if="loading" class="flex items-center justify-center h-32">
      <div class="w-8 h-8 border-4 border-neon-blue border-t-transparent rounded-full animate-spin"></div>
    </div>

    <div v-else class="bg-slate-800 rounded-xl border border-white/5 overflow-hidden">
      <div class="overflow-x-auto">
        <table class="w-full text-left text-sm">
          <thead class="bg-slate-900/50 border-b border-white/5 text-gray-400">
            <tr>
              <th class="p-4">用户</th>
              <th class="p-4">角色</th>
              <th class="p-4">内容摘要</th>
              <th class="p-4">Token</th>
              <th class="p-4 text-right">时间</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-white/5">
            <tr v-for="log in logs" :key="log.id" class="hover:bg-white/5">
              <td class="p-4 text-white">{{ log.username || log.userId }}</td>
              <td class="p-4">
                <span v-if="log.role === 'user'" class="text-neon-blue flex items-center gap-1"><Activity class="w-3 h-3" /> 用户</span>
                <span v-else class="text-neon-purple flex items-center gap-1"><Bot class="w-3 h-3" /> AI</span>
              </td>
              <td class="p-4 text-gray-300 truncate max-w-[300px]">{{ log.content?.substring(0, 80) }}</td>
              <td class="p-4 font-mono text-neon-blue">{{ log.tokenUsage || '-' }}</td>
              <td class="p-4 text-right text-gray-500 text-xs">{{ log.createdAt?.substring(0, 16) }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="logs.length === 0" class="p-8 text-center text-gray-500">暂无记录</div>

      <div v-if="total > pageSize" class="flex items-center justify-between p-4 border-t border-white/5">
        <span class="text-sm text-gray-400">共 {{ total }} 条</span>
        <div class="flex items-center gap-2">
          <button @click="prevPage" :disabled="currentPage <= 1" class="p-1.5 rounded hover:bg-white/10 text-gray-400 disabled:opacity-30"><ChevronLeft class="w-4 h-4" /></button>
          <span class="text-sm text-gray-300">{{ currentPage }} / {{ totalPages() }}</span>
          <button @click="nextPage" :disabled="currentPage >= totalPages()" class="p-1.5 rounded hover:bg-white/10 text-gray-400 disabled:opacity-30"><ChevronRight class="w-4 h-4" /></button>
        </div>
      </div>
    </div>
  </div>
</template>

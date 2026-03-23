<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { CheckCircle, Clock, XCircle, Search, ChevronLeft, ChevronRight } from 'lucide-vue-next'
import { useToastStore } from '@/stores/toast'
import { getAdminOrders } from '@/api/admin'

const toast = useToastStore()

const orders = ref<any[]>([])
const loading = ref(false)
const searchQuery = ref('')
const statusFilter = ref('')
const bizTypeFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(15)
const total = ref(0)

let searchTimer: ReturnType<typeof setTimeout> | null = null

const loadOrders = async () => {
  loading.value = true
  try {
    const res = await getAdminOrders({
      page: currentPage.value, size: pageSize.value,
      keyword: searchQuery.value || undefined,
      status: statusFilter.value || undefined,
      bizType: bizTypeFilter.value || undefined
    })
    orders.value = res.records || []
    total.value = res.total || 0
  } catch { toast.error('加载订单失败') }
  finally { loading.value = false }
}

watch([searchQuery, statusFilter, bizTypeFilter], () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => { currentPage.value = 1; loadOrders() }, 400)
})

const totalPages = () => Math.max(1, Math.ceil(total.value / pageSize.value))
const prevPage = () => { if (currentPage.value > 1) { currentPage.value--; loadOrders() } }
const nextPage = () => { if (currentPage.value < totalPages()) { currentPage.value++; loadOrders() } }

const statusMap: Record<string, { label: string; class: string; icon: any }> = {
  PAID: { label: '已支付', class: 'text-green-400', icon: CheckCircle },
  PENDING: { label: '待支付', class: 'text-yellow-400', icon: Clock },
  CLOSED: { label: '已关闭', class: 'text-gray-400', icon: XCircle },
  REFUNDED: { label: '已退款', class: 'text-red-400', icon: XCircle },
}

const getStatus = (status: string) => statusMap[status] || { label: status, class: 'text-gray-400', icon: Clock }

const bizTypeMap: Record<string, string> = {
  VIP: '会员',
  CREDITS: '点数',
  WORKSHOP: '工坊',
  GAME: '游戏',
}

onMounted(loadOrders)
</script>

<template>
  <div class="space-y-6 animate__animated animate__fadeIn">
    <div class="flex flex-col md:flex-row justify-between items-center gap-4">
      <h2 class="text-2xl font-bold text-white">订单管理</h2>
      <div class="flex gap-3 flex-wrap">
        <select v-model="statusFilter" class="bg-slate-800 border border-slate-700 text-sm text-white rounded-lg px-3 py-2 focus:border-neon-blue focus:outline-none">
          <option value="">全部状态</option>
          <option value="PAID">已支付</option>
          <option value="PENDING">待支付</option>
          <option value="CLOSED">已关闭</option>
          <option value="REFUNDED">已退款</option>
        </select>
        <select v-model="bizTypeFilter" class="bg-slate-800 border border-slate-700 text-sm text-white rounded-lg px-3 py-2 focus:border-neon-blue focus:outline-none">
          <option value="">全部类型</option>
          <option value="VIP">会员</option>
          <option value="CREDITS">点数</option>
          <option value="WORKSHOP">工坊</option>
          <option value="GAME">游戏</option>
        </select>
        <div class="relative w-56">
          <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input v-model="searchQuery" type="text" placeholder="搜索订单号..." class="w-full bg-slate-800 border border-slate-700 rounded-lg pl-10 pr-4 py-2 text-sm text-white focus:border-neon-blue focus:outline-none" />
        </div>
      </div>
    </div>

    <div v-if="loading" class="flex items-center justify-center h-32">
      <div class="w-8 h-8 border-4 border-neon-blue border-t-transparent rounded-full animate-spin"></div>
    </div>

    <div v-else class="bg-slate-800 rounded-xl border border-white/5 overflow-hidden">
      <div class="overflow-x-auto">
        <table class="w-full text-left text-sm">
          <thead class="bg-slate-900/50 border-b border-white/5 text-gray-400">
            <tr>
              <th class="p-4">订单号</th>
              <th class="p-4">用户</th>
              <th class="p-4">类型</th>
              <th class="p-4">商品</th>
              <th class="p-4">金额</th>
              <th class="p-4">状态</th>
              <th class="p-4 text-right">时间</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-white/5">
            <tr v-for="order in orders" :key="order.id" class="hover:bg-white/5">
              <td class="p-4 font-mono text-gray-400 text-xs">{{ order.orderNo }}</td>
              <td class="p-4 text-white">{{ order.username || order.userId }}</td>
              <td class="p-4 text-gray-300">
                <span class="bg-slate-700 px-2 py-0.5 rounded text-xs">{{ bizTypeMap[order.bizType] || order.bizType }}</span>
              </td>
              <td class="p-4 text-gray-300 max-w-[200px] truncate">{{ order.subject || '-' }}</td>
              <td class="p-4 font-bold text-neon-blue">¥ {{ order.totalAmount }}</td>
              <td class="p-4">
                <span :class="getStatus(order.status).class" class="flex items-center gap-1">
                  <component :is="getStatus(order.status).icon" class="w-3 h-3" />
                  {{ getStatus(order.status).label }}
                </span>
              </td>
              <td class="p-4 text-right text-gray-500 text-xs">{{ order.createdAt?.substring(0, 16) }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="orders.length === 0" class="p-8 text-center text-gray-500">暂无订单</div>

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

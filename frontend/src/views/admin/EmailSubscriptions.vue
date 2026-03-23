<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ChevronLeft, ChevronRight } from 'lucide-vue-next'
import { useToastStore } from '@/stores/toast'
import { getEmailSubscriptions } from '@/api/admin'

const toast = useToastStore()
const items = ref<any[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

const loadData = async () => {
  loading.value = true
  try {
    const res = await getEmailSubscriptions({ page: currentPage.value, size: pageSize.value })
    items.value = res.records || []
    total.value = res.total || 0
  } catch { toast.error('加载订阅列表失败') }
  finally { loading.value = false }
}

const totalPages = () => Math.max(1, Math.ceil(total.value / pageSize.value))
const prevPage = () => { if (currentPage.value > 1) { currentPage.value--; loadData() } }
const nextPage = () => { if (currentPage.value < totalPages()) { currentPage.value++; loadData() } }

onMounted(loadData)
</script>

<template>
  <div class="space-y-6 animate__animated animate__fadeIn">
    <h2 class="text-2xl font-bold text-white">邮件订阅管理</h2>

    <div v-if="loading" class="flex items-center justify-center h-32">
      <div class="w-8 h-8 border-4 border-neon-blue border-t-transparent rounded-full animate-spin"></div>
    </div>

    <div v-else class="bg-slate-800 rounded-xl border border-white/5 overflow-hidden">
      <div class="overflow-x-auto">
        <table class="w-full text-left text-sm">
          <thead class="bg-slate-900/50 border-b border-white/5 text-gray-400">
            <tr>
              <th class="p-4">ID</th>
              <th class="p-4">邮箱地址</th>
              <th class="p-4">订阅时间</th>
              <th class="p-4">状态</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-white/5">
            <tr v-for="item in items" :key="item.id" class="hover:bg-white/5">
              <td class="p-4 text-gray-500 font-mono text-xs">{{ item.id }}</td>
              <td class="p-4 text-white">{{ item.email }}</td>
              <td class="p-4 text-gray-400">{{ item.createdAt?.substring(0, 16) }}</td>
              <td class="p-4">
                <span v-if="item.status === 1" class="text-green-400">活跃</span>
                <span v-else class="text-gray-400">已退订</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="items.length === 0" class="p-8 text-center text-gray-500">暂无订阅记录</div>

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

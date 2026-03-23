<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { Trash2, Search, Eye, EyeOff, ChevronLeft, ChevronRight } from 'lucide-vue-next'
import { useToastStore } from '@/stores/toast'
import { useModalStore } from '@/stores/modal'
import { getAdminWorkshop, updateWorkshopStatus, deleteWorkshopProject } from '@/api/admin'

const toast = useToastStore()
const modalStore = useModalStore()

const items = ref<any[]>([])
const loading = ref(false)
const searchQuery = ref('')
const categoryFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(15)
const total = ref(0)

let searchTimer: ReturnType<typeof setTimeout> | null = null

const loadProjects = async () => {
  loading.value = true
  try {
    const res = await getAdminWorkshop({ page: currentPage.value, size: pageSize.value, keyword: searchQuery.value || undefined, category: categoryFilter.value || undefined })
    items.value = res.records || []
    total.value = res.total || 0
  } catch { toast.error('加载工坊项目失败') }
  finally { loading.value = false }
}

watch([searchQuery, categoryFilter], () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => { currentPage.value = 1; loadProjects() }, 400)
})

const totalPages = () => Math.max(1, Math.ceil(total.value / pageSize.value))
const prevPage = () => { if (currentPage.value > 1) { currentPage.value--; loadProjects() } }
const nextPage = () => { if (currentPage.value < totalPages()) { currentPage.value++; loadProjects() } }

const handleToggleStatus = async (item: any) => {
  const newStatus = item.status === 1 ? 0 : 1
  try { await updateWorkshopStatus(item.id, newStatus); toast.success(newStatus === 1 ? '已上架' : '已下架'); loadProjects() }
  catch { toast.error('操作失败') }
}

const handleDelete = async (item: any) => {
  const confirmed = await modalStore.open({
    title: '删除项目',
    content: `确定删除「${item.title}」？操作不可恢复。`,
    type: 'error',
    confirmText: '确认删除'
  })
  if (confirmed) {
    try { await deleteWorkshopProject(item.id); toast.success('已删除'); loadProjects() }
    catch { toast.error('删除失败') }
  }
}

onMounted(loadProjects)
</script>

<template>
  <div class="space-y-6 animate__animated animate__fadeIn">
    <div class="flex flex-col md:flex-row justify-between items-center gap-4">
      <h2 class="text-2xl font-bold text-white">创意工坊管理</h2>
      <div class="flex gap-3">
        <select v-model="categoryFilter" class="bg-slate-800 border border-slate-700 text-sm text-white rounded-lg px-3 py-2 focus:border-neon-blue focus:outline-none">
          <option value="">全部分类</option>
          <option value="mod">MOD</option>
          <option value="skin">皮肤</option>
          <option value="map">地图</option>
          <option value="tool">工具</option>
          <option value="other">其他</option>
        </select>
        <div class="relative w-56">
          <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input v-model="searchQuery" type="text" placeholder="搜索项目..." class="w-full bg-slate-800 border border-slate-700 rounded-lg pl-10 pr-4 py-2 text-sm text-white focus:border-neon-blue focus:outline-none" />
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
              <th class="p-4">封面</th>
              <th class="p-4">项目名称</th>
              <th class="p-4">作者</th>
              <th class="p-4">分类</th>
              <th class="p-4">下载量</th>
              <th class="p-4">状态</th>
              <th class="p-4 text-right">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-white/5">
            <tr v-for="item in items" :key="item.id" class="hover:bg-white/5 transition-colors">
              <td class="p-4">
                <img v-if="item.coverUrl" :src="item.coverUrl" class="w-16 h-10 rounded object-cover" />
                <div v-else class="w-16 h-10 rounded bg-slate-700"></div>
              </td>
              <td class="p-4 font-medium text-white max-w-[200px] truncate">{{ item.title }}</td>
              <td class="p-4 text-gray-400">{{ item.authorName || '-' }}</td>
              <td class="p-4 text-gray-400">
                <span class="bg-slate-700 px-2 py-0.5 rounded text-xs">{{ item.category || '-' }}</span>
              </td>
              <td class="p-4 text-neon-blue font-mono">{{ item.downloadCount || 0 }}</td>
              <td class="p-4">
                <span v-if="item.status === 1" class="text-green-400">上架</span>
                <span v-else class="text-red-400">下架</span>
              </td>
              <td class="p-4 text-right space-x-1">
                <button @click="handleToggleStatus(item)" class="p-2 hover:bg-white/10 rounded text-gray-400 hover:text-white" :title="item.status === 1 ? '下架' : '上架'">
                  <component :is="item.status === 1 ? EyeOff : Eye" class="w-4 h-4" />
                </button>
                <button @click="handleDelete(item)" class="p-2 hover:bg-red-500/20 rounded text-gray-400 hover:text-red-400" title="删除">
                  <Trash2 class="w-4 h-4" />
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="items.length === 0" class="p-8 text-center text-gray-500">暂无项目</div>

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

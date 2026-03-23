<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { Trash2, Search, Eye, EyeOff, ChevronLeft, ChevronRight } from 'lucide-vue-next'
import { useToastStore } from '@/stores/toast'
import { useModalStore } from '@/stores/modal'
import { getAdminGames, updateGameStatus, deleteGame } from '@/api/admin'

const toast = useToastStore()
const modalStore = useModalStore()

const items = ref<any[]>([])
const loading = ref(false)
const searchQuery = ref('')
const genreFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(15)
const total = ref(0)

let searchTimer: ReturnType<typeof setTimeout> | null = null

const loadGames = async () => {
  loading.value = true
  try {
    const res = await getAdminGames({ page: currentPage.value, size: pageSize.value, keyword: searchQuery.value || undefined, genre: genreFilter.value || undefined })
    items.value = res.records || []
    total.value = res.total || 0
  } catch { toast.error('加载游戏失败') }
  finally { loading.value = false }
}

watch([searchQuery, genreFilter], () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => { currentPage.value = 1; loadGames() }, 400)
})

const totalPages = () => Math.max(1, Math.ceil(total.value / pageSize.value))
const prevPage = () => { if (currentPage.value > 1) { currentPage.value--; loadGames() } }
const nextPage = () => { if (currentPage.value < totalPages()) { currentPage.value++; loadGames() } }

const handleToggleStatus = async (item: any) => {
  const newStatus = item.status === 1 ? 0 : 1
  try { await updateGameStatus(item.id, newStatus); toast.success(newStatus === 1 ? '已上架' : '已下架'); loadGames() }
  catch { toast.error('操作失败') }
}

const handleDelete = async (item: any) => {
  const confirmed = await modalStore.open({
    title: '删除游戏',
    content: `确定删除「${item.title}」？操作不可恢复。`,
    type: 'error',
    confirmText: '确认删除'
  })
  if (confirmed) {
    try { await deleteGame(item.id); toast.success('已删除'); loadGames() }
    catch { toast.error('删除失败') }
  }
}

onMounted(loadGames)
</script>

<template>
  <div class="space-y-6 animate__animated animate__fadeIn">
    <div class="flex flex-col md:flex-row justify-between items-center gap-4">
      <h2 class="text-2xl font-bold text-white">游戏管理</h2>
      <div class="flex gap-3">
        <select v-model="genreFilter" class="bg-slate-800 border border-slate-700 text-sm text-white rounded-lg px-3 py-2 focus:border-neon-blue focus:outline-none">
          <option value="">全部类型</option>
          <option value="RPG">RPG</option>
          <option value="FPS">FPS</option>
          <option value="MOBA">MOBA</option>
          <option value="Strategy">策略</option>
          <option value="Adventure">冒险</option>
        </select>
        <div class="relative w-56">
          <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input v-model="searchQuery" type="text" placeholder="搜索游戏..." class="w-full bg-slate-800 border border-slate-700 rounded-lg pl-10 pr-4 py-2 text-sm text-white focus:border-neon-blue focus:outline-none" />
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
              <th class="p-4">名称</th>
              <th class="p-4">类型</th>
              <th class="p-4">评分</th>
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
              <td class="p-4 text-gray-400">{{ item.genre || '-' }}</td>
              <td class="p-4 text-yellow-400 font-bold">{{ item.rating || '-' }}</td>
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

      <div v-if="items.length === 0" class="p-8 text-center text-gray-500">暂无游戏</div>

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

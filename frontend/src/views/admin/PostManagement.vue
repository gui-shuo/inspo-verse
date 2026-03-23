<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { Trash2, Star, Award, Search, ChevronLeft, ChevronRight, Ban, CheckCircle } from 'lucide-vue-next'
import { useToastStore } from '@/stores/toast'
import { useModalStore } from '@/stores/modal'
import { getAdminPosts, togglePostTop, togglePostEssence, deletePost as apiDeletePost, updatePostStatus } from '@/api/admin'

const toast = useToastStore()
const modalStore = useModalStore()

const posts = ref<any[]>([])
const loading = ref(false)
const searchQuery = ref('')
const currentPage = ref(1)
const pageSize = ref(15)
const total = ref(0)
const statusFilter = ref('')

let searchTimer: ReturnType<typeof setTimeout> | null = null

const loadPosts = async () => {
  loading.value = true
  try {
    const res = await getAdminPosts({ page: currentPage.value, size: pageSize.value, keyword: searchQuery.value, status: statusFilter.value || undefined })
    posts.value = res.records || []
    total.value = res.total || 0
  } catch { toast.error('加载帖子失败') }
  finally { loading.value = false }
}

watch([searchQuery, statusFilter], () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => { currentPage.value = 1; loadPosts() }, 400)
})

const totalPages = () => Math.max(1, Math.ceil(total.value / pageSize.value))
const prevPage = () => { if (currentPage.value > 1) { currentPage.value--; loadPosts() } }
const nextPage = () => { if (currentPage.value < totalPages()) { currentPage.value++; loadPosts() } }

const handleToggleTop = async (post: any) => {
  try { await togglePostTop(post.id); toast.success(post.isTop ? '已取消置顶' : '已置顶'); loadPosts() }
  catch { toast.error('操作失败') }
}

const handleToggleEssence = async (post: any) => {
  try { await togglePostEssence(post.id); toast.success(post.isEssence ? '已取消精华' : '已设为精华'); loadPosts() }
  catch { toast.error('操作失败') }
}

const handleDelete = async (post: any) => {
  const confirmed = await modalStore.open({
    title: '删除帖子',
    content: `确定要删除帖子「${post.title}」吗？操作不可恢复。`,
    type: 'error',
    confirmText: '确认删除'
  })
  if (confirmed) {
    try { await apiDeletePost(post.id); toast.success('已删除'); loadPosts() }
    catch { toast.error('删除失败') }
  }
}

const handleToggleStatus = async (post: any) => {
  const newStatus = post.status === 1 ? 0 : 1
  try { await updatePostStatus(post.id, newStatus); toast.success(newStatus === 1 ? '已恢复' : '已隐藏'); loadPosts() }
  catch { toast.error('操作失败') }
}

onMounted(loadPosts)
</script>

<template>
  <div class="space-y-6 animate__animated animate__fadeIn">
    <div class="flex flex-col md:flex-row justify-between items-center gap-4">
      <h2 class="text-2xl font-bold text-white">帖子管理</h2>
      <div class="flex gap-3">
        <select v-model="statusFilter" class="bg-slate-800 border border-slate-700 text-sm text-white rounded-lg px-3 py-2 focus:border-neon-blue focus:outline-none">
          <option value="">全部状态</option>
          <option value="1">正常</option>
          <option value="0">隐藏</option>
        </select>
        <div class="relative w-64">
          <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input v-model="searchQuery" type="text" placeholder="搜索帖子标题..." class="w-full bg-slate-800 border border-slate-700 rounded-lg pl-10 pr-4 py-2 text-sm text-white focus:border-neon-blue focus:outline-none" />
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
              <th class="p-4">标题</th>
              <th class="p-4">作者</th>
              <th class="p-4">状态</th>
              <th class="p-4">浏览/回复</th>
              <th class="p-4">发布时间</th>
              <th class="p-4 text-right">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-white/5">
            <tr v-for="post in posts" :key="post.id" class="hover:bg-white/5 transition-colors">
              <td class="p-4 font-medium text-white max-w-[300px] truncate">
                <span v-if="post.isTop" class="bg-red-500/20 text-red-400 text-xs px-1.5 py-0.5 rounded mr-2">置顶</span>
                <span v-if="post.isEssence" class="bg-yellow-500/20 text-yellow-400 text-xs px-1.5 py-0.5 rounded mr-2">精华</span>
                {{ post.title }}
              </td>
              <td class="p-4 text-gray-400">{{ post.authorName || '-' }}</td>
              <td class="p-4">
                <span v-if="post.status === 1" class="text-green-400">正常</span>
                <span v-else class="text-red-400">已隐藏</span>
              </td>
              <td class="p-4 text-gray-400">{{ post.viewCount || 0 }} / {{ post.replyCount || 0 }}</td>
              <td class="p-4 text-gray-400">{{ post.createdAt?.substring(0, 10) }}</td>
              <td class="p-4 text-right flex justify-end gap-1">
                <button @click="handleToggleTop(post)" class="p-2 hover:bg-white/10 rounded text-gray-400 hover:text-yellow-400" title="置顶/取消">
                  <Star class="w-4 h-4" :class="post.isTop ? 'fill-yellow-400 text-yellow-400' : ''" />
                </button>
                <button @click="handleToggleEssence(post)" class="p-2 hover:bg-white/10 rounded text-gray-400 hover:text-orange-400" title="精华/取消">
                  <Award class="w-4 h-4" :class="post.isEssence ? 'fill-orange-400 text-orange-400' : ''" />
                </button>
                <button @click="handleToggleStatus(post)" class="p-2 hover:bg-white/10 rounded text-gray-400 hover:text-white" :title="post.status === 1 ? '隐藏' : '恢复'">
                  <component :is="post.status === 1 ? Ban : CheckCircle" class="w-4 h-4" />
                </button>
                <button @click="handleDelete(post)" class="p-2 hover:bg-red-500/20 rounded text-gray-400 hover:text-red-400" title="删除">
                  <Trash2 class="w-4 h-4" />
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="posts.length === 0" class="p-8 text-center text-gray-500">暂无帖子</div>

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

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import GlitchText from '@/components/ui/GlitchText.vue'
import { Download, Star, Cpu, X, Share2, Heart, Plus, Upload, Search, Filter, Edit, Trash2, Loader2 } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import type { WorkshopProject } from '@/api/workshop'
import {
  getWorkshopProjects,
  getWorkshopProject,
  createWorkshopProject,
  updateWorkshopProject,
  deleteWorkshopProject,
  toggleSubscription,
  toggleLike,
  toggleFavorite,
  rateProject,
  getInteractionStatus,
  uploadWorkshopCover,
} from '@/api/workshop'

const router = useRouter()
const auth = useAuthStore()
const toast = useToastStore()

// ====== 列表数据 ======
const projects = ref<WorkshopProject[]>([])
const loading = ref(false)
const totalItems = ref(0)
const currentPage = ref(1)
const pageSize = ref(12)
const searchKeyword = ref('')
const selectedCategory = ref('')
const sortBy = ref('hot')

const categories = [
  { value: '', label: '全部' },
  { value: 'ui', label: 'UI组件' },
  { value: 'tool', label: '工具' },
  { value: 'shader', label: '着色器' },
  { value: 'theme', label: '主题' },
  { value: 'audio', label: '音频' },
  { value: 'model', label: '3D模型' },
  { value: 'other', label: '其他' },
]

const totalPages = computed(() => Math.ceil(totalItems.value / pageSize.value))

async function fetchProjects() {
  loading.value = true
  try {
    const res = await getWorkshopProjects({
      category: selectedCategory.value || undefined,
      keyword: searchKeyword.value || undefined,
      sortBy: sortBy.value,
      page: currentPage.value,
      pageSize: pageSize.value,
    })
    if (res.code === 0 && res.data) {
      projects.value = res.data.items
      totalItems.value = res.data.total
    }
  } catch (e) {
    toast.error('加载项目列表失败')
  } finally {
    loading.value = false
  }
}

function onSearch() {
  currentPage.value = 1
  fetchProjects()
}

function onCategoryChange(cat: string) {
  selectedCategory.value = cat
  currentPage.value = 1
  fetchProjects()
}

function onSortChange(sort: string) {
  sortBy.value = sort
  currentPage.value = 1
  fetchProjects()
}

function prevPage() {
  if (currentPage.value > 1) { currentPage.value--; fetchProjects() }
}

function nextPage() {
  if (currentPage.value < totalPages.value) { currentPage.value++; fetchProjects() }
}

// ====== 详情弹窗 ======
const selectedItem = ref<WorkshopProject | null>(null)
const detailLoading = ref(false)
const interaction = ref({ liked: false, favorited: false, subscribed: false, myScore: 0 })
const interactionLoading = ref(false)

async function openDetail(item: WorkshopProject) {
  selectedItem.value = item
  detailLoading.value = true
  try {
    const res = await getWorkshopProject(item.id)
    if (res.code === 0 && res.data) {
      selectedItem.value = res.data
    }
  } catch (e) { /* use list data as fallback */ }
  detailLoading.value = false

  if (auth.isAuthenticated) {
    try {
      const iRes = await getInteractionStatus(item.id)
      if (iRes.code === 0 && iRes.data) {
        interaction.value = iRes.data
      }
    } catch (e) { /* ignore */ }
  }
}

function closeDetail() {
  selectedItem.value = null
  interaction.value = { liked: false, favorited: false, subscribed: false, myScore: 0 }
}

function parseTags(tags: string | null | undefined): string[] {
  if (!tags) return []
  try { return JSON.parse(tags) } catch { return tags.split(',').map(t => t.trim()).filter(Boolean) }
}

function formatDownloads(count: number): string {
  if (count >= 10000) return (count / 10000).toFixed(1) + 'w'
  if (count >= 1000) return (count / 1000).toFixed(1) + 'k'
  return String(count)
}

function formatRating(item: WorkshopProject): string {
  if (!item.rating || item.rating === 0) return '暂无'
  return item.rating.toFixed(1)
}

// ====== 交互操作 ======
async function handleSubscribe() {
  if (!auth.isAuthenticated) { router.push('/login'); return }
  if (!selectedItem.value || interactionLoading.value) return
  interactionLoading.value = true
  try {
    const res = await toggleSubscription(selectedItem.value.id)
    if (res.code === 0 && res.data) {
      interaction.value.subscribed = res.data.subscribed
      selectedItem.value.downloadCount += res.data.subscribed ? 1 : -1
      toast.success(res.data.subscribed ? '订阅成功' : '已取消订阅')
    }
  } catch (e) { toast.error('操作失败') }
  interactionLoading.value = false
}

async function handleLike() {
  if (!auth.isAuthenticated) { router.push('/login'); return }
  if (!selectedItem.value || interactionLoading.value) return
  interactionLoading.value = true
  try {
    const res = await toggleLike(selectedItem.value.id)
    if (res.code === 0 && res.data) {
      interaction.value.liked = res.data.liked
      selectedItem.value.likeCount += res.data.liked ? 1 : -1
      toast.success(res.data.liked ? '已点赞' : '已取消点赞')
    }
  } catch (e) { toast.error('操作失败') }
  interactionLoading.value = false
}

async function handleFavorite() {
  if (!auth.isAuthenticated) { router.push('/login'); return }
  if (!selectedItem.value || interactionLoading.value) return
  interactionLoading.value = true
  try {
    const res = await toggleFavorite(selectedItem.value.id)
    if (res.code === 0 && res.data) {
      interaction.value.favorited = res.data.favorited
      selectedItem.value.favoriteCount += res.data.favorited ? 1 : -1
      toast.success(res.data.favorited ? '已收藏' : '已取消收藏')
    }
  } catch (e) { toast.error('操作失败') }
  interactionLoading.value = false
}

// ====== 评分 ======
const showRatingPanel = ref(false)
const ratingScore = ref(0)
const ratingSubmitting = ref(false)

function openRating() {
  if (!auth.isAuthenticated) { router.push('/login'); return }
  ratingScore.value = interaction.value.myScore || 0
  showRatingPanel.value = true
}

async function submitRating() {
  if (!selectedItem.value || ratingScore.value < 1 || ratingSubmitting.value) return
  ratingSubmitting.value = true
  try {
    const res = await rateProject(selectedItem.value.id, ratingScore.value)
    if (res.code === 0 && res.data) {
      selectedItem.value.rating = res.data.rating
      selectedItem.value.ratingCount = res.data.ratingCount
      interaction.value.myScore = res.data.myScore
      toast.success('评分成功')
      showRatingPanel.value = false
    }
  } catch (e) { toast.error('评分失败') }
  ratingSubmitting.value = false
}

// ====== 分享 ======
function handleShare() {
  if (!selectedItem.value) return
  const url = window.location.origin + '/workshop?id=' + selectedItem.value.id
  navigator.clipboard.writeText(url).then(() => toast.success('链接已复制到剪贴板')).catch(() => toast.info('请手动复制链接'))
}

// ====== 发布/编辑弹窗 ======
const showEditor = ref(false)
const editorMode = ref<'create' | 'edit'>('create')
const editorLoading = ref(false)
const editingId = ref<number | null>(null)
const editorForm = ref({
  title: '',
  description: '',
  coverUrl: '',
  tags: '',
  version: 'v1.0.0',
  fileUrl: '',
  fileSize: '',
  category: 'other',
})
const coverUploading = ref(false)

function openCreateEditor() {
  if (!auth.isAuthenticated) { router.push('/login'); return }
  editorMode.value = 'create'
  editingId.value = null
  editorForm.value = { title: '', description: '', coverUrl: '', tags: '', version: 'v1.0.0', fileUrl: '', fileSize: '', category: 'other' }
  showEditor.value = true
}

function openEditEditor(item: WorkshopProject) {
  editorMode.value = 'edit'
  editingId.value = item.id
  editorForm.value = {
    title: item.title,
    description: item.description || '',
    coverUrl: item.coverUrl || '',
    tags: (() => { try { return JSON.parse(item.tags || '[]').join(', ') } catch { return item.tags || '' } })(),
    version: item.version || 'v1.0.0',
    fileUrl: item.fileUrl || '',
    fileSize: item.fileSize || '',
    category: item.category || 'other',
  }
  showEditor.value = true
  closeDetail()
}

async function handleCoverUpload(e: Event) {
  const input = e.target as HTMLInputElement
  if (!input.files?.length) return
  coverUploading.value = true
  try {
    const res = await uploadWorkshopCover(input.files[0])
    const url = res.data?.data?.url
    if (url) {
      editorForm.value.coverUrl = url
      toast.success('封面上传成功')
    }
  } catch (e) { toast.error('封面上传失败') }
  coverUploading.value = false
  input.value = ''
}

function formatTagsForSubmit(tags: string): string {
  const arr = tags.split(/[,，]/).map(t => t.trim()).filter(Boolean)
  return JSON.stringify(arr)
}

async function submitProject() {
  if (!editorForm.value.title.trim()) { toast.warning('请填写项目标题'); return }
  editorLoading.value = true
  try {
    const payload = {
      ...editorForm.value,
      tags: formatTagsForSubmit(editorForm.value.tags),
    }
    if (editorMode.value === 'create') {
      const res = await createWorkshopProject(payload)
      if (res.code === 0) {
        toast.success('项目发布成功')
        showEditor.value = false
        fetchProjects()
      } else {
        toast.error(res.message || '发布失败')
      }
    } else if (editingId.value) {
      const res = await updateWorkshopProject(editingId.value, payload)
      if (res.code === 0) {
        toast.success('项目更新成功')
        showEditor.value = false
        fetchProjects()
      } else {
        toast.error(res.message || '更新失败')
      }
    }
  } catch (e) { toast.error('操作失败') }
  editorLoading.value = false
}

async function handleDelete(item: WorkshopProject) {
  if (!confirm('确定要删除此项目吗？')) return
  try {
    const res = await deleteWorkshopProject(item.id)
    if (res.code === 0) {
      toast.success('删除成功')
      closeDetail()
      fetchProjects()
    } else {
      toast.error(res.message || '删除失败')
    }
  } catch (e) { toast.error('删除失败') }
}

// ====== 初始化 ======
onMounted(() => {
  fetchProjects()
})
</script>

<template>
  <div class="container mx-auto px-4 py-20 min-h-screen">
    <!-- 顶部标题 -->
    <div class="text-center mb-10">
      <h1 class="text-5xl font-black mb-4"><GlitchText text="WORKSHOP" /></h1>
      <p class="text-neon-green">社区共建 · 无限扩展</p>
    </div>

    <!-- 工具栏：搜索 + 筛选 + 发布 -->
    <div class="flex flex-col md:flex-row items-center gap-4 mb-8">
      <!-- 搜索 -->
      <div class="relative flex-1 w-full">
        <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
        <input
          v-model="searchKeyword"
          @keyup.enter="onSearch"
          type="text"
          placeholder="搜索工坊项目..."
          class="w-full pl-10 pr-4 py-2.5 bg-slate-800 border border-white/10 rounded-lg text-white text-sm placeholder-gray-500 focus:border-neon-green/50 focus:outline-none transition-colors"
        />
      </div>
      <!-- 分类筛选 -->
      <div class="flex items-center gap-2 flex-wrap">
        <button
          v-for="cat in categories" :key="cat.value"
          @click="onCategoryChange(cat.value)"
          :class="[
            'px-3 py-1.5 rounded-lg text-xs font-medium transition-all border',
            selectedCategory === cat.value
              ? 'bg-neon-green/20 border-neon-green/50 text-neon-green'
              : 'bg-slate-800 border-white/5 text-gray-400 hover:border-white/20'
          ]"
        >{{ cat.label }}</button>
      </div>
      <!-- 排序 -->
      <div class="flex items-center gap-2">
        <button @click="onSortChange('hot')" :class="['px-3 py-1.5 rounded-lg text-xs font-medium transition-all', sortBy === 'hot' ? 'bg-cyan-500/20 text-cyan-400' : 'text-gray-500 hover:text-gray-300']">热门</button>
        <button @click="onSortChange('new')" :class="['px-3 py-1.5 rounded-lg text-xs font-medium transition-all', sortBy === 'new' ? 'bg-cyan-500/20 text-cyan-400' : 'text-gray-500 hover:text-gray-300']">最新</button>
        <button @click="onSortChange('rating')" :class="['px-3 py-1.5 rounded-lg text-xs font-medium transition-all', sortBy === 'rating' ? 'bg-cyan-500/20 text-cyan-400' : 'text-gray-500 hover:text-gray-300']">评分</button>
      </div>
      <!-- 发布按钮 -->
      <button
        @click="openCreateEditor"
        class="flex items-center gap-2 px-5 py-2.5 bg-neon-green text-slate-900 font-bold rounded-lg hover:shadow-[0_0_15px_rgba(0,255,157,0.4)] transition-all text-sm whitespace-nowrap"
      >
        <Plus class="w-4 h-4" /> 发布项目
      </button>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="flex justify-center py-20">
      <Loader2 class="w-8 h-8 text-neon-green animate-spin" />
    </div>

    <!-- 空状态 -->
    <div v-else-if="projects.length === 0" class="text-center py-20">
      <Cpu class="w-16 h-16 text-gray-600 mx-auto mb-4" />
      <p class="text-gray-500 text-lg">暂无工坊项目</p>
      <p class="text-gray-600 text-sm mt-2">成为第一个发布者吧！</p>
    </div>

    <!-- 项目列表 -->
    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <div
        v-for="item in projects"
        :key="item.id"
        @click="openDetail(item)"
        class="bg-slate-800 rounded-xl overflow-hidden border border-white/5 hover:border-neon-green/50 transition-all hover:-translate-y-1 group cursor-pointer"
      >
        <div class="h-40 overflow-hidden relative">
          <img
            v-if="item.coverUrl"
            :src="item.coverUrl"
            class="w-full h-full object-cover group-hover:scale-110 transition-transform duration-500"
          />
          <div v-else class="w-full h-full bg-gradient-to-br from-slate-700 to-slate-800 flex items-center justify-center">
            <Cpu class="w-12 h-12 text-gray-600" />
          </div>
          <div class="absolute inset-0 bg-black/50 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
            <button class="bg-neon-green text-slate-900 px-4 py-2 rounded-lg font-bold flex items-center gap-2 transform translate-y-4 group-hover:translate-y-0 transition-transform">
              <Download class="w-4 h-4" /> 查看详情
            </button>
          </div>
        </div>

        <div class="p-4">
          <div class="flex items-start justify-between mb-2">
            <h3 class="font-bold text-white truncate flex-1 mr-2">{{ item.title }}</h3>
            <div class="flex items-center text-yellow-400 text-xs font-bold shrink-0">
              <Star class="w-3 h-3 fill-current mr-1" /> {{ formatRating(item) }}
            </div>
          </div>

          <div class="flex items-center justify-between text-xs text-gray-400">
            <span class="flex items-center gap-1"><Cpu class="w-3 h-3" /> {{ item.author?.nickname || item.author?.username || '匿名' }}</span>
            <span>{{ formatDownloads(item.downloadCount) }} 下载</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="totalPages > 1" class="flex justify-center items-center gap-4 mt-10">
      <button @click="prevPage" :disabled="currentPage <= 1" class="px-4 py-2 bg-slate-800 text-gray-300 rounded-lg disabled:opacity-30 hover:bg-slate-700 transition-colors text-sm">上一页</button>
      <span class="text-gray-400 text-sm">{{ currentPage }} / {{ totalPages }}</span>
      <button @click="nextPage" :disabled="currentPage >= totalPages" class="px-4 py-2 bg-slate-800 text-gray-300 rounded-lg disabled:opacity-30 hover:bg-slate-700 transition-colors text-sm">下一页</button>
    </div>

    <!-- ====== 详情弹窗 ====== -->
    <transition name="modal">
      <div v-if="selectedItem" class="fixed inset-0 z-[100] flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-black/90 backdrop-blur-sm" @click="closeDetail"></div>

        <div class="relative w-full max-w-4xl bg-slate-900 rounded-2xl overflow-hidden shadow-2xl border border-white/10 animate__animated animate__zoomIn flex flex-col md:flex-row max-h-[90vh]">
          <button @click="closeDetail" class="absolute top-4 right-4 z-50 p-2 bg-black/50 text-white rounded-full hover:bg-black/80 transition-colors border border-white/10">
            <X class="w-5 h-5" />
          </button>

          <!-- 左侧：封面 -->
          <div class="w-full md:w-5/12 h-64 md:h-auto relative overflow-hidden group">
            <img v-if="selectedItem.coverUrl" :src="selectedItem.coverUrl" class="w-full h-full object-cover transition-transform duration-700 group-hover:scale-110" />
            <div v-else class="w-full h-full bg-gradient-to-br from-slate-700 to-slate-800 flex items-center justify-center">
              <Cpu class="w-20 h-20 text-gray-600" />
            </div>
            <div class="absolute inset-0 bg-gradient-to-t from-slate-900 to-transparent opacity-60"></div>
          </div>

          <!-- 右侧：信息 -->
          <div class="w-full md:w-7/12 p-8 flex flex-col bg-slate-900 overflow-y-auto custom-scrollbar">
            <div class="flex justify-between items-start mb-4">
              <div class="flex-1 mr-4">
                <h2 class="text-3xl font-bold text-white mb-1">{{ selectedItem.title }}</h2>
                <p class="text-neon-green text-sm font-mono flex items-center gap-2">
                  <Cpu class="w-4 h-4" /> {{ selectedItem.author?.nickname || selectedItem.author?.username || '匿名' }}
                </p>
              </div>
              <div class="flex flex-col items-end shrink-0">
                <div class="flex items-center gap-1 text-yellow-400 font-bold text-xl cursor-pointer" @click="openRating" title="点击评分">
                  <Star class="w-5 h-5 fill-current" /> {{ formatRating(selectedItem) }}
                </div>
                <span class="text-xs text-gray-500">{{ formatDownloads(selectedItem.downloadCount) }} 次下载</span>
                <span v-if="selectedItem.ratingCount > 0" class="text-xs text-gray-600">{{ selectedItem.ratingCount }} 人评分</span>
              </div>
            </div>

            <!-- 标签 -->
            <div class="flex flex-wrap gap-2 mb-6">
              <span
                v-for="tag in parseTags(selectedItem.tags)" :key="tag"
                class="px-2 py-1 bg-white/5 rounded text-xs text-gray-300 border border-white/5"
              >#{{ tag }}</span>
            </div>

            <!-- 描述 -->
            <p class="text-gray-300 leading-relaxed mb-6 text-sm flex-1 pr-2">
              {{ selectedItem.description || '暂无描述' }}
            </p>

            <!-- 版本/大小/更新时间 -->
            <div class="grid grid-cols-3 gap-4 mb-6 border-t border-b border-white/5 py-4 text-center">
              <div>
                <p class="text-xs text-gray-500 mb-1">版本</p>
                <p class="text-white font-mono text-sm">{{ selectedItem.version || '-' }}</p>
              </div>
              <div>
                <p class="text-xs text-gray-500 mb-1">大小</p>
                <p class="text-white font-mono text-sm">{{ selectedItem.fileSize || '-' }}</p>
              </div>
              <div>
                <p class="text-xs text-gray-500 mb-1">更新于</p>
                <p class="text-white font-mono text-sm">{{ selectedItem.updatedAt?.substring(0, 10) || '-' }}</p>
              </div>
            </div>

            <!-- 评分面板 -->
            <div v-if="showRatingPanel" class="mb-6 p-4 bg-slate-800 rounded-xl border border-white/10">
              <p class="text-sm text-gray-300 mb-3">为此项目评分 (1-10)</p>
              <div class="flex items-center gap-1 mb-3">
                <button
                  v-for="s in 10" :key="s"
                  @click="ratingScore = s"
                  :class="['w-8 h-8 rounded-lg text-sm font-bold transition-all', s <= ratingScore ? 'bg-yellow-400 text-slate-900' : 'bg-slate-700 text-gray-400 hover:bg-slate-600']"
                >{{ s }}</button>
              </div>
              <div class="flex gap-2">
                <button @click="submitRating" :disabled="ratingScore < 1 || ratingSubmitting" class="px-4 py-2 bg-neon-green text-slate-900 rounded-lg text-sm font-bold disabled:opacity-50">确认</button>
                <button @click="showRatingPanel = false" class="px-4 py-2 bg-slate-700 text-gray-300 rounded-lg text-sm">取消</button>
              </div>
            </div>

            <!-- 操作按钮 -->
            <div class="flex gap-3">
              <button
                @click="handleSubscribe"
                :disabled="interactionLoading"
                :class="[
                  'flex-1 py-3 font-bold rounded-xl transition-all flex items-center justify-center gap-2',
                  interaction.subscribed
                    ? 'bg-slate-700 text-gray-300 border border-white/10'
                    : 'bg-neon-green text-slate-900 hover:shadow-[0_0_15px_rgba(0,255,157,0.4)]'
                ]"
              >
                <Download class="w-5 h-5" /> {{ interaction.subscribed ? '已订阅' : '立即订阅' }}
              </button>
              <button
                @click="handleLike"
                :disabled="interactionLoading"
                :class="[
                  'px-4 py-3 rounded-xl transition-colors border',
                  interaction.liked ? 'bg-red-500/20 border-red-500/50 text-red-400' : 'bg-slate-800 border-white/10 text-white hover:bg-slate-700'
                ]"
                :title="interaction.liked ? '取消点赞' : '点赞'"
              >
                <Heart class="w-5 h-5" :class="interaction.liked ? 'fill-current' : ''" />
              </button>
              <button
                @click="handleFavorite"
                :disabled="interactionLoading"
                :class="[
                  'px-4 py-3 rounded-xl transition-colors border',
                  interaction.favorited ? 'bg-yellow-500/20 border-yellow-500/50 text-yellow-400' : 'bg-slate-800 border-white/10 text-white hover:bg-slate-700'
                ]"
                :title="interaction.favorited ? '取消收藏' : '收藏'"
              >
                <Star class="w-5 h-5" :class="interaction.favorited ? 'fill-current' : ''" />
              </button>
              <button
                @click="handleShare"
                class="px-4 py-3 bg-slate-800 text-white rounded-xl hover:bg-slate-700 transition-colors border border-white/10"
                title="分享"
              >
                <Share2 class="w-5 h-5" />
              </button>
            </div>

            <!-- 作者操作：编辑/删除 -->
            <div v-if="auth.user?.id && selectedItem.author?.id === auth.user.id" class="flex gap-3 mt-3">
              <button @click="openEditEditor(selectedItem!)" class="flex-1 py-2 bg-cyan-500/20 text-cyan-400 rounded-xl text-sm font-medium flex items-center justify-center gap-2 hover:bg-cyan-500/30 transition-colors">
                <Edit class="w-4 h-4" /> 编辑
              </button>
              <button @click="handleDelete(selectedItem!)" class="flex-1 py-2 bg-red-500/20 text-red-400 rounded-xl text-sm font-medium flex items-center justify-center gap-2 hover:bg-red-500/30 transition-colors">
                <Trash2 class="w-4 h-4" /> 删除
              </button>
            </div>
          </div>
        </div>
      </div>
    </transition>

    <!-- ====== 发布/编辑弹窗 ====== -->
    <transition name="modal">
      <div v-if="showEditor" class="fixed inset-0 z-[110] flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-black/90 backdrop-blur-sm" @click="showEditor = false"></div>

        <div class="relative w-full max-w-2xl bg-slate-900 rounded-2xl shadow-2xl border border-white/10 p-8 max-h-[90vh] overflow-y-auto custom-scrollbar">
          <button @click="showEditor = false" class="absolute top-4 right-4 p-2 bg-black/50 text-white rounded-full hover:bg-black/80 transition-colors border border-white/10">
            <X class="w-5 h-5" />
          </button>

          <h2 class="text-2xl font-bold text-white mb-6">
            {{ editorMode === 'create' ? '发布新项目' : '编辑项目' }}
          </h2>

          <div class="space-y-5">
            <!-- 标题 -->
            <div>
              <label class="block text-sm text-gray-400 mb-1">项目标题 <span class="text-red-400">*</span></label>
              <input v-model="editorForm.title" type="text" maxlength="128" placeholder="为你的项目起个名字" class="w-full px-4 py-2.5 bg-slate-800 border border-white/10 rounded-lg text-white text-sm focus:border-neon-green/50 focus:outline-none" />
            </div>

            <!-- 分类 -->
            <div>
              <label class="block text-sm text-gray-400 mb-1">分类</label>
              <select v-model="editorForm.category" class="w-full px-4 py-2.5 bg-slate-800 border border-white/10 rounded-lg text-white text-sm focus:border-neon-green/50 focus:outline-none">
                <option v-for="cat in categories.filter(c => c.value)" :key="cat.value" :value="cat.value">{{ cat.label }}</option>
              </select>
            </div>

            <!-- 描述 -->
            <div>
              <label class="block text-sm text-gray-400 mb-1">项目描述</label>
              <textarea v-model="editorForm.description" rows="4" maxlength="2000" placeholder="详细介绍你的项目..." class="w-full px-4 py-2.5 bg-slate-800 border border-white/10 rounded-lg text-white text-sm focus:border-neon-green/50 focus:outline-none resize-none"></textarea>
            </div>

            <!-- 封面上传 -->
            <div>
              <label class="block text-sm text-gray-400 mb-1">封面图</label>
              <div class="flex items-center gap-4">
                <div v-if="editorForm.coverUrl" class="w-32 h-20 rounded-lg overflow-hidden border border-white/10">
                  <img :src="editorForm.coverUrl" class="w-full h-full object-cover" />
                </div>
                <label class="flex items-center gap-2 px-4 py-2 bg-slate-800 border border-white/10 rounded-lg text-sm text-gray-300 cursor-pointer hover:border-white/30 transition-colors">
                  <Upload class="w-4 h-4" />
                  {{ coverUploading ? '上传中...' : '上传封面' }}
                  <input type="file" accept="image/*" class="hidden" @change="handleCoverUpload" :disabled="coverUploading" />
                </label>
              </div>
            </div>

            <!-- 标签 -->
            <div>
              <label class="block text-sm text-gray-400 mb-1">标签（逗号分隔）</label>
              <input v-model="editorForm.tags" type="text" placeholder="UI, Vue, React" class="w-full px-4 py-2.5 bg-slate-800 border border-white/10 rounded-lg text-white text-sm focus:border-neon-green/50 focus:outline-none" />
            </div>

            <!-- 版本 + 文件大小 -->
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="block text-sm text-gray-400 mb-1">版本号</label>
                <input v-model="editorForm.version" type="text" placeholder="v1.0.0" class="w-full px-4 py-2.5 bg-slate-800 border border-white/10 rounded-lg text-white text-sm focus:border-neon-green/50 focus:outline-none" />
              </div>
              <div>
                <label class="block text-sm text-gray-400 mb-1">文件大小</label>
                <input v-model="editorForm.fileSize" type="text" placeholder="15.4 MB" class="w-full px-4 py-2.5 bg-slate-800 border border-white/10 rounded-lg text-white text-sm focus:border-neon-green/50 focus:outline-none" />
              </div>
            </div>

            <!-- 资源链接 -->
            <div>
              <label class="block text-sm text-gray-400 mb-1">资源链接</label>
              <input v-model="editorForm.fileUrl" type="text" placeholder="https://github.com/xxx 或其他下载链接" class="w-full px-4 py-2.5 bg-slate-800 border border-white/10 rounded-lg text-white text-sm focus:border-neon-green/50 focus:outline-none" />
            </div>

            <!-- 提交按钮 -->
            <div class="flex gap-3 pt-2">
              <button
                @click="submitProject"
                :disabled="editorLoading"
                class="flex-1 py-3 bg-neon-green text-slate-900 font-bold rounded-xl hover:shadow-[0_0_15px_rgba(0,255,157,0.4)] transition-all flex items-center justify-center gap-2 disabled:opacity-50"
              >
                <Loader2 v-if="editorLoading" class="w-5 h-5 animate-spin" />
                <template v-else>{{ editorMode === 'create' ? '发布' : '保存修改' }}</template>
              </button>
              <button @click="showEditor = false" class="px-6 py-3 bg-slate-800 text-gray-300 rounded-xl hover:bg-slate-700 transition-colors border border-white/10">取消</button>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.3s ease;
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: rgba(255,255,255,0.1);
  border-radius: 2px;
}
</style>

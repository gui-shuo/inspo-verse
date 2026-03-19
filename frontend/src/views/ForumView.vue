<script setup lang="ts">
import { ref, computed, reactive, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  MessageSquare, Flame, Clock, Hash, Search, Plus, User, X, PenTool, Image as ImageIcon,
  ChevronLeft, ChevronRight, Eye
} from 'lucide-vue-next'
import GlitchText from '@/components/ui/GlitchText.vue'
import { useToastStore } from '@/stores/toast'
import { useAuthStore } from '@/stores/auth'
import { forumApi, type ForumPost, type CommunityStats } from '@/api/forum'
import { useDebounceFn } from '@vueuse/core'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

const router = useRouter()
const toast = useToastStore()
const authStore = useAuthStore()

// --- 状态 ---
const activeBoard = ref('all')
const sortMode = ref<'hot' | 'new'>('new')
const searchInput = ref('')
const searchQuery = ref('')
const isNewPostModalOpen = ref(false)
const isPublishing = ref(false)
const isUploadingImage = ref(false)
const imageFileInput = ref<HTMLInputElement | null>(null)

// 帖子列表状态
const posts = ref<ForumPost[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = 20
const isLoading = ref(false)

// 社区统计
const stats = ref<CommunityStats>({ todayPosts: 0, onlineUsers: 0 })

// 发布新帖表单
const newPostForm = reactive({
  title: '',
  board: 'chat',
  content: '',
  tags: '',
})

const boards = [
  { id: 'all', name: '全部板块', icon: Hash },
  { id: 'game', name: '游戏综合', icon: MessageSquare },
  { id: 'anime', name: '动漫新番', icon: MessageSquare },
  { id: 'ai', name: 'AI 绘图', icon: MessageSquare },
  { id: 'chat', name: '闲聊灌水', icon: MessageSquare },
]

const totalPages = computed(() => Math.ceil(total.value / pageSize))

function formatTime(dateStr: string) {
  return dayjs(dateStr).fromNow()
}

function formatCount(n: number) {
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return String(n)
}

function getBoardName(category: string) {
  return boards.find(b => b.id === category)?.name || category
}

// --- 加载帖子 ---
async function loadPosts() {
  isLoading.value = true
  try {
    const result = await forumApi.getPosts({
      category: activeBoard.value === 'all' ? undefined : activeBoard.value,
      keyword: searchQuery.value || undefined,
      sortBy: sortMode.value,
      page: currentPage.value,
      pageSize,
    })
    posts.value = result.posts
    total.value = result.total
  } catch {
    toast.error('加载帖子失败，请稍后重试')
  } finally {
    isLoading.value = false
  }
}

// --- 加载社区统计 ---
async function loadStats() {
  try {
    stats.value = await forumApi.getStats()
  } catch {
    // 统计失败不影响主功能
  }
}

// 防抖搜索
const debouncedSearch = useDebounceFn(() => {
  searchQuery.value = searchInput.value
  currentPage.value = 1
}, 500)

watch([activeBoard, sortMode, searchQuery], () => {
  currentPage.value = 1
  loadPosts()
})

watch(currentPage, () => {
  loadPosts()
  window.scrollTo({ top: 0, behavior: 'smooth' })
})

onMounted(() => {
  loadPosts()
  loadStats()
})

// --- 新帖模态框 ---
function openNewPostModal() {
  if (!authStore.isAuthenticated) {
    toast.warning('请先登录后再发布帖子')
    return
  }
  newPostForm.title = ''
  newPostForm.content = ''
  newPostForm.board = activeBoard.value === 'all' ? 'chat' : activeBoard.value
  newPostForm.tags = ''
  isNewPostModalOpen.value = true
}

// --- 上传图片至帖子内容 ---
function triggerImageUpload() {
  imageFileInput.value?.click()
}

async function handleImageSelect(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  isUploadingImage.value = true
  try {
    const result = await forumApi.uploadImage(file)
    newPostForm.content += `\n![图片](${result.url})\n`
    toast.success('图片上传成功')
  } catch {
    toast.error('图片上传失败，请检查格式（JPG/PNG/GIF/WebP）')
  } finally {
    isUploadingImage.value = false
    if (imageFileInput.value) imageFileInput.value.value = ''
  }
}

// --- 发布帖子 ---
async function submitNewPost() {
  if (!newPostForm.title.trim()) { toast.error('标题不能为空'); return }
  if (!newPostForm.content.trim()) { toast.error('内容不能为空'); return }

  isPublishing.value = true
  try {
    const result = await forumApi.createPost({
      category: newPostForm.board,
      title: newPostForm.title.trim(),
      content: newPostForm.content.trim(),
      tags: newPostForm.tags.trim() || undefined,
    })
    isNewPostModalOpen.value = false
    toast.success('发布成功！')
    router.push({ name: 'forum-post', params: { id: result.postId } })
  } catch (e: any) {
    toast.error(e?.message || '发布失败，请稍后再试')
  } finally {
    isPublishing.value = false
  }
}

function goToPost(post: ForumPost) {
  router.push({ name: 'forum-post', params: { id: post.id } })
}
</script>

<template>
  <div class="container mx-auto px-4 py-20 min-h-screen flex flex-col md:flex-row gap-8">
    <!-- Sidebar -->
    <aside class="w-full md:w-64 flex-shrink-0 space-y-6">
      <div class="bg-slate-800 rounded-xl p-6 border border-white/5 sticky top-24">
        <button
          @click="openNewPostModal"
          class="w-full bg-gradient-to-r from-neon-blue to-neon-purple text-slate-900 font-bold py-3 rounded-lg mb-6 flex items-center justify-center gap-2 hover:shadow-lg transition-all hover:scale-105"
        >
          <Plus class="w-5 h-5" /> 发布新帖
        </button>

        <nav class="space-y-2">
          <button
            v-for="board in boards"
            :key="board.id"
            @click="activeBoard = board.id"
            class="w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-all"
            :class="activeBoard === board.id ? 'bg-white/10 text-white font-bold' : 'text-gray-400 hover:bg-white/5 hover:text-gray-200'"
          >
            <component :is="board.icon" class="w-4 h-4" />
            {{ board.name }}
          </button>
        </nav>
      </div>

      <!-- Stats -->
      <div class="bg-slate-800 rounded-xl p-6 border border-white/5 hidden md:block sticky top-[400px]">
        <h3 class="text-white font-bold mb-4">社区统计</h3>
        <div class="grid grid-cols-2 gap-4 text-center">
          <div>
            <div class="text-2xl font-bold text-neon-blue">{{ formatCount(stats.todayPosts) }}</div>
            <div class="text-xs text-gray-500">今日发帖</div>
          </div>
          <div>
            <div class="text-2xl font-bold text-neon-purple">{{ formatCount(stats.onlineUsers) }}</div>
            <div class="text-xs text-gray-500">在线用户</div>
          </div>
        </div>
      </div>
    </aside>

    <!-- Main Content -->
    <main class="flex-1">
      <div class="flex flex-col sm:flex-row justify-between items-center mb-6 gap-4">
        <h1 class="text-3xl font-bold"><GlitchText text="FORUM" /></h1>
        <div class="flex items-center gap-4 w-full sm:w-auto">
          <div class="relative flex-1 sm:w-64">
            <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500" />
            <input
              v-model="searchInput"
              @input="debouncedSearch"
              type="text"
              placeholder="搜索帖子、作者..."
              class="w-full bg-slate-800 border border-white/10 rounded-lg pl-10 pr-4 py-2 text-sm text-white focus:border-neon-blue focus:outline-none transition-colors"
            />
          </div>
          <div class="flex bg-slate-800 rounded-lg p-1 border border-white/10">
            <button
              @click="sortMode = 'hot'"
              class="p-2 rounded hover:bg-white/10 transition-colors"
              :class="sortMode === 'hot' ? 'text-neon-blue' : 'text-gray-500'"
              title="热门排序"
            ><Flame class="w-4 h-4" /></button>
            <button
              @click="sortMode = 'new'"
              class="p-2 rounded hover:bg-white/10 transition-colors"
              :class="sortMode === 'new' ? 'text-neon-blue' : 'text-gray-500'"
              title="最新排序"
            ><Clock class="w-4 h-4" /></button>
          </div>
        </div>
      </div>

      <!-- Loading Skeleton -->
      <div v-if="isLoading" class="space-y-4">
        <div v-for="i in 6" :key="i" class="bg-slate-800/50 border border-white/5 rounded-xl p-5 animate-pulse">
          <div class="flex gap-4">
            <div class="w-10 h-10 rounded-full bg-slate-700 flex-shrink-0"></div>
            <div class="flex-1 space-y-2">
              <div class="h-4 bg-slate-700 rounded w-3/4"></div>
              <div class="h-3 bg-slate-700 rounded w-1/2"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- Post List -->
      <div v-else class="space-y-3 min-h-[50vh]">
        <transition-group name="list">
          <div
            v-for="post in posts"
            :key="post.id"
            @click="goToPost(post)"
            class="bg-slate-800/50 border border-white/5 rounded-xl p-5 hover:border-neon-blue/30 transition-all hover:bg-slate-800 cursor-pointer group"
          >
            <div class="flex gap-4">
              <div class="flex-shrink-0">
                <img
                  :src="post.author.avatarUrl || `https://api.dicebear.com/7.x/avataaars/svg?seed=${post.author.username}`"
                  class="w-10 h-10 rounded-full bg-slate-700 object-cover"
                  loading="lazy"
                />
              </div>
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-2 mb-1 flex-wrap">
                  <span v-if="post.isTop === 1" class="px-1.5 py-0.5 rounded bg-red-500/20 text-red-400 text-xs font-bold flex-shrink-0">置顶</span>
                  <span v-if="post.isEssence === 1" class="px-1.5 py-0.5 rounded bg-orange-500/20 text-orange-400 text-xs font-bold flex-shrink-0">HOT</span>
                  <h3 class="text-base font-bold text-white truncate group-hover:text-neon-blue transition-colors">{{ post.title }}</h3>
                </div>
                <div class="flex items-center gap-3 text-xs text-gray-400 flex-wrap">
                  <span class="flex items-center gap-1 text-gray-300">
                    <User class="w-3 h-3" /> {{ post.author.nickname || post.author.username }}
                  </span>
                  <span>{{ formatTime(post.createdAt) }}</span>
                  <span class="px-2 py-0.5 rounded bg-white/5 text-gray-500">#{{ getBoardName(post.category) }}</span>
                </div>
              </div>
              <div class="hidden sm:flex flex-col items-end justify-center gap-1 text-gray-500 min-w-[60px]">
                <div class="flex items-center gap-1 text-sm font-bold text-gray-300">
                  <MessageSquare class="w-4 h-4" /> {{ formatCount(post.commentCount) }}
                </div>
                <div class="flex items-center gap-1 text-xs">
                  <Eye class="w-3 h-3" /> {{ formatCount(post.viewCount) }}
                </div>
              </div>
            </div>
          </div>
        </transition-group>

        <div v-if="posts.length === 0" class="text-center py-20 text-gray-500">
          <Search class="w-12 h-12 mx-auto mb-4 opacity-20" />
          <p>没有找到相关帖子，换个关键词试试？</p>
        </div>
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="flex items-center justify-center gap-2 mt-8 flex-wrap">
        <button
          @click="currentPage--"
          :disabled="currentPage <= 1"
          class="p-2 rounded-lg bg-slate-800 border border-white/10 text-gray-400 hover:text-white hover:border-neon-blue/50 disabled:opacity-40 disabled:cursor-not-allowed transition-all"
        ><ChevronLeft class="w-4 h-4" /></button>

        <template v-for="p in totalPages" :key="p">
          <button
            v-if="p === 1 || p === totalPages || Math.abs(p - currentPage) <= 2"
            @click="currentPage = p"
            class="w-9 h-9 rounded-lg text-sm font-medium transition-all border"
            :class="p === currentPage
              ? 'bg-neon-blue/20 border-neon-blue text-neon-blue'
              : 'bg-slate-800 border-white/10 text-gray-400 hover:text-white hover:border-white/30'"
          >{{ p }}</button>
          <span v-else-if="p === 2 && currentPage > 4" class="text-gray-600 px-1">…</span>
          <span v-else-if="p === totalPages - 1 && currentPage < totalPages - 3" class="text-gray-600 px-1">…</span>
        </template>

        <button
          @click="currentPage++"
          :disabled="currentPage >= totalPages"
          class="p-2 rounded-lg bg-slate-800 border border-white/10 text-gray-400 hover:text-white hover:border-neon-blue/50 disabled:opacity-40 disabled:cursor-not-allowed transition-all"
        ><ChevronRight class="w-4 h-4" /></button>

        <span class="text-xs text-gray-500 ml-2">第 {{ currentPage }} / {{ totalPages }} 页，共 {{ total }} 条</span>
      </div>
    </main>

    <!-- Hidden image uploader -->
    <input
      ref="imageFileInput"
      type="file"
      accept="image/jpeg,image/png,image/gif,image/webp"
      class="hidden"
      @change="handleImageSelect"
    />

    <!-- New Post Modal -->
    <transition name="modal">
      <div v-if="isNewPostModalOpen" class="fixed inset-0 z-[100] flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-black/80 backdrop-blur-sm" @click="isNewPostModalOpen = false"></div>
        <div class="relative w-full max-w-2xl bg-slate-900 rounded-2xl border border-white/10 shadow-2xl overflow-hidden animate__animated animate__fadeInUp">
          <div class="p-6 border-b border-white/5 flex justify-between items-center">
            <h3 class="text-xl font-bold text-white flex items-center gap-2">
              <PenTool class="w-5 h-5 text-neon-blue" /> 发布新帖
            </h3>
            <button @click="isNewPostModalOpen = false" class="text-gray-400 hover:text-white"><X class="w-5 h-5" /></button>
          </div>

          <div class="p-6 space-y-4">
            <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div class="md:col-span-2">
                <label class="block text-xs text-gray-400 mb-1">标题</label>
                <input
                  v-model="newPostForm.title"
                  type="text"
                  placeholder="请输入引人注目的标题"
                  maxlength="200"
                  class="w-full bg-slate-800 border border-white/10 rounded-lg px-4 py-2 text-white focus:border-neon-blue focus:outline-none"
                />
              </div>
              <div>
                <label class="block text-xs text-gray-400 mb-1">发布板块</label>
                <select v-model="newPostForm.board" class="w-full bg-slate-800 border border-white/10 rounded-lg px-4 py-2 text-white focus:border-neon-blue focus:outline-none">
                  <option v-for="b in boards.slice(1)" :key="b.id" :value="b.id">{{ b.name }}</option>
                </select>
              </div>
            </div>

            <div>
              <label class="block text-xs text-gray-400 mb-1">
                内容 <span class="text-gray-600">（支持 Markdown 语法）</span>
              </label>
              <div class="relative">
                <textarea
                  v-model="newPostForm.content"
                  rows="8"
                  placeholder="分享你的想法... (支持 Markdown 语法，如 **粗体**、## 标题、```代码块```)"
                  class="w-full bg-slate-800 border border-white/10 rounded-lg p-4 pb-12 text-white focus:border-neon-blue focus:outline-none resize-none font-mono text-sm"
                ></textarea>
                <button
                  @click="triggerImageUpload"
                  :disabled="isUploadingImage"
                  class="absolute bottom-3 right-3 p-2 bg-white/10 hover:bg-white/20 rounded text-gray-300 transition-colors disabled:opacity-50"
                  title="上传图片"
                >
                  <span v-if="isUploadingImage" class="animate-spin inline-block w-4 h-4 border-2 border-gray-300 border-t-transparent rounded-full"></span>
                  <ImageIcon v-else class="w-4 h-4" />
                </button>
              </div>
            </div>

            <div>
              <label class="block text-xs text-gray-400 mb-1">标签 <span class="text-gray-600">（可选，逗号分隔）</span></label>
              <input
                v-model="newPostForm.tags"
                type="text"
                placeholder="例如：黑神话,攻略,BOSS"
                class="w-full bg-slate-800 border border-white/10 rounded-lg px-4 py-2 text-white focus:border-neon-blue focus:outline-none"
              />
            </div>
          </div>

          <div class="p-6 pt-0 flex justify-end gap-3">
            <button @click="isNewPostModalOpen = false" class="px-6 py-2 rounded-lg text-gray-400 hover:text-white font-medium">取消</button>
            <button
              @click="submitNewPost"
              :disabled="isPublishing"
              class="px-6 py-2 rounded-lg bg-neon-blue text-slate-900 font-bold hover:shadow-lg hover:shadow-neon-blue/20 transition-all flex items-center gap-2 disabled:opacity-60"
            >
              <span v-if="isPublishing" class="animate-spin w-4 h-4 border-2 border-slate-900 border-t-transparent rounded-full"></span>
              {{ isPublishing ? '发布中...' : '立即发布' }}
            </button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.modal-enter-active, .modal-leave-active { transition: opacity 0.3s ease; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
.list-enter-active, .list-leave-active { transition: all 0.4s ease; }
.list-enter-from, .list-leave-to { opacity: 0; transform: translateY(-10px); }
</style>

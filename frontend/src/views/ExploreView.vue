<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Search, Heart, MessageSquare, Filter, Zap, ImageIcon, Gamepad2, Tv, X, Share2, MoreHorizontal, Send, UserPlus, UserCheck, Eye, Loader2, Plus, Upload, Tag } from 'lucide-vue-next'
import GlitchText from '@/components/ui/GlitchText.vue'
import { exploreApi, type ExploreItem, type ExploreComment } from '@/api/explore'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'

const authStore = useAuthStore()
const toast = useToastStore()

// ==================== 分类 ====================
const categories = [
  { id: 'all', name: '全部推荐', icon: Zap },
  { id: 'ai-art', name: 'AI 绘图', icon: ImageIcon },
  { id: 'game', name: '游戏攻略', icon: Gamepad2 },
  { id: 'anime', name: '番剧二创', icon: Tv },
]

// ==================== 状态 ====================
const posts = ref<ExploreItem[]>([])
const activeCategory = ref('all')
const searchInput = ref('')
const searchQuery = ref('')
const sortBy = ref('new')
const currentPage = ref(1)
const totalItems = ref(0)
const pageSize = ref(20)
const loading = ref(false)
const loadingMore = ref(false)
const hasMore = ref(true)

// 详情弹窗
const selectedPost = ref<ExploreItem | null>(null)
const detailLoading = ref(false)
const comments = ref<ExploreComment[]>([])
const commentText = ref('')
const commentSubmitting = ref(false)
const commentTotal = ref(0)

// 筛选面板
const showFilterPanel = ref(false)

// 发布弹窗
const showPublishModal = ref(false)
const publishForm = ref({
  category: 'ai-art',
  title: '',
  description: '',
  tag: '',
  coverUrl: '',
})
const publishSubmitting = ref(false)
const coverUploading = ref(false)
const coverPreview = ref('')

// ==================== 数据加载 ====================

async function loadContent(reset = true) {
  if (reset) {
    loading.value = true
    currentPage.value = 1
    posts.value = []
  } else {
    loadingMore.value = true
  }

  try {
    const res = await exploreApi.getList({
      category: activeCategory.value === 'all' ? undefined : activeCategory.value,
      keyword: searchQuery.value || undefined,
      sortBy: sortBy.value,
      page: currentPage.value,
      pageSize: pageSize.value,
    })

    if (reset) {
      posts.value = res.items
    } else {
      posts.value.push(...res.items)
    }
    totalItems.value = res.total
    hasMore.value = posts.value.length < res.total
  } catch (e: any) {
    toast.add(e.message || '加载失败，请重试', 'error')
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

function loadMore() {
  if (loadingMore.value || !hasMore.value) return
  currentPage.value++
  loadContent(false)
}

// ==================== 分类切换 ====================

function updateCategory(id: string) {
  activeCategory.value = id
  loadContent(true)
}

// ==================== 搜索 ====================

let searchTimer: ReturnType<typeof setTimeout> | null = null
function onSearchInput() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    searchQuery.value = searchInput.value
    loadContent(true)
  }, 400)
}

function onSearchEnter() {
  if (searchTimer) clearTimeout(searchTimer)
  searchQuery.value = searchInput.value
  loadContent(true)
}

// ==================== 排序切换 ====================

function toggleSort() {
  showFilterPanel.value = !showFilterPanel.value
}

function selectSort(val: string) {
  sortBy.value = val
  showFilterPanel.value = false
  loadContent(true)
}

// ==================== 作品详情 ====================

async function openDetail(post: ExploreItem) {
  selectedPost.value = { ...post }
  document.body.style.overflow = 'hidden'
  detailLoading.value = true
  comments.value = []
  commentText.value = ''

  try {
    const [detail, commentsRes] = await Promise.all([
      exploreApi.getDetail(post.id),
      exploreApi.getComments(post.id),
    ])
    selectedPost.value = detail
    comments.value = commentsRes.comments
    commentTotal.value = commentsRes.total
  } catch (e: any) {
    toast.add(e.message || '加载详情失败', 'error')
  } finally {
    detailLoading.value = false
  }
}

function closeDetail() {
  selectedPost.value = null
  document.body.style.overflow = ''
}

// ==================== 点赞 ====================

async function toggleLike(post: ExploreItem, event?: Event) {
  if (event) event.stopPropagation()
  if (!authStore.isAuthenticated) {
    toast.add('请先登录后再点赞', 'warning')
    return
  }

  const prevLiked = post.isLiked
  const prevLikes = post.likes
  post.isLiked = !post.isLiked
  post.likes += post.isLiked ? 1 : -1

  try {
    const res = await exploreApi.toggleLike(post.id)
    post.isLiked = res.isLiked
    post.likes = res.likes

    const listItem = posts.value.find(p => p.id === post.id)
    if (listItem && listItem !== post) {
      listItem.isLiked = res.isLiked
      listItem.likes = res.likes
    }
  } catch (e: any) {
    post.isLiked = prevLiked
    post.likes = prevLikes
    toast.add(e.message || '操作失败', 'error')
  }
}

// ==================== 评论 ====================

async function submitComment() {
  if (!authStore.isAuthenticated) {
    toast.add('请先登录后再评论', 'warning')
    return
  }
  if (!commentText.value.trim() || !selectedPost.value) return

  commentSubmitting.value = true
  try {
    const res = await exploreApi.addComment(selectedPost.value.id, {
      content: commentText.value.trim(),
    })

    comments.value.unshift({
      id: res.commentId,
      contentId: selectedPost.value.id,
      userId: authStore.user?.id || 0,
      content: commentText.value.trim(),
      parentCommentId: 0,
      replyToUserId: null,
      likeCount: 0,
      createdAt: new Date().toISOString(),
      author: res.author,
    })
    commentTotal.value++

    selectedPost.value.comments = res.commentCount
    const listItem = posts.value.find(p => p.id === selectedPost.value!.id)
    if (listItem) listItem.comments = res.commentCount

    commentText.value = ''
    toast.add('评论成功', 'success')
  } catch (e: any) {
    toast.add(e.message || '评论失败', 'error')
  } finally {
    commentSubmitting.value = false
  }
}

// ==================== 关注 ====================

async function toggleFollow() {
  if (!authStore.isAuthenticated) {
    toast.add('请先登录后再关注', 'warning')
    return
  }
  if (!selectedPost.value) return

  try {
    const res = await exploreApi.toggleFollow(selectedPost.value.id)
    selectedPost.value.isFollowed = res.isFollowed
    toast.add(res.isFollowed ? '关注成功' : '已取消关注', 'success')
  } catch (e: any) {
    toast.add(e.message || '操作失败', 'error')
  }
}

// ==================== 分享 ====================

async function shareContent() {
  if (!selectedPost.value) return
  const url = window.location.origin + '/explore?id=' + selectedPost.value.id
  try {
    await navigator.clipboard.writeText(url)
    toast.add('链接已复制到剪贴板', 'success')
  } catch {
    toast.add('复制失败，请手动复制', 'error')
  }
}

// ==================== 时间格式化 ====================

function formatTime(dateStr: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return minutes + ' 分钟前'
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return hours + ' 小时前'
  const days = Math.floor(hours / 24)
  if (days < 30) return days + ' 天前'
  return date.toLocaleDateString('zh-CN')
}

// ==================== 发布 ====================

function openPublish() {
  if (!authStore.isAuthenticated) {
    toast.add('请先登录后再发布', 'warning')
    return
  }
  publishForm.value = { category: 'ai-art', title: '', description: '', tag: '', coverUrl: '' }
  coverPreview.value = ''
  showPublishModal.value = true
  document.body.style.overflow = 'hidden'
}

function closePublish() {
  showPublishModal.value = false
  document.body.style.overflow = ''
}

async function handleCoverUpload(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  if (!file.type.startsWith('image/')) {
    toast.add('请上传图片文件', 'warning')
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    toast.add('图片不能超过 10MB', 'warning')
    return
  }

  coverUploading.value = true
  try {
    const res = await exploreApi.uploadImage(file)
    publishForm.value.coverUrl = res.url
    coverPreview.value = URL.createObjectURL(file)
    toast.add('封面上传成功', 'success')
  } catch (e: any) {
    toast.add(e.message || '上传失败', 'error')
  } finally {
    coverUploading.value = false
  }
}

async function submitPublish() {
  const form = publishForm.value
  if (!form.title.trim()) {
    toast.add('请输入标题', 'warning')
    return
  }
  if (!form.coverUrl) {
    toast.add('请上传封面图片', 'warning')
    return
  }

  publishSubmitting.value = true
  try {
    await exploreApi.publish({
      category: form.category,
      title: form.title.trim(),
      description: form.description.trim(),
      coverUrl: form.coverUrl,
      images: JSON.stringify([form.coverUrl]),
      tag: form.tag.trim() || undefined,
    })
    toast.add('发布成功！', 'success')
    closePublish()
    loadContent(true)
  } catch (e: any) {
    toast.add(e.message || '发布失败', 'error')
  } finally {
    publishSubmitting.value = false
  }
}

// ==================== 初始化 ====================
onMounted(() => {
  loadContent(true)
})
</script>

<template>
  <div class="container mx-auto px-4 py-20 min-h-screen">
    <!-- Header Section -->
    <div class="text-center mb-16 relative z-10">
      <h1 class="text-4xl md:text-6xl font-bold mb-6">
        <GlitchText text="EXPLORE" />
      </h1>
      <p class="text-gray-400 max-w-2xl mx-auto mb-8">
        发现社区中最令人惊叹的创意作品，从 AI 艺术到硬核游戏攻略。
      </p>

      <!-- Search Bar -->
      <div class="max-w-xl mx-auto relative group">
        <div class="absolute inset-0 bg-gradient-to-r from-neon-blue to-neon-purple rounded-full opacity-20 group-hover:opacity-40 blur-xl transition-opacity"></div>
        <div class="relative flex items-center bg-slate-800/80 backdrop-blur border border-white/10 rounded-full px-6 py-4 shadow-xl focus-within:border-neon-blue/50 transition-all">
          <Search class="w-5 h-5 text-gray-400 mr-4" />
          <input
            v-model="searchInput"
            @input="onSearchInput"
            @keydown.enter="onSearchEnter"
            type="text"
            placeholder="搜索关键词：Cyberpunk, AI, 攻略..."
            class="w-full bg-transparent text-white focus:outline-none placeholder-gray-500"
          >
          <div class="relative">
            <button @click="toggleSort" class="bg-white/10 p-2 rounded-full hover:bg-white/20 transition-colors">
              <Filter class="w-4 h-4 text-white" />
            </button>
            <!-- Sort Dropdown -->
            <transition name="fade">
              <div v-if="showFilterPanel" class="absolute right-0 top-12 w-40 bg-slate-800 border border-white/10 rounded-xl shadow-2xl overflow-hidden z-50">
                <button @click="selectSort('new')" class="w-full px-4 py-3 text-left text-sm transition-colors"
                  :class="sortBy === 'new' ? 'text-neon-blue bg-neon-blue/10' : 'text-gray-300 hover:bg-white/5'">
                  最新发布
                </button>
                <button @click="selectSort('hot')" class="w-full px-4 py-3 text-left text-sm transition-colors"
                  :class="sortBy === 'hot' ? 'text-neon-blue bg-neon-blue/10' : 'text-gray-300 hover:bg-white/5'">
                  最热推荐
                </button>
              </div>
            </transition>
          </div>
        </div>
      </div>

      <!-- Categories -->
      <div class="flex flex-wrap justify-center gap-4 mt-10">
        <button
          v-for="cat in categories"
          :key="cat.id"
          @click="updateCategory(cat.id)"
          class="flex items-center gap-2 px-6 py-2 rounded-full border transition-all duration-300"
          :class="activeCategory === cat.id
            ? 'bg-neon-blue text-slate-900 border-neon-blue font-bold shadow-[0_0_15px_rgba(0,243,255,0.4)]'
            : 'bg-slate-800/50 text-gray-400 border-white/10 hover:border-white/30 hover:bg-slate-800'"
        >
          <component :is="cat.icon" class="w-4 h-4" />
          {{ cat.name }}
        </button>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="flex justify-center py-20">
      <Loader2 class="w-10 h-10 text-neon-blue animate-spin" />
    </div>

    <!-- Masonry Grid -->
    <div v-else class="columns-1 md:columns-2 lg:columns-4 gap-6 space-y-6">
      <div
        v-for="post in posts"
        :key="post.id"
        @click="openDetail(post)"
        class="break-inside-avoid group relative bg-slate-800 border border-white/5 rounded-2xl overflow-hidden hover:border-neon-blue/30 transition-all duration-300 hover:-translate-y-2 hover:shadow-2xl cursor-pointer"
      >
        <!-- Image -->
        <div class="relative overflow-hidden aspect-auto">
          <img
            :src="post.image"
            :alt="post.title"
            class="w-full h-full object-cover transition-transform duration-700 group-hover:scale-110"
            loading="lazy"
          >
          <!-- Tag Overlay -->
          <div class="absolute top-3 left-3 px-3 py-1 bg-black/60 backdrop-blur text-xs font-bold text-white rounded-full border border-white/10">
            #{{ post.tag }}
          </div>
          <!-- Hover Overlay -->
          <div class="absolute inset-0 bg-gradient-to-t from-slate-900 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300"></div>
        </div>

        <!-- Content -->
        <div class="p-5">
          <h3 class="font-bold text-lg text-white mb-3 line-clamp-2 leading-tight group-hover:text-neon-blue transition-colors">
            {{ post.title }}
          </h3>

          <div class="flex items-center justify-between mt-4 pt-4 border-t border-white/5">
            <div class="flex items-center gap-2">
              <img :src="post.author.avatarUrl || 'https://api.dicebear.com/7.x/avataaars/svg?seed=' + post.author.username" class="w-6 h-6 rounded-full bg-slate-700" />
              <span class="text-xs text-gray-400 truncate max-w-[80px]">{{ post.author.nickname || post.author.username }}</span>
            </div>

            <div class="flex items-center gap-3 text-xs text-gray-400">
              <button
                @click.stop="toggleLike(post, $event)"
                class="flex items-center gap-1 hover:text-pink-500 transition-colors"
                :class="post.isLiked ? 'text-pink-500' : ''"
              >
                <Heart class="w-4 h-4" :class="post.isLiked ? 'fill-pink-500' : ''" />
                {{ post.likes }}
              </button>
              <div class="flex items-center gap-1">
                <MessageSquare class="w-4 h-4" />
                {{ post.comments }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Load More -->
    <div v-if="!loading && hasMore && posts.length > 0" class="flex justify-center mt-12">
      <button
        @click="loadMore"
        :disabled="loadingMore"
        class="px-8 py-3 bg-slate-800 text-gray-300 rounded-full border border-white/10 hover:border-neon-blue/30 hover:bg-slate-700 transition-all disabled:opacity-50"
      >
        <Loader2 v-if="loadingMore" class="w-5 h-5 animate-spin inline mr-2" />
        {{ loadingMore ? '加载中...' : '加载更多' }}
      </button>
    </div>

    <!-- Empty State -->
    <div v-if="!loading && posts.length === 0" class="text-center py-20 animate__animated animate__fadeIn">
      <div class="inline-flex justify-center items-center w-20 h-20 rounded-full bg-slate-800 mb-6">
        <Search class="w-8 h-8 text-gray-500" />
      </div>
      <h3 class="text-xl font-bold text-white mb-2">未找到相关内容</h3>
      <p class="text-gray-400">尝试更换关键词或分类</p>
    </div>

    <!-- ==================== Publish FAB Button ==================== -->
    <button
      @click="openPublish"
      class="fixed bottom-8 right-8 z-50 w-14 h-14 bg-gradient-to-r from-neon-blue to-neon-purple rounded-full shadow-lg shadow-neon-blue/30 flex items-center justify-center text-white hover:scale-110 hover:shadow-neon-blue/50 transition-all duration-300 group"
      title="发布作品"
    >
      <Plus class="w-7 h-7 transition-transform group-hover:rotate-90 duration-300" />
    </button>

    <!-- ==================== Publish Modal ==================== -->
    <transition name="modal">
      <div v-if="showPublishModal" class="fixed inset-0 z-[100] flex items-center justify-center bg-black/90 backdrop-blur-sm overflow-y-auto">
        <div class="absolute inset-0 -z-10 cursor-pointer" @click="closePublish"></div>
        <div class="relative w-full max-w-lg mx-4 my-6 bg-slate-900 rounded-2xl overflow-hidden shadow-2xl border border-white/10 animate__animated animate__zoomIn">
          <!-- Header -->
          <div class="flex items-center justify-between px-5 py-4 border-b border-white/5">
            <h2 class="text-lg font-bold text-white">发布作品</h2>
            <button @click="closePublish" class="p-1.5 text-gray-400 hover:text-white transition-colors rounded-full hover:bg-white/10">
              <X class="w-5 h-5" />
            </button>
          </div>

          <!-- Form -->
          <div class="px-5 py-4 space-y-4 max-h-[65vh] overflow-y-auto custom-scrollbar">
            <!-- Category -->
            <div>
              <label class="block text-xs font-medium text-gray-400 mb-2">分类</label>
              <div class="flex flex-wrap gap-2">
                <button
                  v-for="cat in categories.filter(c => c.id !== 'all')"
                  :key="cat.id"
                  @click="publishForm.category = cat.id"
                  class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg border text-xs transition-all"
                  :class="publishForm.category === cat.id
                    ? 'bg-neon-blue/10 text-neon-blue border-neon-blue/50 font-bold'
                    : 'bg-slate-800 text-gray-400 border-white/10 hover:border-white/30'"
                >
                  <component :is="cat.icon" class="w-4 h-4" />
                  {{ cat.name }}
                </button>
              </div>
            </div>

            <!-- Title -->
            <div>
              <label class="block text-xs font-medium text-gray-400 mb-1.5">标题 <span class="text-pink-500">*</span></label>
              <input
                v-model="publishForm.title"
                type="text"
                maxlength="100"
                placeholder="给作品起个好名字..."
                class="w-full bg-slate-800 text-white text-sm px-3 py-2 rounded-lg border border-white/10 focus:border-neon-blue/50 focus:outline-none placeholder-gray-500 transition-colors"
              />
              <p class="mt-0.5 text-xs text-gray-500 text-right">{{ publishForm.title.length }}/100</p>
            </div>

            <!-- Cover Image -->
            <div>
              <label class="block text-xs font-medium text-gray-400 mb-1.5">封面图 <span class="text-pink-500">*</span></label>
              <div
                v-if="!coverPreview"
                class="relative border-2 border-dashed border-white/10 rounded-xl hover:border-neon-blue/30 transition-colors cursor-pointer"
              >
                <input type="file" accept="image/*" @change="handleCoverUpload" class="absolute inset-0 opacity-0 cursor-pointer z-10" />
                <div class="flex flex-col items-center justify-center py-6 text-gray-500">
                  <Loader2 v-if="coverUploading" class="w-8 h-8 mb-2 animate-spin text-neon-blue" />
                  <Upload v-else class="w-8 h-8 mb-2" />
                  <span class="text-xs">{{ coverUploading ? '上传中...' : '点击上传封面图' }}</span>
                  <span class="text-xs mt-0.5 text-gray-600">JPG/PNG/GIF/WebP，10MB 以内</span>
                </div>
              </div>
              <div v-else class="relative rounded-lg overflow-hidden">
                <img :src="coverPreview" class="w-full h-36 object-cover" />
                <button
                  @click="coverPreview = ''; publishForm.coverUrl = ''"
                  class="absolute top-2 right-2 p-1.5 bg-black/60 text-white rounded-full hover:bg-black/80 transition-colors"
                >
                  <X class="w-4 h-4" />
                </button>
              </div>
            </div>

            <!-- Description -->
            <div>
              <label class="block text-xs font-medium text-gray-400 mb-1.5">描述</label>
              <textarea
                v-model="publishForm.description"
                rows="3"
                maxlength="2000"
                placeholder="描述你的作品..."
                class="w-full bg-slate-800 text-white text-sm px-3 py-2 rounded-lg border border-white/10 focus:border-neon-blue/50 focus:outline-none placeholder-gray-500 resize-none transition-colors"
              />
              <p class="mt-0.5 text-xs text-gray-500 text-right">{{ publishForm.description.length }}/2000</p>
            </div>

            <!-- Tag -->
            <div>
              <label class="block text-xs font-medium text-gray-400 mb-1.5">标签</label>
              <div class="relative">
                <Tag class="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-gray-500" />
                <input
                  v-model="publishForm.tag"
                  type="text"
                  maxlength="30"
                  placeholder="如：Cyberpunk, AI Art..."
                  class="w-full bg-slate-800 text-white text-sm pl-9 pr-3 py-2 rounded-lg border border-white/10 focus:border-neon-blue/50 focus:outline-none placeholder-gray-500 transition-colors"
                />
              </div>
            </div>
          </div>

          <!-- Actions -->
          <div class="flex gap-3 px-5 py-4 border-t border-white/5">
            <button
              @click="closePublish"
              class="flex-1 py-2.5 bg-slate-700 text-gray-300 text-sm rounded-lg font-bold hover:bg-slate-600 transition-colors"
            >
              取消
            </button>
            <button
              @click="submitPublish"
              :disabled="publishSubmitting || !publishForm.title.trim() || !publishForm.coverUrl"
              class="flex-1 py-2.5 bg-gradient-to-r from-neon-blue to-neon-purple text-white text-sm rounded-lg font-bold hover:opacity-90 transition-all disabled:opacity-40 disabled:cursor-not-allowed flex items-center justify-center gap-2"
            >
              <Loader2 v-if="publishSubmitting" class="w-4 h-4 animate-spin" />
              <Send v-else class="w-4 h-4" />
              {{ publishSubmitting ? '发布中...' : '发布' }}
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- ==================== Post Detail Modal ==================== -->
    <transition name="modal">
      <div v-if="selectedPost" class="fixed inset-0 z-[100] flex justify-center bg-black/90 backdrop-blur-sm overflow-y-auto custom-scrollbar">
        <!-- Backdrop Click Area -->
        <div class="absolute inset-0 -z-10 cursor-pointer" @click="closeDetail"></div>
        <!-- Modal Body -->
        <div class="relative w-full max-w-4xl mt-20 mb-4 md:mt-20 md:mb-8 mx-4 bg-slate-900 rounded-2xl overflow-hidden shadow-2xl border border-white/10 animate__animated animate__zoomIn flex flex-col md:flex-row h-fit max-h-[80vh]">
          <!-- Close Button -->
          <button @click="closeDetail" class="absolute top-4 right-4 z-50 p-2 bg-black/50 text-white rounded-full hover:bg-black/80 transition-colors border border-white/10">
            <X class="w-6 h-6" />
          </button>
          <!-- Left: Image -->
          <div class="w-full md:w-3/5 bg-black flex items-center justify-center relative group overflow-hidden">
            <img :src="selectedPost.image" class="w-full h-auto object-contain max-h-[85vh]" />
            <div class="absolute bottom-4 right-4 flex gap-2 opacity-100 transition-opacity">
              <button @click="shareContent" class="p-2 bg-black/50 hover:bg-black/70 backdrop-blur rounded-lg text-white" title="分享">
                <Share2 class="w-5 h-5"/>
              </button>
              <button class="p-2 bg-black/50 hover:bg-black/70 backdrop-blur rounded-lg text-white" title="更多">
                <MoreHorizontal class="w-5 h-5"/>
              </button>
            </div>
            <!-- View Count -->
            <div class="absolute top-4 left-4 flex items-center gap-1 px-3 py-1 bg-black/50 backdrop-blur rounded-full text-xs text-gray-300">
              <Eye class="w-3.5 h-3.5" />
              {{ selectedPost.views }}
            </div>
          </div>
          <!-- Right: Info -->
          <div class="w-full md:w-2/5 bg-slate-800 flex flex-col border-l border-white/5">
            <!-- Content -->
            <div class="flex-1 p-5 overflow-y-auto custom-scrollbar">
              <!-- Author -->
              <div class="flex items-center gap-2.5 mb-5">
                <img :src="selectedPost.author.avatarUrl || 'https://api.dicebear.com/7.x/avataaars/svg?seed=' + selectedPost.author.username" class="w-9 h-9 rounded-full border border-white/20" />
                <div>
                  <h4 class="text-white font-bold">{{ selectedPost.author.nickname || selectedPost.author.username }}</h4>
                  <p class="text-xs text-gray-400">{{ formatTime(selectedPost.createdAt) }}</p>
                </div>
                <button
                  @click="toggleFollow"
                  class="ml-auto px-4 py-1.5 text-xs font-bold rounded-full transition-colors flex items-center gap-1"
                  :class="selectedPost.isFollowed
                    ? 'bg-slate-600 text-gray-300 hover:bg-slate-500'
                    : 'bg-neon-blue/10 text-neon-blue hover:bg-neon-blue hover:text-slate-900'"
                >
                  <UserCheck v-if="selectedPost.isFollowed" class="w-3.5 h-3.5" />
                  <UserPlus v-else class="w-3.5 h-3.5" />
                  {{ selectedPost.isFollowed ? '已关注' : '关注' }}
                </button>
              </div>
              <h2 class="text-xl font-bold text-white mb-3 leading-tight">{{ selectedPost.title }}</h2>
              <p class="text-gray-300 leading-relaxed whitespace-pre-line">{{ selectedPost.description }}</p>
              <div class="mt-4 flex flex-wrap gap-2">
                <span v-if="selectedPost.tag" class="px-3 py-1 bg-white/5 rounded-lg text-xs text-gray-400">#{{ selectedPost.tag }}</span>
                <span class="px-3 py-1 bg-white/5 rounded-lg text-xs text-gray-400">#{{ selectedPost.category }}</span>
              </div>
              <!-- Comments Section -->
              <div class="mt-6 pt-4 border-t border-white/5">
                <h5 class="text-sm font-bold text-gray-400 mb-3">评论 ({{ commentTotal }})</h5>
                <!-- Comment Input -->
                <div class="flex gap-3 mb-6">
                  <div class="w-8 h-8 rounded-full bg-slate-700 flex-shrink-0 overflow-hidden">
                    <img v-if="authStore.user?.avatar" :src="authStore.user.avatar" class="w-full h-full object-cover" />
                  </div>
                  <div class="flex-1 flex gap-2">
                    <input
                      v-model="commentText"
                      @keydown.enter="submitComment"
                      type="text"
                      placeholder="写下你的评论..."
                      class="flex-1 bg-slate-700/50 text-white text-sm px-4 py-2 rounded-xl border border-white/5 focus:border-neon-blue/50 focus:outline-none placeholder-gray-500"
                    />
                    <button
                      @click="submitComment"
                      :disabled="!commentText.trim() || commentSubmitting"
                      class="p-2 bg-neon-blue text-slate-900 rounded-xl hover:bg-neon-blue/80 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      <Loader2 v-if="commentSubmitting" class="w-4 h-4 animate-spin" />
                      <Send v-else class="w-4 h-4" />
                    </button>
                  </div>
                </div>
                <!-- Loading -->
                <div v-if="detailLoading" class="flex justify-center py-8">
                  <Loader2 class="w-6 h-6 text-neon-blue animate-spin" />
                </div>
                <!-- Comment List -->
                <div v-else class="space-y-6">
                  <div v-for="c in comments" :key="c.id" class="flex gap-3">
                    <img :src="c.author.avatarUrl || 'https://api.dicebear.com/7.x/avataaars/svg?seed=' + c.author.username" class="w-8 h-8 rounded-full flex-shrink-0 bg-slate-700" />
                    <div>
                      <div class="flex items-center gap-2 mb-1">
                        <span class="text-xs font-bold text-gray-300">{{ c.author.nickname || c.author.username }}</span>
                        <span class="text-xs text-gray-500">{{ formatTime(c.createdAt) }}</span>
                      </div>
                      <p class="text-sm text-gray-400">{{ c.content }}</p>
                    </div>
                  </div>
                  <div v-if="!detailLoading && comments.length === 0" class="text-center py-8 text-gray-500 text-sm">
                    暂无评论，来抢沙发吧！
                  </div>
                </div>
              </div>
            </div>
            <!-- Fixed Bottom Actions -->
            <div class="mt-auto p-4 border-t border-white/5 flex gap-4 bg-slate-800 sticky bottom-0">
              <button
                @click="toggleLike(selectedPost!)"
                class="flex-1 py-3 rounded-xl font-bold flex items-center justify-center gap-2 transition-all"
                :class="selectedPost.isLiked ? 'bg-pink-500 text-white' : 'bg-slate-700 text-gray-300 hover:bg-slate-600'"
              >
                <Heart class="w-5 h-5" :class="selectedPost.isLiked ? 'fill-current' : ''" />
                {{ selectedPost.isLiked ? '已赞' : '点赞' }} ({{ selectedPost.likes }})
              </button>
              <button
                @click="shareContent"
                class="flex-1 bg-slate-700 text-gray-300 py-3 rounded-xl font-bold hover:bg-slate-600 transition-all flex items-center justify-center gap-2"
              >
                <Share2 class="w-5 h-5" /> 分享
              </button>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.no-scrollbar::-webkit-scrollbar {
  display: none;
}
.no-scrollbar {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.3s ease;
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 4px;
}
</style>

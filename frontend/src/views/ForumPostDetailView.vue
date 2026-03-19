<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft, MessageSquare, Heart, Eye, Clock, Tag, Send,
  ChevronLeft, ChevronRight, Trash2, Share2
} from 'lucide-vue-next'
import { useToastStore } from '@/stores/toast'
import { useAuthStore } from '@/stores/auth'
import { forumApi, type ForumPost, type ForumComment } from '@/api/forum'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

const route = useRoute()
const router = useRouter()
const toast = useToastStore()
const authStore = useAuthStore()

const postId = computed(() => Number(route.params.id))

// --- 帖子数据 ---
const post = ref<ForumPost | null>(null)
const isPostLoading = ref(true)
const hasLiked = ref(false)
const isLiking = ref(false)

// --- 评论数据 ---
const comments = ref<ForumComment[]>([])
const commentTotal = ref(0)
const commentPage = ref(1)
const commentPageSize = 20
const isCommentsLoading = ref(false)

// --- 发表评论 ---
const commentContent = ref('')
const isSubmittingComment = ref(false)
const replyTo = ref<{ id: number; nickname: string } | null>(null)

const commentTotalPages = computed(() => Math.ceil(commentTotal.value / commentPageSize))

const boards: Record<string, string> = {
  game: '游戏综合',
  anime: '动漫新番',
  ai: 'AI 绘图',
  chat: '闲聊灌水',
}

function formatTime(dateStr: string) {
  return dayjs(dateStr).fromNow()
}

function formatDate(dateStr: string) {
  return dayjs(dateStr).format('YYYY-MM-DD HH:mm')
}

function formatCount(n: number) {
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return String(n)
}

// Markdown 安全渲染
function renderMarkdown(content: string): string {
  const raw = marked.parse(content, { async: false }) as string
  return DOMPurify.sanitize(raw, {
    ALLOWED_TAGS: [
      'p', 'br', 'strong', 'em', 'u', 's', 'del', 'code', 'pre',
      'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
      'ul', 'ol', 'li', 'blockquote', 'hr',
      'a', 'img', 'table', 'thead', 'tbody', 'tr', 'th', 'td',
    ],
    ALLOWED_ATTR: ['href', 'src', 'alt', 'title', 'class', 'target'],
  })
}

// --- 加载帖子详情 ---
async function loadPost() {
  isPostLoading.value = true
  try {
    post.value = await forumApi.getPost(postId.value)
    // 检查是否已点赞
    if (authStore.isAuthenticated) {
      try {
        const res = await forumApi.checkInteraction('post', postId.value, 'like')
        hasLiked.value = res.hasInteracted
      } catch { /* 非致命错误 */ }
    }
  } catch {
    toast.error('帖子不存在或已被删除')
    router.push('/forum')
  } finally {
    isPostLoading.value = false
  }
}

// --- 加载评论 ---
async function loadComments() {
  isCommentsLoading.value = true
  try {
    const result = await forumApi.getComments(postId.value, commentPage.value, commentPageSize)
    comments.value = result.comments
    commentTotal.value = result.total
  } catch {
    toast.error('加载评论失败')
  } finally {
    isCommentsLoading.value = false
  }
}

// --- 点赞帖子 ---
async function toggleLike() {
  if (!authStore.isAuthenticated) {
    toast.warning('请先登录后再操作')
    return
  }
  if (isLiking.value || !post.value) return

  isLiking.value = true
  try {
    await forumApi.interact({ targetType: 'post', targetId: postId.value, actionType: 'like' })
    hasLiked.value = !hasLiked.value
    post.value.likeCount += hasLiked.value ? 1 : -1
  } catch {
    toast.error('操作失败')
  } finally {
    isLiking.value = false
  }
}

// --- 设置回复对象 ---
function setReply(comment: ForumComment) {
  replyTo.value = {
    id: comment.userId,
    nickname: comment.author.nickname || comment.author.username,
  }
  commentContent.value = `@${replyTo.value.nickname} `
  document.getElementById('comment-input')?.focus()
}

function clearReply() {
  replyTo.value = null
  if (commentContent.value.startsWith('@')) {
    commentContent.value = ''
  }
}

// --- 提交评论 ---
async function submitComment() {
  if (!authStore.isAuthenticated) {
    toast.warning('请先登录后再评论')
    return
  }
  if (!commentContent.value.trim()) {
    toast.error('评论内容不能为空')
    return
  }

  isSubmittingComment.value = true
  try {
    await forumApi.createComment({
      postId: postId.value,
      content: commentContent.value.trim(),
      replyToUserId: replyTo.value?.id,
    })
    commentContent.value = ''
    replyTo.value = null
    toast.success('评论发布成功')

    // 刷新评论列表（回到第一页查看最新评论）
    commentPage.value = 1
    await loadComments()
    if (post.value) post.value.commentCount++
  } catch (e: any) {
    toast.error(e?.message || '评论发布失败')
  } finally {
    isSubmittingComment.value = false
  }
}

// --- 删除帖子 ---
async function deletePost() {
  if (!confirm('确定要删除这篇帖子吗？此操作不可撤销。')) return
  try {
    await forumApi.deletePost(postId.value)
    toast.success('帖子已删除')
    router.push('/forum')
  } catch {
    toast.error('删除失败')
  }
}

// --- 分享帖子 ---
function sharePost() {
  const url = window.location.href
  if (navigator.clipboard) {
    navigator.clipboard.writeText(url)
    toast.success('链接已复制到剪贴板')
  } else {
    toast.info(`帖子链接：${url}`)
  }
}

// 切换评论页
async function changeCommentPage(p: number) {
  commentPage.value = p
  await loadComments()
  document.getElementById('comments-section')?.scrollIntoView({ behavior: 'smooth' })
}

onMounted(async () => {
  await loadPost()
  await loadComments()
})
</script>

<template>
  <div class="container mx-auto px-4 py-20 max-w-4xl min-h-screen">
    <!-- Back Button -->
    <button
      @click="router.push('/forum')"
      class="flex items-center gap-2 text-gray-400 hover:text-white mb-6 transition-colors group"
    >
      <ArrowLeft class="w-4 h-4 group-hover:-translate-x-1 transition-transform" />
      返回论坛
    </button>

    <!-- Loading Skeleton -->
    <div v-if="isPostLoading" class="space-y-6 animate-pulse">
      <div class="h-8 bg-slate-800 rounded w-3/4"></div>
      <div class="h-4 bg-slate-800 rounded w-1/3"></div>
      <div class="space-y-3">
        <div class="h-4 bg-slate-800 rounded"></div>
        <div class="h-4 bg-slate-800 rounded w-5/6"></div>
        <div class="h-4 bg-slate-800 rounded w-4/6"></div>
      </div>
    </div>

    <!-- Post Content -->
    <article v-else-if="post" class="bg-slate-800/60 border border-white/5 rounded-2xl overflow-hidden mb-8">
      <!-- Post Header -->
      <div class="p-6 border-b border-white/5">
        <div class="flex items-center gap-2 mb-3 flex-wrap">
          <span v-if="post.isTop === 1" class="px-2 py-0.5 rounded bg-red-500/20 text-red-400 text-xs font-bold">置顶</span>
          <span v-if="post.isEssence === 1" class="px-2 py-0.5 rounded bg-orange-500/20 text-orange-400 text-xs font-bold">HOT</span>
          <span class="px-2 py-0.5 rounded bg-blue-500/10 text-blue-400 text-xs">
            #{{ boards[post.category] || post.category }}
          </span>
        </div>

        <h1 class="text-2xl md:text-3xl font-bold text-white mb-4 leading-snug">{{ post.title }}</h1>

        <div class="flex items-center justify-between flex-wrap gap-4">
          <div class="flex items-center gap-3">
            <img
              :src="post.author.avatarUrl || `https://api.dicebear.com/7.x/avataaars/svg?seed=${post.author.username}`"
              class="w-10 h-10 rounded-full bg-slate-700 object-cover"
            />
            <div>
              <div class="text-white font-medium">{{ post.author.nickname || post.author.username }}</div>
              <div class="text-xs text-gray-500 flex items-center gap-1">
                <Clock class="w-3 h-3" /> {{ formatDate(post.createdAt) }}（{{ formatTime(post.createdAt) }}）
              </div>
            </div>
          </div>

          <div class="flex items-center gap-4 text-sm text-gray-400">
            <span class="flex items-center gap-1"><Eye class="w-4 h-4" /> {{ formatCount(post.viewCount) }}</span>
            <span class="flex items-center gap-1"><MessageSquare class="w-4 h-4" /> {{ formatCount(post.commentCount) }}</span>
            <span class="flex items-center gap-1"><Heart class="w-4 h-4" /> {{ formatCount(post.likeCount) }}</span>

            <button @click="sharePost" class="p-1.5 rounded hover:bg-white/10 transition-colors" title="分享">
              <Share2 class="w-4 h-4" />
            </button>

            <button
              v-if="authStore.user?.id === post.userId || authStore.user?.username === post.author.username"
              @click="deletePost"
              class="p-1.5 rounded hover:bg-red-500/20 text-red-400 transition-colors"
              title="删除帖子"
            >
              <Trash2 class="w-4 h-4" />
            </button>
          </div>
        </div>

        <!-- Tags -->
        <div v-if="post.tags" class="flex items-center gap-2 mt-3 flex-wrap">
          <Tag class="w-3 h-3 text-gray-500" />
          <span
            v-for="tag in post.tags.split(',')"
            :key="tag"
            class="px-2 py-0.5 rounded-full bg-white/5 text-gray-400 text-xs border border-white/5"
          >{{ tag.trim() }}</span>
        </div>
      </div>

      <!-- Post Body (Markdown rendered) -->
      <div class="p-6">
        <div
          class="post-content prose prose-invert max-w-none"
          v-html="renderMarkdown(post.content)"
        ></div>
      </div>

      <!-- Like Button -->
      <div class="p-6 pt-0 border-t border-white/5 mt-4 flex justify-center">
        <button
          @click="toggleLike"
          :disabled="isLiking"
          class="flex items-center gap-2 px-6 py-2.5 rounded-full border transition-all font-medium"
          :class="hasLiked
            ? 'bg-red-500/20 border-red-500/50 text-red-400 hover:bg-red-500/30'
            : 'bg-white/5 border-white/10 text-gray-400 hover:bg-white/10 hover:text-white'"
        >
          <Heart class="w-4 h-4" :class="{ 'fill-current': hasLiked }" />
          {{ hasLiked ? '已点赞' : '点赞' }} {{ post.likeCount > 0 ? formatCount(post.likeCount) : '' }}
        </button>
      </div>
    </article>

    <!-- Comments Section -->
    <section id="comments-section" v-if="post">
      <h2 class="text-xl font-bold text-white mb-4 flex items-center gap-2">
        <MessageSquare class="w-5 h-5 text-neon-blue" />
        评论 <span class="text-sm text-gray-500 font-normal">（{{ commentTotal }}条）</span>
      </h2>

      <!-- Comment Input -->
      <div class="bg-slate-800/60 border border-white/5 rounded-xl p-4 mb-6">
        <div v-if="replyTo" class="flex items-center gap-2 mb-2 text-sm text-gray-400">
          <span>回复 <span class="text-neon-blue">@{{ replyTo.nickname }}</span></span>
          <button @click="clearReply" class="ml-auto text-gray-600 hover:text-gray-400">
            <span class="text-xs">× 取消回复</span>
          </button>
        </div>
        <textarea
          id="comment-input"
          v-model="commentContent"
          rows="3"
          :placeholder="authStore.isAuthenticated ? '分享你的看法...' : '登录后参与讨论'"
          :disabled="!authStore.isAuthenticated"
          class="w-full bg-slate-700/50 border border-white/5 rounded-lg p-3 text-white text-sm focus:border-neon-blue/50 focus:outline-none resize-none disabled:opacity-50 disabled:cursor-not-allowed"
        ></textarea>
        <div class="flex justify-between items-center mt-2">
          <span class="text-xs text-gray-600">{{ commentContent.length }} 字</span>
          <button
            @click="submitComment"
            :disabled="isSubmittingComment || !authStore.isAuthenticated"
            class="flex items-center gap-2 px-4 py-1.5 rounded-lg bg-neon-blue text-slate-900 font-bold text-sm hover:opacity-90 transition-all disabled:opacity-50"
          >
            <span v-if="isSubmittingComment" class="animate-spin w-3 h-3 border-2 border-slate-900 border-t-transparent rounded-full"></span>
            <Send v-else class="w-3 h-3" />
            {{ isSubmittingComment ? '发送中...' : '发表评论' }}
          </button>
        </div>
      </div>

      <!-- Comment Loading -->
      <div v-if="isCommentsLoading" class="space-y-4">
        <div v-for="i in 3" :key="i" class="bg-slate-800/40 rounded-xl p-4 animate-pulse">
          <div class="flex gap-3">
            <div class="w-8 h-8 rounded-full bg-slate-700 flex-shrink-0"></div>
            <div class="flex-1 space-y-2">
              <div class="h-3 bg-slate-700 rounded w-1/4"></div>
              <div class="h-4 bg-slate-700 rounded w-3/4"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- Comment List -->
      <div v-else class="space-y-4">
        <div
          v-for="comment in comments"
          :key="comment.id"
          class="bg-slate-800/40 border border-white/5 rounded-xl p-4 hover:border-white/10 transition-colors"
          :class="{ 'ml-8 border-l-2 border-l-neon-blue/30': comment.parentCommentId > 0 }"
        >
          <div class="flex gap-3">
            <img
              :src="comment.author.avatarUrl || `https://api.dicebear.com/7.x/avataaars/svg?seed=${comment.author.username}`"
              class="w-8 h-8 rounded-full bg-slate-700 object-cover flex-shrink-0"
            />
            <div class="flex-1 min-w-0">
              <div class="flex items-center justify-between gap-2 mb-1">
                <div class="flex items-center gap-2">
                  <span class="text-sm font-medium text-white">{{ comment.author.nickname || comment.author.username }}</span>
                  <span class="text-xs text-gray-500">{{ formatTime(comment.createdAt) }}</span>
                </div>
                <button
                  v-if="authStore.isAuthenticated"
                  @click="setReply(comment)"
                  class="text-xs text-gray-500 hover:text-neon-blue transition-colors"
                >回复</button>
              </div>
              <p class="text-gray-300 text-sm whitespace-pre-wrap break-words">{{ comment.content }}</p>
              <div class="flex items-center gap-3 mt-2 text-xs text-gray-500">
                <span class="flex items-center gap-1"><Heart class="w-3 h-3" /> {{ comment.likeCount }}</span>
              </div>
            </div>
          </div>
        </div>

        <div v-if="comments.length === 0 && !isCommentsLoading" class="text-center py-12 text-gray-500">
          <MessageSquare class="w-10 h-10 mx-auto mb-3 opacity-20" />
          <p>还没有评论，快来抢沙发吧！</p>
        </div>
      </div>

      <!-- Comment Pagination -->
      <div v-if="commentTotalPages > 1" class="flex items-center justify-center gap-2 mt-6">
        <button
          @click="changeCommentPage(commentPage - 1)"
          :disabled="commentPage <= 1"
          class="p-2 rounded-lg bg-slate-800 border border-white/10 text-gray-400 hover:text-white disabled:opacity-40 disabled:cursor-not-allowed transition-all"
        ><ChevronLeft class="w-4 h-4" /></button>

        <template v-for="p in commentTotalPages" :key="p">
          <button
            v-if="p === 1 || p === commentTotalPages || Math.abs(p - commentPage) <= 2"
            @click="changeCommentPage(p)"
            class="w-9 h-9 rounded-lg text-sm font-medium transition-all border"
            :class="p === commentPage
              ? 'bg-neon-blue/20 border-neon-blue text-neon-blue'
              : 'bg-slate-800 border-white/10 text-gray-400 hover:text-white hover:border-white/30'"
          >{{ p }}</button>
          <span v-else-if="p === 2 && commentPage > 4" class="text-gray-600">…</span>
          <span v-else-if="p === commentTotalPages - 1 && commentPage < commentTotalPages - 3" class="text-gray-600">…</span>
        </template>

        <button
          @click="changeCommentPage(commentPage + 1)"
          :disabled="commentPage >= commentTotalPages"
          class="p-2 rounded-lg bg-slate-800 border border-white/10 text-gray-400 hover:text-white disabled:opacity-40 disabled:cursor-not-allowed transition-all"
        ><ChevronRight class="w-4 h-4" /></button>
      </div>
    </section>
  </div>
</template>

<style scoped>
/* Markdown content styles */
.post-content :deep(h1),
.post-content :deep(h2),
.post-content :deep(h3) {
  @apply font-bold text-white mt-6 mb-3;
}
.post-content :deep(h1) { @apply text-2xl; }
.post-content :deep(h2) { @apply text-xl border-b border-white/10 pb-2; }
.post-content :deep(h3) { @apply text-lg; }

.post-content :deep(p) {
  @apply text-gray-300 leading-relaxed mb-4;
}

.post-content :deep(strong) { @apply text-white font-bold; }
.post-content :deep(em) { @apply italic text-gray-200; }

.post-content :deep(a) {
  @apply text-neon-blue hover:underline;
}

.post-content :deep(code) {
  @apply bg-slate-700 text-green-400 px-1.5 py-0.5 rounded text-sm font-mono;
}

.post-content :deep(pre) {
  @apply bg-slate-900 rounded-lg p-4 overflow-x-auto my-4;
}

.post-content :deep(pre code) {
  @apply bg-transparent p-0 text-gray-300;
}

.post-content :deep(blockquote) {
  @apply border-l-4 border-neon-blue/50 pl-4 my-4 text-gray-400 italic;
}

.post-content :deep(ul),
.post-content :deep(ol) {
  @apply text-gray-300 pl-5 mb-4 space-y-1;
}

.post-content :deep(ul) { @apply list-disc; }
.post-content :deep(ol) { @apply list-decimal; }

.post-content :deep(hr) {
  @apply border-white/10 my-6;
}

.post-content :deep(img) {
  @apply rounded-lg max-w-full my-4;
}

.post-content :deep(table) {
  @apply w-full border-collapse my-4;
}

.post-content :deep(th),
.post-content :deep(td) {
  @apply border border-white/10 px-3 py-2 text-sm;
}

.post-content :deep(th) {
  @apply bg-slate-700/50 text-white font-bold;
}

.post-content :deep(td) {
  @apply text-gray-300;
}
</style>

<script setup lang="ts">
import { ref, computed, reactive } from 'vue'
import { MessageSquare, Flame, Clock, Hash, Search, Plus, User, X, PenTool, Image as ImageIcon } from 'lucide-vue-next'
import GlitchText from '@/components/ui/GlitchText.vue'
import { useToastStore } from '@/stores/toast'
import { useAuthStore } from '@/stores/auth'

const toast = useToastStore()
const authStore = useAuthStore()

const activeBoard = ref('all')
const sortMode = ref('hot') // hot, new
const searchQuery = ref('')
const isNewPostModalOpen = ref(false)
const isPublishing = ref(false)

const newPostForm = reactive({
  title: '',
  board: 'chat',
  content: ''
})

const boards = [
  { id: 'all', name: '全部板块', icon: Hash },
  { id: 'game', name: '游戏综合', icon: MessageSquare },
  { id: 'anime', name: '动漫新番', icon: MessageSquare },
  { id: 'ai', name: 'AI 绘图', icon: MessageSquare },
  { id: 'chat', name: '闲聊灌水', icon: MessageSquare },
]

// 模拟帖子数据
const posts = ref([
  {
    id: 1,
    title: '【置顶】Inspo-Verse 社区版规 v2.0 (必读)',
    author: 'Admin',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Admin',
    board: 'chat',
    replyCount: 999,
    viewCount: '10w+',
    time: '2024-01-01',
    isTop: true,
    isHot: true
  },
  // ... 现有数据保持不变 ...
  {
    id: 2,
    title: '黑神话：悟空 隐藏 BOSS 触发条件汇总 (持续更新)',
    author: 'MonkeyKing',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Monkey',
    board: 'game',
    replyCount: 234,
    viewCount: '5.6k',
    time: '3小时前',
    isTop: false,
    isHot: true
  },
  {
    id: 3,
    title: 'Midjourney V6 这一波更新太强了，写实感拉满！',
    author: 'AI_Artist',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=AI',
    board: 'ai',
    replyCount: 89,
    viewCount: '2.1k',
    time: '5小时前',
    isTop: false,
    isHot: true
  },
  {
    id: 4,
    title: '鬼灭之刃新一集虽然经费爆炸，但这剧情节奏...',
    author: 'Tanjiro_Fan',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Tanjiro',
    board: 'anime',
    replyCount: 156,
    viewCount: '3.4k',
    time: '昨天',
    isTop: false,
    isHot: false
  },
  {
    id: 5,
    title: '萌新刚入坑 Vue 3，求推荐好的 UI 组件库',
    author: 'Vue_Learner',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Vue',
    board: 'chat',
    replyCount: 12,
    viewCount: '340',
    time: '昨天',
    isTop: false,
    isHot: false
  },
  {
    id: 6,
    title: '有人在玩 Starfield 吗？这飞船建造系统太复杂了',
    author: 'Space_Walker',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Space',
    board: 'game',
    replyCount: 45,
    viewCount: '1.2k',
    time: '2天前',
    isTop: false,
    isHot: false
  },
])

const filteredPosts = computed(() => {
  let result = posts.value
  
  // 搜索过滤
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    result = result.filter(p => 
      p.title.toLowerCase().includes(query) || 
      p.author.toLowerCase().includes(query)
    )
  }

  // 板块过滤
  if (activeBoard.value !== 'all') {
    result = result.filter(p => p.board === activeBoard.value)
  }
  
  // 排序
  if (sortMode.value === 'hot') {
    result = [...result].sort((a, b) => b.replyCount - a.replyCount)
  } else {
    result = [...result] // 默认顺序
  }
  
  // 置顶优先
  return result.sort((a, b) => (b.isTop ? 1 : 0) - (a.isTop ? 1 : 0))
})

const openNewPostModal = () => {
  if (!authStore.isAuthenticated) {
    toast.warning('请先登录后再发布帖子')
    return
  }
  newPostForm.title = ''
  newPostForm.content = ''
  newPostForm.board = activeBoard.value === 'all' ? 'chat' : activeBoard.value
  isNewPostModalOpen.value = true
}

const submitNewPost = async () => {
  if (!newPostForm.title.trim() || !newPostForm.content.trim()) {
    toast.error('标题和内容不能为空')
    return
  }

  isPublishing.value = true
  
  // 模拟 API 延迟
  await new Promise(resolve => setTimeout(resolve, 1000))

  const newPost = {
    id: Date.now(),
    title: newPostForm.title,
    author: authStore.user?.nickname || '匿名用户',
    avatar: authStore.user?.avatar || `https://api.dicebear.com/7.x/avataaars/svg?seed=${Date.now()}`,
    board: newPostForm.board,
    replyCount: 0,
    viewCount: '1',
    time: '刚刚',
    isTop: false,
    isHot: false
  }

  posts.value.unshift(newPost)
  isPublishing.value = false
  isNewPostModalOpen.value = false
  toast.success('发布成功！')
}
</script>

<template>
  <div class="container mx-auto px-4 py-20 min-h-screen flex flex-col md:flex-row gap-8">
    <!-- Sidebar (Board Nav) -->
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

      <!-- Stats Widget -->
      <div class="bg-slate-800 rounded-xl p-6 border border-white/5 hidden md:block sticky top-[400px]">
        <h3 class="text-white font-bold mb-4">社区统计</h3>
        <div class="grid grid-cols-2 gap-4 text-center">
          <div>
            <div class="text-2xl font-bold text-neon-blue">{{ posts.length + 1240 }}</div>
            <div class="text-xs text-gray-500">今日发帖</div>
          </div>
          <div>
            <div class="text-2xl font-bold text-neon-purple">5.8w</div>
            <div class="text-xs text-gray-500">在线用户</div>
          </div>
        </div>
      </div>
    </aside>

    <!-- Main Content -->
    <main class="flex-1">
      <!-- Header -->
      <div class="flex flex-col sm:flex-row justify-between items-center mb-6 gap-4">
        <h1 class="text-3xl font-bold"><GlitchText text="FORUM" /></h1>
        
        <div class="flex items-center gap-4 w-full sm:w-auto">
          <div class="relative flex-1 sm:w-64">
            <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500" />
            <input 
              v-model="searchQuery"
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
              title="热门"
            >
              <Flame class="w-4 h-4" />
            </button>
            <button 
              @click="sortMode = 'new'"
              class="p-2 rounded hover:bg-white/10 transition-colors"
              :class="sortMode === 'new' ? 'text-neon-blue' : 'text-gray-500'"
              title="最新"
            >
              <Clock class="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>

      <!-- Post List -->
      <div class="space-y-4 min-h-[50vh]">
        <transition-group name="list">
          <div 
            v-for="post in filteredPosts" 
            :key="post.id"
            class="bg-slate-800/50 border border-white/5 rounded-xl p-5 hover:border-neon-blue/30 transition-all hover:bg-slate-800 cursor-pointer group"
          >
            <div class="flex gap-4">
              <div class="flex-shrink-0">
                <img :src="post.avatar" class="w-10 h-10 rounded-full bg-slate-700" />
              </div>
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-2 mb-1">
                  <span v-if="post.isTop" class="px-1.5 py-0.5 rounded bg-red-500/20 text-red-400 text-xs font-bold">置顶</span>
                  <span v-if="post.isHot" class="px-1.5 py-0.5 rounded bg-orange-500/20 text-orange-400 text-xs font-bold">HOT</span>
                  <h3 class="text-lg font-bold text-white truncate group-hover:text-neon-blue transition-colors">{{ post.title }}</h3>
                </div>
                <div class="flex items-center gap-4 text-xs text-gray-400">
                  <span class="flex items-center gap-1 text-gray-300"><User class="w-3 h-3" /> {{ post.author }}</span>
                  <span>{{ post.time }}</span>
                  <span class="px-2 py-0.5 rounded bg-white/5 text-gray-500">#{{ boards.find(b => b.id === post.board)?.name }}</span>
                </div>
              </div>
              <div class="hidden sm:flex flex-col items-end justify-center gap-1 text-gray-500 min-w-[60px]">
                <div class="flex items-center gap-1 text-sm font-bold text-gray-300">
                  <MessageSquare class="w-4 h-4" /> {{ post.replyCount }}
                </div>
                <div class="text-xs">{{ post.viewCount }} 阅</div>
              </div>
            </div>
          </div>
        </transition-group>

        <!-- Empty State -->
        <div v-if="filteredPosts.length === 0" class="text-center py-20 text-gray-500">
          <Search class="w-12 h-12 mx-auto mb-4 opacity-20" />
          <p>没有找到相关帖子，换个关键词试试？</p>
        </div>
      </div>
    </main>

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
                <input v-model="newPostForm.title" type="text" placeholder="请输入引人注目的标题" class="w-full bg-slate-800 border border-white/10 rounded-lg px-4 py-2 text-white focus:border-neon-blue focus:outline-none" />
              </div>
              <div>
                <label class="block text-xs text-gray-400 mb-1">发布板块</label>
                <select v-model="newPostForm.board" class="w-full bg-slate-800 border border-white/10 rounded-lg px-4 py-2 text-white focus:border-neon-blue focus:outline-none">
                  <option v-for="b in boards.slice(1)" :key="b.id" :value="b.id">{{ b.name }}</option>
                </select>
              </div>
            </div>
            
            <div>
              <label class="block text-xs text-gray-400 mb-1">内容</label>
              <div class="relative">
                <textarea 
                  v-model="newPostForm.content" 
                  rows="8" 
                  placeholder="分享你的想法... (支持 Markdown 语法)" 
                  class="w-full bg-slate-800 border border-white/10 rounded-lg p-4 text-white focus:border-neon-blue focus:outline-none resize-none"
                ></textarea>
                <button class="absolute bottom-4 right-4 p-2 bg-white/10 hover:bg-white/20 rounded text-gray-300 transition-colors" title="上传图片">
                  <ImageIcon class="w-4 h-4" />
                </button>
              </div>
            </div>
          </div>

          <div class="p-6 pt-0 flex justify-end gap-3">
            <button @click="isNewPostModalOpen = false" class="px-6 py-2 rounded-lg text-gray-400 hover:text-white font-medium">取消</button>
            <button 
              @click="submitNewPost" 
              :disabled="isPublishing"
              class="px-6 py-2 rounded-lg bg-neon-blue text-slate-900 font-bold hover:shadow-lg hover:shadow-neon-blue/20 transition-all flex items-center gap-2"
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
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.3s ease;
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.list-enter-active,
.list-leave-active {
  transition: all 0.5s ease;
}
.list-enter-from,
.list-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}
</style>

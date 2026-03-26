<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import GlitchText from '@/components/ui/GlitchText.vue'
import { Star, Play, X, Calendar, User, Gamepad2, Heart, Plus, Search, Lock, Unlock, Clock, Smartphone, CheckCircle2, AlertCircle } from 'lucide-vue-next'
import { useToastStore } from '@/stores/toast'
import {
  getGameList,
  getGameDetail,
  toggleGameFavorite,
  publishGame,
  updateGame,
  uploadGameImage,
  createGamePurchase,
  queryGameOrderStatus,
  mockConfirmGameOrder,
  type GameItem,
  type PublishGameData,
  type GamePurchaseResp,
} from '@/api/game'

const toast = useToastStore()

// ==================== 列表状态 ====================
const games = ref<GameItem[]>([])
const loading = ref(false)
const totalGames = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const selectedGenre = ref('all')
const searchKeyword = ref('')
const sortBy = ref('new')
const activeIndex = ref(0)

// ==================== 详情弹窗 ====================
const selectedGame = ref<GameItem | null>(null)
const detailLoading = ref(false)

// ==================== 发布/编辑弹窗 ====================
const showPublishModal = ref(false)
const publishLoading = ref(false)
const editingGame = ref<GameItem | null>(null)
const publishForm = ref<PublishGameData>({
  title: '',
  genre: '',
  description: '',
  coverUrl: '',
  heroUrl: '',
  gameUrl: '',
  tags: '',
  developer: '',
  releaseDate: '',
  isPaid: 0,
  priceCents: 0,
  trialMinutes: 0,
})

// ==================== 支付弹窗 ====================
const showPayModal = ref(false)
const payStep = ref<'select' | 'qr' | 'success' | 'failed'>('select')
const payMethod = ref<'ALIPAY' | 'WECHAT'>('WECHAT')
const payOrder = ref<GamePurchaseResp | null>(null)
const payCreating = ref(false)
const payMockConfirming = ref(false)
const payGameId = ref(0)
const payRemainSeconds = ref(300)
let payCountdownTimer: ReturnType<typeof setInterval> | null = null
let payPollingTimer: ReturnType<typeof setInterval> | null = null

const payCountdownText = computed(() => {
  const m = Math.floor(payRemainSeconds.value / 60).toString().padStart(2, '0')
  const s = (payRemainSeconds.value % 60).toString().padStart(2, '0')
  return `${m}:${s}`
})

const genres = [
  { value: 'all', label: '全部' },
  { value: 'Action RPG', label: '动作RPG' },
  { value: 'Roguelike', label: 'Roguelike' },
  { value: 'Strategy', label: '策略' },
  { value: 'Adventure', label: '冒险' },
]

// ==================== 加载游戏列表 ====================
const loadGames = async () => {
  loading.value = true
  try {
    const res = await getGameList({
      genre: selectedGenre.value === 'all' ? undefined : selectedGenre.value,
      keyword: searchKeyword.value || undefined,
      sortBy: sortBy.value,
      page: currentPage.value,
      pageSize: pageSize.value,
    })
    if (res.code === 0 && res.data) {
      games.value = res.data.items || []
      totalGames.value = res.data.total || 0
      if (activeIndex.value >= games.value.length) {
        activeIndex.value = 0
      }
    }
  } catch {
    toast.error('加载游戏列表失败')
  } finally {
    loading.value = false
  }
}

// ==================== 搜索和筛选 ====================
const handleSearch = () => {
  currentPage.value = 1
  loadGames()
}

const handleGenreChange = (genre: string) => {
  selectedGenre.value = genre
  currentPage.value = 1
  loadGames()
}

const handleSortChange = (sort: string) => {
  sortBy.value = sort
  currentPage.value = 1
  loadGames()
}

// ==================== 游戏详情 ====================
const openGameDetail = async (game: GameItem) => {
  detailLoading.value = true
  selectedGame.value = game
  try {
    const res = await getGameDetail(game.id)
    if (res.code === 0 && res.data) {
      selectedGame.value = res.data
    }
  } catch {
    // 使用列表数据
  } finally {
    detailLoading.value = false
  }
}

const closeDetail = () => {
  selectedGame.value = null
}

// ==================== 收藏 ====================
const handleFavorite = async (game: GameItem) => {
  try {
    const res = await toggleGameFavorite(game.id)
    if (res.code === 0 && res.data) {
      game.isFavorited = res.data.isFavorited
      game.favoriteCount = res.data.favoriteCount
      if (selectedGame.value && selectedGame.value.id === game.id) {
        selectedGame.value.isFavorited = res.data.isFavorited
        selectedGame.value.favoriteCount = res.data.favoriteCount
      }
      toast.success(res.data.isFavorited ? '已加入收藏' : '已取消收藏')
    }
  } catch {
    toast.error('请先登录')
  }
}

// ==================== 开始游戏 ====================
const handleStartGame = (game: GameItem) => {
  if (game.isPaid === 1 && !game.isPurchased) {
    payGameId.value = game.id
    payStep.value = 'select'
    payMethod.value = 'WECHAT'
    payOrder.value = null
    showPayModal.value = true
    return
  }
  if (game.gameUrl) {
    window.open(game.gameUrl, '_blank')
  } else {
    toast.info('游戏链接暂未配置')
  }
}

// ==================== 试玩 ====================
const handleTrial = (game: GameItem) => {
  if (game.gameUrl) {
    toast.info(`试玩时间：${game.trialMinutes || 10} 分钟`)
    window.open(game.gameUrl, '_blank')
  } else {
    toast.info('游戏链接暂未配置')
  }
}

// ==================== 发布/编辑弹窗 ====================
const openPublishModal = () => {
  editingGame.value = null
  publishForm.value = {
    title: '',
    genre: '',
    description: '',
    coverUrl: '',
    heroUrl: '',
    gameUrl: '',
    tags: '',
    developer: '',
    releaseDate: '',
    isPaid: 0,
    priceCents: 0,
    trialMinutes: 0,
  }
  showPublishModal.value = true
}

const handleImageUpload = async (e: Event, field: 'coverUrl' | 'heroUrl') => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  try {
    const res = await uploadGameImage(file)
    if (res.code === 0 && res.data) {
      publishForm.value[field] = res.data.url
      toast.success('图片上传成功')
    }
  } catch {
    toast.error('图片上传失败')
  }
}

const handleSubmitPublish = async () => {
  if (!publishForm.value.title || !publishForm.value.genre || !publishForm.value.description
      || !publishForm.value.developer || !publishForm.value.releaseDate || !publishForm.value.gameUrl
      || !publishForm.value.tags || !publishForm.value.coverUrl || !publishForm.value.heroUrl) {
    toast.error('请填写所有必填项')
    return
  }
  publishLoading.value = true

  const tagsStr = publishForm.value.tags || ''
  const tagsArr = tagsStr.split(/[,，]/).map(t => t.trim()).filter(Boolean)
  const submitData = {
    ...publishForm.value,
    tags: JSON.stringify(tagsArr),
  }

  try {
    if (editingGame.value) {
      const res = await updateGame(editingGame.value.id, submitData)
      if (res.code === 0) {
        toast.success('游戏更新成功')
        showPublishModal.value = false
        loadGames()
      } else {
        toast.error(res.message || '更新失败')
      }
    } else {
      const res = await publishGame(submitData)
      if (res.code === 0) {
        toast.success('游戏发布成功')
        showPublishModal.value = false
        loadGames()
      } else {
        toast.error(res.message || '发布失败')
      }
    }
  } catch (e: any) {
    toast.error(e?.message || '操作失败')
  } finally {
    publishLoading.value = false
  }
}

// ==================== 支付流程 ====================
const handleCreatePurchase = async () => {
  payCreating.value = true
  try {
    const res = await createGamePurchase(payGameId.value, payMethod.value)
    if (res.code !== 0) {
      toast.error(res.message || '创建订单失败')
      return
    }
    payOrder.value = res.data
    payStep.value = 'qr'
    startPayCountdown()
    startPayPolling(res.data!.orderNo)
  } catch (e: any) {
    toast.error(e?.message || '创建订单失败')
  } finally {
    payCreating.value = false
  }
}

const startPayCountdown = () => {
  payRemainSeconds.value = 300
  payCountdownTimer = setInterval(() => {
    payRemainSeconds.value--
    if (payRemainSeconds.value <= 0) {
      stopPayCountdown()
      payStep.value = 'failed'
      stopPayPolling()
    }
  }, 1000)
}

const stopPayCountdown = () => {
  if (payCountdownTimer) { clearInterval(payCountdownTimer); payCountdownTimer = null }
}

const startPayPolling = (orderNo: string) => {
  payPollingTimer = setInterval(async () => {
    try {
      const res = await queryGameOrderStatus(orderNo)
      if (res.code !== 0) return
      if (res.data?.status === 'PAID') {
        stopPayPolling()
        stopPayCountdown()
        payStep.value = 'success'
        if (selectedGame.value) {
          selectedGame.value.isPurchased = true
        }
      } else if (res.data?.status === 'EXPIRED' || res.data?.status === 'FAILED') {
        stopPayPolling()
        stopPayCountdown()
        payStep.value = 'failed'
      }
    } catch { /* 忽略轮询错误 */ }
  }, 2000)
}

const stopPayPolling = () => {
  if (payPollingTimer) { clearInterval(payPollingTimer); payPollingTimer = null }
}

const handleMockConfirm = async () => {
  if (!payOrder.value) return
  payMockConfirming.value = true
  try {
    const res = await mockConfirmGameOrder(payOrder.value.orderNo)
    if (res.code === 0) {
      stopPayPolling()
      stopPayCountdown()
      payStep.value = 'success'
      if (selectedGame.value) {
        selectedGame.value.isPurchased = true
      }
    }
  } catch {
    toast.error('模拟支付失败')
  } finally {
    payMockConfirming.value = false
  }
}

const closePayModal = () => {
  showPayModal.value = false
  stopPayCountdown()
  stopPayPolling()
}

// 解析 tags JSON
const parseTags = (tags: string): string[] => {
  if (!tags) return []
  try {
    const arr = JSON.parse(tags)
    return Array.isArray(arr) ? arr : []
  } catch {
    return tags.split(/[,，]/).map(t => t.trim()).filter(Boolean)
  }
}

// 格式化价格
const formatPrice = (cents: number) => {
  return (cents / 100).toFixed(2)
}

const methodConfig: Record<string, { bg: string; emoji: string; label: string }> = {
  WECHAT: { bg: 'bg-[#07C160]', emoji: '💚', label: '微信支付' },
  ALIPAY: { bg: 'bg-[#1677FF]', emoji: '💙', label: '支付宝' },
}

// ==================== 生命周期 ====================
onMounted(() => {
  loadGames()
})

onUnmounted(() => {
  stopPayCountdown()
  stopPayPolling()
})
</script>

<template>
  <div class="container mx-auto px-4 py-20 min-h-screen">
    <div class="text-center mb-16">
      <h1 class="text-5xl md:text-7xl font-black mb-4">
        <GlitchText text="GAME HUB" />
      </h1>
      <p class="text-neon-blue font-bold tracking-[0.2em] animate-pulse">探索无限可能，畅玩精品游戏世界</p>
    </div>

    <!-- 搜索/筛选/操作栏 -->
    <div class="flex flex-col md:flex-row gap-4 mb-8 items-center justify-between">
      <div class="flex flex-wrap gap-2">
        <button
          v-for="g in genres" :key="g.value"
          @click="handleGenreChange(g.value)"
          :class="[
            'px-4 py-2 rounded-full text-sm font-bold transition-all',
            selectedGenre === g.value
              ? 'bg-neon-blue text-slate-900'
              : 'bg-slate-800 text-gray-300 hover:bg-slate-700'
          ]"
        >{{ g.label }}</button>
      </div>
      <div class="flex items-center gap-3">
        <div class="relative">
          <input
            v-model="searchKeyword"
            @keyup.enter="handleSearch"
            type="text"
            placeholder="搜索游戏..."
            class="w-48 px-4 py-2 pl-10 bg-slate-800 border border-white/10 rounded-full text-white text-sm focus:outline-none focus:border-neon-blue/50"
          />
          <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500" />
        </div>
        <select
          v-model="sortBy"
          @change="handleSortChange(sortBy)"
          class="px-4 py-2 bg-slate-800 border border-white/10 rounded-full text-white text-sm focus:outline-none"
        >
          <option value="new">最新</option>
          <option value="hot">最热</option>
          <option value="rating">评分</option>
        </select>
        <button
          @click="openPublishModal"
          class="px-6 py-2 bg-neon-purple text-white font-bold rounded-full hover:shadow-[0_0_20px_rgba(168,85,247,0.5)] transition-all flex items-center gap-2 whitespace-nowrap"
        >
          <Plus class="w-4 h-4 shrink-0" /> 发布游戏
        </button>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading && games.length === 0" class="text-center py-20">
      <div class="animate-spin w-12 h-12 border-4 border-neon-blue border-t-transparent rounded-full mx-auto mb-4"></div>
      <p class="text-gray-400">加载中...</p>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!loading && games.length === 0" class="text-center py-20">
      <Gamepad2 class="w-16 h-16 text-gray-600 mx-auto mb-4" />
      <p class="text-gray-400 text-lg">暂无游戏</p>
      <p class="text-gray-500 text-sm mt-2">成为第一个发布游戏的人吧！</p>
    </div>

    <template v-else>
      <!-- Featured Game (Hero) -->
      <div v-if="games.length > 0" class="relative rounded-3xl overflow-hidden mb-16 group border border-neon-blue/20 shadow-[0_0_50px_rgba(0,243,255,0.1)]">
        <div class="absolute inset-0 bg-gradient-to-t from-slate-900 via-slate-900/40 to-transparent z-10"></div>
        <img :src="games[activeIndex]?.hero || games[activeIndex]?.cover" class="w-full h-[500px] object-cover object-top transition-transform duration-1000 group-hover:scale-105" />
        
        <div class="absolute bottom-0 left-0 p-8 md:p-12 z-20 max-w-2xl">
          <div class="flex items-center gap-3 mb-4">
            <span class="px-3 py-1 bg-neon-purple/80 backdrop-blur rounded text-xs font-bold">{{ games[activeIndex]?.genre }}</span>
            <div class="flex items-center gap-1 text-yellow-400 font-bold">
              <Star class="w-4 h-4 fill-yellow-400" /> {{ games[activeIndex]?.rating }}
            </div>
            <span v-if="games[activeIndex]?.isPaid === 1" class="px-3 py-1 bg-yellow-500/80 backdrop-blur rounded text-xs font-bold text-slate-900">
              ¥{{ formatPrice(games[activeIndex]?.priceCents || 0) }}
            </span>
            <span v-else class="px-3 py-1 bg-green-500/80 backdrop-blur rounded text-xs font-bold text-white">
              免费
            </span>
          </div>
          <h2 class="text-4xl md:text-6xl font-black text-white mb-6 leading-tight">{{ games[activeIndex]?.title }}</h2>
          <div class="flex gap-4">
            <button 
              @click="openGameDetail(games[activeIndex])"
              class="px-8 py-3 bg-neon-blue text-slate-900 font-bold rounded-full hover:shadow-[0_0_20px_rgba(0,243,255,0.5)] transition-all flex items-center gap-2"
            >
              <Play class="w-5 h-5 fill-current" /> 立即试玩
            </button>
            <button
              @click="openPublishModal"
              class="px-8 py-3 border border-neon-purple/50 text-neon-purple font-bold rounded-full hover:bg-neon-purple/10 flex items-center gap-2"
            >
              <Plus class="w-5 h-5" /> 发布游戏
            </button>
          </div>
        </div>
      </div>

      <!-- Game Grid -->
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
        <div 
          v-for="(game, index) in games" 
          :key="game.id"
          @mouseenter="activeIndex = index"
          @click="openGameDetail(game)"
          class="group relative bg-slate-800 rounded-2xl overflow-hidden border border-white/5 hover:border-neon-blue/50 transition-all hover:-translate-y-2 cursor-pointer"
        >
          <div class="aspect-video relative overflow-hidden">
            <img :src="game.cover || game.hero" class="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110" />
            <div class="absolute inset-0 bg-black/40 group-hover:bg-transparent transition-colors"></div>
            <!-- 收费标识 -->
            <div v-if="game.isPaid === 1" class="absolute top-3 right-3 px-2 py-1 bg-yellow-500/90 rounded text-xs font-bold text-slate-900">
              ¥{{ formatPrice(game.priceCents) }}
            </div>
            <div v-else class="absolute top-3 right-3 px-2 py-1 bg-green-500/90 rounded text-xs font-bold text-white">
              免费
            </div>
          </div>
          
          <div class="p-6">
            <div class="flex justify-between items-start mb-2">
              <h3 class="text-xl font-bold text-white group-hover:text-neon-blue transition-colors">{{ game.title }}</h3>
              <span class="text-sm font-bold text-yellow-400">{{ game.rating }}</span>
            </div>
            <div class="flex items-center gap-3 text-xs text-gray-400 mb-3">
              <span class="bg-white/5 px-2 py-1 rounded">{{ game.genre }}</span>
              <span v-if="game.developer">{{ game.developer }}</span>
            </div>
            <div class="flex flex-wrap gap-2">
              <span v-for="tag in parseTags(game.tags)" :key="tag" class="text-xs text-gray-400 bg-white/5 px-2 py-1 rounded">#{{ tag }}</span>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- ==================== Game Detail Modal ==================== -->
    <transition name="modal">
      <div v-if="selectedGame" class="fixed inset-0 z-[100] flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-black/90 backdrop-blur-sm" @click="closeDetail"></div>
        
        <div class="relative w-full max-w-5xl bg-slate-900 rounded-3xl overflow-hidden shadow-2xl border border-white/10 animate__animated animate__zoomIn flex flex-col md:flex-row max-h-[90vh]">
          
          <button @click="closeDetail" class="absolute top-4 right-4 z-50 p-2 bg-black/50 text-white rounded-full hover:bg-black/80 transition-colors border border-white/10">
            <X class="w-6 h-6" />
          </button>

          <!-- Left: Hero Image -->
          <div class="w-full md:w-1/2 h-64 md:h-auto relative">
            <img :src="selectedGame.hero || selectedGame.cover" class="w-full h-full object-cover" />
            <div class="absolute inset-0 bg-gradient-to-t md:bg-gradient-to-r from-slate-900/90 to-transparent"></div>
          </div>

          <!-- Right: Info -->
          <div class="w-full md:w-1/2 p-8 md:p-12 overflow-y-auto custom-scrollbar bg-slate-900">
            <div class="flex items-center gap-3 mb-4">
              <span class="px-3 py-1 bg-neon-blue/10 text-neon-blue border border-neon-blue/20 rounded text-xs font-bold">{{ selectedGame.genre }}</span>
              <div class="flex items-center gap-1 text-yellow-400 font-bold text-sm">
                <Star class="w-4 h-4 fill-yellow-400" /> {{ selectedGame.rating }} / 10
              </div>
            </div>

            <h2 class="text-4xl font-black text-white mb-6 leading-tight">{{ selectedGame.title }}</h2>
            
            <div class="grid grid-cols-2 gap-4 mb-8 text-sm text-gray-400">
              <div class="flex items-center gap-2">
                <User class="w-4 h-4" />
                <span>{{ selectedGame.developer || '未知开发者' }}</span>
              </div>
              <div class="flex items-center gap-2">
                <Calendar class="w-4 h-4" />
                <span>{{ selectedGame.releaseDate || '未知' }}</span>
              </div>
            </div>

            <p class="text-gray-300 leading-relaxed mb-8">{{ selectedGame.description || '暂无介绍' }}</p>

            <!-- 收费信息 -->
            <div v-if="selectedGame.isPaid === 1" class="mb-6 p-4 rounded-xl bg-yellow-500/10 border border-yellow-500/20">
              <div class="flex items-center gap-2 mb-2">
                <Lock class="w-4 h-4 text-yellow-400" />
                <span class="text-yellow-400 font-bold">付费游戏</span>
              </div>
              <p class="text-sm text-gray-400">
                价格：<span class="text-yellow-400 font-bold text-lg">¥{{ formatPrice(selectedGame.priceCents) }}</span>
                <span v-if="selectedGame.trialMinutes" class="ml-3">| 试玩时间：{{ selectedGame.trialMinutes }} 分钟</span>
              </p>
              <p v-if="selectedGame.isPurchased" class="text-green-400 text-sm mt-1 flex items-center gap-1">
                <CheckCircle2 class="w-4 h-4" /> 您已购买此游戏
              </p>
            </div>
            <div v-else class="mb-6 p-4 rounded-xl bg-green-500/10 border border-green-500/20">
              <div class="flex items-center gap-2">
                <Unlock class="w-4 h-4 text-green-400" />
                <span class="text-green-400 font-bold">免费游戏</span>
              </div>
            </div>

            <div class="mb-8">
              <h4 class="text-sm font-bold text-white mb-3">游戏标签</h4>
              <div class="flex flex-wrap gap-2">
                <span v-for="tag in parseTags(selectedGame.tags)" :key="tag" class="px-3 py-1 bg-white/5 rounded-full text-xs text-gray-300 border border-white/5"># {{ tag }}</span>
              </div>
            </div>

            <div class="flex gap-4 mt-auto pt-8 border-t border-white/5">
              <template v-if="selectedGame.isPaid !== 1 || selectedGame.isPurchased">
                <button
                  @click="handleStartGame(selectedGame)"
                  class="flex-1 py-3 bg-neon-blue text-slate-900 font-bold rounded-xl hover:shadow-[0_0_20px_rgba(0,243,255,0.4)] transition-all flex items-center justify-center gap-2"
                >
                  <Gamepad2 class="w-5 h-5" /> 开始游戏
                </button>
              </template>
              <template v-else>
                <button
                  v-if="selectedGame.trialMinutes"
                  @click="handleTrial(selectedGame)"
                  class="flex-1 py-3 bg-slate-700 text-white font-bold rounded-xl hover:bg-slate-600 transition-all flex items-center justify-center gap-2 border border-white/10"
                >
                  <Clock class="w-5 h-5" /> 试玩 {{ selectedGame.trialMinutes }}分钟
                </button>
                <button
                  @click="handleStartGame(selectedGame)"
                  class="flex-1 py-3 bg-yellow-500 text-slate-900 font-bold rounded-xl hover:shadow-[0_0_20px_rgba(234,179,8,0.4)] transition-all flex items-center justify-center gap-2"
                >
                  <Lock class="w-4 h-4" /> 购买 ¥{{ formatPrice(selectedGame.priceCents) }}
                </button>
              </template>
              <button
                @click="handleFavorite(selectedGame)"
                :class="[
                  'flex-1 py-3 font-bold rounded-xl transition-all border flex items-center justify-center gap-2',
                  selectedGame.isFavorited
                    ? 'bg-pink-500/20 text-pink-400 border-pink-500/30'
                    : 'bg-slate-800 text-white border-white/10 hover:bg-slate-700'
                ]"
              >
                <Heart :class="['w-5 h-5', selectedGame.isFavorited ? 'fill-pink-400' : '']" />
                {{ selectedGame.isFavorited ? '已收藏' : '加入收藏' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </transition>

    <!-- ==================== Publish/Edit Modal ==================== -->
    <transition name="modal">
      <div v-if="showPublishModal" class="fixed inset-0 z-[100] flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-black/90 backdrop-blur-sm" @click="showPublishModal = false"></div>
        
        <div class="relative w-full max-w-3xl bg-slate-900 rounded-3xl overflow-hidden shadow-2xl border border-white/10 max-h-[90vh] overflow-y-auto custom-scrollbar">
          <button @click="showPublishModal = false" class="absolute top-4 right-4 z-50 p-2 bg-black/50 text-white rounded-full hover:bg-black/80 transition-colors border border-white/10">
            <X class="w-6 h-6" />
          </button>

          <div class="p-8 md:p-12">
            <h2 class="text-3xl font-black text-white mb-8">{{ editingGame ? '编辑游戏' : '发布游戏' }}</h2>

            <div class="space-y-6">
              <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                  <label class="block text-sm font-bold text-gray-300 mb-2">游戏名称 <span class="text-red-400">*</span></label>
                  <input v-model="publishForm.title" type="text" placeholder="输入游戏名称"
                    class="w-full px-4 py-3 bg-slate-800 border border-white/10 rounded-xl text-white focus:outline-none focus:border-neon-blue/50" />
                </div>
                <div>
                  <label class="block text-sm font-bold text-gray-300 mb-2">游戏类型 <span class="text-red-400">*</span></label>
                  <select v-model="publishForm.genre"
                    class="w-full px-4 py-3 bg-slate-800 border border-white/10 rounded-xl text-white focus:outline-none focus:border-neon-blue/50">
                    <option value="">选择类型</option>
                    <option v-for="g in genres.filter(i => i.value !== 'all')" :key="g.value" :value="g.value">{{ g.label }}</option>
                  </select>
                </div>
              </div>

              <div>
                <label class="block text-sm font-bold text-gray-300 mb-2">游戏介绍 <span class="text-red-400">*</span></label>
                <textarea v-model="publishForm.description" rows="4" placeholder="输入游戏描述..."
                  class="w-full px-4 py-3 bg-slate-800 border border-white/10 rounded-xl text-white focus:outline-none focus:border-neon-blue/50 resize-none"></textarea>
              </div>

              <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                  <label class="block text-sm font-bold text-gray-300 mb-2">开发者/作者 <span class="text-red-400">*</span></label>
                  <input v-model="publishForm.developer" type="text" placeholder="开发者名称"
                    class="w-full px-4 py-3 bg-slate-800 border border-white/10 rounded-xl text-white focus:outline-none focus:border-neon-blue/50" />
                </div>
                <div>
                  <label class="block text-sm font-bold text-gray-300 mb-2">发布日期 <span class="text-red-400">*</span></label>
                  <input v-model="publishForm.releaseDate" type="date"
                    class="w-full px-4 py-3 bg-slate-800 border border-white/10 rounded-xl text-white focus:outline-none focus:border-neon-blue/50" />
                </div>
              </div>

              <div>
                <label class="block text-sm font-bold text-gray-300 mb-2">游戏链接 <span class="text-red-400">*</span></label>
                <input v-model="publishForm.gameUrl" type="url" placeholder="https://game.example.com"
                  class="w-full px-4 py-3 bg-slate-800 border border-white/10 rounded-xl text-white focus:outline-none focus:border-neon-blue/50" />
              </div>

              <div>
                <label class="block text-sm font-bold text-gray-300 mb-2">标签（用逗号分隔） <span class="text-red-400">*</span></label>
                <input v-model="publishForm.tags" type="text" placeholder="RPG, 开放世界, 3A"
                  class="w-full px-4 py-3 bg-slate-800 border border-white/10 rounded-xl text-white focus:outline-none focus:border-neon-blue/50" />
              </div>

              <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                  <label class="block text-sm font-bold text-gray-300 mb-2">封面图 <span class="text-red-400">*</span></label>
                  <div class="relative">
                    <input v-model="publishForm.coverUrl" type="text" placeholder="封面图URL（或上传）"
                      class="w-full px-4 py-3 pr-20 bg-slate-800 border border-white/10 rounded-xl text-white focus:outline-none focus:border-neon-blue/50 text-sm" />
                    <label class="absolute right-2 top-1/2 -translate-y-1/2 px-3 py-1 bg-neon-blue/20 text-neon-blue rounded-lg cursor-pointer text-xs font-bold hover:bg-neon-blue/30">
                      上传
                      <input type="file" accept="image/*" class="hidden" @change="(e) => handleImageUpload(e, 'coverUrl')" />
                    </label>
                  </div>
                  <img v-if="publishForm.coverUrl" :src="publishForm.coverUrl" class="mt-2 h-24 rounded-lg object-cover" />
                </div>
                <div>
                  <label class="block text-sm font-bold text-gray-300 mb-2">横版大图 <span class="text-red-400">*</span></label>
                  <div class="relative">
                    <input v-model="publishForm.heroUrl" type="text" placeholder="大图URL（或上传）"
                      class="w-full px-4 py-3 pr-20 bg-slate-800 border border-white/10 rounded-xl text-white focus:outline-none focus:border-neon-blue/50 text-sm" />
                    <label class="absolute right-2 top-1/2 -translate-y-1/2 px-3 py-1 bg-neon-blue/20 text-neon-blue rounded-lg cursor-pointer text-xs font-bold hover:bg-neon-blue/30">
                      上传
                      <input type="file" accept="image/*" class="hidden" @change="(e) => handleImageUpload(e, 'heroUrl')" />
                    </label>
                  </div>
                  <img v-if="publishForm.heroUrl" :src="publishForm.heroUrl" class="mt-2 h-24 rounded-lg object-cover" />
                </div>
              </div>

              <div class="p-6 rounded-xl bg-slate-800/50 border border-white/5">
                <h3 class="text-lg font-bold text-white mb-4">收费设置</h3>
                <div class="flex items-center gap-4 mb-4">
                  <label class="flex items-center gap-2 cursor-pointer">
                    <input type="radio" :value="0" v-model="publishForm.isPaid" class="accent-neon-blue" />
                    <span class="text-gray-300">免费</span>
                  </label>
                  <label class="flex items-center gap-2 cursor-pointer">
                    <input type="radio" :value="1" v-model="publishForm.isPaid" class="accent-neon-blue" />
                    <span class="text-gray-300">收费</span>
                  </label>
                </div>
                <div v-if="publishForm.isPaid === 1" class="grid grid-cols-2 gap-4">
                  <div>
                    <label class="block text-sm text-gray-400 mb-1">价格（元）</label>
                    <input type="number" :value="(publishForm.priceCents || 0) / 100" @input="publishForm.priceCents = Math.round(parseFloat(($event.target as HTMLInputElement).value || '0') * 100)" min="0" step="0.01"
                      class="w-full px-4 py-2 bg-slate-700 border border-white/10 rounded-lg text-white focus:outline-none focus:border-neon-blue/50" />
                  </div>
                  <div>
                    <label class="block text-sm text-gray-400 mb-1">试玩时长（分钟，0=不限）</label>
                    <input type="number" v-model.number="publishForm.trialMinutes" min="0"
                      class="w-full px-4 py-2 bg-slate-700 border border-white/10 rounded-lg text-white focus:outline-none focus:border-neon-blue/50" />
                  </div>
                </div>
              </div>

              <div class="flex gap-4 pt-4">
                <button
                  @click="handleSubmitPublish"
                  :disabled="publishLoading"
                  class="flex-1 py-3 bg-neon-blue text-slate-900 font-bold rounded-xl hover:shadow-[0_0_20px_rgba(0,243,255,0.4)] transition-all disabled:opacity-50"
                >
                  {{ publishLoading ? '提交中...' : (editingGame ? '保存修改' : '发布游戏') }}
                </button>
                <button
                  @click="showPublishModal = false"
                  class="px-8 py-3 bg-slate-800 text-white font-bold rounded-xl hover:bg-slate-700 transition-all border border-white/10"
                >
                  取消
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </transition>

    <!-- ==================== Game Payment Modal ==================== -->
    <transition name="modal">
      <div v-if="showPayModal" class="fixed inset-0 z-[110] flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-black/90 backdrop-blur-sm" @click="closePayModal"></div>
        
        <div class="relative w-full max-w-md bg-slate-900 rounded-3xl overflow-hidden shadow-2xl border border-white/10">
          <button @click="closePayModal" class="absolute top-4 right-4 z-50 p-2 bg-black/50 text-white rounded-full hover:bg-black/80 transition-colors border border-white/10">
            <X class="w-6 h-6" />
          </button>

          <div class="p-8">
            <!-- Step 1: 选择支付方式 -->
            <template v-if="payStep === 'select'">
              <h3 class="text-2xl font-black text-white mb-6">购买游戏</h3>
              <p class="text-gray-400 mb-6">选择支付方式完成购买</p>

              <div class="flex gap-3 mb-8">
                <button
                  v-for="m in (['WECHAT', 'ALIPAY'] as const)" :key="m"
                  @click="payMethod = m"
                  :class="[
                    'flex-1 py-4 rounded-xl border-2 transition-all flex flex-col items-center gap-2',
                    payMethod === m
                      ? 'border-white/30 ' + methodConfig[m].bg + '/20'
                      : 'border-white/5 bg-slate-800 hover:border-white/10'
                  ]"
                >
                  <span class="text-2xl">{{ methodConfig[m].emoji }}</span>
                  <span class="text-sm font-bold text-white">{{ methodConfig[m].label }}</span>
                </button>
              </div>

              <button
                @click="handleCreatePurchase"
                :disabled="payCreating"
                :class="[
                  'w-full py-4 font-bold rounded-xl text-white transition-all',
                  payMethod === 'WECHAT' ? 'bg-[#07C160] hover:bg-[#06AD56]' : 'bg-[#1677FF] hover:bg-[#1266DB]',
                  payCreating ? 'opacity-50 cursor-not-allowed' : ''
                ]"
              >
                {{ payCreating ? '创建订单中...' : '立即支付' }}
              </button>
            </template>

            <!-- Step 2: QR 扫码 -->
            <template v-else-if="payStep === 'qr'">
              <div class="text-center">
                <h3 class="text-xl font-bold text-white mb-2">扫码支付</h3>
                <p class="text-gray-400 text-sm mb-1">{{ payOrder?.gameTitle }}</p>
                <p class="text-2xl font-black text-yellow-400 mb-4">¥{{ payOrder ? (payOrder.amountCents / 100).toFixed(2) : '0.00' }}</p>

                <div class="bg-white p-4 rounded-2xl inline-block mb-4">
                  <img v-if="payOrder?.qrCode" :src="payOrder.qrCode" class="w-48 h-48" />
                </div>

                <div class="flex items-center justify-center gap-2 text-gray-400 mb-4">
                  <Clock class="w-4 h-4" />
                  <span class="text-sm">支付剩余时间：<span class="text-white font-bold">{{ payCountdownText }}</span></span>
                </div>

                <div class="flex items-center justify-center gap-2 text-sm text-gray-500">
                  <Smartphone class="w-4 h-4" />
                  <span>请使用{{ payMethod === 'WECHAT' ? '微信' : '支付宝' }}扫描二维码</span>
                </div>

                <button
                  v-if="payOrder?.mockMode"
                  @click="handleMockConfirm"
                  :disabled="payMockConfirming"
                  class="mt-6 w-full py-3 bg-slate-800 text-neon-blue font-bold rounded-xl hover:bg-slate-700 transition-all border border-neon-blue/20 text-sm"
                >
                  {{ payMockConfirming ? '确认中...' : '🧪 模拟扫码支付成功（开发模式）' }}
                </button>
              </div>
            </template>

            <!-- Step 3: 成功 -->
            <template v-else-if="payStep === 'success'">
              <div class="text-center py-8">
                <CheckCircle2 class="w-20 h-20 text-green-400 mx-auto mb-6" />
                <h3 class="text-2xl font-black text-white mb-2">购买成功！</h3>
                <p class="text-gray-400 mb-8">游戏已解锁，快去体验吧</p>
                <button
                  @click="closePayModal"
                  class="w-full py-3 bg-neon-blue text-slate-900 font-bold rounded-xl hover:shadow-[0_0_20px_rgba(0,243,255,0.4)] transition-all"
                >
                  开始游戏
                </button>
              </div>
            </template>

            <!-- Step 4: 失败/超时 -->
            <template v-else-if="payStep === 'failed'">
              <div class="text-center py-8">
                <AlertCircle class="w-20 h-20 text-red-400 mx-auto mb-6" />
                <h3 class="text-2xl font-black text-white mb-2">支付超时</h3>
                <p class="text-gray-400 mb-8">订单已过期，请重新发起支付</p>
                <button
                  @click="payStep = 'select'; payOrder = null"
                  class="w-full py-3 bg-slate-800 text-white font-bold rounded-xl hover:bg-slate-700 transition-all border border-white/10"
                >
                  重新支付
                </button>
              </div>
            </template>
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
</style>

<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import GlitchText from '@/components/ui/GlitchText.vue'
import { Calendar, Tv, PlayCircle, Bell, BellOff, X, Star, Plus, Edit3, Trash2, Lock, CreditCard, Link2 } from 'lucide-vue-next'
import { useToastStore } from '@/stores/toast'
import { useAuthStore } from '@/stores/auth'
import {
  getAnimeSchedule, getAnimeDetail, createAnime, updateAnime, deleteAnime,
  subscribeAnime, unsubscribeAnime,
  createAnimePayOrder, queryAnimePayStatus, mockConfirmAnimePay,
  type AnimeItem, type CreateAnimeForm
} from '@/api/anime'

const toast = useToastStore()
const auth = useAuthStore()

const weekDays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
const statusOptions = [
  { value: 'ONGOING', label: '更新中' },
  { value: 'COMPLETED', label: '已完结' },
  { value: 'UPCOMING', label: '即将上映' },
  { value: 'AIRING', label: '长篇连载' }
]
const statusMap: Record<string, string> = {
  ONGOING: '更新中', COMPLETED: '已完结', UPCOMING: '即将上映', AIRING: '长篇连载'
}

const today = new Date().getDay()
const activeDay = ref((today + 6) % 7)

const animeList = ref<AnimeItem[]>([])
const loading = ref(false)
const selectedAnime = ref<AnimeItem | null>(null)
const detailLoading = ref(false)

// 发布/编辑弹窗
const showPublishModal = ref(false)
const editMode = ref(false)
const editingAnimeId = ref<number | null>(null)
const publishForm = ref<CreateAnimeForm>({
  title: '',
  description: '',
  coverUrl: '',
  heroUrl: '',
  score: 8.0,
  scheduleDay: 0,
  updateTime: '23:00',
  currentEpisode: '第 1 话',
  status: 'ONGOING',
  isPaid: false,
  freeEpisodes: 3,
  priceCents: 0,
  totalEpisodes: 12,
  linkUrl: ''
})
const publishing = ref(false)

// 支付弹窗
const showPayModal = ref(false)
const payStep = ref<'select' | 'qr' | 'success' | 'failed'>('select')
const payMethod = ref<'ALIPAY' | 'WECHAT'>('ALIPAY')
const payOrderNo = ref('')
const payQrCode = ref('')
const payAmount = ref(0)
const payMockMode = ref(true)
const payExpiredAt = ref('')
const payAnimeTitle = ref('')
const payPollingTimer = ref<number | null>(null)
const payCountdown = ref(300)
const payCountdownTimer = ref<number | null>(null)
const payingAnimeId = ref<number | null>(null)

const danmaku = [
  '前方高能！！！', '泪目了 T_T', '这个作画绝了', '燃烧经费啊', '终于等到更新了',
  'AWSL', '这波配合无敌', 'bgm好评', '颜艺满分', '在这里养生？', '这也太帅了吧'
]

// ── 加载番剧列表 ──────────────────────────────────────────────────────────────
async function loadSchedule() {
  loading.value = true
  try {
    const res = await getAnimeSchedule(activeDay.value)
    if (res.code === 0) {
      animeList.value = res.data || []
    } else {
      toast.error(res.message || '加载失败')
    }
  } catch (e: any) {
    toast.error('加载番剧失败')
  } finally {
    loading.value = false
  }
}

watch(activeDay, () => loadSchedule())
onMounted(() => loadSchedule())

// ── 打开详情 ──────────────────────────────────────────────────────────────────
async function openDetail(anime: AnimeItem) {
  selectedAnime.value = anime
  detailLoading.value = true
  try {
    const res = await getAnimeDetail(anime.id)
    if (res.code === 0) {
      selectedAnime.value = res.data
    }
  } catch { /* 使用列表数据 */ }
  finally { detailLoading.value = false }
}

function closeDetail() { selectedAnime.value = null }

// ── 追番 / 取消追番 ──────────────────────────────────────────────────────────
async function toggleSubscribe(anime: AnimeItem, e?: Event) {
  e?.stopPropagation()
  if (!auth.isAuthenticated) { toast.warning('请先登录'); return }

  try {
    if (anime.subscribed) {
      const res = await unsubscribeAnime(anime.id)
      if (res.code === 0) {
        anime.subscribed = false
        anime.subscribeCount = Math.max(0, (anime.subscribeCount || 0) - 1)
        if (selectedAnime.value?.id === anime.id) {
          selectedAnime.value.subscribed = false
          selectedAnime.value.subscribeCount = anime.subscribeCount
        }
        toast.success('已取消追番')
      }
    } else {
      const res = await subscribeAnime(anime.id)
      if (res.code === 0) {
        anime.subscribed = true
        anime.subscribeCount = (anime.subscribeCount || 0) + 1
        if (selectedAnime.value?.id === anime.id) {
          selectedAnime.value.subscribed = true
          selectedAnime.value.subscribeCount = anime.subscribeCount
        }
        toast.success('追番成功！')
      }
    }
  } catch (err: any) {
    toast.error(err?.response?.data?.message || '操作失败')
  }
}

// ── 发布弹窗 ──────────────────────────────────────────────────────────────────
function openPublishModal() {
  if (!auth.isAuthenticated) { toast.warning('请先登录'); return }
  editMode.value = false
  editingAnimeId.value = null
  publishForm.value = {
    title: '', description: '', coverUrl: '', heroUrl: '',
    score: 8.0, scheduleDay: activeDay.value, updateTime: '23:00',
    currentEpisode: '第 1 话', status: 'ONGOING',
    isPaid: false, freeEpisodes: 3, priceCents: 0, totalEpisodes: 12,
    linkUrl: ''
  }
  showPublishModal.value = true
}

function openEditModal(anime: AnimeItem, e?: Event) {
  e?.stopPropagation()
  if (!auth.isAuthenticated) { toast.warning('请先登录'); return }
  editMode.value = true
  editingAnimeId.value = anime.id
  publishForm.value = {
    title: anime.title,
    description: anime.description || '',
    coverUrl: anime.coverUrl || '',
    heroUrl: anime.heroUrl || '',
    score: anime.score,
    scheduleDay: anime.scheduleDay,
    updateTime: anime.updateTime || '23:00',
    currentEpisode: anime.currentEpisode || '',
    status: anime.status,
    isPaid: anime.isPaid,
    freeEpisodes: anime.freeEpisodes || 3,
    priceCents: anime.priceCents || 0,
    totalEpisodes: anime.totalEpisodes || 0,
    linkUrl: anime.linkUrl || ''
  }
  showPublishModal.value = true
}

async function submitPublish() {
  const f = publishForm.value
  if (!f.title.trim()) { toast.warning('请填写番剧名称'); return }
  if (!f.description.trim()) { toast.warning('请填写番剧介绍'); return }
  if (!f.coverUrl.trim()) { toast.warning('请填写封面图 URL'); return }
  if (!f.heroUrl.trim()) { toast.warning('请填写详情大图 URL'); return }
  if (f.score == null || f.score < 0 || f.score > 10) { toast.warning('请填写正确的评分 (0-10)'); return }
  if (!f.updateTime.trim()) { toast.warning('请填写更新时间'); return }
  if (!f.currentEpisode.trim()) { toast.warning('请填写当前进度'); return }
  if (!f.status) { toast.warning('请选择状态'); return }
  if (f.totalEpisodes == null || f.totalEpisodes < 0) { toast.warning('请填写正确的总集数'); return }
  if (!f.linkUrl.trim()) { toast.warning('请填写番剧链接 URL'); return }
  if (f.isPaid && (!f.priceCents || f.priceCents <= 0)) { toast.warning('付费番剧请填写正确的价格'); return }
  publishing.value = true
  try {
    if (editMode.value && editingAnimeId.value) {
      const res = await updateAnime(editingAnimeId.value, publishForm.value)
      if (res.code === 0) {
        toast.success('番剧更新成功')
        showPublishModal.value = false
        loadSchedule()
      } else {
        toast.error(res.message || '更新失败')
      }
    } else {
      const res = await createAnime(publishForm.value)
      if (res.code === 0) {
        toast.success('番剧发布成功')
        showPublishModal.value = false
        loadSchedule()
      } else {
        toast.error(res.message || '发布失败')
      }
    }
  } catch (err: any) {
    toast.error(err?.response?.data?.message || '操作失败')
  } finally {
    publishing.value = false
  }
}

// ── 删除番剧 ──────────────────────────────────────────────────────────────────
async function handleDelete(anime: AnimeItem, e?: Event) {
  e?.stopPropagation()
  if (!confirm(`确定删除「${anime.title}」吗？`)) return
  try {
    const res = await deleteAnime(anime.id)
    if (res.code === 0) {
      toast.success('已删除')
      if (selectedAnime.value?.id === anime.id) selectedAnime.value = null
      loadSchedule()
    } else {
      toast.error(res.message || '删除失败')
    }
  } catch (err: any) {
    toast.error(err?.response?.data?.message || '删除失败')
  }
}

function isAnimeOwner(anime: AnimeItem) {
  return auth.user?.id === anime.userId
}

// ── 付费观看 ──────────────────────────────────────────────────────────────────
function openPayModal(anime: AnimeItem) {
  if (!auth.isAuthenticated) { toast.warning('请先登录'); return }
  if (anime.purchased) { toast.info('已购买，可直接观看'); return }
  payingAnimeId.value = anime.id
  payAnimeTitle.value = anime.title
  payAmount.value = anime.priceCents / 100
  payStep.value = 'select'
  payMethod.value = 'ALIPAY'
  showPayModal.value = true
}

async function startPay() {
  if (!payingAnimeId.value) return
  try {
    const res = await createAnimePayOrder(payingAnimeId.value, payMethod.value)
    if (res.code === 0) {
      const data = res.data
      payOrderNo.value = data.orderNo
      payQrCode.value = data.qrCode
      payAmount.value = data.amount
      payMockMode.value = data.mockMode
      payExpiredAt.value = data.expiredAt
      payStep.value = 'qr'
      payCountdown.value = 300
      startPolling()
      startCountdown()
    } else {
      toast.error(res.message || '创建订单失败')
    }
  } catch (err: any) {
    toast.error(err?.response?.data?.message || '创建订单失败')
  }
}

function startPolling() {
  stopPolling()
  payPollingTimer.value = window.setInterval(async () => {
    try {
      const res = await queryAnimePayStatus(payOrderNo.value)
      if (res.code === 0) {
        if (res.data.status === 'PAID') {
          onPaySuccess()
        } else if (res.data.status === 'EXPIRED' || res.data.status === 'FAILED') {
          payStep.value = 'failed'
          stopPolling()
          stopCountdown()
        }
      }
    } catch { /* ignore */ }
  }, 2000)
}

function stopPolling() {
  if (payPollingTimer.value) {
    clearInterval(payPollingTimer.value)
    payPollingTimer.value = null
  }
}

function startCountdown() {
  stopCountdown()
  payCountdownTimer.value = window.setInterval(() => {
    payCountdown.value--
    if (payCountdown.value <= 0) {
      payStep.value = 'failed'
      stopCountdown()
      stopPolling()
    }
  }, 1000)
}

function stopCountdown() {
  if (payCountdownTimer.value) {
    clearInterval(payCountdownTimer.value)
    payCountdownTimer.value = null
  }
}

async function mockPay() {
  try {
    const res = await mockConfirmAnimePay(payOrderNo.value)
    if (res.code === 0) {
      onPaySuccess()
    }
  } catch (err: any) {
    toast.error(err?.response?.data?.message || '模拟支付失败')
  }
}

function onPaySuccess() {
  payStep.value = 'success'
  stopPolling()
  stopCountdown()
  toast.success('支付成功！已解锁番剧')
  const target = animeList.value.find(a => a.id === payingAnimeId.value)
  if (target) target.purchased = true
  if (selectedAnime.value?.id === payingAnimeId.value) {
    selectedAnime.value!.purchased = true
  }
}

function closePayModal() {
  showPayModal.value = false
  stopPolling()
  stopCountdown()
}

const countdownFormatted = computed(() => {
  const m = Math.floor(payCountdown.value / 60)
  const s = payCountdown.value % 60
  return `${m}:${s.toString().padStart(2, '0')}`
})
</script>

<template>
  <div class="container mx-auto px-4 py-20 min-h-screen relative">
    <!-- Header -->
    <div class="text-center mb-12 relative z-10">
      <h1 class="text-5xl md:text-7xl font-black mb-4"><GlitchText text="ANIME ON-AIR" /></h1>
      <p class="text-pink-400 font-bold tracking-widest">新番导视 · 实时追番</p>
    </div>

    <!-- Publish Button -->
    <div class="flex justify-center mb-4 relative z-30" v-if="auth.isAuthenticated">
      <button
        @click="openPublishModal"
        class="px-6 py-3 bg-gradient-to-r from-pink-500 to-purple-600 text-white rounded-2xl font-bold shadow-lg hover:shadow-pink-500/30 transition-all flex items-center gap-2"
      >
        <Plus class="w-5 h-5" /> 发布番剧
      </button>
    </div>

    <!-- Week Timeline -->
    <div class="flex justify-center mb-16 sticky top-20 z-30">
      <div class="flex bg-slate-900/80 backdrop-blur-lg border border-white/10 p-1 rounded-2xl shadow-2xl overflow-x-auto max-w-full">
        <button
          v-for="(day, index) in weekDays"
          :key="index"
          @click="activeDay = index"
          class="px-6 py-3 rounded-xl font-bold transition-all whitespace-nowrap text-sm md:text-base relative overflow-hidden"
          :class="activeDay === index ? 'text-white shadow-lg' : 'text-gray-500 hover:text-gray-300'"
        >
          <div v-if="activeDay === index" class="absolute inset-0 bg-gradient-to-r from-pink-500 to-purple-600 -z-10"></div>
          {{ day }}
        </button>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="text-center py-20 text-gray-500">
      <div class="inline-block w-8 h-8 border-4 border-pink-500 border-t-transparent rounded-full animate-spin mb-4"></div>
      <p>加载中...</p>
    </div>

    <!-- Anime List -->
    <div v-else class="grid grid-cols-1 gap-8 max-w-5xl mx-auto relative z-10">
      <transition-group name="list">
        <div
          v-for="(anime, index) in animeList"
          :key="anime.id"
          @click="openDetail(anime)"
          class="group relative bg-slate-800 rounded-3xl overflow-hidden border border-white/5 hover:border-pink-500/50 transition-all duration-500 hover:-translate-y-1 shadow-xl cursor-pointer"
        >
          <div class="absolute inset-0 opacity-0 group-hover:opacity-20 transition-opacity duration-700 pointer-events-none">
            <img :src="anime.coverUrl" class="w-full h-full object-cover blur-2xl" />
          </div>

          <div class="flex flex-col md:flex-row">
            <div class="md:w-56 h-64 md:h-auto relative overflow-hidden shrink-0">
              <img :src="anime.coverUrl" class="w-full h-full object-cover group-hover:scale-110 transition-transform duration-700" />
              <div class="absolute top-0 left-0 px-3 py-1 bg-black/60 backdrop-blur text-xs font-bold text-white rounded-br-lg border-b border-r border-white/10">
                {{ anime.updateTime }} 更新
              </div>
              <div v-if="anime.isPaid && !anime.purchased" class="absolute top-0 right-0 px-2 py-1 bg-yellow-500/90 text-xs font-bold text-black rounded-bl-lg">
                <Lock class="w-3 h-3 inline -mt-0.5" /> 付费
              </div>
            </div>

            <div class="p-6 md:p-8 flex-1 flex flex-col justify-between relative overflow-hidden">
              <div class="absolute top-4 right-0 w-full opacity-10 pointer-events-none select-none overflow-hidden">
                <div class="whitespace-nowrap animate-marquee text-white text-sm font-mono">{{ danmaku[index % danmaku.length] }}   {{ danmaku[(index + 3) % danmaku.length] }}</div>
              </div>

              <div>
                <div class="flex justify-between items-start mb-2">
                  <h3 class="text-2xl font-bold text-white group-hover:text-pink-400 transition-colors line-clamp-1">{{ anime.title }}</h3>
                  <span
                    class="px-3 py-1 rounded-full text-xs font-bold border shrink-0"
                    :class="anime.status === 'ONGOING' || anime.status === 'AIRING' ? 'bg-green-500/10 text-green-400 border-green-500/30' : 'bg-gray-700/50 text-gray-400 border-gray-600'"
                  >
                    {{ statusMap[anime.status] || anime.status }}
                  </span>
                </div>
                <p class="text-gray-400 flex items-center gap-2 mt-2">
                  <Tv class="w-4 h-4 text-pink-500" />
                  当前进度: <span class="text-white font-bold">{{ anime.currentEpisode || '未知' }}</span>
                  <span v-if="anime.authorName" class="text-gray-500 text-sm ml-4">作者: {{ anime.authorName }}</span>
                </p>
                <p class="text-gray-500 text-sm mt-4 line-clamp-2 pr-8">{{ anime.description }}</p>
              </div>

              <div class="mt-8 flex gap-4">
                <button
                  @click.stop="anime.isPaid && !anime.purchased ? openPayModal(anime) : toggleSubscribe(anime, $event)"
                  class="flex-1 bg-gradient-to-r from-pink-500 to-purple-600 text-white py-3 rounded-xl font-bold shadow-lg hover:shadow-pink-500/30 transition-all flex items-center justify-center gap-2 group/btn"
                >
                  <template v-if="anime.isPaid && !anime.purchased">
                    <CreditCard class="w-5 h-5" /> ¥{{ (anime.priceCents / 100).toFixed(2) }} 购买观看
                  </template>
                  <template v-else>
                    <PlayCircle class="w-5 h-5 group-hover/btn:scale-110 transition-transform" /> 立即追番
                  </template>
                </button>
                <button
                  @click.stop="toggleSubscribe(anime, $event)"
                  class="px-4 bg-slate-700/50 border border-white/10 hover:bg-white/10 text-white rounded-xl transition-colors"
                  :class="{ 'text-pink-400 border-pink-500/30': anime.subscribed }"
                >
                  <BellOff v-if="anime.subscribed" class="w-5 h-5" />
                  <Bell v-else class="w-5 h-5" />
                </button>
                <template v-if="isAnimeOwner(anime)">
                  <button @click.stop="openEditModal(anime, $event)" class="px-3 bg-slate-700/50 border border-white/10 hover:bg-blue-500/20 text-blue-400 rounded-xl transition-colors">
                    <Edit3 class="w-4 h-4" />
                  </button>
                  <button @click.stop="handleDelete(anime, $event)" class="px-3 bg-slate-700/50 border border-white/10 hover:bg-red-500/20 text-red-400 rounded-xl transition-colors">
                    <Trash2 class="w-4 h-4" />
                  </button>
                </template>
              </div>
            </div>
          </div>
        </div>
      </transition-group>

      <div v-if="!loading && animeList.length === 0" class="text-center py-20 text-gray-500">
        <Calendar class="w-16 h-16 mx-auto mb-4 opacity-20" />
        <p>今日暂无更新，去补补旧番吧~</p>
      </div>
    </div>

    <!-- ═══════════ Anime Detail Modal ═══════════ -->
    <transition name="modal">
      <div v-if="selectedAnime" class="fixed inset-0 z-[100] flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-black/90 backdrop-blur-sm" @click="closeDetail"></div>

        <div class="relative w-full max-w-4xl bg-slate-900 rounded-3xl overflow-hidden shadow-2xl border border-white/10 animate__animated animate__zoomIn flex flex-col max-h-[90vh]">
          <button @click="closeDetail" class="absolute top-4 right-4 z-50 p-2 bg-black/50 text-white rounded-full hover:bg-black/80 transition-colors border border-white/10">
            <X class="w-6 h-6" />
          </button>

          <div class="w-full h-56 md:h-80 relative bg-black group">
            <img :src="selectedAnime.heroUrl || selectedAnime.coverUrl" class="w-full h-full object-cover object-top" />
            <div class="absolute inset-0 bg-gradient-to-t from-slate-900 via-transparent to-transparent"></div>
          </div>

          <div class="p-8 md:p-10 overflow-y-auto custom-scrollbar bg-slate-900 flex-1">
            <div class="flex items-center justify-between mb-2">
              <div class="flex items-center gap-3">
                <span class="px-3 py-1 bg-pink-500/10 text-pink-400 border border-pink-500/20 rounded text-xs font-bold">{{ selectedAnime.updateTime }} 更新</span>
                <span
                  class="px-3 py-1 rounded text-xs font-bold border"
                  :class="selectedAnime.status === 'ONGOING' || selectedAnime.status === 'AIRING' ? 'bg-green-500/10 text-green-400 border-green-500/30' : 'bg-gray-700/50 text-gray-400 border-gray-600'"
                >{{ statusMap[selectedAnime.status] }}</span>
                <span v-if="selectedAnime.isPaid && !selectedAnime.purchased" class="px-3 py-1 bg-yellow-500/10 text-yellow-400 border border-yellow-500/20 rounded text-xs font-bold">
                  <Lock class="w-3 h-3 inline -mt-0.5" /> 付费内容
                </span>
              </div>
              <div class="flex items-center gap-1 text-yellow-400 font-bold text-lg">
                <Star class="w-5 h-5 fill-yellow-400" /> {{ selectedAnime.score }}/10
              </div>
            </div>

            <h2 class="text-3xl md:text-4xl font-black text-white mb-4 leading-tight">{{ selectedAnime.title }}</h2>

            <div class="flex flex-wrap gap-4 text-sm text-gray-400 mb-6">
              <span v-if="selectedAnime.authorName">作者: <b class="text-white">{{ selectedAnime.authorName }}</b></span>
              <span v-if="selectedAnime.currentEpisode">进度: <b class="text-white">{{ selectedAnime.currentEpisode }}</b></span>
              <span>播放: <b class="text-white">{{ (selectedAnime.viewCount || 0).toLocaleString() }}</b></span>
              <span>追番: <b class="text-white">{{ (selectedAnime.subscribeCount || 0).toLocaleString() }}</b></span>
              <span>更新日: <b class="text-white">{{ weekDays[selectedAnime.scheduleDay] }}</b></span>
            </div>

            <p class="text-gray-300 leading-relaxed mb-8 text-lg">{{ selectedAnime.description }}</p>

            <div v-if="selectedAnime.linkUrl" class="mb-6">
              <a :href="selectedAnime.linkUrl" target="_blank" rel="noopener noreferrer"
                class="inline-flex items-center gap-2 px-4 py-2 bg-pink-500/10 text-pink-400 border border-pink-500/20 rounded-xl text-sm hover:bg-pink-500/20 transition-colors">
                <Link2 class="w-4 h-4" /> 番剧链接
              </a>
            </div>

            <div v-if="selectedAnime.isPaid && !selectedAnime.purchased" class="mb-6 p-4 bg-yellow-500/5 border border-yellow-500/20 rounded-xl">
              <p class="text-yellow-400 font-bold mb-1"><Lock class="w-4 h-4 inline -mt-0.5" /> 付费番剧</p>
              <p class="text-gray-400 text-sm">本番剧为付费内容，可免费试看 {{ selectedAnime.freeEpisodes }} 话。完整观看需支付 ¥{{ (selectedAnime.priceCents / 100).toFixed(2) }}。</p>
            </div>

            <div class="flex gap-4 mt-auto pt-4 border-t border-white/5">
              <template v-if="selectedAnime.isPaid && !selectedAnime.purchased">
                <button
                  @click="openPayModal(selectedAnime)"
                  class="flex-1 py-4 bg-gradient-to-r from-yellow-500 to-orange-500 text-white font-bold rounded-xl hover:shadow-[0_0_20px_rgba(234,179,8,0.4)] transition-all flex items-center justify-center gap-2 text-lg"
                >
                  <CreditCard class="w-6 h-6" /> ¥{{ (selectedAnime.priceCents / 100).toFixed(2) }} 购买观看
                </button>
                <button class="px-6 py-4 bg-gradient-to-r from-pink-500 to-purple-600 text-white font-bold rounded-xl hover:shadow-[0_0_20px_rgba(236,72,153,0.4)] transition-all flex items-center gap-2">
                  <PlayCircle class="w-5 h-5" /> 试看
                </button>
              </template>
              <template v-else>
                <button class="flex-1 py-4 bg-gradient-to-r from-pink-500 to-purple-600 text-white font-bold rounded-xl hover:shadow-[0_0_20px_rgba(236,72,153,0.4)] transition-all flex items-center justify-center gap-2 text-lg">
                  <PlayCircle class="w-6 h-6 fill-current" /> 开始观看
                </button>
              </template>
              <button
                @click="toggleSubscribe(selectedAnime!)"
                class="px-6 py-4 font-bold rounded-xl transition-all border flex items-center gap-2"
                :class="selectedAnime.subscribed
                  ? 'bg-pink-500/10 text-pink-400 border-pink-500/30 hover:bg-pink-500/20'
                  : 'bg-slate-800 text-white border-white/10 hover:bg-slate-700'"
              >
                <BellOff v-if="selectedAnime.subscribed" class="w-5 h-5" />
                <Bell v-else class="w-5 h-5" />
                {{ selectedAnime.subscribed ? '已追番' : '追番' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </transition>

    <!-- ═══════════ Publish/Edit Modal ═══════════ -->
    <transition name="modal">
      <div v-if="showPublishModal" class="fixed inset-0 z-[100] flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-black/90 backdrop-blur-sm" @click="showPublishModal = false"></div>
        <div class="relative w-full max-w-2xl bg-slate-900 rounded-3xl overflow-hidden shadow-2xl border border-white/10 flex flex-col max-h-[90vh]">
          <div class="p-6 border-b border-white/10 flex items-center justify-between">
            <h2 class="text-2xl font-bold text-white">{{ editMode ? '编辑番剧' : '发布番剧' }}</h2>
            <button @click="showPublishModal = false" class="p-2 text-gray-400 hover:text-white transition-colors"><X class="w-6 h-6" /></button>
          </div>

          <div class="p-6 overflow-y-auto flex-1 space-y-5">
            <div>
              <label class="block text-sm font-bold text-gray-300 mb-1">番剧名称 *</label>
              <input v-model="publishForm.title" type="text" class="w-full bg-slate-800 border border-white/10 rounded-xl px-4 py-3 text-white focus:border-pink-500 focus:outline-none" placeholder="输入番剧名称" />
            </div>
            <div>
              <label class="block text-sm font-bold text-gray-300 mb-1">番剧介绍 *</label>
              <textarea v-model="publishForm.description" rows="3" class="w-full bg-slate-800 border border-white/10 rounded-xl px-4 py-3 text-white focus:border-pink-500 focus:outline-none resize-none" placeholder="输入番剧简介"></textarea>
            </div>
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="block text-sm font-bold text-gray-300 mb-1">封面图 URL *</label>
                <input v-model="publishForm.coverUrl" type="text" class="w-full bg-slate-800 border border-white/10 rounded-xl px-4 py-3 text-white focus:border-pink-500 focus:outline-none text-sm" placeholder="https://..." />
              </div>
              <div>
                <label class="block text-sm font-bold text-gray-300 mb-1">详情大图 URL *</label>
                <input v-model="publishForm.heroUrl" type="text" class="w-full bg-slate-800 border border-white/10 rounded-xl px-4 py-3 text-white focus:border-pink-500 focus:outline-none text-sm" placeholder="https://..." />
              </div>
            </div>
            <div>
              <label class="block text-sm font-bold text-gray-300 mb-1">番剧链接 URL *</label>
              <input v-model="publishForm.linkUrl" type="text" class="w-full bg-slate-800 border border-white/10 rounded-xl px-4 py-3 text-white focus:border-pink-500 focus:outline-none text-sm" placeholder="https://..." />
            </div>
            <div class="grid grid-cols-3 gap-4">
              <div>
                <label class="block text-sm font-bold text-gray-300 mb-1">评分 (0-10) *</label>
                <input v-model.number="publishForm.score" type="number" min="0" max="10" step="0.1" class="w-full bg-slate-800 border border-white/10 rounded-xl px-4 py-3 text-white focus:border-pink-500 focus:outline-none" />
              </div>
              <div>
                <label class="block text-sm font-bold text-gray-300 mb-1">更新日 *</label>
                <select v-model.number="publishForm.scheduleDay" class="w-full bg-slate-800 border border-white/10 rounded-xl px-4 py-3 text-white focus:border-pink-500 focus:outline-none">
                  <option v-for="(d, i) in weekDays" :key="i" :value="i">{{ d }}</option>
                </select>
              </div>
              <div>
                <label class="block text-sm font-bold text-gray-300 mb-1">更新时间 *</label>
                <input v-model="publishForm.updateTime" type="text" class="w-full bg-slate-800 border border-white/10 rounded-xl px-4 py-3 text-white focus:border-pink-500 focus:outline-none" placeholder="23:00" />
              </div>
            </div>
            <div class="grid grid-cols-3 gap-4">
              <div>
                <label class="block text-sm font-bold text-gray-300 mb-1">当前进度 *</label>
                <input v-model="publishForm.currentEpisode" type="text" class="w-full bg-slate-800 border border-white/10 rounded-xl px-4 py-3 text-white focus:border-pink-500 focus:outline-none" placeholder="第 1 话" />
              </div>
              <div>
                <label class="block text-sm font-bold text-gray-300 mb-1">状态 *</label>
                <select v-model="publishForm.status" class="w-full bg-slate-800 border border-white/10 rounded-xl px-4 py-3 text-white focus:border-pink-500 focus:outline-none">
                  <option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                </select>
              </div>
              <div>
                <label class="block text-sm font-bold text-gray-300 mb-1">总集数 *</label>
                <input v-model.number="publishForm.totalEpisodes" type="number" min="0" class="w-full bg-slate-800 border border-white/10 rounded-xl px-4 py-3 text-white focus:border-pink-500 focus:outline-none" />
              </div>
            </div>

            <div class="border-t border-white/5 pt-5">
              <div class="flex items-center gap-3 mb-4">
                <label class="text-sm font-bold text-gray-300">付费设置</label>
                <button
                  @click="publishForm.isPaid = !publishForm.isPaid"
                  class="relative w-12 h-6 rounded-full transition-colors"
                  :class="publishForm.isPaid ? 'bg-pink-500' : 'bg-slate-700'"
                >
                  <div class="absolute top-0.5 w-5 h-5 bg-white rounded-full transition-transform" :class="publishForm.isPaid ? 'translate-x-6' : 'translate-x-0.5'"></div>
                </button>
                <span class="text-sm text-gray-400">{{ publishForm.isPaid ? '收费' : '免费' }}</span>
              </div>
              <div v-if="publishForm.isPaid" class="grid grid-cols-2 gap-4">
                <div>
                  <label class="block text-sm font-bold text-gray-300 mb-1">价格（分） *</label>
                  <input v-model.number="publishForm.priceCents" type="number" min="1" class="w-full bg-slate-800 border border-white/10 rounded-xl px-4 py-3 text-white focus:border-pink-500 focus:outline-none" />
                  <p class="text-xs text-gray-500 mt-1">即 ¥{{ ((publishForm.priceCents || 0) / 100).toFixed(2) }}</p>
                </div>
                <div>
                  <label class="block text-sm font-bold text-gray-300 mb-1">免费试看集数 *</label>
                  <input v-model.number="publishForm.freeEpisodes" type="number" min="0" class="w-full bg-slate-800 border border-white/10 rounded-xl px-4 py-3 text-white focus:border-pink-500 focus:outline-none" />
                </div>
              </div>
            </div>
          </div>

          <div class="p-6 border-t border-white/10 flex gap-4">
            <button @click="showPublishModal = false" class="flex-1 py-3 bg-slate-800 text-white font-bold rounded-xl border border-white/10 hover:bg-slate-700 transition-colors">取消</button>
            <button
              @click="submitPublish"
              :disabled="publishing"
              class="flex-1 py-3 bg-gradient-to-r from-pink-500 to-purple-600 text-white font-bold rounded-xl hover:shadow-pink-500/30 shadow-lg transition-all disabled:opacity-50"
            >{{ publishing ? '提交中...' : (editMode ? '保存修改' : '发布') }}</button>
          </div>
        </div>
      </div>
    </transition>

    <!-- ═══════════ Payment Modal ═══════════ -->
    <transition name="modal">
      <div v-if="showPayModal" class="fixed inset-0 z-[110] flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-black/90 backdrop-blur-sm" @click="closePayModal"></div>
        <div class="relative w-full max-w-lg bg-slate-900 rounded-3xl overflow-hidden shadow-2xl border border-white/10">
          <button @click="closePayModal" class="absolute top-4 right-4 z-50 p-2 text-gray-400 hover:text-white transition-colors"><X class="w-6 h-6" /></button>

          <!-- Step: Select -->
          <div v-if="payStep === 'select'" class="p-8">
            <h2 class="text-2xl font-bold text-white mb-2">购买番剧</h2>
            <p class="text-gray-400 mb-6">{{ payAnimeTitle }}</p>
            <div class="bg-slate-800 rounded-xl p-4 mb-6 flex items-center justify-between">
              <span class="text-gray-300">支付金额</span>
              <span class="text-3xl font-black text-pink-400">¥{{ payAmount.toFixed(2) }}</span>
            </div>
            <div class="space-y-3 mb-6">
              <label
                class="flex items-center gap-4 p-4 rounded-xl border cursor-pointer transition-colors"
                :class="payMethod === 'ALIPAY' ? 'border-blue-500 bg-blue-500/5' : 'border-white/10 hover:border-white/20'"
              >
                <input type="radio" v-model="payMethod" value="ALIPAY" class="hidden" />
                <div class="w-10 h-10 bg-blue-500 rounded-lg flex items-center justify-center text-white font-bold text-sm">支</div>
                <div><p class="text-white font-bold">支付宝</p><p class="text-gray-500 text-xs">Alipay</p></div>
                <div class="ml-auto w-5 h-5 rounded-full border-2 flex items-center justify-center" :class="payMethod === 'ALIPAY' ? 'border-blue-500' : 'border-gray-600'">
                  <div v-if="payMethod === 'ALIPAY'" class="w-3 h-3 bg-blue-500 rounded-full"></div>
                </div>
              </label>
              <label
                class="flex items-center gap-4 p-4 rounded-xl border cursor-pointer transition-colors"
                :class="payMethod === 'WECHAT' ? 'border-green-500 bg-green-500/5' : 'border-white/10 hover:border-white/20'"
              >
                <input type="radio" v-model="payMethod" value="WECHAT" class="hidden" />
                <div class="w-10 h-10 bg-green-500 rounded-lg flex items-center justify-center text-white font-bold text-sm">微</div>
                <div><p class="text-white font-bold">微信支付</p><p class="text-gray-500 text-xs">WeChat Pay</p></div>
                <div class="ml-auto w-5 h-5 rounded-full border-2 flex items-center justify-center" :class="payMethod === 'WECHAT' ? 'border-green-500' : 'border-gray-600'">
                  <div v-if="payMethod === 'WECHAT'" class="w-3 h-3 bg-green-500 rounded-full"></div>
                </div>
              </label>
            </div>
            <button @click="startPay" class="w-full py-4 bg-gradient-to-r from-pink-500 to-purple-600 text-white font-bold rounded-xl text-lg hover:shadow-pink-500/30 shadow-lg transition-all">确认支付</button>
          </div>

          <!-- Step: QR Code -->
          <div v-else-if="payStep === 'qr'" class="p-8 text-center">
            <h2 class="text-xl font-bold text-white mb-2">扫码支付</h2>
            <p class="text-gray-400 text-sm mb-4">{{ payAnimeTitle }}</p>
            <div class="bg-white rounded-2xl p-4 inline-block mb-4">
              <img v-if="payQrCode" :src="payQrCode" class="w-48 h-48" alt="QR" />
            </div>
            <p class="text-gray-400 text-sm mb-2">请使用{{ payMethod === 'WECHAT' ? '微信' : '支付宝' }}扫码支付 <b class="text-pink-400">¥{{ payAmount.toFixed(2) }}</b></p>
            <p class="text-gray-500 text-xs mb-4">剩余时间: {{ countdownFormatted }}</p>
            <button v-if="payMockMode" @click="mockPay" class="w-full py-3 bg-gradient-to-r from-green-500 to-emerald-600 text-white font-bold rounded-xl hover:shadow-green-500/30 shadow-lg transition-all">模拟支付（开发模式）</button>
          </div>

          <!-- Step: Success -->
          <div v-else-if="payStep === 'success'" class="p-8 text-center">
            <div class="text-6xl mb-4">🎉</div>
            <h2 class="text-2xl font-bold text-white mb-2">支付成功！</h2>
            <p class="text-gray-400 mb-6">番剧已解锁，可以开始观看了</p>
            <button @click="closePayModal" class="w-full py-3 bg-gradient-to-r from-pink-500 to-purple-600 text-white font-bold rounded-xl">开始观看</button>
          </div>

          <!-- Step: Failed -->
          <div v-else-if="payStep === 'failed'" class="p-8 text-center">
            <div class="text-6xl mb-4">😔</div>
            <h2 class="text-2xl font-bold text-white mb-2">支付失败</h2>
            <p class="text-gray-400 mb-6">订单已过期或支付失败，请重试</p>
            <button @click="payStep = 'select'" class="w-full py-3 bg-gradient-to-r from-pink-500 to-purple-600 text-white font-bold rounded-xl">重新支付</button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.animate-marquee {
  animation: marquee 15s linear infinite;
}
@keyframes marquee {
  from { transform: translateX(100%); }
  to { transform: translateX(-100%); }
}

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
  transform: translateY(30px);
}
</style>

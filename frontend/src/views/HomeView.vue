<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import GlitchText from '@/components/ui/GlitchText.vue'
import { Sparkles, Gamepad2, Tv, Zap, ArrowRight, Star, Heart, Compass, MessageSquare, Crown, Palette, Info, FileText, Headphones } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { homeApi, type TopCreator } from '@/api/home'
const router = useRouter()
const authStore = useAuthStore()

// 游客点击需要登录的功能时跳转登录页
const requireLogin = () => {
  if (!authStore.isAuthenticated) {
    router.push('/login')
    return false
  }
  return true
}

const features = [
  { icon: Sparkles, title: 'AI 创意风暴', desc: '基于 GPT-4 的灵感生成引擎，一键生成攻略、文案与脚本', link: '/ai-chat' },
  { icon: Gamepad2, title: '游戏乌托邦', desc: '汇聚全网最硬核玩家社区，覆盖 3A 大作与独立游戏', link: '/games' },
  { icon: Tv, title: '番剧放映室', desc: '4K 画质沉浸式追番体验，实时弹幕互动', link: '/anime' },
  { icon: Compass, title: '发现广场', desc: '探索社区最新热门内容，发现更多灵感创作', link: '/explore' },
  { icon: MessageSquare, title: '社区论坛', desc: '深度交流讨论区，分享创意与技术心得', link: '/forum' },
  { icon: Palette, title: '创意工坊', desc: '上传与分享你的创意作品，获得社区评价与点赞', link: '/workshop' },
  { icon: Crown, title: '会员中心', desc: '解锁专属特权，享受高级 AI 服务与尊贵体验', link: '/vip' },
  { icon: Info, title: '关于我们', desc: '了解 Inspo-Verse 的愿景与团队故事', link: '/about' },
  { icon: FileText, title: '社区规范', desc: '共建健康创意社区，请遵守我们的社区规范', link: '/rules' },
  { icon: Headphones, title: '联系客服', desc: '遇到问题？我们的团队随时为你提供帮助', link: '/contact' },
]

// 热门创作者 - 从后端动态加载
const creators = ref<TopCreator[]>([])
const creatorsLoading = ref(true)

const loadTopCreators = async () => {
  try {
    creatorsLoading.value = true
    creators.value = await homeApi.getTopCreators()
  } catch (e) {
    console.warn('加载热门创作者失败', e)
  } finally {
    creatorsLoading.value = false
  }
}

const activeBanner = ref(0)
const banners = [
  { title: '赛博朋克 2077：往日之影', subtitle: '最新 DLC 深度解析与 AI 攻略生成', color: 'from-yellow-400 to-red-500' },
  { title: 'AI 绘画创作大赛', subtitle: '使用 Inspo-AI 生成你的专属机甲', color: 'from-neon-blue to-neon-purple' },
  { title: '独立游戏开发者沙龙', subtitle: '本周六晚 20:00 线上直播', color: 'from-green-400 to-emerald-600' },
]

let bannerTimer: ReturnType<typeof setInterval>

onMounted(() => {
  bannerTimer = setInterval(() => {
    activeBanner.value = (activeBanner.value + 1) % banners.length
  }, 5000)
  loadTopCreators()
})

onUnmounted(() => {
  clearInterval(bannerTimer)
})

const formatLikes = (count: number) => {
  if (count >= 10000) return (count / 10000).toFixed(1) + 'w'
  if (count >= 1000) return (count / 1000).toFixed(1) + 'k'
  return String(count)
}
</script>

<template>
  <div class="container mx-auto px-4 py-20 text-center relative z-10">
    <!-- Hero / Banner Section -->
    <div class="mb-24 relative min-h-[500px] flex flex-col justify-center items-center">
      <!-- Dynamic Banner Background -->
      <transition-group name="fade">
        <div 
          v-for="(banner, index) in banners" 
          :key="index"
          v-show="activeBanner === index"
          class="absolute inset-0 -z-10 opacity-20 blur-[100px] bg-gradient-to-tr rounded-full transition-all duration-1000"
          :class="banner.color"
        ></div>
      </transition-group>

      <div class="inline-block mb-6 px-4 py-1.5 rounded-full border border-neon-blue/30 bg-neon-blue/10 text-neon-blue text-sm font-bold tracking-wide animate__animated animate__fadeInDown">
        ✨ NEXT-GEN CREATIVE COMMUNITY
      </div>
      
      <h1 class="text-6xl md:text-8xl font-black mb-8 tracking-tighter animate__animated animate__zoomIn">
        <GlitchText text="INSPO-VERSE" />
      </h1>
      
      <!-- Dynamic Subtitle -->
      <div class="h-24 relative w-full max-w-2xl mx-auto">
        <transition-group name="slide-up">
          <div 
            v-for="(banner, index) in banners" 
            :key="index"
            v-show="activeBanner === index"
            class="absolute top-0 left-0 w-full"
          >
            <h2 class="text-2xl md:text-3xl font-bold text-white mb-2">{{ banner.title }}</h2>
            <p class="text-gray-400">{{ banner.subtitle }}</p>
          </div>
        </transition-group>
      </div>
      
      <div class="mt-12 flex flex-col md:flex-row justify-center gap-6 animate__animated animate__fadeInUp animate__delay-1s">
        <button @click="requireLogin() && router.push('/explore')" class="group relative px-8 py-4 bg-neon-blue text-slate-900 font-bold text-lg rounded-full overflow-hidden transition-all hover:scale-105 hover:shadow-[0_0_30px_rgba(0,243,255,0.6)]">
          <span class="relative z-10 flex items-center gap-2">
            <Zap class="w-5 h-5" /> 立即探索
          </span>
          <div class="absolute inset-0 bg-white/20 translate-y-full group-hover:translate-y-0 transition-transform duration-300"></div>
        </button>

        <button @click="requireLogin()" class="px-8 py-4 border border-white/20 text-white font-bold text-lg rounded-full hover:bg-white/10 hover:border-white/50 transition-all backdrop-blur-sm flex items-center gap-2 group">
          观看演示 <ArrowRight class="w-5 h-5 group-hover:translate-x-1 transition-transform" />
        </button>
      </div>

      <!-- Banner Indicators -->
      <div class="flex gap-2 mt-12 justify-center">
        <button 
          v-for="(_, index) in banners" 
          :key="index"
          @click="activeBanner = index"
          class="w-3 h-3 rounded-full transition-all duration-300"
          :class="activeBanner === index ? 'bg-white w-8' : 'bg-white/20 hover:bg-white/50'"
        ></button>
      </div>
    </div>

    <!-- Features Grid -->
    <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-6 mt-20 animate__animated animate__fadeInUp animate__delay-2s">
      <div
        v-for="(feat, index) in features"
        :key="index"
        @click="requireLogin() && router.push(feat.link)"
        class="group p-6 rounded-2xl bg-slate-800/40 border border-white/5 hover:border-neon-purple/50 hover:bg-slate-800/60 transition-all duration-300 hover:-translate-y-2 relative overflow-hidden cursor-pointer"
      >
        <div class="absolute top-0 right-0 p-3 opacity-0 group-hover:opacity-100 transition-opacity">
          <ArrowRight class="w-5 h-5 text-neon-purple -rotate-45" />
        </div>
        <div class="w-14 h-14 mx-auto mb-4 rounded-2xl bg-gradient-to-br from-slate-700 to-slate-800 flex items-center justify-center group-hover:scale-110 transition-transform shadow-lg">
          <component :is="feat.icon" class="w-7 h-7 text-neon-purple group-hover:text-neon-pink transition-colors" />
        </div>
        <h3 class="text-lg font-bold text-white mb-2">{{ feat.title }}</h3>
        <p class="text-gray-400 text-xs leading-relaxed">{{ feat.desc }}</p>
      </div>
    </div>

    <!-- Top Creators Section -->
    <div class="mt-32">
      <h2 class="text-3xl font-bold mb-12 flex items-center justify-center gap-3">
        <Star class="w-8 h-8 text-yellow-400 fill-yellow-400" /> 
        本周热门创作者 
        <Star class="w-8 h-8 text-yellow-400 fill-yellow-400" />
      </h2>
      
      <!-- 加载骨架屏 -->
      <div v-if="creatorsLoading" class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-6">
        <div v-for="i in 4" :key="i" class="bg-slate-800/50 rounded-xl p-6 animate-pulse">
          <div class="w-20 h-20 mx-auto mb-4 rounded-full bg-slate-700"></div>
          <div class="h-5 bg-slate-700 rounded mx-auto w-24 mb-2"></div>
          <div class="h-3 bg-slate-700 rounded mx-auto w-16 mb-3"></div>
          <div class="h-4 bg-slate-700 rounded mx-auto w-20"></div>
        </div>
      </div>

      <!-- 创作者列表 -->
      <div v-else-if="creators.length" class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-6">
        <div
          v-for="creator in creators"
          :key="creator.userId"
          @click="requireLogin()"
          class="bg-slate-800/50 backdrop-blur border border-white/5 rounded-xl p-6 hover:border-neon-blue/30 transition-all hover:transform hover:scale-105 cursor-pointer"
        >
          <div class="relative w-20 h-20 mx-auto mb-4">
            <img :src="creator.avatarUrl || `https://api.dicebear.com/7.x/avataaars/svg?seed=${creator.nickname}`" class="rounded-full bg-slate-700 w-full h-full object-cover border-2 border-white/10" :alt="creator.nickname" />
            <div class="absolute bottom-0 right-0 w-6 h-6 bg-neon-green rounded-full border-2 border-slate-800 flex items-center justify-center">
              <span class="text-[10px] font-bold text-slate-900">V</span>
            </div>
          </div>
          <h4 class="font-bold text-lg text-white">{{ creator.nickname }}</h4>
          <p class="text-xs text-neon-blue mb-3">{{ creator.bio || '创意达人' }}</p>
          <div class="flex items-center justify-center gap-1 text-gray-400 text-sm">
            <Heart class="w-4 h-4 text-red-500 fill-red-500" /> {{ formatLikes(creator.totalLikes) }} 获赞
          </div>
        </div>
      </div>
      
      <!-- 空状态 -->
      <div v-else class="text-center text-gray-500 py-12">
        暂无数据，快来成为第一位创作者吧！
      </div>
    </div>
  </div>
</template>

<style scoped>
.slide-up-enter-active,
.slide-up-leave-active {
  transition: all 0.5s ease;
  position: absolute;
  width: 100%;
}

.slide-up-enter-from {
  opacity: 0;
  transform: translateY(20px);
}

.slide-up-leave-to {
  opacity: 0;
  transform: translateY(-20px);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 1s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>

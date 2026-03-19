<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import GlitchText from '@/components/ui/GlitchText.vue'
import { Sparkles, Gamepad2, Tv, Zap, ArrowRight, Star, Heart } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'

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
]

const creators = [
  { name: 'Neo_Anderson', role: '3D 艺术家', avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Neo', likes: '12k' },
  { name: 'Trinity_Dev', role: '全栈开发', avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Trinity', likes: '8.5k' },
  { name: 'Morpheus_AI', role: '提示词工程', avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Morpheus', likes: '20k' },
  { name: 'Oracle_Design', role: 'UI 设计师', avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Oracle', likes: '15k' },
]

const activeBanner = ref(0)
const banners = [
  { title: '赛博朋克 2077：往日之影', subtitle: '最新 DLC 深度解析与 AI 攻略生成', color: 'from-yellow-400 to-red-500' },
  { title: 'AI 绘画创作大赛', subtitle: '使用 Inspo-AI 生成你的专属机甲', color: 'from-neon-blue to-neon-purple' },
  { title: '独立游戏开发者沙龙', subtitle: '本周六晚 20:00 线上直播', color: 'from-green-400 to-emerald-600' },
]

onMounted(() => {
  setInterval(() => {
    activeBanner.value = (activeBanner.value + 1) % banners.length
  }, 5000)
})
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
    <div class="grid grid-cols-1 md:grid-cols-3 gap-8 mt-20 animate__animated animate__fadeInUp animate__delay-2s">
      <div
        v-for="(feat, index) in features"
        :key="index"
        @click="requireLogin() && router.push(feat.link)"
        class="group p-8 rounded-2xl bg-slate-800/40 border border-white/5 hover:border-neon-purple/50 hover:bg-slate-800/60 transition-all duration-300 hover:-translate-y-2 relative overflow-hidden cursor-pointer"
      >
        <div class="absolute top-0 right-0 p-4 opacity-0 group-hover:opacity-100 transition-opacity">
          <ArrowRight class="w-6 h-6 text-neon-purple -rotate-45" />
        </div>
        <div class="w-16 h-16 mx-auto mb-6 rounded-2xl bg-gradient-to-br from-slate-700 to-slate-800 flex items-center justify-center group-hover:scale-110 transition-transform shadow-lg">
          <component :is="feat.icon" class="w-8 h-8 text-neon-purple group-hover:text-neon-pink transition-colors" />
        </div>
        <h3 class="text-xl font-bold text-white mb-3">{{ feat.title }}</h3>
        <p class="text-gray-400 text-sm leading-relaxed">{{ feat.desc }}</p>
      </div>
    </div>

    <!-- Top Creators Section -->
    <div class="mt-32">
      <h2 class="text-3xl font-bold mb-12 flex items-center justify-center gap-3">
        <Star class="w-8 h-8 text-yellow-400 fill-yellow-400" /> 
        本周热门创作者 
        <Star class="w-8 h-8 text-yellow-400 fill-yellow-400" />
      </h2>
      <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-6">
        <div
          v-for="(creator, i) in creators"
          :key="i"
          @click="requireLogin()"
          class="bg-slate-800/50 backdrop-blur border border-white/5 rounded-xl p-6 hover:border-neon-blue/30 transition-all hover:transform hover:scale-105 cursor-pointer"
        >
          <div class="relative w-20 h-20 mx-auto mb-4">
            <img :src="creator.avatar" class="rounded-full bg-slate-700 w-full h-full object-cover border-2 border-white/10" />
            <div class="absolute bottom-0 right-0 w-6 h-6 bg-neon-green rounded-full border-2 border-slate-800 flex items-center justify-center">
              <span class="text-[10px] font-bold text-slate-900">V</span>
            </div>
          </div>
          <h4 class="font-bold text-lg text-white">{{ creator.name }}</h4>
          <p class="text-xs text-neon-blue mb-3">{{ creator.role }}</p>
          <div class="flex items-center justify-center gap-1 text-gray-400 text-sm">
            <Heart class="w-4 h-4 text-red-500 fill-red-500" /> {{ creator.likes }} 获赞
          </div>
        </div>
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

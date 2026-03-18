<script setup lang="ts">
import { ref } from 'vue'
import GlitchText from '@/components/ui/GlitchText.vue'
import { Star, Download, Play, X, Calendar, User, Gamepad2 } from 'lucide-vue-next'

const selectedGame = ref<any>(null)

const games = [
  { 
    id: 1, 
    title: 'Cyberpunk 2077', 
    genre: 'Open World RPG', 
    rating: 9.8, 
    cover: 'https://cdn.cloudflare.steamstatic.com/steam/apps/1091500/header.jpg', 
    hero: 'https://cdn.cloudflare.steamstatic.com/steam/apps/1091500/library_hero.jpg',
    tags: ['Ray Tracing', 'Sci-Fi'],
    description: '《赛博朋克 2077》是一款开放世界动作冒险 RPG 游戏。故事发生在夜之城，这是一座五光十色、危机四伏的超级大都会，权力更迭和身体改造是这里不变的主题。你将扮演一名野心勃勃的雇佣兵：V，追寻一种独一无二的植入体。只要得到它，就能掌握获得永生的关键。',
    developer: 'CD PROJEKT RED',
    releaseDate: '2020-12-10'
  },
  { 
    id: 2, 
    title: 'Elden Ring', 
    genre: 'Action RPG', 
    rating: 9.9, 
    cover: 'https://cdn.cloudflare.steamstatic.com/steam/apps/1245620/header.jpg', 
    hero: 'https://cdn.cloudflare.steamstatic.com/steam/apps/1245620/library_hero.jpg',
    tags: ['Souls-like', 'Dark Fantasy'],
    description: '起来吧，褪色者。让我们一同走上成王之路。艾尔登法环是一款黑暗幻想风格的动作角色扮演游戏，由 FromSoftware 开发，乔治·R·R·马丁参与世界观构建。探索广袤的交界地，挑战半神，重铸法环。',
    developer: 'FromSoftware Inc.',
    releaseDate: '2022-02-25'
  },
  { 
    id: 3, 
    title: 'Starfield', 
    genre: 'Space RPG', 
    rating: 8.5, 
    cover: 'https://cdn.cloudflare.steamstatic.com/steam/apps/1716740/header.jpg', 
    hero: 'https://cdn.cloudflare.steamstatic.com/steam/apps/1716740/library_hero.jpg',
    tags: ['Space', 'Exploration'],
    description: '《Starfield》出自打造了《The Elder Scrolls V: Skyrim》和《Fallout 4》的获奖制作团队 Bethesda Game Studios 之手，是该团队 25 年来的首个全新 IP。在这款以茫茫星际为舞台的次世代角色扮演游戏中，你可以创建任意角色，探索世界并体验前所未有的自由。',
    developer: 'Bethesda Game Studios',
    releaseDate: '2023-09-06'
  },
  { 
    id: 4, 
    title: 'Hades II', 
    genre: 'Roguelike', 
    rating: 9.7, 
    cover: 'https://cdn.cloudflare.steamstatic.com/steam/apps/1145350/header.jpg', 
    hero: 'https://cdn.cloudflare.steamstatic.com/steam/apps/1145350/library_hero.jpg',
    tags: ['Indie', 'Action'],
    description: 'Supergiant Games 备受赞誉的肉鸽迷宫探索游戏续作。作为冥界的不朽公主，你将探索更加宏大深邃的神话世界，在奥林匹斯众神的加持下与时间泰坦抗衡。',
    developer: 'Supergiant Games',
    releaseDate: '2024-05-07'
  },
  { 
    id: 5, 
    title: "Baldur's Gate 3", 
    genre: 'CRPG', 
    rating: 10.0, 
    cover: 'https://cdn.cloudflare.steamstatic.com/steam/apps/1086940/header.jpg', 
    hero: 'https://cdn.cloudflare.steamstatic.com/steam/apps/1086940/library_hero.jpg',
    tags: ['D&D', 'Story Rich'],
    description: '召集你的队伍，返回被遗忘的国度，开启一段记载着友谊与背叛、牺牲与生存、以及至上权力诱惑的传奇故事。神秘的力量正在你体内苏醒，而这一切都来源于夺心魔种在你大脑里的寄生虫。',
    developer: 'Larian Studios',
    releaseDate: '2023-08-03'
  },
  { 
    id: 6, 
    title: 'Black Myth: Wukong', 
    genre: 'Action RPG', 
    rating: 9.5, 
    cover: 'https://cdn.cloudflare.steamstatic.com/steam/apps/2358720/header.jpg', 
    hero: 'https://cdn.cloudflare.steamstatic.com/steam/apps/2358720/library_hero.jpg',
    tags: ['Mythology', 'Souls-like'],
    description: '《黑神话：悟空》是一款以中国神话为背景的动作角色扮演游戏。你将扮演一位“天命人”，为了探寻昔日传说的真相，踏上条充满危险与惊奇的西游之路。',
    developer: 'Game Science',
    releaseDate: '2024-08-20'
  },
]

const activeIndex = ref(0)

const openGameDetail = (game: any) => {
  selectedGame.value = game
}

const closeDetail = () => {
  selectedGame.value = null
}
</script>

<template>
  <div class="container mx-auto px-4 py-20 min-h-screen">
    <div class="text-center mb-16">
      <h1 class="text-5xl md:text-7xl font-black mb-4">
        <GlitchText text="GAME HUB" />
      </h1>
      <p class="text-neon-blue font-bold tracking-[0.2em] animate-pulse">精选全网最硬核的游戏大作</p>
    </div>

    <!-- Featured Game (Hero) -->
    <div class="relative rounded-3xl overflow-hidden mb-16 group border border-neon-blue/20 shadow-[0_0_50px_rgba(0,243,255,0.1)]">
      <div class="absolute inset-0 bg-gradient-to-t from-slate-900 via-slate-900/40 to-transparent z-10"></div>
      <!-- 使用 Hero 横版大图 -->
      <img :src="games[activeIndex].hero" class="w-full h-[500px] object-cover object-top transition-transform duration-1000 group-hover:scale-105" />
      
      <div class="absolute bottom-0 left-0 p-8 md:p-12 z-20 max-w-2xl">
        <div class="flex items-center gap-3 mb-4">
          <span class="px-3 py-1 bg-neon-purple/80 backdrop-blur rounded text-xs font-bold">{{ games[activeIndex].genre }}</span>
          <div class="flex items-center gap-1 text-yellow-400 font-bold">
            <Star class="w-4 h-4 fill-yellow-400" /> {{ games[activeIndex].rating }}
          </div>
        </div>
        <h2 class="text-4xl md:text-6xl font-black text-white mb-6 leading-tight">{{ games[activeIndex].title }}</h2>
        <div class="flex gap-4">
          <button 
            @click="openGameDetail(games[activeIndex])"
            class="px-8 py-3 bg-neon-blue text-slate-900 font-bold rounded-full hover:shadow-[0_0_20px_rgba(0,243,255,0.5)] transition-all flex items-center gap-2"
          >
            <Play class="w-5 h-5 fill-current" /> 立即试玩
          </button>
          <button class="px-8 py-3 border border-white/20 text-white font-bold rounded-full hover:bg-white/10 flex items-center gap-2">
            <Download class="w-5 h-5" /> 下载资源
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
          <!-- 使用 Cover 封面图 -->
          <img :src="game.cover" class="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110" />
          <div class="absolute inset-0 bg-black/40 group-hover:bg-transparent transition-colors"></div>
        </div>
        
        <div class="p-6">
          <div class="flex justify-between items-start mb-2">
            <h3 class="text-xl font-bold text-white group-hover:text-neon-blue transition-colors">{{ game.title }}</h3>
            <span class="text-sm font-bold text-yellow-400">{{ game.rating }}</span>
          </div>
          <div class="flex flex-wrap gap-2">
            <span v-for="tag in game.tags" :key="tag" class="text-xs text-gray-400 bg-white/5 px-2 py-1 rounded">#{{ tag }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Game Detail Modal -->
    <transition name="modal">
      <div v-if="selectedGame" class="fixed inset-0 z-[100] flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-black/90 backdrop-blur-sm" @click="closeDetail"></div>
        
        <div class="relative w-full max-w-5xl bg-slate-900 rounded-3xl overflow-hidden shadow-2xl border border-white/10 animate__animated animate__zoomIn flex flex-col md:flex-row max-h-[90vh]">
          
          <button @click="closeDetail" class="absolute top-4 right-4 z-50 p-2 bg-black/50 text-white rounded-full hover:bg-black/80 transition-colors border border-white/10">
            <X class="w-6 h-6" />
          </button>

          <!-- Left: Hero Image -->
          <div class="w-full md:w-1/2 h-64 md:h-auto relative">
            <img :src="selectedGame.hero" class="w-full h-full object-cover" />
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
                <span>{{ selectedGame.developer }}</span>
              </div>
              <div class="flex items-center gap-2">
                <Calendar class="w-4 h-4" />
                <span>{{ selectedGame.releaseDate }}</span>
              </div>
            </div>

            <p class="text-gray-300 leading-relaxed mb-8">{{ selectedGame.description }}</p>

            <div class="mb-8">
              <h4 class="text-sm font-bold text-white mb-3">游戏标签</h4>
              <div class="flex flex-wrap gap-2">
                <span v-for="tag in selectedGame.tags" :key="tag" class="px-3 py-1 bg-white/5 rounded-full text-xs text-gray-300 border border-white/5"># {{ tag }}</span>
              </div>
            </div>

            <div class="flex gap-4 mt-auto pt-8 border-t border-white/5">
              <button class="flex-1 py-3 bg-neon-blue text-slate-900 font-bold rounded-xl hover:shadow-[0_0_20px_rgba(0,243,255,0.4)] transition-all flex items-center justify-center gap-2">
                <Gamepad2 class="w-5 h-5" /> 开始游戏
              </button>
              <button class="flex-1 py-3 bg-slate-800 text-white font-bold rounded-xl hover:bg-slate-700 transition-all border border-white/10">
                加入收藏
              </button>
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
</style>

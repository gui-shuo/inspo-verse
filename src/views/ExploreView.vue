<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search, Heart, MessageSquare, Filter, Zap, ImageIcon, Gamepad2, Tv, X, Share2, MoreHorizontal } from 'lucide-vue-next'
import GlitchText from '@/components/ui/GlitchText.vue'

const route = useRoute()
const router = useRouter()

// 模拟分类
const categories = [
  { id: 'all', name: '全部推荐', icon: Zap },
  { id: 'ai-art', name: 'AI 绘图', icon: ImageIcon },
  { id: 'game', name: '游戏攻略', icon: Gamepad2 },
  { id: 'anime', name: '番剧二创', icon: Tv },
]

// 模拟内容数据 (补充 description 字段)
const posts = ref([
  {
    id: 1,
    title: '赛博朋克 2077：幻影自由 - 隐藏结局触发全流程',
    author: 'V_Official',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=V',
    image: 'https://images.unsplash.com/photo-1535905557558-afc4877a26fc?w=800&q=80',
    category: 'game',
    likes: 1240,
    comments: 88,
    isLiked: false,
    tag: 'Cyberpunk',
    description: '《赛博朋克 2077：幻影自由》中共有四个主要结局。要触发隐藏结局“高塔”，你需要在关键任务“比远方更远”中做出特定选择...'
  },
  {
    id: 2,
    title: 'Midjourney V6 实测：机甲少女生成关键词分享',
    author: 'AI_Wizard',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Wizard',
    image: 'https://images.unsplash.com/photo-1618172193763-c511deb635ca?w=800&q=80',
    category: 'ai-art',
    likes: 3500,
    comments: 210,
    isLiked: true,
    tag: 'AI Art',
    description: 'Prompts: A futuristic mecha girl, neon glowing armor, cyberpunk city background, volumetric lighting, 8k resolution, detailed texture...'
  },
  {
    id: 3,
    title: '进击的巨人最终章：致两千年后的你',
    author: 'Eren_Y',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Eren',
    image: 'https://images.unsplash.com/photo-1541562232579-512a21360020?w=800&q=80',
    category: 'anime',
    likes: 890,
    comments: 45,
    isLiked: false,
    tag: 'Anime',
    description: '那一天，人类终于回想起了被巨人支配的恐惧。最终章的结局充满了争议，但也完美地闭合了整个故事的循环。'
  },
  {
    id: 4,
    title: '虚幻引擎 5 渲染的夜之城，这光影太绝了',
    author: 'Tech_Artist',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Tech',
    image: 'https://images.unsplash.com/photo-1555680202-c86f0e12f086?w=800&q=80',
    category: 'game',
    likes: 2100,
    comments: 156,
    isLiked: false,
    tag: 'UE5',
    description: '使用了 Lumen 全局光照和 Nanite 虚拟几何体技术，在 RTX 4090 上跑出了 60fps 的稳定帧率。'
  },
  {
    id: 5,
    title: 'Stable Diffusion 生成赛博猫咪，可爱到犯规！',
    author: 'Kitty_Punk',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Kitty',
    image: 'https://images.unsplash.com/photo-1520697830682-bbb6e85e2b0b?w=800&q=80',
    category: 'ai-art',
    likes: 5600,
    comments: 420,
    isLiked: true,
    tag: 'Cat',
    description: '模型：ChilloutMix，LoRA：CyberPunk Style v2.0。生成的猫咪既有机械感又不失毛茸茸的可爱。'
  },
  {
    id: 6,
    title: '鬼灭之刃：柱训练篇最新情报解析',
    author: 'Demon_Slayer',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Tanjiro',
    image: 'https://images.unsplash.com/photo-1578632767115-351597cf2477?w=800&q=80',
    category: 'anime',
    likes: 1200,
    comments: 99,
    isLiked: false,
    tag: 'News',
    description: '柱训练篇将重点展示炭治郎与各位柱的互动，为无限城决战做最后的铺垫。作画质量依旧是业界天花板。'
  },
  {
    id: 7,
    title: '黑客帝国代码雨特效教程 (Vue 3 + Canvas)',
    author: 'Code_Master',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Code',
    image: 'https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=800&q=80',
    category: 'game',
    likes: 4500,
    comments: 300,
    isLiked: false,
    tag: 'Coding',
    description: '不到 100 行代码，教你用 Canvas 实现经典的黑客帝国数字雨效果。源码已上传 GitHub。'
  },
  {
    id: 8,
    title: '霓虹灯下的孤独：街头摄影作品赏析',
    author: 'Street_Eye',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Eye',
    // 修复：使用 Pexels 官方 CDN 链接，通常更稳定
    image: 'https://images.pexels.com/photos/2614818/pexels-photo-2614818.jpeg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=1',
    category: 'ai-art',
    likes: 880,
    comments: 32,
    isLiked: true,
    tag: 'Photography',
    description: '拍摄于东京新宿的雨夜。那把透明雨伞反射出的霓虹灯光，是这幅作品的点睛之笔。'
  }
])

const activeCategory = ref('all')
const searchQuery = ref('')
const selectedPost = ref<any>(null)

// 修复：添加 updateCategory 方法
const updateCategory = (id: string) => {
  activeCategory.value = id
}

// 修复：作品详情逻辑
const openDetail = (post: any) => {
  selectedPost.value = post
  // 全平台锁定背景滚动，让模态框内部(wrapper)处理滚动
  document.body.style.overflow = 'hidden'
}

const closeDetail = () => {
  selectedPost.value = null
  document.body.style.overflow = ''
}

const filteredPosts = computed(() => {
  return posts.value.filter(post => {
    const matchCategory = activeCategory.value === 'all' || post.category === activeCategory.value
    const matchSearch = post.title.toLowerCase().includes(searchQuery.value.toLowerCase()) || 
                       post.tag.toLowerCase().includes(searchQuery.value.toLowerCase())
    return matchCategory && matchSearch
  })
})

const toggleLike = (post: any) => {
  post.isLiked = !post.isLiked
  post.likes += post.isLiked ? 1 : -1
}
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
            v-model="searchQuery"
            type="text" 
            placeholder="搜索关键词：Cyberpunk, AI, 攻略..." 
            class="w-full bg-transparent text-white focus:outline-none placeholder-gray-500"
          >
          <button class="bg-white/10 p-2 rounded-full hover:bg-white/20 transition-colors">
            <Filter class="w-4 h-4 text-white" />
          </button>
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

    <!-- Masonry Grid -->
    <div class="columns-1 md:columns-2 lg:columns-4 gap-6 space-y-6">
      <div 
        v-for="post in filteredPosts" 
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
              <img :src="post.avatar" class="w-6 h-6 rounded-full bg-slate-700" />
              <span class="text-xs text-gray-400 truncate max-w-[80px]">{{ post.author }}</span>
            </div>
            
            <div class="flex items-center gap-3 text-xs text-gray-400">
              <button 
                @click.stop="toggleLike(post)"
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
    
    <!-- Empty State -->
    <div v-if="filteredPosts.length === 0" class="text-center py-20 animate__animated animate__fadeIn">
      <div class="inline-flex justify-center items-center w-20 h-20 rounded-full bg-slate-800 mb-6">
        <Search class="w-8 h-8 text-gray-500" />
      </div>
      <h3 class="text-xl font-bold text-white mb-2">未找到相关内容</h3>
      <p class="text-gray-400">尝试更换关键词或分类</p>
    </div>

    <!-- Post Detail Modal (Dynamic Height & Scrollable) -->
    <transition name="modal">
      <!-- Wrapper: handles scrolling for the whole modal -->
      <div v-if="selectedPost" class="fixed inset-0 z-[100] flex justify-center bg-black/90 backdrop-blur-sm overflow-y-auto custom-scrollbar">
        
        <!-- Backdrop Click Area -->
        <div class="absolute inset-0 -z-10 cursor-pointer" @click="closeDetail"></div>
        
        <!-- Modal Body -->
        <!-- 核心优化: h-auto min-h-[60vh] 允许高度自适应, flex-row stretch 实现左右等高 -->
        <div class="relative w-full max-w-6xl my-4 md:my-10 mx-4 bg-slate-900 rounded-3xl overflow-hidden shadow-2xl border border-white/10 animate__animated animate__zoomIn flex flex-col md:flex-row h-fit min-h-[60vh]">
          
          <!-- Close Button -->
          <button @click="closeDetail" class="absolute top-4 right-4 z-50 p-2 bg-black/50 text-white rounded-full hover:bg-black/80 transition-colors border border-white/10">
            <X class="w-6 h-6" />
          </button>

          <!-- Left: Image (Natural Height, Dynamic Matching) -->
          <!-- w-2/3, h-full (stretched by flex container) -->
          <div class="w-full md:w-2/3 bg-black flex items-center justify-center relative group">
            <img :src="selectedPost.image" class="w-full h-auto object-contain max-h-none" />
            <div class="absolute bottom-4 right-4 flex gap-2 opacity-100 transition-opacity">
               <button class="p-2 bg-black/50 hover:bg-black/70 backdrop-blur rounded-lg text-white"><Share2 class="w-5 h-5"/></button>
               <button class="p-2 bg-black/50 hover:bg-black/70 backdrop-blur rounded-lg text-white"><MoreHorizontal class="w-5 h-5"/></button>
            </div>
          </div>

          <!-- Right: Info (Dynamic Matching) -->
          <!-- w-1/3, h-full (stretched by flex container), flex-col -->
          <div class="w-full md:w-1/3 bg-slate-800 flex flex-col border-l border-white/5">
            
            <!-- Content -->
            <div class="flex-1 p-6 md:p-8">
              <!-- Author -->
              <div class="flex items-center gap-3 mb-6">
                <img :src="selectedPost.avatar" class="w-10 h-10 rounded-full border border-white/20" />
                <div>
                  <h4 class="text-white font-bold">{{ selectedPost.author }}</h4>
                  <p class="text-xs text-gray-400">发布于 2 小时前</p>
                </div>
                <button class="ml-auto px-4 py-1.5 bg-neon-blue/10 text-neon-blue text-xs font-bold rounded-full hover:bg-neon-blue hover:text-slate-900 transition-colors">关注</button>
              </div>

              <h2 class="text-2xl font-bold text-white mb-4 leading-tight">{{ selectedPost.title }}</h2>
              <p class="text-gray-300 leading-relaxed whitespace-pre-line">{{ selectedPost.description }}</p>
              
              <div class="mt-6 flex flex-wrap gap-2">
                <span class="px-3 py-1 bg-white/5 rounded-lg text-xs text-gray-400">#{{ selectedPost.tag }}</span>
                <span class="px-3 py-1 bg-white/5 rounded-lg text-xs text-gray-400">#Recommended</span>
              </div>

              <!-- Comments Section -->
              <div class="mt-8 pt-6 border-t border-white/5">
                <h5 class="text-sm font-bold text-gray-400 mb-4">评论 ({{ selectedPost.comments }})</h5>
                <div class="space-y-6">
                  <div class="flex gap-3">
                    <div class="w-8 h-8 rounded-full bg-slate-700 flex-shrink-0"></div>
                    <div>
                      <div class="text-xs font-bold text-gray-300 mb-1">User_One</div>
                      <p class="text-sm text-gray-400">这真的太酷了！这就是我想要的赛博朋克感觉。</p>
                    </div>
                  </div>
                  <div class="flex gap-3">
                    <div class="w-8 h-8 rounded-full bg-slate-700 flex-shrink-0"></div>
                    <div>
                      <div class="text-xs font-bold text-gray-300 mb-1">Cyber_Fan</div>
                      <p class="text-sm text-gray-400">求高清原图做壁纸！</p>
                    </div>
                  </div>
                   <!-- More comments to make it scroll -->
                   <div class="flex gap-3" v-for="i in 5" :key="i">
                    <div class="w-8 h-8 rounded-full bg-slate-700 flex-shrink-0"></div>
                    <div>
                      <div class="text-xs font-bold text-gray-300 mb-1">User_{{ i + 2 }}</div>
                      <p class="text-sm text-gray-400">太强了，学习了！希望多出点这种类型的教程。</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Fixed Bottom Actions -->
            <div class="mt-auto p-4 border-t border-white/5 flex gap-4 bg-slate-800 sticky bottom-0">
              <button 
                @click="toggleLike(selectedPost)"
                class="flex-1 py-3 rounded-xl font-bold flex items-center justify-center gap-2 transition-all"
                :class="selectedPost.isLiked ? 'bg-pink-500 text-white' : 'bg-slate-700 text-gray-300 hover:bg-slate-600'"
              >
                <Heart class="w-5 h-5" :class="selectedPost.isLiked ? 'fill-current' : ''" />
                {{ selectedPost.isLiked ? '已赞' : '点赞' }}
              </button>
              <button class="flex-1 bg-slate-700 text-gray-300 py-3 rounded-xl font-bold hover:bg-slate-600 transition-all flex items-center justify-center gap-2">
                <MessageSquare class="w-5 h-5" /> 评论
              </button>
            </div>
          </div>
        </div>
      </div>
    </transition>

  </div>
</template>

<style scoped>
/* 隐藏滚动条但保留功能 */
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

.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 4px;
}
</style>

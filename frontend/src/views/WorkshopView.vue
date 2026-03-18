<script setup lang="ts">
import { ref } from 'vue'
import GlitchText from '@/components/ui/GlitchText.vue'
import { Download, Star, Cpu, X, Share2, Heart } from 'lucide-vue-next'

const selectedItem = ref<any>(null)

const openDetail = (item: any) => {
  selectedItem.value = item
}

const closeDetail = () => {
  selectedItem.value = null
}

const mods = [
  { 
    title: 'Cyberpunk UI Kit', 
    author: 'NeonDev', 
    downloads: '12k', 
    rating: 4.9, 
    image: 'https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=800&q=80',
    description: '一套完整的赛博朋克风格 UI 组件库，包含霓虹按钮、全息面板、故障风文字特效等。完美适配 Vue 3 和 React，开箱即用。',
    version: 'v2.4.0',
    size: '15.4 MB',
    updated: '2024-05-20',
    tags: ['UI', 'Cyberpunk', 'Vue', 'React']
  },
  { 
    title: 'AI Prompt Helper', 
    author: 'PromptMaster', 
    downloads: '8.5k', 
    rating: 4.8, 
    image: 'https://images.unsplash.com/photo-1620712943543-bcc4688e7485?w=800&q=80',
    description: '智能 Prompt 优化助手，帮助你编写更好的 AI 绘画和写作提示词。内置超过 5000 个经过验证的优质 Prompt 模板。',
    version: 'v1.1.2',
    size: '5.8 MB',
    updated: '2024-06-01',
    tags: ['AI', 'Tool', 'Productivity']
  },
  { 
    title: 'Retro Glitch Shader', 
    author: 'ShaderWizard', 
    downloads: '5k', 
    rating: 5.0, 
    image: 'https://images.unsplash.com/photo-1550751827-4bd374c3f58b?w=800&q=80',
    description: '高性能的复古故障风格着色器（Shader），支持 Unity 和 Unreal Engine。包含 RGB 分离、扫描线、画面撕裂等多种经典特效。',
    version: 'v3.0.0',
    size: '2.1 MB',
    updated: '2024-04-15',
    tags: ['Shader', 'VFX', 'GameDev']
  },
  { 
    title: 'Dark Mode Theme', 
    author: 'UX_Guru', 
    downloads: '20k', 
    rating: 4.7, 
    image: 'https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=800&q=80',
    description: '为 IDE 和常用开发工具设计的终极深色主题。精心调配的对比度，护眼且美观，让你的深夜编码体验更上一层楼。',
    version: 'v1.5.5',
    size: '1.2 MB',
    updated: '2024-05-10',
    tags: ['Theme', 'Developer', 'Dark Mode']
  },
  { 
    title: 'Sci-Fi Sound Pack', 
    author: 'AudioLab', 
    downloads: '3.2k', 
    rating: 4.6, 
    image: 'https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=800&q=80',
    description: '包含 200+ 高品质科幻音效：激光武器、飞船引擎、全息界面操作音等。所有音频均为无损 WAV 格式。',
    version: 'v1.0.0',
    size: '450 MB',
    updated: '2024-03-22',
    tags: ['Audio', 'SFX', 'Sci-Fi']
  },
  { 
    title: 'Low Poly City Assets', 
    author: 'PolyBuilder', 
    downloads: '15k', 
    rating: 4.9, 
    image: 'https://images.unsplash.com/photo-1480796927426-f609979314bd?w=800&q=80',
    description: '构建你的低多边形城市！包含建筑、车辆、道路、植被等 100+ 3D 模型。非常适合移动端游戏开发。',
    version: 'v4.2.1',
    size: '35 MB',
    updated: '2024-06-05',
    tags: ['3D Model', 'Low Poly', 'City']
  }
]
</script>

<template>
  <div class="container mx-auto px-4 py-20 min-h-screen">
    <div class="text-center mb-16">
      <h1 class="text-5xl font-black mb-4"><GlitchText text="WORKSHOP" /></h1>
      <p class="text-neon-green">社区共建 · 无限扩展</p>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <div 
        v-for="(mod, index) in mods" 
        :key="index"
        @click="openDetail(mod)"
        class="bg-slate-800 rounded-xl overflow-hidden border border-white/5 hover:border-neon-green/50 transition-all hover:-translate-y-1 group cursor-pointer"
      >
        <div class="h-40 overflow-hidden relative">
          <img :src="mod.image" class="w-full h-full object-cover group-hover:scale-110 transition-transform duration-500" />
          <div class="absolute inset-0 bg-black/50 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
            <button class="bg-neon-green text-slate-900 px-4 py-2 rounded-lg font-bold flex items-center gap-2 transform translate-y-4 group-hover:translate-y-0 transition-transform">
              <Download class="w-4 h-4" /> 查看详情
            </button>
          </div>
        </div>
        
        <div class="p-4">
          <div class="flex items-start justify-between mb-2">
            <h3 class="font-bold text-white truncate">{{ mod.title }}</h3>
            <div class="flex items-center text-yellow-400 text-xs font-bold">
              <Star class="w-3 h-3 fill-current mr-1" /> {{ mod.rating }}
            </div>
          </div>
          
          <div class="flex items-center justify-between text-xs text-gray-400">
            <span class="flex items-center gap-1"><Cpu class="w-3 h-3" /> {{ mod.author }}</span>
            <span>{{ mod.downloads }} 下载</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Workshop Detail Modal -->
    <transition name="modal">
      <div v-if="selectedItem" class="fixed inset-0 z-[100] flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-black/90 backdrop-blur-sm" @click="closeDetail"></div>
        
        <div class="relative w-full max-w-4xl bg-slate-900 rounded-2xl overflow-hidden shadow-2xl border border-white/10 animate__animated animate__zoomIn flex flex-col md:flex-row max-h-[90vh]">
          <button @click="closeDetail" class="absolute top-4 right-4 z-50 p-2 bg-black/50 text-white rounded-full hover:bg-black/80 transition-colors border border-white/10">
            <X class="w-5 h-5" />
          </button>

          <!-- Left: Image -->
          <div class="w-full md:w-5/12 h-64 md:h-auto relative overflow-hidden group">
            <img :src="selectedItem.image" class="w-full h-full object-cover transition-transform duration-700 group-hover:scale-110" />
            <div class="absolute inset-0 bg-gradient-to-t from-slate-900 to-transparent opacity-60"></div>
          </div>

          <!-- Right: Info -->
          <div class="w-full md:w-7/12 p-8 flex flex-col bg-slate-900">
            <div class="flex justify-between items-start mb-4">
              <div>
                <h2 class="text-3xl font-bold text-white mb-1">{{ selectedItem.title }}</h2>
                <p class="text-neon-green text-sm font-mono flex items-center gap-2">
                  <Cpu class="w-4 h-4" /> {{ selectedItem.author }}
                </p>
              </div>
              <div class="flex flex-col items-end">
                <div class="flex items-center gap-1 text-yellow-400 font-bold text-xl">
                  <Star class="w-5 h-5 fill-current" /> {{ selectedItem.rating }}
                </div>
                <span class="text-xs text-gray-500">{{ selectedItem.downloads }} 次下载</span>
              </div>
            </div>

            <div class="flex flex-wrap gap-2 mb-6">
              <span v-for="tag in selectedItem.tags" :key="tag" class="px-2 py-1 bg-white/5 rounded text-xs text-gray-300 border border-white/5">
                #{{ tag }}
              </span>
            </div>

            <p class="text-gray-300 leading-relaxed mb-8 text-sm flex-1 overflow-y-auto custom-scrollbar pr-2">
              {{ selectedItem.description }}
            </p>

            <div class="grid grid-cols-3 gap-4 mb-8 border-t border-b border-white/5 py-4 text-center">
              <div>
                <p class="text-xs text-gray-500 mb-1">版本</p>
                <p class="text-white font-mono text-sm">{{ selectedItem.version }}</p>
              </div>
              <div>
                <p class="text-xs text-gray-500 mb-1">大小</p>
                <p class="text-white font-mono text-sm">{{ selectedItem.size }}</p>
              </div>
              <div>
                <p class="text-xs text-gray-500 mb-1">更新于</p>
                <p class="text-white font-mono text-sm">{{ selectedItem.updated }}</p>
              </div>
            </div>

            <div class="flex gap-3">
              <button class="flex-1 py-3 bg-neon-green text-slate-900 font-bold rounded-xl hover:shadow-[0_0_15px_rgba(0,255,157,0.4)] transition-all flex items-center justify-center gap-2">
                <Download class="w-5 h-5" /> 立即订阅
              </button>
              <button class="px-4 py-3 bg-slate-800 text-white rounded-xl hover:bg-slate-700 transition-colors border border-white/10">
                <Heart class="w-5 h-5" />
              </button>
              <button class="px-4 py-3 bg-slate-800 text-white rounded-xl hover:bg-slate-700 transition-colors border border-white/10">
                <Share2 class="w-5 h-5" />
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

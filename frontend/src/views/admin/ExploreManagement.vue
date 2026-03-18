<script setup lang="ts">
import { ref } from 'vue'
import { Plus, Trash2, Upload } from 'lucide-vue-next'

const banners = ref([
  { id: 1, title: '赛博朋克 2077', image: 'https://cdn.cloudflare.steamstatic.com/steam/apps/1091500/library_hero.jpg' },
  { id: 2, title: 'AI 绘画大赛', image: 'https://images.unsplash.com/photo-1618172193763-c511deb635ca?w=800&q=80' },
])

const addBanner = () => {
  alert('模拟：已添加新 Banner')
}
</script>

<template>
  <div class="space-y-6 animate__animated animate__fadeIn">
    <div class="flex justify-between items-center">
      <h2 class="text-2xl font-bold text-white">内容推荐管理</h2>
      <button @click="addBanner" class="bg-neon-blue text-slate-900 px-4 py-2 rounded-lg font-bold flex items-center gap-2 hover:shadow-lg transition-all">
        <Plus class="w-4 h-4" /> 新增 Banner
      </button>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
      <div v-for="banner in banners" :key="banner.id" class="group relative aspect-video bg-slate-800 rounded-xl overflow-hidden border border-white/5">
        <img :src="banner.image" class="w-full h-full object-cover" />
        <div class="absolute inset-0 bg-black/50 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-4">
          <button class="p-3 bg-white/10 hover:bg-white/20 rounded-full text-white backdrop-blur">
            <Upload class="w-5 h-5" />
          </button>
          <button class="p-3 bg-red-500/80 hover:bg-red-500 rounded-full text-white backdrop-blur">
            <Trash2 class="w-5 h-5" />
          </button>
        </div>
        <div class="absolute bottom-0 left-0 w-full p-4 bg-gradient-to-t from-black/80 to-transparent">
          <p class="font-bold text-white">{{ banner.title }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

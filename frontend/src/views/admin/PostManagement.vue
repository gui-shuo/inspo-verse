<script setup lang="ts">
import { ref } from 'vue'
import { Trash2, Star } from 'lucide-vue-next'
import { useToastStore } from '@/stores/toast'
import { useModalStore } from '@/stores/modal'

const toast = useToastStore()
const modalStore = useModalStore()

const posts = ref([
  { id: 1, title: 'Inspo-Verse 社区版规 v2.0', author: 'Admin', status: 'normal', isTop: true },
  { id: 2, title: '关于 AI 绘图版权的讨论', author: 'Artist_X', status: 'normal', isTop: false },
  { id: 3, title: '出售二手显卡 4090 (骗子)', author: 'Scammer', status: 'reported', isTop: false },
  { id: 4, title: '黑神话：悟空 通关感想', author: 'Gamer_Pro', status: 'normal', isTop: true },
])

const toggleTop = (post: any) => {
  post.isTop = !post.isTop
  toast.success(post.isTop ? '帖子已置顶' : '已取消置顶')
}

const deletePost = async (id: number) => {
  const confirmed = await modalStore.open({
    title: '删除帖子',
    content: '确定要删除此贴吗？操作不可恢复。',
    type: 'error',
    confirmText: '确认删除'
  })

  if (confirmed) {
    posts.value = posts.value.filter(p => p.id !== id)
    toast.success('帖子已删除')
  }
}
</script>

<template>
  <div class="space-y-6 animate__animated animate__fadeIn">
    <h2 class="text-2xl font-bold text-white">帖子管理</h2>
    
    <div class="bg-slate-800 rounded-xl border border-white/5 overflow-hidden">
      <table class="w-full text-left text-sm">
        <thead class="bg-slate-900/50 border-b border-white/5 text-gray-400">
          <tr>
            <th class="p-4">标题</th>
            <th class="p-4">作者</th>
            <th class="p-4">状态</th>
            <th class="p-4 text-right">操作</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-white/5">
          <tr v-for="post in posts" :key="post.id" class="hover:bg-white/5 transition-colors">
            <td class="p-4 font-medium text-white">
              <span v-if="post.isTop" class="bg-red-500/20 text-red-400 text-xs px-1.5 py-0.5 rounded mr-2">置顶</span>
              {{ post.title }}
            </td>
            <td class="p-4 text-gray-400">{{ post.author }}</td>
            <td class="p-4">
              <span v-if="post.status === 'reported'" class="text-yellow-400">被举报</span>
              <span v-else class="text-green-400">正常</span>
            </td>
            <td class="p-4 text-right flex justify-end gap-2">
              <button @click="toggleTop(post)" class="p-2 hover:bg-white/10 rounded text-gray-400 hover:text-yellow-400" title="置顶/取消">
                <Star class="w-4 h-4" :class="post.isTop ? 'fill-yellow-400 text-yellow-400' : ''" />
              </button>
              <button @click="deletePost(post.id)" class="p-2 hover:bg-red-500/20 rounded text-gray-400 hover:text-red-400" title="删除">
                <Trash2 class="w-4 h-4" />
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

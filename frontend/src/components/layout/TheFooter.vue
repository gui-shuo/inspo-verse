<script setup lang="ts">
import { ref } from 'vue'
import { homeApi } from '@/api/home'
import { useToastStore } from '@/stores/toast'

const toastStore = useToastStore()
const email = ref('')
const subscribing = ref(false)
const subscribed = ref(false)

const handleSubscribe = async () => {
  const val = email.value.trim()
  if (!val) {
    toastStore.warning('请输入邮箱地址')
    return
  }
  if (!/^[\w.-]+@[\w.-]+\.[a-zA-Z]{2,}$/.test(val)) {
    toastStore.warning('请输入有效的邮箱地址')
    return
  }
  try {
    subscribing.value = true
    const res = await homeApi.subscribe(val)
    subscribed.value = true
    toastStore.success(res.message || '订阅成功！')
    email.value = ''
  } catch (e: any) {
    toastStore.error(e?.message || '订阅失败，请稍后重试')
  } finally {
    subscribing.value = false
  }
}
</script>

<template>
  <footer class="bg-slate-900 border-t border-white/10 pt-16 pb-8 mt-20">
    <div class="container mx-auto px-4">
      <div class="grid grid-cols-1 md:grid-cols-4 gap-12 mb-12">
        <div class="space-y-4">
          <h3 class="text-xl font-bold text-white">Inspo-Verse</h3>
          <p class="text-gray-400 text-sm leading-relaxed">
            连接每一个创意灵魂的泛娱乐社区。<br>在这里，灵感永不枯竭。
          </p>
        </div>
        
        <div>
          <h4 class="text-white font-bold mb-6">探索</h4>
          <ul class="space-y-3 text-sm text-gray-400">
            <li><RouterLink to="/games" class="hover:text-neon-blue transition-colors">热门游戏</RouterLink></li>
            <li><RouterLink to="/anime" class="hover:text-neon-blue transition-colors">动漫番剧</RouterLink></li>
            <li><RouterLink to="/workshop" class="hover:text-neon-blue transition-colors">创意工坊</RouterLink></li>
          </ul>
        </div>

        <div>
          <h4 class="text-white font-bold mb-6">社区</h4>
          <ul class="space-y-3 text-sm text-gray-400">
            <li><RouterLink to="/about" class="hover:text-neon-blue transition-colors">关于我们</RouterLink></li>
            <li><RouterLink to="/rules" class="hover:text-neon-blue transition-colors">社区规范</RouterLink></li>
            <li><RouterLink to="/contact" class="hover:text-neon-blue transition-colors">联系客服</RouterLink></li>
          </ul>
        </div>

        <div>
          <h4 class="text-white font-bold mb-6">订阅动态</h4>
          <div v-if="!subscribed" class="space-y-3">
            <div class="flex gap-2">
              <input
                v-model="email"
                type="email"
                placeholder="输入您的邮箱"
                class="bg-slate-800 border border-slate-700 rounded-lg px-4 py-2 text-sm w-full focus:outline-none focus:border-neon-blue transition-colors"
                @keyup.enter="handleSubscribe"
                :disabled="subscribing"
              >
              <button
                @click="handleSubscribe"
                :disabled="subscribing"
                class="p-2 px-4 bg-neon-blue rounded-lg text-slate-900 hover:bg-neon-blue/80 transition-colors font-medium text-sm whitespace-nowrap disabled:opacity-50"
              >
                {{ subscribing ? '...' : 'Go' }}
              </button>
            </div>
            <p class="text-xs text-gray-500">我们会定期发送社区精选内容到您的邮箱</p>
          </div>
          <div v-else class="text-neon-green text-sm">
            ✓ 订阅成功！感谢您的关注
          </div>
        </div>
      </div>
      
      <div class="border-t border-white/5 pt-8 text-center text-gray-500 text-sm">
        &copy; {{ new Date().getFullYear() }} Inspo-Verse. All rights reserved. Powered by creativity and AI.
      </div>
    </div>
  </footer>
</template>

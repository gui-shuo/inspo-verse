<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-950 text-white">
    <div class="text-center space-y-4">
      <template v-if="status === 'loading'">
        <div class="w-10 h-10 border-4 border-purple-500 border-t-transparent rounded-full animate-spin mx-auto"></div>
        <p class="text-gray-300">正在绑定账户，请稍候...</p>
      </template>
      <template v-else-if="status === 'success'">
        <div class="text-5xl">✅</div>
        <p class="text-lg text-green-400">绑定成功！</p>
        <p class="text-gray-400 text-sm">即将返回安全设置...</p>
      </template>
      <template v-else>
        <div class="text-5xl">❌</div>
        <p class="text-lg text-red-400">绑定失败</p>
        <p class="text-gray-400 text-sm">{{ errorMsg }}</p>
        <button
          class="mt-4 px-4 py-2 bg-purple-600 hover:bg-purple-500 rounded-lg text-sm"
          @click="goBack"
        >返回个人中心</button>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { oauthCallback } from '@/api/security'

const route = useRoute()
const router = useRouter()

const status = ref<'loading' | 'success' | 'error'>('loading')
const errorMsg = ref('')

const goBack = () => router.replace('/user?tab=security')

onMounted(async () => {
  const provider = route.params.provider as string
  const code = route.query.code as string | undefined
  const error = route.query.error as string | undefined

  if (error || !code) {
    status.value = 'error'
    errorMsg.value = error === 'access_denied' ? '您取消了授权' : (error || '未收到授权码')
    return
  }

  try {
    const res = await oauthCallback(provider, code)
    if (res.code === 0) {
      status.value = 'success'
      setTimeout(() => router.replace('/user?tab=security'), 1500)
    } else {
      status.value = 'error'
      errorMsg.value = res.message || '绑定失败，请重试'
    }
  } catch (err: any) {
    status.value = 'error'
    errorMsg.value = err?.message || '网络错误，绑定失败'
  }
})
</script>

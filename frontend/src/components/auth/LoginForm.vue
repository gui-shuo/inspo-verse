<script setup lang="ts">
import { ref } from 'vue'
import { Form, Field, ErrorMessage } from 'vee-validate'
import * as yup from 'yup'
import { Eye, EyeOff, LogIn } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import Captcha from './Captcha.vue'

const router = useRouter()
const authStore = useAuthStore()

// 状态
const showPassword = ref(false)
const loginAttempts = ref(0)
const showCaptcha = ref(false)
const captchaRef = ref<InstanceType<typeof Captcha> | null>(null)
const generatedCaptcha = ref('')
const isSubmitting = ref(false)

// 验证规则
const schema = yup.object({
  username: yup.string().required('请输入用户名').min(4, '用户名至少4位'),
  password: yup.string().required('请输入密码').min(6, '密码至少6位'),
  captcha: yup.string().test('captcha-check', '验证码错误', (value) => {
    if (!showCaptcha.value) return true
    return value?.toLowerCase() === generatedCaptcha.value.toLowerCase()
  })
})

// 处理登录
const handleLogin = async (values: any) => {
  isSubmitting.value = true
  
  // 模拟网络延迟
  await new Promise(resolve => setTimeout(resolve, 1000))

  // 模拟验证 (硬编码 admin/admin123)
  if (values.username === 'admin' && values.password === 'admin123') {
    authStore.login({
      username: 'admin',
      nickname: 'InspoAdmin',
      avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix',
      level: 'gold',
      token: 'mock-token-' + Date.now()
    })
    router.push('/')
  } else {
    loginAttempts.value++
    if (loginAttempts.value >= 5) {
      showCaptcha.value = true
      captchaRef.value?.refresh()
    }
    alert('用户名或密码错误 (试一试: admin / admin123)')
  }
  
  isSubmitting.value = false
}
</script>

<template>
  <Form :validation-schema="schema" @submit="handleLogin" class="space-y-6">
    <!-- Username -->
    <div class="space-y-2">
      <label class="text-gray-300 text-sm font-medium">用户名</label>
      <Field name="username" type="text" 
        class="w-full bg-slate-800/50 border border-slate-700 rounded-lg px-4 py-3 text-white focus:outline-none focus:border-neon-blue focus:ring-1 focus:ring-neon-blue transition-all"
        placeholder="请输入用户名" />
      <ErrorMessage name="username" class="text-red-400 text-xs" />
    </div>

    <!-- Password -->
    <div class="space-y-2">
      <label class="text-gray-300 text-sm font-medium">密码</label>
      <div class="relative">
        <Field name="password" :type="showPassword ? 'text' : 'password'" 
          class="w-full bg-slate-800/50 border border-slate-700 rounded-lg px-4 py-3 text-white focus:outline-none focus:border-neon-blue focus:ring-1 focus:ring-neon-blue transition-all pr-10"
          placeholder="请输入密码" />
        <button type="button" @click="showPassword = !showPassword" class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-white">
          <component :is="showPassword ? EyeOff : Eye" class="w-5 h-5" />
        </button>
      </div>
      <ErrorMessage name="password" class="text-red-400 text-xs" />
    </div>

    <!-- Captcha (Conditional) -->
    <div v-if="showCaptcha" class="space-y-2 animate__animated animate__fadeIn">
      <label class="text-gray-300 text-sm font-medium">验证码</label>
      <div class="flex gap-4">
        <Field name="captcha" type="text" 
          class="w-full bg-slate-800/50 border border-slate-700 rounded-lg px-4 py-3 text-white focus:outline-none focus:border-neon-blue transition-all"
          placeholder="验证码" />
        <Captcha ref="captchaRef" @update:code="c => generatedCaptcha = c" />
      </div>
      <ErrorMessage name="captcha" class="text-red-400 text-xs" />
    </div>

    <!-- Submit -->
    <button type="submit" :disabled="isSubmitting" 
      class="w-full bg-gradient-to-r from-neon-blue to-neon-purple text-white font-bold py-3 rounded-lg hover:shadow-[0_0_15px_rgba(188,19,254,0.5)] transition-all transform hover:scale-[1.02] flex justify-center items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed">
      <LogIn v-if="!isSubmitting" class="w-5 h-5" />
      <span v-if="isSubmitting" class="animate-spin w-5 h-5 border-2 border-white border-t-transparent rounded-full"></span>
      <span>{{ isSubmitting ? '登录中...' : '立即登录' }}</span>
    </button>
  </Form>
</template>

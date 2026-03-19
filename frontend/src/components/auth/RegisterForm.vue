<script setup lang="ts">
import { ref } from 'vue'
import { Form, Field, ErrorMessage } from 'vee-validate'
import * as yup from 'yup'
import { UserPlus, CheckCircle, Eye, EyeOff } from 'lucide-vue-next'
import Captcha from './Captcha.vue'
import { register } from '@/api/auth'

// 状态
const captchaRef = ref<InstanceType<typeof Captcha> | null>(null)
const generatedCaptcha = ref('')
const isSubmitting = ref(false)
const showSuccess = ref(false)
const showPassword = ref(false)
const showConfirmPassword = ref(false)
const errorMsg = ref('')

// 正则表达式
const usernameRegex = /^[a-zA-Z0-9]{4,16}$/
const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&.])[A-Za-z\d@$!%*?&.]{8,}$/
const phoneRegex = /^1[3-9]\d{9}$/

// 验证规则
const schema = yup.object({
  username: yup.string()
    .required('请输入用户名')
    .matches(usernameRegex, '用户名需4-16位字母数字组合'),

  phone: yup.string()
    .required('请输入手机号')
    .matches(phoneRegex, '请输入有效的手机号'),

  password: yup.string()
    .required('请输入密码')
    .matches(passwordRegex, '需8位以上含大小写字母、数字及符号'),

  confirmPassword: yup.string()
    .oneOf([yup.ref('password')], '两次输入密码不一致')
    .required('请确认密码'),

  captcha: yup.string()
    .test('captcha-check', '验证码错误', (value) => {
      return value?.toLowerCase() === generatedCaptcha.value.toLowerCase()
    })
    .required('请输入验证码')
})

const handleRegister = async (values: any) => {
  isSubmitting.value = true
  errorMsg.value = ''
  try {
    await register({
      username: values.username,
      phone: values.phone,
      password: values.password
    })
    showSuccess.value = true
  } catch (error: any) {
    errorMsg.value = error.message || '注册失败，请重试'
    captchaRef.value?.refresh()
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <div v-if="showSuccess" class="text-center py-10 animate__animated animate__fadeIn">
    <div class="inline-flex items-center justify-center w-20 h-20 rounded-full bg-neon-green/20 mb-6">
      <CheckCircle class="w-10 h-10 text-neon-green" />
    </div>
    <h3 class="text-2xl font-bold text-white mb-2">注册成功！</h3>
    <p class="text-gray-400 mb-6">欢迎加入 Inspo-Verse，请登录开启您的旅程。</p>
    <button @click="$emit('switch-mode')" class="text-neon-blue hover:underline">
      返回登录
    </button>
  </div>

  <Form v-else :validation-schema="schema" @submit="handleRegister" class="space-y-4">
    <!-- Username -->
    <div class="space-y-1">
      <label class="text-gray-300 text-xs font-medium">用户名</label>
      <Field name="username" type="text" class="input-field" placeholder="4-16位字母数字" />
      <ErrorMessage name="username" class="error-msg" />
    </div>

    <!-- Phone -->
    <div class="space-y-1">
      <label class="text-gray-300 text-xs font-medium">手机号</label>
      <Field name="phone" type="text" class="input-field" placeholder="请输入手机号" />
      <ErrorMessage name="phone" class="error-msg" />
    </div>

    <!-- Password -->
    <div class="space-y-1">
      <label class="text-gray-300 text-xs font-medium">密码</label>
      <div class="relative">
        <Field name="password" :type="showPassword ? 'text' : 'password'" class="input-field pr-10" placeholder="设置密码" />
        <button type="button" @click="showPassword = !showPassword" class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-white transition-colors">
          <Eye v-if="!showPassword" class="w-5 h-5" />
          <EyeOff v-else class="w-5 h-5" />
        </button>
      </div>
      <ErrorMessage name="password" class="error-msg" />
    </div>

    <!-- Confirm Password -->
    <div class="space-y-1">
      <label class="text-gray-300 text-xs font-medium">确认密码</label>
      <div class="relative">
        <Field name="confirmPassword" :type="showConfirmPassword ? 'text' : 'password'" class="input-field pr-10" placeholder="确认密码" />
        <button type="button" @click="showConfirmPassword = !showConfirmPassword" class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-white transition-colors">
          <Eye v-if="!showConfirmPassword" class="w-5 h-5" />
          <EyeOff v-else class="w-5 h-5" />
        </button>
      </div>
      <ErrorMessage name="confirmPassword" class="error-msg" />
    </div>

    <!-- Captcha -->
    <div class="space-y-1">
      <label class="text-gray-300 text-xs font-medium">验证码</label>
      <div class="flex gap-3">
        <div class="flex-1">
          <Field name="captcha" type="text" class="input-field" placeholder="请输入验证码" maxlength="4" />
        </div>
        <Captcha ref="captchaRef" @update:code="c => generatedCaptcha = c" :width="120" :height="42" />
      </div>
      <ErrorMessage name="captcha" class="error-msg" />
    </div>

    <!-- Submit -->
    <div v-if="errorMsg" class="text-red-400 text-sm text-center bg-red-400/10 border border-red-400/30 rounded-lg py-2 px-3">
      {{ errorMsg }}
    </div>
    <button type="submit" :disabled="isSubmitting" 
      class="w-full mt-4 bg-slate-800 border border-neon-blue/50 text-neon-blue font-bold py-3 rounded-lg hover:bg-neon-blue hover:text-slate-900 transition-all flex justify-center items-center gap-2">
      <UserPlus v-if="!isSubmitting" class="w-5 h-5" />
      <span v-if="isSubmitting" class="animate-spin w-5 h-5 border-2 border-current border-t-transparent rounded-full"></span>
      <span>{{ isSubmitting ? '创建账号' : '立即注册' }}</span>
    </button>
  </Form>
</template>

<style scoped>
.input-field {
  @apply w-full bg-slate-800/50 border border-slate-700 rounded-lg px-4 py-3 text-white text-sm focus:outline-none focus:border-neon-blue focus:ring-1 focus:ring-neon-blue transition-all;
}
.error-msg {
  @apply text-red-400 text-xs block mt-1;
}
</style>

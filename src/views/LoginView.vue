<script setup lang="ts">
import { ref } from 'vue'
import LoginForm from '@/components/auth/LoginForm.vue'
import RegisterForm from '@/components/auth/RegisterForm.vue'

const isLoginMode = ref(true)

const switchMode = () => {
  isLoginMode.value = !isLoginMode.value
}
</script>

<template>
  <div class="min-h-[calc(100vh-80px)] flex items-center justify-center py-10 px-4">
    <div class="w-full max-w-md">
      <!-- Card Container -->
      <div class="relative bg-slate-900/60 backdrop-blur-xl border border-white/10 rounded-2xl shadow-[0_0_50px_rgba(0,0,0,0.5)] overflow-hidden">
        
        <!-- Header Decoration -->
        <div class="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-neon-blue via-neon-purple to-neon-pink"></div>

        <div class="p-8">
          <div class="text-center mb-8">
            <h2 class="text-3xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-white to-gray-400 mb-2">
              {{ isLoginMode ? '欢迎回来' : '加入 Inspo-Verse' }}
            </h2>
            <p class="text-gray-400 text-sm">
              {{ isLoginMode ? '登录以继续您的创作之旅' : '探索无限灵感的世界' }}
            </p>
          </div>

          <!-- Forms with Transition -->
          <transition 
            mode="out-in"
            enter-active-class="animate__animated animate__fadeIn animate__faster"
            leave-active-class="animate__animated animate__fadeOut animate__faster"
          >
            <component 
              :is="isLoginMode ? LoginForm : RegisterForm" 
              @switch-mode="isLoginMode = true"
            />
          </transition>

          <!-- Toggle -->
          <div class="mt-8 pt-6 border-t border-white/5 text-center">
            <p class="text-gray-400 text-sm">
              {{ isLoginMode ? '还没有账号？' : '已有账号？' }}
              <button 
                @click="switchMode" 
                class="text-neon-blue font-medium hover:text-neon-purple transition-colors ml-1"
              >
                {{ isLoginMode ? '立即注册' : '直接登录' }}
              </button>
            </p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

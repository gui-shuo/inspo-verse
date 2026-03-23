<script setup lang="ts">
import { defineAsyncComponent, onMounted, onUnmounted, ref } from 'vue'
import { RouterView } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/index'
import TheHeader from '@/components/layout/TheHeader.vue'
import CyberBackground from '@/components/ui/CyberBackground.vue'
import ToastContainer from '@/components/ui/ToastContainer.vue'
import GlobalModal from '@/components/ui/GlobalModal.vue'

// 核心考核点：采用组件懒加载策略
const TheFooter = defineAsyncComponent(() => import('@/components/layout/TheFooter.vue'))

const authStore = useAuthStore()
const appStore = useAppStore()

// 鼠标光晕效果
const mouseX = ref(0)
const mouseY = ref(0)
const handleMouseMove = (e: MouseEvent) => {
  mouseX.value = e.clientX
  mouseY.value = e.clientY
}

onMounted(() => {
  authStore.initAuth()
  window.addEventListener('mousemove', handleMouseMove)
})

onUnmounted(() => {
  window.removeEventListener('mousemove', handleMouseMove)
})
</script>

<template>
  <div class="min-h-screen flex flex-col relative overflow-x-hidden bg-slate-900 text-white">
    <!-- 动态粒子星空背景 -->
    <CyberBackground />
    <ToastContainer />
    <GlobalModal />
    
    <!-- 鼠标跟随光晕 -->
    <div 
      class="pointer-events-none fixed inset-0 z-0 transition-opacity duration-300"
      :style="{
        background: appStore.theme === 'dark'
          ? `radial-gradient(600px circle at ${mouseX}px ${mouseY}px, rgba(29, 78, 216, 0.15), transparent 40%)`
          : `radial-gradient(600px circle at ${mouseX}px ${mouseY}px, rgba(168, 85, 247, 0.12), transparent 40%)`
      }"
    ></div>

    <!-- 导航栏 -->
    <TheHeader />

    <!-- 光效装饰 (保留，增强层次感) -->
    <div class="fixed top-0 left-0 w-full h-full pointer-events-none z-0">
      <div class="absolute top-[-20%] left-[-10%] w-[50%] h-[50%] bg-neon-blue/5 rounded-full blur-[150px] animate-pulse"></div>
      <div class="absolute bottom-[-20%] right-[-10%] w-[50%] h-[50%] bg-neon-purple/5 rounded-full blur-[150px] animate-pulse"></div>
    </div>


    <!-- 路由视图 -->
    <main class="relative z-10 flex-1 pt-20">
      <router-view v-slot="{ Component }">
        <transition 
          name="fade-scale" 
          mode="out-in"
          enter-active-class="animate__animated animate__fadeIn animate__faster"
          leave-active-class="animate__animated animate__fadeOut animate__faster"
        >
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <!-- 异步加载的页脚 -->
    <TheFooter class="relative z-10" />
  </div>
</template>

<style>
/* 可以在这里添加一些特定的全局过渡样式 */
</style>

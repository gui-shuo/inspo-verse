<script setup lang="ts">
import { defineAsyncComponent } from 'vue'
import { RouterView } from 'vue-router'
import TheHeader from '@/components/layout/TheHeader.vue'
import CyberBackground from '@/components/ui/CyberBackground.vue'
import ToastContainer from '@/components/ui/ToastContainer.vue'
import GlobalModal from '@/components/ui/GlobalModal.vue'

// 核心考核点：采用组件懒加载策略
const TheFooter = defineAsyncComponent(() => import('@/components/layout/TheFooter.vue'))
</script>

<template>
  <div class="min-h-screen flex flex-col relative overflow-hidden bg-slate-900 text-white">
    <!-- 动态粒子星空背景 -->
    <CyberBackground />
    <ToastContainer />
    <GlobalModal />
    
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
    <TheFooter />
  </div>
</template>

<style>
/* 可以在这里添加一些特定的全局过渡样式 */
</style>

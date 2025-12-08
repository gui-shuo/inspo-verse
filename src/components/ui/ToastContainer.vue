<script setup lang="ts">
import { useToastStore } from '@/stores/toast'
import { CheckCircle, AlertCircle, Info, XCircle, X } from 'lucide-vue-next'
import { storeToRefs } from 'pinia'

const toastStore = useToastStore()
const { toasts } = storeToRefs(toastStore)

const getIcon = (type: string) => {
  switch (type) {
    case 'success': return CheckCircle
    case 'error': return XCircle
    case 'warning': return AlertCircle
    default: return Info
  }
}

const getStyles = (type: string) => {
  switch (type) {
    case 'success': return 'border-neon-green/50 bg-slate-900/90 text-neon-green shadow-[0_0_15px_rgba(10,255,0,0.2)]'
    case 'error': return 'border-red-500/50 bg-slate-900/90 text-red-400 shadow-[0_0_15px_rgba(239,68,68,0.2)]'
    case 'warning': return 'border-yellow-500/50 bg-slate-900/90 text-yellow-400 shadow-[0_0_15px_rgba(234,179,8,0.2)]'
    default: return 'border-neon-blue/50 bg-slate-900/90 text-neon-blue shadow-[0_0_15px_rgba(0,243,255,0.2)]'
  }
}
</script>

<template>
  <div class="fixed top-24 right-4 z-[100] flex flex-col gap-3 pointer-events-none">
    <transition-group 
      enter-active-class="animate__animated animate__fadeInRight animate__faster"
      leave-active-class="animate__animated animate__fadeOutRight animate__faster"
    >
      <div 
        v-for="toast in toasts" 
        :key="toast.id"
        class="pointer-events-auto flex items-center gap-3 min-w-[300px] p-4 rounded-xl border backdrop-blur-md"
        :class="getStyles(toast.type)"
      >
        <component :is="getIcon(toast.type)" class="w-5 h-5 flex-shrink-0" />
        <p class="text-sm font-medium flex-1 text-white">{{ toast.message }}</p>
        <button @click="toastStore.remove(toast.id)" class="text-white/50 hover:text-white transition-colors">
          <X class="w-4 h-4" />
        </button>
      </div>
    </transition-group>
  </div>
</template>

<script setup lang="ts">
import { useModalStore } from '@/stores/modal'
import { storeToRefs } from 'pinia'
import { AlertTriangle, CheckCircle, Info, XCircle, X } from 'lucide-vue-next'

const modalStore = useModalStore()
const { isOpen, options } = storeToRefs(modalStore)

const getIcon = () => {
  switch (options.value.type) {
    case 'warning': return AlertTriangle
    case 'success': return CheckCircle
    case 'error': return XCircle
    default: return Info
  }
}

const getColorClass = () => {
  switch (options.value.type) {
    case 'warning': return 'text-yellow-400'
    case 'success': return 'text-green-400'
    case 'error': return 'text-red-400'
    default: return 'text-neon-blue'
  }
}
</script>

<template>
  <teleport to="body">
    <transition name="modal-fade">
      <div v-if="isOpen" class="fixed inset-0 z-[999] flex items-center justify-center px-4">
        <!-- Backdrop -->
        <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" @click="modalStore.cancel"></div>

        <!-- Modal Content -->
        <div class="relative w-full max-w-md bg-slate-800 rounded-2xl border border-white/10 shadow-2xl overflow-hidden transform transition-all">
          <!-- Header -->
          <div class="flex justify-between items-center p-6 border-b border-white/5">
            <h3 class="text-xl font-bold text-white flex items-center gap-2">
              <component :is="getIcon()" class="w-6 h-6" :class="getColorClass()" />
              {{ options.title }}
            </h3>
            <button @click="modalStore.cancel" class="text-gray-400 hover:text-white transition-colors">
              <X class="w-5 h-5" />
            </button>
          </div>

          <!-- Body -->
          <div class="p-6">
            <p class="text-gray-300 leading-relaxed text-base">
              {{ options.content }}
            </p>
          </div>

          <!-- Footer -->
          <div class="p-6 pt-0 flex justify-end gap-3">
            <button 
              v-if="options.showCancel"
              @click="modalStore.cancel"
              class="px-5 py-2.5 rounded-lg border border-white/10 text-gray-300 hover:bg-white/5 transition-colors font-medium"
            >
              {{ options.cancelText }}
            </button>
            <button 
              @click="modalStore.confirm"
              class="px-5 py-2.5 rounded-lg font-bold text-slate-900 transition-all hover:shadow-lg"
              :class="options.type === 'error' ? 'bg-red-500 hover:bg-red-400' : 'bg-neon-blue hover:bg-neon-blue/90'"
            >
              {{ options.confirmText }}
            </button>
          </div>
        </div>
      </div>
    </transition>
  </teleport>
</template>

<style scoped>
.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: all 0.3s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

.modal-fade-enter-from .relative,
.modal-fade-leave-to .relative {
  transform: scale(0.95) translateY(10px);
  opacity: 0;
}
</style>

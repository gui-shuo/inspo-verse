import { defineStore } from 'pinia'
import { ref } from 'vue'
import { nanoid } from 'nanoid'

export type ToastType = 'success' | 'error' | 'info' | 'warning'

export interface Toast {
  id: string
  message: string
  type: ToastType
  duration?: number
}

export const useToastStore = defineStore('toast', () => {
  const toasts = ref<Toast[]>([])

  function add(message: string, type: ToastType = 'info', duration = 3000) {
    const id = Date.now().toString() + Math.random().toString() // Simple ID generation
    toasts.value.push({ id, message, type, duration })

    if (duration > 0) {
      setTimeout(() => {
        remove(id)
      }, duration)
    }
  }

  function remove(id: string) {
    const index = toasts.value.findIndex(t => t.id === id)
    if (index !== -1) {
      toasts.value.splice(index, 1)
    }
  }

  function success(msg: string) { add(msg, 'success') }
  function error(msg: string) { add(msg, 'error') }
  function warning(msg: string) { add(msg, 'warning') }
  function info(msg: string) { add(msg, 'info') }

  return { toasts, add, remove, success, error, warning, info }
})

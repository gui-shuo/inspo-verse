import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface ModalOptions {
  title?: string
  content: string
  confirmText?: string
  cancelText?: string
  type?: 'info' | 'warning' | 'error' | 'success'
  showCancel?: boolean
}

export const useModalStore = defineStore('modal', () => {
  const isOpen = ref(false)
  const options = ref<ModalOptions>({ content: '' })
  const resolvePromise = ref<((value: boolean) => void) | null>(null)

  function open(opts: ModalOptions): Promise<boolean> {
    options.value = {
      title: '提示',
      confirmText: '确定',
      cancelText: '取消',
      type: 'info',
      showCancel: true,
      ...opts
    }
    isOpen.value = true
    
    return new Promise((resolve) => {
      resolvePromise.value = resolve
    })
  }

  function confirm() {
    isOpen.value = false
    if (resolvePromise.value) {
      resolvePromise.value(true)
      resolvePromise.value = null
    }
  }

  function cancel() {
    isOpen.value = false
    if (resolvePromise.value) {
      resolvePromise.value(false)
      resolvePromise.value = null
    }
  }

  return { isOpen, options, open, confirm, cancel }
})

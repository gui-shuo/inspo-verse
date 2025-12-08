import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const theme = ref<'dark' | 'light'>('dark')
  
  function toggleTheme() {
    theme.value = theme.value === 'dark' ? 'light' : 'dark'
    // 这里可以添加持久化逻辑
  }

  return { theme, toggleTheme }
})

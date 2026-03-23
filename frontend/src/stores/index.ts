import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export type ThemeMode = 'dark' | 'light'

export const useAppStore = defineStore('app', () => {
  // 从 localStorage 读取用户选择的主题，默认暗色
  const saved = localStorage.getItem('inspo-theme') as ThemeMode | null
  const theme = ref<ThemeMode>(saved || 'dark')

  // 应用主题到 <html> 元素
  function applyTheme(t: ThemeMode) {
    const html = document.documentElement
    if (t === 'light') {
      html.classList.add('light-theme')
      html.classList.remove('dark-theme')
    } else {
      html.classList.add('dark-theme')
      html.classList.remove('light-theme')
    }
  }

  function toggleTheme() {
    theme.value = theme.value === 'dark' ? 'light' : 'dark'
  }

  function setTheme(t: ThemeMode) {
    theme.value = t
  }

  // 监听主题变化，持久化并应用
  watch(theme, (val) => {
    localStorage.setItem('inspo-theme', val)
    applyTheme(val)
  }, { immediate: true })

  return { theme, toggleTheme, setTheme, applyTheme }
})

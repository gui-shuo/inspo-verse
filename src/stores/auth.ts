import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import http, { unwrapResponse } from '@/api/http'

export interface User {
  username: string
  nickname: string
  avatar: string
  level: 'normal' | 'silver' | 'gold'
  token: string
}

export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)
  const token = ref<string | null>(localStorage.getItem('token'))

  const isAuthenticated = computed(() => !!token.value)

  function login(userData: User) {
    user.value = userData
    token.value = userData.token
    localStorage.setItem('token', userData.token)
    // 模拟从后端获取用户信息并缓存
    localStorage.setItem('user_info', JSON.stringify(userData))
  }

  async function loginWithPassword(username: string, password: string) {
    try {
      const data = await unwrapResponse<{ accessToken: string }>(
        http.post('/auth/login', { username, password })
      )
      token.value = data.accessToken
      localStorage.setItem('token', data.accessToken)
      return true
    } catch {
      return false
    }
  }

  function logout() {
    user.value = null
    token.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user_info')
    window.location.href = '/login'
  }

  // 初始化检查
  function initAuth() {
    const storedToken = localStorage.getItem('token')
    const storedUser = localStorage.getItem('user_info')
    if (storedToken && storedUser) {
      token.value = storedToken
      user.value = JSON.parse(storedUser)
    }
  }

  return { user, token, isAuthenticated, login, loginWithPassword, logout, initAuth }
})

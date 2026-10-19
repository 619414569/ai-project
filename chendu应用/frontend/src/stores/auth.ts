import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { authApi } from '@/api/modules/auth'
import { TOKEN_KEY } from '@/api/http'
import type { LoginRequest, UserProfile } from '@/types/api'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) ?? '')
  const profile = ref<UserProfile | null>(null)
  const isAuthenticated = computed(() => Boolean(token.value))

  async function login(credentials: LoginRequest) {
    const result = await authApi.login(credentials)
    token.value = result.accessToken
    localStorage.setItem(TOKEN_KEY, result.accessToken)
    profile.value = await authApi.me()
  }

  async function restore() {
    if (!token.value || profile.value) return
    try {
      profile.value = await authApi.me()
    } catch {
      logout()
    }
  }

  function logout() {
    token.value = ''
    profile.value = null
    localStorage.removeItem(TOKEN_KEY)
  }

  return { token, profile, isAuthenticated, login, restore, logout }
})

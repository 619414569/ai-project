import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { authApi } from '@/api/modules/auth'
import { TOKEN_KEY } from '@/api/http'
import type { ChangePasswordRequest, LoginRequest, UserProfile } from '@/types/api'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) ?? '')
  const profile = ref<UserProfile | null>(null)
  const passwordChangeRequired = computed(() => profile.value?.passwordChangeRequired ?? false)
  const isAuthenticated = computed(() => Boolean(token.value))

  async function login(credentials: LoginRequest) {
    // A stale token must never turn the public login request into an authenticated request.
    logout()
    const result = await authApi.login(credentials)
    token.value = result.accessToken
    localStorage.setItem(TOKEN_KEY, result.accessToken)
    try {
      profile.value = await authApi.me()
    } catch (error) {
      logout()
      throw error
    }
  }

  async function restore() {
    if (!token.value || profile.value) return
    try {
      profile.value = await authApi.me()
    } catch {
      logout()
    }
  }

  async function changePassword(request: ChangePasswordRequest) {
    await authApi.changePassword(request)
    if (profile.value) profile.value.passwordChangeRequired = false
  }

  function logout() {
    token.value = ''
    profile.value = null
    localStorage.removeItem(TOKEN_KEY)
  }

  return {
    token,
    profile,
    isAuthenticated,
    passwordChangeRequired,
    login,
    restore,
    changePassword,
    logout,
  }
})

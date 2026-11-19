import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { authApi } from '@/api/auth'
import type { AuthUser, LoginPayload, Role } from '@/api/types'
import { clearSession, readUser, writeSession, writeUser } from '@/utils/session'

/**
 * Authentication state. The token itself lives in localStorage so the Axios
 * interceptor can read it without touching Pinia; this store mirrors the profile
 * and exposes the role helpers used by the router and the layout.
 */
export const useAuthStore = defineStore('auth', () => {
  const stored = readUser()

  const user = ref<AuthUser | null>(stored)

  const role = computed<Role | null>(() => user.value?.role ?? null)

  const isAuthenticated = computed(() => user.value !== null)

  const isSuperAdmin = computed(() => role.value === 'SUPER_ADMIN')

  const canManage = computed(() => role.value === 'SUPER_ADMIN' || role.value === 'TENANT_ADMIN')

  const displayName = computed(() => user.value?.nickname || user.value?.username || 'Guest')

  const initials = computed(() => displayName.value.slice(0, 1).toUpperCase())

  async function login(payload: LoginPayload): Promise<AuthUser> {
    const result = await authApi.login(payload)
    user.value = result.user
    writeSession(result.accessToken, result.user)
    return result.user
  }

  async function fetchProfile(): Promise<void> {
    const profile = await authApi.me()
    user.value = profile
    writeUser(profile)
  }

  async function logout(): Promise<void> {
    try {
      await authApi.logout()
    } finally {
      reset()
    }
  }

  function reset(): void {
    user.value = null
    clearSession()
  }

  return {
    user,
    role,
    isAuthenticated,
    isSuperAdmin,
    canManage,
    displayName,
    initials,
    login,
    fetchProfile,
    logout,
    reset
  }
})

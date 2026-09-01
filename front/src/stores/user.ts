import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserInfo, RegisterRequest } from '@/types'
import { post, get } from '@/utils/http'

export const useUserStore = defineStore('user', () => {
  const userInfo = ref<UserInfo | null>(null)
  const token = ref(localStorage.getItem('token') || '')

  const isLoggedIn = computed(() => !!token.value)

  const login = async (account: string, password: string): Promise<{ code: number; message: string }> => {
    const result = await post<{ token: string; user: UserInfo }>('/users/login', { account, password })
    if (result.code === 200) {
      token.value = result.data!.token
      userInfo.value = result.data!.user
      localStorage.setItem('token', token.value)
    }
    return { code: result.code, message: result.message }
  }

  // 仅清除本地会话状态（用于 token 失效等被动场景，不请求后端）
  const clearLocal = () => {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
  }

  // 主动退出：先通知后端（保留 token 供接口鉴权），无论成败均清除本地状态
  const logout = async (): Promise<void> => {
    try {
      await post('/users/logout')
    } catch {
      /* ignore */
    }
    clearLocal()
  }

  const register = async (data: RegisterRequest): Promise<{ code: number; message: string }> => {
    const result = await post('/users/register', data as unknown as Record<string, unknown>)
    return { code: result.code, message: result.message }
  }

  const getUserInfo = async (forceRefresh = false): Promise<void> => {
    if (!forceRefresh && userInfo.value) {
      return
    }
    try {
      const result = await get<UserInfo>('/users/info')
      if (result.code === 200) {
        userInfo.value = result.data!
      } else if (result.code === 401) {
        logout()
      }
    } catch {
      userInfo.value = null
    }
  }

  const setUserInfo = (u: UserInfo) => {
    userInfo.value = u
  }

  const refreshUserInfo = async (): Promise<boolean> => {
    try {
      const result = await get<UserInfo>('/users/info')
      if (result.code === 200 && result.data) {
        userInfo.value = result.data
        return true
      }
    } catch {
      /* ignore */
    }
    return false
  }

  const sendCode = async (email: string, userNo: string, username: string): Promise<{ code: number; message: string; data: unknown }> => {
    const result = await post('/users/getregcode', { email, userNo, username })
    return { code: result.code, message: result.message, data: result.data }
  }

  window.addEventListener('auth-expired', () => {
    token.value = ''
    userInfo.value = null
  })

  return {
    userInfo,
    token,
    isLoggedIn,
    login,
    logout,
    clearLocal,
    register,
    getUserInfo,
    setUserInfo,
    refreshUserInfo,
    sendCode
  }
})

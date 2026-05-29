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

  const logout = () => {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
  }

  const register = async (data: RegisterRequest): Promise<{ code: number; message: string }> => {
    const result = await post('/users/register', data as unknown as Record<string, unknown>)
    return { code: result.code, message: result.message }
  }

  const getUserInfo = async (): Promise<void> => {
    const result = await get<UserInfo>('/users/info')
    if (result.code === 200) {
      userInfo.value = result.data!
    }
  }

  const sendCode = async (email: string, userNo: string, username: string): Promise<{ code: number; message: string }> => {
    const result = await post('/users/getregcode', { email, userNo, username })
    return { code: result.code, message: result.message }
  }

  return {
    userInfo,
    token,
    isLoggedIn,
    login,
    logout,
    register,
    getUserInfo,
    sendCode
  }
})

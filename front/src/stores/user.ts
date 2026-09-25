import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserInfo, RegisterRequest } from '@/types'
import { post, get, del } from '@/utils/http'
import { useSchoolStore } from '@/stores/school'

export const useUserStore = defineStore('user', () => {
  const userInfo = ref<UserInfo | null>(null)
  const token = ref(localStorage.getItem('token') || '')

  const isLoggedIn = computed(() => !!token.value)

  // 有效权限节点列表
  const permissions = computed(() => userInfo.value?.permissions ?? [])
  // 是否为平台管理员（OP）：拥有全部权限节点
  const isOp = computed(() => userInfo.value?.isOp === true)
  // 是否拥有任一管理权限（用于是否展示管理面板入口）
  const isAdmin = computed(() => isOp.value || permissions.value.length > 0)
  // 是否拥有指定权限节点
  const hasPermission = (node: string) => isOp.value || permissions.value.includes(node)
  // 是否拥有其中任意一个权限节点
  const hasAnyPermission = (nodes: string[]) =>
    isOp.value || nodes.some(node => permissions.value.includes(node))

  const login = async (account: string, password: string): Promise<{ code: number; message: string }> => {
    const result = await post<{ token: string; user: UserInfo }>('/users/login', { account, password })
    if (result.code === 200) {
      token.value = result.data!.token
      userInfo.value = result.data!.user
      localStorage.setItem('token', token.value)
      // 换账号登录时清空上一个账号的学校身份
      useSchoolStore().clear()
    }
    return { code: result.code, message: result.message }
  }

  // 仅清除本地会话状态（用于 token 失效等被动场景，不请求后端）
  const clearLocal = () => {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    useSchoolStore().clear()
  }

  // 主动退出：后端登出成功后清除本地状态并返回后端提示内容，失败返回 null；401 由响应拦截器统一处理会话过期
  const logout = async (): Promise<string | null> => {
    try {
      const res = await post('/users/logout')
      if (res.code === 200) {
        clearLocal()
        return res.message
      }
      return null
    } catch {
      return null
    }
  }

  // 注销账号（不可逆）：凭登录密码验证身份；成功后后端已删除账号，本地清除会话
  const deleteAccount = async (password: string): Promise<{ code: number; message: string }> => {
    const res = await del('/users/account', { password })
    if (res.code === 200) {
      clearLocal()
    }
    return { code: res.code, message: res.message }
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
      if (result.code === 200 && result.data) {
        userInfo.value = result.data
        return
      }
      // 认证已失效（Token 无效 / 账号不存在或已被删除）时清理本地会话
      if (result.code === 401 || result.code === 404) {
        clearLocal()
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

  const sendCode = async (email: string, username: string): Promise<{ code: number; message: string; data: unknown }> => {
    const result = await post('/users/getregcode', { email, username })
    return { code: result.code, message: result.message, data: result.data }
  }

  window.addEventListener('auth-expired', () => {
    token.value = ''
    userInfo.value = null
    useSchoolStore().clear()
  })

  return {
    userInfo,
    token,
    isLoggedIn,
    permissions,
    isOp,
    isAdmin,
    hasPermission,
    hasAnyPermission,
    login,
    logout,
    deleteAccount,
    clearLocal,
    register,
    getUserInfo,
    setUserInfo,
    refreshUserInfo,
    sendCode
  }
})

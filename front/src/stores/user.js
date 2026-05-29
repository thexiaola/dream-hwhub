import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import http, { setToken, setUser, removeToken, removeUser, getUser } from '../utils/http'

export const useUserStore = defineStore('user', () => {
  const user = ref(getUser())

  const isLoggedIn = computed(() => !!user.value)
  const isAdmin = computed(() => user.value?.permission >= 100)

  async function login(account, password) {
    const response = await http.post('/users/login', { account, password })
    if (response.code === 200) {
      user.value = response.data
      setToken(response.data.token)
      setUser(response.data)
    }
    return response
  }

  async function register(data) {
    return await http.post('/users/register', data)
  }

  async function logout() {
    const response = await http.post('/users/logout')
    if (response.code === 200) {
      user.value = null
      removeToken()
      removeUser()
    }
    return response
  }

  async function sendRegisterCode(data) {
    return await http.post('/users/getregcode', data)
  }

  async function sendRetrieveCode(account) {
    return await http.post('/users/retrieve/sendcode', { account })
  }

  async function resetPassword(data) {
    return await http.put('/users/retrieve/resetpassword', data)
  }

  async function modifyUserInfo(username) {
    const response = await http.put('/users/modify/info', { username })
    if (response.code === 200) {
      user.value = response.data
      setUser(response.data)
    }
    return response
  }

  async function modifyPassword(oldPassword, newPassword) {
    return await http.put('/users/modify/password', { oldPassword, newPassword })
  }

  async function sendOldEmailCode() {
    return await http.post('/users/modify/getmodifycode/before')
  }

  async function sendNewEmailCode(newEmail) {
    return await http.post('/users/modify/getmodifycode/after', { newEmail })
  }

  async function modifyEmail(data) {
    const response = await http.put('/users/modify/email', data)
    if (response.code === 200) {
      user.value = response.data
      setUser(response.data)
    }
    return response
  }

  return {
    user,
    isLoggedIn,
    isAdmin,
    login,
    register,
    logout,
    sendRegisterCode,
    sendRetrieveCode,
    resetPassword,
    modifyUserInfo,
    modifyPassword,
    sendOldEmailCode,
    sendNewEmailCode,
    modifyEmail
  }
})
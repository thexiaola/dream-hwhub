import axios from 'axios'
import CryptoJS from 'crypto-js'

const CSRF_SECRET = 'dream-hwhub-csrf-secret'

const axiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000
})

axiosInstance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      const csrfToken = generateCsrfToken(token)
      config.headers['Authorization'] = `Bearer ${token}`
      config.headers['X-CSRF-Token'] = csrfToken
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

axiosInstance.interceptors.response.use(
  (response) => {
    return response.data
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response
      if (status === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        const currentPath = window.location.pathname
        if (currentPath !== '/login') {
          window.location.href = '/login'
        }
      }
      return Promise.reject(data || error.message)
    }
    return Promise.reject(error.message)
  }
)

function generateCsrfToken(jwtToken) {
  return CryptoJS.HmacSHA256(jwtToken, CSRF_SECRET).toString(CryptoJS.enc.Base64)
}

export function setToken(token) {
  localStorage.setItem('token', token)
}

export function getToken() {
  return localStorage.getItem('token')
}

export function removeToken() {
  localStorage.removeItem('token')
}

export function setUser(user) {
  localStorage.setItem('user', JSON.stringify(user))
}

export function getUser() {
  const user = localStorage.getItem('user')
  return user ? JSON.parse(user) : null
}

export function removeUser() {
  localStorage.removeItem('user')
}

export function isLoggedIn() {
  return !!localStorage.getItem('token')
}

export default axiosInstance
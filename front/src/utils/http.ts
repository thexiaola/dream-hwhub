import axios, { type AxiosInstance, type InternalAxiosRequestConfig, type AxiosResponse } from 'axios'
import type { ApiResponse } from '@/types'

const instance: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

const JWT_SECRET = 'DreamHwhub2026SecureJWTSecretKeyForProductionUseOnly'

async function generateCsrfToken(jwtToken: string): Promise<string> {
  const encoder = new TextEncoder()
  const keyData = encoder.encode(JWT_SECRET)
  const messageData = encoder.encode(jwtToken)
  
  const key = await crypto.subtle.importKey(
    'raw',
    keyData,
    { name: 'HMAC', hash: 'SHA-256' },
    false,
    ['sign']
  )
  
  const signature = await crypto.subtle.sign('HMAC', key, messageData)
  
  // Convert to base64 URL-safe without padding
  const bytes = new Uint8Array(signature)
  let binary = ''
  for (let i = 0; i < bytes.byteLength; i++) {
    binary += String.fromCharCode(bytes[i])
  }
  const base64 = btoa(binary)
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '')
  return base64
}

instance.interceptors.request.use(
  async (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers = config.headers || {}
      config.headers['Authorization'] = `Bearer ${token}`
      
      const method = config.method?.toUpperCase()
      if (method && ['POST', 'PUT', 'DELETE', 'PATCH'].includes(method)) {
        try {
          const csrfToken = await generateCsrfToken(token)
          config.headers['X-CSRF-Token'] = csrfToken
        } catch (e) {
          console.error('Failed to generate CSRF token:', e)
        }
      }
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

instance.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    return response.data
  },
  (error) => {
    if (error.response?.data) {
      return error.response.data
    }
    return { code: -1, message: '网络请求失败', data: null }
  }
)

export const get = <T = null>(url: string, params?: Record<string, unknown>): Promise<ApiResponse<T>> => {
  return instance.get(url, { params })
}

export const post = <T = null>(url: string, data?: Record<string, unknown>, params?: Record<string, unknown>): Promise<ApiResponse<T>> => {
  return instance.post(url, data, { params })
}

export const put = <T = null>(url: string, data?: Record<string, unknown>, params?: Record<string, unknown>): Promise<ApiResponse<T>> => {
  return instance.put(url, data, { params })
}

export const patch = <T = null>(url: string, data?: Record<string, unknown>): Promise<ApiResponse<T>> => {
  return instance.patch(url, data)
}

export const del = <T = null>(url: string, params?: Record<string, unknown>): Promise<ApiResponse<T>> => {
  return instance.delete(url, { params })
}

export default instance
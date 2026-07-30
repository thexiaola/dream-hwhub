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
    return response
  },
  (error) => {
    if (error.response) {
      const status = error.response.status
      const data = error.response.data
      
      if (status === 401 || (data && data.code === 401)) {
        localStorage.removeItem('token')
        window.dispatchEvent(new CustomEvent('auth-expired'))
        if (!window.location.pathname.includes('/login')) {
          window.location.href = '/login'
        }
        return { data: { code: 401, message: '登录已过期，请重新登录', data: null } } as AxiosResponse<ApiResponse>
      }
      
      if (data) {
        return error.response
      }
    }
    return { data: { code: -1, message: '网络请求失败', data: null } } as AxiosResponse<ApiResponse>
  }
)

export const get = <T = null>(url: string, params?: Record<string, unknown>): Promise<ApiResponse<T>> => {
  return instance.get<ApiResponse<T>>(url, { params }).then(res => res.data)
}

export const post = <T = null>(url: string, data?: Record<string, unknown>, params?: Record<string, unknown>): Promise<ApiResponse<T>> => {
  return instance.post<ApiResponse<T>>(url, data, { params }).then(res => res.data)
}

export const postForm = <T = null>(url: string, formData: FormData): Promise<ApiResponse<T>> => {
  return instance.post<ApiResponse<T>>(url, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }).then(res => res.data)
}

export const putForm = <T = null>(url: string, formData: FormData): Promise<ApiResponse<T>> => {
  return instance.put<ApiResponse<T>>(url, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }).then(res => res.data)
}

export const put = <T = null>(url: string, data?: Record<string, unknown>, params?: Record<string, unknown>): Promise<ApiResponse<T>> => {
  return instance.put<ApiResponse<T>>(url, data, { params }).then(res => res.data)
}

export const patch = <T = null>(url: string, data?: Record<string, unknown>): Promise<ApiResponse<T>> => {
  return instance.patch<ApiResponse<T>>(url, data).then(res => res.data)
}

export const del = <T = null>(url: string, data?: Record<string, unknown> | unknown[], params?: Record<string, unknown>): Promise<ApiResponse<T>> => {
  return instance.delete<ApiResponse<T>>(url, { data, params }).then(res => res.data)
}

export default instance
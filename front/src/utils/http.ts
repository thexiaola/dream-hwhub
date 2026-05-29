import axios, { type AxiosInstance, type InternalAxiosRequestConfig, type AxiosResponse } from 'axios'
import type { ApiResponse } from '@/types'

const instance: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

instance.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers = config.headers || {}
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

instance.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    return { data: response.data } as AxiosResponse<ApiResponse>
  },
  (error) => {
    const response = error.response?.data as ApiResponse
    if (response) {
      return Promise.reject(response)
    }
    return Promise.reject({ code: -1, message: '网络请求失败', data: null })
  }
)

export const get = <T = null>(url: string, params?: Record<string, unknown>): Promise<ApiResponse<T>> => {
  return instance.get(url, { params })
}

export const post = <T = null>(url: string, data?: Record<string, unknown>): Promise<ApiResponse<T>> => {
  return instance.post(url, data)
}

export const put = <T = null>(url: string, data?: Record<string, unknown>): Promise<ApiResponse<T>> => {
  return instance.put(url, data)
}

export const patch = <T = null>(url: string, data?: Record<string, unknown>): Promise<ApiResponse<T>> => {
  return instance.patch(url, data)
}

export const del = <T = null>(url: string, params?: Record<string, unknown>): Promise<ApiResponse<T>> => {
  return instance.delete(url, { params })
}

export default instance

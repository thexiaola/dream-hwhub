import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { ClassInfo } from '@/types'
import { get, post, put, del } from '@/utils/http'

export const useClassStore = defineStore('class', () => {
  const classes = ref<ClassInfo[]>([])

  const getClasses = async (page: number = 1, size: number = 10): Promise<void> => {
    const result = await get<{ list: ClassInfo[] }>('/classes', { page, size })
    if (result.code === 200) {
      classes.value = result.data!.list
    }
  }

  const getClassById = async (id: number): Promise<ClassInfo | null> => {
    const result = await get<ClassInfo>(`/classes/${id}`)
    if (result.code === 200) {
      return result.data!
    }
    return null
  }

  const createClass = async (data: Record<string, unknown>): Promise<{ code: number; message: string }> => {
    const result = await post('/classes', data)
    return { code: result.code, message: result.message }
  }

  const updateClass = async (id: number, data: Record<string, unknown>): Promise<{ code: number; message: string }> => {
    const result = await put(`/classes/${id}`, data)
    return { code: result.code, message: result.message }
  }

  const deleteClass = async (id: number): Promise<{ code: number; message: string }> => {
    const result = await del(`/classes/${id}`)
    return { code: result.code, message: result.message }
  }

  const joinClass = async (classId: number): Promise<{ code: number; message: string }> => {
    const result = await post(`/classes/${classId}/join`)
    return { code: result.code, message: result.message }
  }

  return {
    classes,
    getClasses,
    getClassById,
    createClass,
    updateClass,
    deleteClass,
    joinClass
  }
})

import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { ClassInfo } from '@/types'
import { get, post, put, del } from '@/utils/http'

export const useClassStore = defineStore('class', () => {
  const classes = ref<ClassInfo[]>([])

  const getClasses = async (page: number = 1, size: number = 10): Promise<void> => {
    const result = await get<{ records: ClassInfo[] }>('/class/mine', { pageNum: page, pageSize: size })
    if (result.code === 200) {
      classes.value = result.data!.records
    }
  }

  const getClassById = async (id: number): Promise<ClassInfo | null> => {
    const result = await get<ClassInfo>(`/class/${id}`)
    if (result.code === 200) {
      return result.data!
    }
    return null
  }

  const createClass = async (data: Record<string, unknown>): Promise<{ code: number; message: string }> => {
    const result = await post('/class', data)
    return { code: result.code, message: result.message }
  }

  const updateClass = async (id: number, data: Record<string, unknown>): Promise<{ code: number; message: string }> => {
    const result = await put(`/class/${id}`, data)
    return { code: result.code, message: result.message }
  }

  const deleteClass = async (id: number): Promise<{ code: number; message: string }> => {
    const result = await del(`/class/${id}`)
    return { code: result.code, message: result.message }
  }

  const joinClass = async (classId: number): Promise<{ code: number; message: string }> => {
    const result = await post(`/class/${classId}/applications/join`, { classId })
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

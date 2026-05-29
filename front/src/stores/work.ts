import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { WorkInfo } from '@/types'
import { get, post, put, del } from '@/utils/http'

export const useWorkStore = defineStore('work', () => {
  const works = ref<WorkInfo[]>([])

  const getWorks = async (page: number = 1, size: number = 10): Promise<void> => {
    const result = await get<{ list: WorkInfo[] }>('/works', { page, size })
    if (result.code === 200) {
      works.value = result.data!.list
    }
  }

  const getWorkById = async (id: number): Promise<WorkInfo | null> => {
    const result = await get<WorkInfo>(`/works/${id}`)
    if (result.code === 200) {
      return result.data!
    }
    return null
  }

  const createWork = async (data: Record<string, unknown>): Promise<{ code: number; message: string }> => {
    const result = await post('/works', data)
    return { code: result.code, message: result.message }
  }

  const updateWork = async (id: number, data: Record<string, unknown>): Promise<{ code: number; message: string }> => {
    const result = await put(`/works/${id}`, data)
    return { code: result.code, message: result.message }
  }

  const deleteWork = async (id: number): Promise<{ code: number; message: string }> => {
    const result = await del(`/works/${id}`)
    return { code: result.code, message: result.message }
  }

  const pinWork = async (id: number, isPinned: boolean): Promise<{ code: number; message: string }> => {
    const result = await post(`/works/${id}/pin`, { isPinned })
    return { code: result.code, message: result.message }
  }

  return {
    works,
    getWorks,
    getWorkById,
    createWork,
    updateWork,
    deleteWork,
    pinWork
  }
})

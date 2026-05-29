import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { WorkSubmission } from '@/types'
import { get, post, put } from '@/utils/http'

export const useSubmissionStore = defineStore('submission', () => {
  const submissions = ref<WorkSubmission[]>([])

  const getSubmissions = async (workId?: number, page: number = 1, size: number = 10): Promise<void> => {
    const params: Record<string, unknown> = { pageNum: page, pageSize: size }
    if (workId) {
      params.workId = workId
    }
    const result = await get<{ records: WorkSubmission[] }>('/submissions/list', params)
    if (result.code === 200) {
      submissions.value = result.data!.records
    }
  }

  const submitWork = async (workId: number, content: string): Promise<{ code: number; message: string }> => {
    const result = await post('/submissions', { workId, content })
    return { code: result.code, message: result.message }
  }

  const gradeWork = async (submissionId: number, grade: number): Promise<{ code: number; message: string }> => {
    const result = await put(`/submissions/${submissionId}/grade`, { grade })
    return { code: result.code, message: result.message }
  }

  const getSubmissionById = async (id: number): Promise<WorkSubmission | null> => {
    const result = await get<WorkSubmission>(`/submissions/${id}`)
    if (result.code === 200) {
      return result.data!
    }
    return null
  }

  return {
    submissions,
    getSubmissions,
    submitWork,
    gradeWork,
    getSubmissionById
  }
})

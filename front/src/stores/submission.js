import { defineStore } from 'pinia'
import { ref } from 'vue'
import http from '../utils/http'

export const useSubmissionStore = defineStore('submission', () => {
  const submissions = ref([])
  const currentSubmission = ref(null)

  async function getSubmissionList(params = {}) {
    const response = await http.get('/submissions/', { params })
    if (response.code === 200) {
      submissions.value = response.data.records
    }
    return response
  }

  async function getSubmissionDetail(submissionId) {
    const response = await http.get(`/submissions/${submissionId}`)
    if (response.code === 200) {
      currentSubmission.value = response.data
    }
    return response
  }

  async function submitWork(formData) {
    const response = await http.post('/submissions/', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    return response
  }

  async function updateSubmission(submissionId, formData) {
    const response = await http.put(`/submissions/${submissionId}`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    return response
  }

  async function deleteSubmission(submissionId) {
    const response = await http.delete(`/submissions/${submissionId}`)
    return response
  }

  async function gradeWork(data) {
    const response = await http.put('/submissions/grade', data)
    return response
  }

  async function getSubmissionListByWork(workId) {
    const response = await http.get(`/submissions/work/${workId}`)
    return response
  }

  return {
    submissions,
    currentSubmission,
    getSubmissionList,
    getSubmissionDetail,
    submitWork,
    updateSubmission,
    deleteSubmission,
    gradeWork,
    getSubmissionListByWork
  }
})
import { defineStore } from 'pinia'
import { ref } from 'vue'
import http from '../utils/http'

export const useWorkStore = defineStore('work', () => {
  const works = ref([])
  const currentWork = ref(null)

  async function getWorkList(params = {}) {
    const response = await http.get('/works/', { params })
    if (response.code === 200) {
      works.value = response.data.records
    }
    return response
  }

  async function getWorkDetail(workId) {
    const response = await http.get(`/works/${workId}`)
    if (response.code === 200) {
      currentWork.value = response.data
    }
    return response
  }

  async function createWork(formData) {
    const response = await http.post('/works/', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    return response
  }

  async function updateWork(workId, formData) {
    const response = await http.put(`/works/${workId}`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    return response
  }

  async function deleteWork(workId) {
    const response = await http.delete(`/works/${workId}`)
    return response
  }

  async function pinWork(workId, isPinned) {
    const response = await http.patch(`/works/${workId}/pin`, { workId, isPinned })
    return response
  }

  return {
    works,
    currentWork,
    getWorkList,
    getWorkDetail,
    createWork,
    updateWork,
    deleteWork,
    pinWork
  }
})
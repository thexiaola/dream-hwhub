import { defineStore } from 'pinia'
import { ref } from 'vue'
import http from '../utils/http'

export const useClassStore = defineStore('class', () => {
  const classes = ref([])
  const currentClass = ref(null)

  async function getMyClasses(params = {}) {
    const response = await http.get('/class/mine', { params })
    if (response.code === 200) {
      classes.value = response.data.records
    }
    return response
  }

  async function getClassDetail(classId) {
    const response = await http.get(`/class/${classId}`)
    if (response.code === 200) {
      currentClass.value = response.data
    }
    return response
  }

  async function getClassMembers(classId, params = {}) {
    const response = await http.get(`/class/${classId}/members`, { params })
    return response
  }

  async function createClass(data) {
    return await http.post('/class/', data)
  }

  async function joinClass(classId) {
    return await http.post(`/class/${classId}/applications/join`, { classId })
  }

  async function exitClass(classId) {
    return await http.delete(`/class/${classId}/members/me`)
  }

  async function deleteClass(classId) {
    return await http.delete(`/class/${classId}`)
  }

  async function updateClass(data) {
    return await http.put(`/class/${data.classId}`, data)
  }

  async function checkMembership(classId) {
    return await http.get(`/class/${classId}/membership`)
  }

  return {
    classes,
    currentClass,
    getMyClasses,
    getClassDetail,
    getClassMembers,
    createClass,
    joinClass,
    exitClass,
    deleteClass,
    updateClass,
    checkMembership
  }
})
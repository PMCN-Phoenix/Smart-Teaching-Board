import { defineStore } from 'pinia'
import { ref } from 'vue'
import { api } from '../services/api'

export const useClassroomStore = defineStore('classroom', () => {
  const classrooms = ref([])
  const currentClassroom = ref(null)

  async function fetchClassrooms() {
    const res = await api.get('/api/classrooms')
    classrooms.value = res.data.data || []
  }

  async function createClassroom(title, description) {
    const res = await api.post('/api/classrooms', { title, description })
    await fetchClassrooms()
    return res.data.data
  }

  function setCurrent(c) { currentClassroom.value = c }

  return { classrooms, currentClassroom, fetchClassrooms, createClassroom, setCurrent }
})
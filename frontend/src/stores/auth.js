import { defineStore } from 'pinia'
import { ref } from 'vue'
import { api } from '../services/api'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const username = ref(localStorage.getItem('username') || '')

  function setAuth(t, u) {
    token.value = t
    username.value = u
    localStorage.setItem('token', t)
    localStorage.setItem('username', u)
  }

  function logout() {
    token.value = ''
    username.value = ''
    localStorage.clear()
  }

  async function login(credentials) {
    const res = await api.post('/api/auth/login', credentials)
    setAuth(res.data.data.token, res.data.data.username)
    return res.data
  }

  async function register(info) {
    const res = await api.post('/api/auth/register', info)
    setAuth(res.data.data.token, res.data.data.username)
    return res.data
  }

  return { token, username, login, register, logout }
})
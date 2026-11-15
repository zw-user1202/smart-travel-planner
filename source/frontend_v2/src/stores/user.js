import { defineStore } from 'pinia'
import api from '@/api'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    username: localStorage.getItem('username') || '',
    nickname: localStorage.getItem('nickname') || '',
    role: Number(localStorage.getItem('role') || 0)
  }),
  actions: {
    async login(payload) {
      const data = await api.post('/auth/login', payload)
      this.setSession(data)
      return data
    },
    async register(payload) {
      const data = await api.post('/auth/register', payload)
      this.setSession(data)
      return data
    },
    setSession(data) {
      this.token = data.token
      this.username = data.username
      this.nickname = data.nickname || data.username
      this.role = data.role || 0
      localStorage.setItem('token', data.token)
      localStorage.setItem('username', data.username)
      localStorage.setItem('nickname', this.nickname)
      localStorage.setItem('role', String(this.role))
    },
    logout() {
      this.token = ''
      this.username = ''
      this.nickname = ''
      this.role = 0
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      localStorage.removeItem('nickname')
      localStorage.removeItem('role')
    }
  }
})

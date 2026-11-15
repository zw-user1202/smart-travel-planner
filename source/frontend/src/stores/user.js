import { defineStore } from 'pinia'
import api from '@/api'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    username: localStorage.getItem('username') || '',
    nickname: localStorage.getItem('nickname') || ''
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
      localStorage.setItem('token', data.token)
      localStorage.setItem('username', data.username)
      localStorage.setItem('nickname', this.nickname)
    },
    logout() {
      this.token = ''
      this.username = ''
      this.nickname = ''
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      localStorage.removeItem('nickname')
    }
  }
})

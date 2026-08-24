import { defineStore } from 'pinia'
import { login as apiLogin, register as apiRegister, getMe } from '@/api/auth'
import type { UserVO } from '@/api/types'

const TOKEN_KEY = 'blog_token'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: null as UserVO | null,
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    displayName: (state) => state.user?.nickname || state.user?.username || '',
  },
  actions: {
    setToken(token: string) {
      this.token = token
      localStorage.setItem(TOKEN_KEY, token)
    },
    async login(username: string, password: string) {
      const res = await apiLogin({ username, password })
      this.setToken(res.data.token)
      this.user = res.data.user
    },
    async register(payload: { username: string; password: string; nickname?: string }) {
      const res = await apiRegister(payload)
      return res
    },
    async fetchMe() {
      const res = await getMe()
      this.user = res.data
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem(TOKEN_KEY)
    },
  },
})

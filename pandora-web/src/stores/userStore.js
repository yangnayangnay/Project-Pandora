import { defineStore } from 'pinia'
import { getToken, setToken, removeToken, getUser, setUser, removeUser } from '@/utils/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken() || '',
    user: getUser() || {}
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    role: (state) => state.user?.role || '',
    username: (state) => state.user?.username || ''
  },
  actions: {
    setLoginData(data) {
      this.token = data.token
      this.user = data.user
      setToken(data.token)
      setUser(data.user)
    },
    logout() {
      this.token = ''
      this.user = {}
      removeToken()
      removeUser()
    }
  }
})
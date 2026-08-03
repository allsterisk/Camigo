import { defineStore } from 'pinia'
import { getCurrentUser, loginUser } from '../api/users'
import { clearSession, readSession, saveSession, updateSessionUser } from '../utils/session'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    session: readSession(),
    loading: false,
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.session?.accessToken),
    currentUser: (state) => state.session?.user || null,
    roles: (state) => state.session?.user?.roles || [],
  },
  actions: {
    async login(account, password) {
      this.loading = true
      try {
        const loginData = await loginUser({ account, password })
        this.session = saveSession(loginData)
        return loginData.user
      } finally {
        this.loading = false
      }
    },
    async refreshUser() {
      const user = await getCurrentUser()
      this.session = updateSessionUser(user)
      return user
    },
    hasRole(role) {
      return this.roles.includes(role)
    },
    logout() {
      clearSession()
      this.session = null
    },
  },
})

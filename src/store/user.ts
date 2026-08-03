import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

export type UserRole = 'USER' | 'COURIER' | 'ADMIN'

export interface UserInfo {
  id: number
  username: string
  nickname?: string
}

interface PersistedSession {
  token: string
  role: UserRole
  userInfo: UserInfo | null
}

const SESSION_KEY = 'camigo.session'

export const useUserStore = defineStore('user', () => {
  const session = readSession()
  const token = ref(session?.token ?? '')
  const role = ref<UserRole | null>(session?.role ?? null)
  const userInfo = ref<UserInfo | null>(session?.userInfo ?? null)
  const isAuthenticated = computed(() => Boolean(token.value && role.value))

  function setSession(nextToken: string, nextRole: UserRole, nextUserInfo: UserInfo | null = null) {
    token.value = nextToken
    role.value = nextRole
    userInfo.value = nextUserInfo
    persistSession()
  }

  function setUserInfo(nextUserInfo: UserInfo | null) {
    userInfo.value = nextUserInfo
    persistSession()
  }

  function logout() {
    token.value = ''
    role.value = null
    userInfo.value = null
    localStorage.removeItem(SESSION_KEY)
  }

  function persistSession() {
    if (!token.value || !role.value) return
    const value: PersistedSession = { token: token.value, role: role.value, userInfo: userInfo.value }
    localStorage.setItem(SESSION_KEY, JSON.stringify(value))
  }

  return { token, role, userInfo, isAuthenticated, setSession, setUserInfo, logout }
})

function readSession(): PersistedSession | null {
  const raw = localStorage.getItem(SESSION_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as PersistedSession
  } catch {
    localStorage.removeItem(SESSION_KEY)
    return null
  }
}

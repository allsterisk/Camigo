import { computed, shallowRef } from 'vue'
import { defineStore } from 'pinia'

import { login as loginRequest } from '@/api/user'
import {
  isUserRole,
  selectPrimaryRole,
  type LoginCredentials,
  type UserInfo,
  type UserRole,
} from '@/types/auth'

interface PersistedSession {
  token: string
  role: UserRole
  userInfo: UserInfo
  locationConfirmed: boolean
}

const SESSION_KEY = 'camigo.session'

export const useUserStore = defineStore('user', () => {
  const session = readSession()
  const token = shallowRef(session?.token ?? '')
  const role = shallowRef<UserRole | null>(session?.role ?? null)
  const userInfo = shallowRef<UserInfo | null>(session?.userInfo ?? null)
  const locationConfirmed = shallowRef(session?.locationConfirmed ?? true)
  const isAuthenticated = computed(() => Boolean(token.value && role.value && userInfo.value))
  const displayName = computed(() => userInfo.value?.realName || userInfo.value?.username || '')

  async function login(credentials: LoginCredentials): Promise<UserRole> {
    const response = await loginRequest({
      account: credentials.account.trim(),
      password: credentials.password,
    })
    const primaryRole = selectPrimaryRole(response.user.roles)
    if (!response.accessToken || !primaryRole) throw new Error('登录响应缺少有效的 Token 或角色')

    setSession(response.accessToken, primaryRole, response.user, response.locationConfirmed)
    return primaryRole
  }

  function setSession(
    nextToken: string,
    nextRole: UserRole,
    nextUserInfo: UserInfo,
    nextLocationConfirmed: boolean,
  ) {
    token.value = nextToken
    role.value = nextRole
    userInfo.value = nextUserInfo
    locationConfirmed.value = nextRole !== 'COURIER' || nextLocationConfirmed
    persistSession()
  }

  function setUserInfo(nextUserInfo: UserInfo) {
    userInfo.value = nextUserInfo
    persistSession()
  }

  function markLocationConfirmed() {
    locationConfirmed.value = true
    persistSession()
  }

  function logout() {
    token.value = ''
    role.value = null
    userInfo.value = null
    locationConfirmed.value = true
    sessionStorage.removeItem(SESSION_KEY)
    localStorage.removeItem(SESSION_KEY)
  }

  function persistSession() {
    if (!token.value || !role.value || !userInfo.value) return
    const value: PersistedSession = {
      token: token.value,
      role: role.value,
      userInfo: userInfo.value,
      locationConfirmed: locationConfirmed.value,
    }
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(value))
  }

  return {
    token,
    role,
    userInfo,
    isAuthenticated,
    displayName,
    locationConfirmed,
    login,
    setSession,
    setUserInfo,
    markLocationConfirmed,
    logout,
  }
})

function readSession(): PersistedSession | null {
  // 旧版使用 localStorage，导致同源标签页互相覆盖账号；升级后不迁移旧会话，
  // 以免把最后一次登录的共享账号复制进当前标签页。
  localStorage.removeItem(SESSION_KEY)
  const raw = sessionStorage.getItem(SESSION_KEY)
  if (!raw) return null

  try {
    const value = JSON.parse(raw) as Partial<PersistedSession>
    if (
      typeof value.token !== 'string' ||
      !value.token ||
      !isUserRole(value.role) ||
      !isUserInfo(value.userInfo)
    ) {
      throw new Error('Invalid session')
    }
    return {
      token: value.token,
      role: value.role,
      userInfo: value.userInfo,
      locationConfirmed: value.role !== 'COURIER' || value.locationConfirmed === true,
    }
  } catch {
    sessionStorage.removeItem(SESSION_KEY)
    return null
  }
}

function isUserInfo(value: unknown): value is UserInfo {
  if (typeof value !== 'object' || value === null) return false
  const user = value as Partial<UserInfo>
  return (
    typeof user.id === 'number' &&
    typeof user.username === 'string' &&
    typeof user.realName === 'string' &&
    Array.isArray(user.roles) &&
    user.roles.some(isUserRole)
  )
}

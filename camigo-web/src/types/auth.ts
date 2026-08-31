export const USER_ROLES = ['USER', 'COURIER', 'ADMIN'] as const

export type UserRole = (typeof USER_ROLES)[number]

export interface LoginCredentials {
  account: string
  password: string
}

export interface UserInfo {
  id: number
  username: string
  realName: string
  phone: string
  studentNo: string | null
  status: string
  roles: UserRole[]
}

export interface LoginResponse {
  tokenType: 'Bearer' | string
  accessToken: string
  user: UserInfo
  locationConfirmed: boolean
}

export const ROLE_HOME: Record<UserRole, string> = {
  USER: '/user',
  COURIER: '/courier',
  ADMIN: '/admin',
}

export function isUserRole(value: unknown): value is UserRole {
  return typeof value === 'string' && USER_ROLES.some((role) => role === value)
}

export function selectPrimaryRole(roles: readonly unknown[]): UserRole | null {
  const validRoles = roles.filter(isUserRole)
  return (['ADMIN', 'COURIER', 'USER'] as const).find((role) => validRoles.includes(role)) ?? null
}

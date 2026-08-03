import type { LoginCredentials, LoginResponse, UserInfo } from '@/types/auth'
import { get, post } from '@/utils/request'

export function login(credentials: LoginCredentials): Promise<LoginResponse> {
  return post<LoginResponse, LoginCredentials>('/users/login', credentials)
}

export function getCurrentUser(): Promise<UserInfo> {
  return get<UserInfo>('/users/info')
}

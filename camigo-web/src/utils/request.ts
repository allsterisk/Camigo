import axios, {
  AxiosError,
  type AxiosRequestConfig,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios'
import { ElMessage } from 'element-plus'

import router from '@/router'
import { pinia } from '@/store'
import { useUserStore } from '@/store/user'

export interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
}

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 10_000,
  headers: { 'Content-Type': 'application/json' },
})

request.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const userStore = useUserStore(pinia)
  if (userStore.token) config.headers.Authorization = `Bearer ${userStore.token}`
  return config
})

request.interceptors.response.use(
  <T>(response: AxiosResponse<ApiEnvelope<T> | T>): T => {
    const payload = response.data
    if (isEnvelope<T>(payload)) {
      if (payload.code === 200) return payload.data
      throw new Error(payload.message || '请求失败')
    }
    return payload
  },
  async (error: AxiosError<ApiEnvelope<unknown>>) => {
    const status = error.response?.status
    const message = error.response?.data?.message
    const isLoginPage = router.currentRoute.value.name === 'login'

    if (status === 401 && !isLoginPage) {
      useUserStore(pinia).logout()
      ElMessage.error(message || '登录状态已失效，请重新登录')
      await router.replace({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
    } else if (status && status >= 500) {
      ElMessage.error(message || '服务器暂不可用，请稍后重试')
    } else if (!error.response) {
      ElMessage.error('无法连接后端服务，请确认服务已启动')
    }

    return Promise.reject(error)
  },
)

export function get<TResponse>(url: string, config?: AxiosRequestConfig): Promise<TResponse> {
  return request.get<ApiEnvelope<TResponse>, TResponse>(url, config)
}

export function post<TResponse, TBody>(url: string, data: TBody): Promise<TResponse> {
  return request.post<ApiEnvelope<TResponse>, TResponse, TBody>(url, data)
}

export function put<TResponse, TBody>(url: string, data?: TBody): Promise<TResponse> {
  return request.put<ApiEnvelope<TResponse>, TResponse, TBody>(url, data)
}

export function getRequestErrorMessage(error: unknown, fallback = '请求未能完成'): string {
  if (axios.isAxiosError<ApiEnvelope<unknown>>(error)) {
    return error.response?.data?.message || fallback
  }
  return error instanceof Error && error.message ? error.message : fallback
}

function isEnvelope<T>(payload: ApiEnvelope<T> | T): payload is ApiEnvelope<T> {
  return typeof payload === 'object' && payload !== null && 'code' in payload && 'data' in payload
}

export default request

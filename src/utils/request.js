import axios from 'axios'
import { ElMessage } from 'element-plus'
import { clearSession, getAccessToken } from './session'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

request.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const payload = response.data
    if (payload && typeof payload.code === 'number') {
      if (payload.code === 200) return payload.data
      return Promise.reject(createBusinessError(payload.message || '请求失败', response))
    }
    return payload
  },
  (error) => {
    const status = error.response?.status
    const backendMessage = error.response?.data?.message

    if (status === 401) {
      clearSession()
      ElMessage.error(backendMessage || '登录状态已失效，请重新登录')
      redirectToLogin()
    } else if (status === 403) {
      ElMessage.warning(backendMessage || '没有执行该操作的权限')
    } else if ([400, 404, 409].includes(status)) {
      ElMessage.error(backendMessage || '请求未能完成')
    } else if (status >= 500) {
      ElMessage.error('服务器暂时不可用，请稍后重试')
    } else if (!error.response) {
      ElMessage.error('无法连接后端服务，请确认服务已启动')
    }
    return Promise.reject(error)
  },
)

function createBusinessError(message, response) {
  const error = new Error(message)
  error.response = response
  return error
}

function redirectToLogin() {
  if (window.location.pathname === '/login') return
  const redirect = `${window.location.pathname}${window.location.search}`
  window.location.replace(`/login?redirect=${encodeURIComponent(redirect)}`)
}

export default request

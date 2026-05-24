import axios from 'axios'
import config from '../config'
import { useAuthStore } from '../stores/auth'

const api = axios.create({
  baseURL: config.apiBaseUrl,
  timeout: config.aiTimeout
})

// 请求拦截器：自动附加 JWT
api.interceptors.request.use(cfg => {
  const auth = useAuthStore()
  if (auth.token) {
    cfg.headers.Authorization = `Bearer ${auth.token}`
  }
  return cfg
})

// 响应拦截器：统一错误提示
api.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      // token 过期，清除登录状态
      const auth = useAuthStore()
      auth.logout()
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export { api, config }
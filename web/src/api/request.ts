import axios from 'axios'
import { toast } from '@/utils/message'
import router from '@/router'

const TOKEN_KEY = 'blog_token'

/** 统一 axios 实例：开发环境经 Vite 代理访问 /api */
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 15000,
})

// 请求拦截：自动附加 Token
request.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截：统一处理 Result 结构
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      toast(res.message || '请求失败', 'error')
      if (res.code === 401) {
        handleUnauthorized()
      }
      return Promise.reject(new Error(res.message))
    }
    return res
  },
  (error) => {
    if (error.response?.status === 401) {
      handleUnauthorized()
    }
    toast(error.response?.data?.message || '网络错误', 'error')
    return Promise.reject(error)
  },
)

function handleUnauthorized() {
  localStorage.removeItem(TOKEN_KEY)
  if (router.currentRoute.value.path !== '/login') {
    router.push('/login')
  }
}

export default request

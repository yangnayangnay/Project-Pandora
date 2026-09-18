import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, removeToken, removeUser } from './auth'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

request.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200) {
      return res
    } else {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
  },
  (error) => {
    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        removeToken()
        removeUser()
        router.push('/login')
        ElMessage.error('登录已过期，请重新登录')
      } else if (status === 403) {
        ElMessage.error('无操作权限')
      } else {
        ElMessage.error(error.response.data?.message || '请求失败')
      }
    } else {
      ElMessage.error('网络错误')
    }
    return Promise.reject(error)
  }
)

export default request
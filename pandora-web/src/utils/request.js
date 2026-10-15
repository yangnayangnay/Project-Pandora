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
      ElMessage.error(res.message || `请求失败（错误码: ${res.code || '未知'}）`)
      return Promise.reject(new Error(res.message || '请求失败'))
    }
  },
  (error) => {
    if (error.response) {
      const status = error.response.status
      const serverMsg = error.response.data?.message
      if (status === 401) {
        removeToken()
        removeUser()
        router.push('/login')
        ElMessage.error('登录已过期，请重新登录')
      } else if (status === 403) {
        ElMessage.error(serverMsg || '无操作权限')
      } else if (status === 404) {
        ElMessage.error(serverMsg || '请求的资源不存在（404）')
      } else if (status === 500) {
        ElMessage.error(serverMsg || '服务器内部错误（500），请检查后端是否正常运行')
      } else if (status === 502 || status === 503 || status === 504) {
        ElMessage.error('后端服务不可用，请确认 localhost:8080 已启动')
      } else if (status === 400) {
        ElMessage.error(serverMsg || '请求参数有误（400）')
      } else {
        ElMessage.error(serverMsg || `请求失败（HTTP ${status}）`)
      }
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请稍后重试')
    } else if (error.code === 'ERR_NETWORK' || error.message?.includes('Network Error')) {
      ElMessage.error('无法连接服务器，请确认后端服务（localhost:8080）已启动')
    } else {
      ElMessage.error(`网络错误: ${error.message || '未知错误'}`)
    }
    return Promise.reject(error)
  }
)

export default request

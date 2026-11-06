import request from '@/utils/request'

export function login(data) {
  return request.post('/auth/login', { ...data, clientType: 'WEB' })
}

export function register(data) {
  return request.post('/auth/register', data)
}

export function logout() {
  return request.post('/auth/logout')
}

export function refresh(data) {
  return request.post('/auth/refresh', data)
}
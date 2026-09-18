import request from '@/utils/request'

export function getCurrentUser() {
  return request.get('/user/me')
}

export function listUsers(params) {
  return request.get('/user/list', { params })
}

export function getUserPanel(userId) {
  return request.get(`/user/${userId}/panel`)
}

export function updateProfile(data) {
  return request.put('/user/me', data)
}
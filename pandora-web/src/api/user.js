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
export function changePassword(data) {
  return request.put('/user/me/password', data)
}
// Round19 REQ-19-06: 管理员创建账号
export function createUser(data) {
  return request.post('/user/create', data)
}
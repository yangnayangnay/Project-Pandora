import request from '@/utils/request'

export function createTask(data) {
  return request.post('/task', data)
}

export function listTasks(params) {
  return request.get('/task/list', { params })
}

export function getTask(taskId) {
  return request.get(`/task/${taskId}`)
}

export function updateProgress(taskId, data) {
  return request.patch(`/task/${taskId}/progress`, data)
}

export function transitionStatus(taskId, data) {
  return request.post(`/task/${taskId}/transition`, data)
}

export function getQuadrantTasks() {
  return request.get('/task/quadrant')
}
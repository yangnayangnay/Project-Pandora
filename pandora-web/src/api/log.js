import request from '@/utils/request'

export function createLog(data) {
  return request.post('/log', data)
}

export function listLogs() {
  return request.get('/log/list')
}

export function listAllLogs(params) {
  return request.get('/log/all', { params })
}

export function cleanupPreview() {
  return request.get('/log/cleanup-preview')
}

export function cleanupExecute() {
  return request.post('/log/cleanup-execute')
}

export function getLogStats() {
  return request.get('/log/stat')
}

export function updateTop10(data) {
  return request.put('/log/top10', data)
}

export function getTop10(params) {
  return request.get('/log/top10', { params })
}
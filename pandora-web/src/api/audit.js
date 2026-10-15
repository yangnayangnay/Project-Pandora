import request from '@/utils/request'

export function getAuditLogs(params) {
  return request.get('/audit/list', { params })
}
export function exportAuditLogs(params) {
  return request.get('/audit/export', { params, responseType: 'blob' })
}
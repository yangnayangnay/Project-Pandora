import request from '@/utils/request'

export function getAuditLogs(params) {
  return request.get('/audit/list', { params })
}
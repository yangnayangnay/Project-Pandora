import request from '@/utils/request'

export function createInvitation(data) {
  return request.post('/perm/invitation', data)
}

export function listInvitations() {
  return request.get('/perm/invitation/list')
}

export function respondInvitation(invitationId, data) {
  return request.put(`/perm/invitation/${invitationId}`, data)
}

export function createAuditRule(data) {
  return request.post('/perm/audit-rule', data)
}

export function listAuditRules() {
  return request.get('/perm/audit-rule/list')
}
export function getFunctionCodes() {
  return request.get('/permission/function/codes')
}

export function getSubjects(functionCode) {
  return request.get('/permission/function/subjects', { params: { functionCode } })
}

export function addSubjects(data) {
  return request.post('/permission/function/subjects', data)
}

export function removeSubjects(data) {
  return request.delete('/permission/function/subjects', { data })
}

export function replaceSubjects(data) {
  return request.put('/permission/function/subjects', data)
}

export function getHistory(params) {
  return request.get('/permission/function/history', { params })
}
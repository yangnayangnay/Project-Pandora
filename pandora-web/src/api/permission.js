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
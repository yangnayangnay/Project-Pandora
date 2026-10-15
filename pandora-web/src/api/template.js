import request from '@/utils/request'

export function saveTemplate(data) {
  return request.post('/template', data)
}

export function listTemplates(templateType) {
  return request.get('/template', { params: { templateType } })
}

export function updateTemplate(templateId, data) {
  return request.put(`/template/${templateId}`, data)
}

export function deleteTemplate(templateId) {
  return request.delete(`/template/${templateId}`)
}

export function migrateTemplates(data) {
  return request.post('/template/migrate', data)
}
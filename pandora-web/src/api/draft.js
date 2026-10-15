import request from '@/utils/request'

export function saveDraft(data) {
  return request.put('/draft', data)
}

export function getDraft(draftType) {
  return request.get('/draft', { params: { draftType } })
}

export function deleteDraft(draftType) {
  return request.delete('/draft', { params: { draftType } })
}
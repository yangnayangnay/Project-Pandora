import request from '@/utils/request'

export function getAiConfig() {
  return request.get('/ai/config')
}

export function updateAiConfig(data) {
  return request.put('/ai/config', data)
}

export function getChannelStatus() {
  return request.get('/ai/channel-status')
}

export function getFreeCatalog() {
  return request.get('/ai/free-catalog')
}
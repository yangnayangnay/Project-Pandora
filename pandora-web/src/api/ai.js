import request from '@/utils/request'

export function submitMbti(data) {
  return request.post('/ai/mbti/submit', data)
}

export function getMbtiReport() {
  return request.get('/ai/mbti/report')
}

export function keywordAnalysis(data) {
  return request.post('/ai/keyword', data)
}

export function getSuggestion() {
  return request.get('/ai/suggestion')
}

export function getDivination() {
  return request.get('/ai/divination')
}
import request from '@/utils/request'

export function getMoonPhase() {
  return request.get('/theme/moon-phase')
}

export function setThemePreference(data) {
  return request.put('/theme/preference', data)
}
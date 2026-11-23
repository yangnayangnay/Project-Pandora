import { hasDepartment, hasTeam } from './organization.js'
import { identityRegionCodes } from './identityRegionCodes.js'

export const validRealName = value => /^\p{Script=Han}{1,15}$/u.test(value || '')
export const validPassword = value => typeof value === 'string' && [...value].length >= 6 && [...value].length <= 16

export function validIdCard(value, today = chinaToday()) {
  const id = String(value || '').trim().toUpperCase()
  if (!/^\d{17}[\dX]$/.test(id) || !identityRegionCodes.has(id.slice(0, 6)) || id.slice(14, 17) === '000') return false
  const birth = birthdayFromIdCard(id)
  if (!birth || birth > today) return false
  // Feb 29 birthdays reach 60 on Feb 28 in a non-leap retirement year, as in LocalDate.plusYears.
  const retirementYear = Number(birth.slice(0, 4)) + 60
  const retirementDay = birth.slice(5) === '02-29' && new Date(retirementYear, 1, 29).getMonth() !== 1 ? '02-28' : birth.slice(5)
  if (`${retirementYear}-${retirementDay}` <= today) return false
  const weights = [7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2]
  return id[17] === '10X98765432'[[...id.slice(0, 17)].reduce((sum, digit, index) => sum + Number(digit) * weights[index], 0) % 11]
}

function chinaToday() {
  const parts = new Intl.DateTimeFormat('en', { timeZone: 'Asia/Shanghai', year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts(new Date())
  const value = type => parts.find(part => part.type === type).value
  return `${value('year')}-${value('month')}-${value('day')}`
}

export const emptyAccountForm = () => ({
  employeeNo: '', password: '', role: '', realName: '', email: '', phone: '',
  idCard: '', birthday: '', departmentId: null, teamId: null
})

export function teamsForDepartment(teams, departmentId) {
  if (departmentId == null || departmentId === '') return []
  return teams.filter(team => String(team.department_id ?? team.departmentid ?? team.departmentId) === String(departmentId))
}

export function accountPayload(form) {
  const employeeNo = form.employeeNo.trim()
  return {
    employeeNo, username: employeeNo, password: form.password, role: form.role,
    realName: form.realName.trim(), email: form.email.trim(), phone: form.phone.trim(),
    idCard: form.idCard.trim().toUpperCase(), birthday: form.birthday,
    departmentId: hasDepartment(form.role) ? form.departmentId : null, teamId: hasTeam(form.role) ? form.teamId : null
  }
}

export function isFutureBirthday(date) {
  const selected = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
  return selected > chinaToday()
}

// Extract only a real, non-future calendar date. Users may still select another birthday.
export function birthdayFromIdCard(value) {
  const id = String(value || '').trim()
  if (!/^\d{17}[\dXx]$/.test(id)) return ''
  const year = Number(id.slice(6, 10)), month = Number(id.slice(10, 12)), day = Number(id.slice(12, 14))
  if (year < 1900) return ''
  const date = new Date(year, month - 1, day)
  if (date.getFullYear() !== year || date.getMonth() !== month - 1 || date.getDate() !== day || isFutureBirthday(date)) return ''
  return `${id.slice(6, 10)}-${id.slice(10, 12)}-${id.slice(12, 14)}`
}

export function allocatedEmployeeNumber(data) {
  const value = data?.employeeno ?? data?.employeeNo
  if (typeof value !== 'string' || !/^\d{6,}$/.test(value)) throw new Error('工号响应格式异常，请确认服务器已更新')
  return value
}

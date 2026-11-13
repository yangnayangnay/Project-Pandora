import { hasDepartment, hasTeam } from './organization.js'

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
  const parts = new Intl.DateTimeFormat('en', { timeZone: 'Asia/Shanghai', year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts(new Date())
  const value = type => parts.find(part => part.type === type).value
  return selected > `${value('year')}-${value('month')}-${value('day')}`
}

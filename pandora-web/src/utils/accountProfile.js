import { isFutureBirthday } from './accountCreation.js'

const fields = ['employeeNo', 'realName', 'email', 'phone', 'idCard', 'birthday']

export function accountProfileForm(row) {
  return {
    employeeNo: row.username ?? '', realName: row.real_name ?? '', email: row.email ?? '',
    phone: row.phone ?? '', idCard: row.id_card ?? '', birthday: row.birthday?.slice(0, 10) ?? ''
  }
}

export function profileChanges(original, form) {
  const changes = {}
  for (const field of fields) {
    let value = String(form[field] ?? '').trim()
    if (field === 'idCard') value = value.toUpperCase()
    const previous = String(original[field] ?? '').trim()
    if (value !== (field === 'idCard' ? previous.toUpperCase() : previous)) changes[field] = value
  }
  return changes
}

/** Validate edited fields only, so a legacy account can be maintained one field at a time. */
export function profileFieldError(field, value) {
  const labels = { employeeNo: '工号', realName: '真实姓名', email: '邮箱', phone: '手机号', idCard: '身份证号', birthday: '生日' }
  if (!value) return `${labels[field]}不能为空`
  if (field === 'employeeNo' && !/^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$/.test(value)) return '工号须为1至64位字母、数字、点、下划线或短横线'
  if (field === 'realName' && value.length > 64) return '真实姓名不能超过64字'
  if (field === 'email' && (value.length > 128 || !/^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(value))) return '邮箱格式不正确'
  if (field === 'phone' && !/^1[3-9]\d{9}$/.test(value)) return '手机号格式不正确'
  if (field === 'idCard' && !/^\d{17}[\dX]$/.test(value)) return '请输入18位身份证号，末位可为X'
  if (field === 'birthday') {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return '请选择有效生日'
    const date = new Date(`${value}T12:00:00`)
    const [year, month, day] = value.split('-').map(Number)
    if (year < 1 || Number.isNaN(date.getTime()) || date.getFullYear() !== year || date.getMonth() + 1 !== month || date.getDate() !== day) return '请选择有效生日'
    if (isFutureBirthday(date)) return '生日不能晚于今天'
  }
  return ''
}

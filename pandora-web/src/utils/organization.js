export const businessRoles = ['FOUNDER', 'DEPT_HEAD', 'TEAM_LEADER', 'EMPLOYEE']
export const hasDepartment = role => role !== 'FOUNDER' && role !== 'ADMIN'
export const hasTeam = role => hasDepartment(role) && role !== 'DEPT_HEAD'
const field = (row, name) => Object.entries(row).find(([key]) => key.replaceAll('_', '').toLowerCase() === name.toLowerCase())?.[1]
const sameUnit = (a, b, key) => field(a, key) != null && String(field(a, key)) === String(field(b, key))

export function automaticSuperiors(form, people, userId) {
  const expected = { DEPT_HEAD: 'FOUNDER', TEAM_LEADER: 'DEPT_HEAD', EMPLOYEE: 'TEAM_LEADER' }[form.role]
  if (!expected) return []
  return people.filter(person => person.role === expected && String(person.id) !== String(userId)
    && field(person, 'approvalStatus') === 'APPROVED'
    && (expected === 'FOUNDER' || sameUnit(form, person, 'departmentId'))
    && (expected !== 'TEAM_LEADER' || sameUnit(form, person, 'teamId')))
    .sort((a, b) => Number(a.id) - Number(b.id))
}

export function organizationError(form) {
  if (!businessRoles.includes(form.role)) return '请选择有效公司岗位'
  if (hasDepartment(form.role) && !form.departmentId) return '请选择部门'
  if (hasTeam(form.role) && !form.teamId) return '请选择团队'
  return ''
}

export const departmentMembers = (people, id) => people.map(normalizeOrganization)
  .filter(person => businessRoles.includes(person.role) && hasDepartment(person.role) && String(person.department_id) === String(id))
  .sort(compareOrganizationMembers)
export const teamMembers = (people, id) => people.map(normalizeOrganization)
  .filter(person => businessRoles.includes(person.role) && hasTeam(person.role) && String(person.team_id) === String(id))
  .sort(compareOrganizationMembers)

// The summary chips and expanded member list receive the same stable post order.
export function compareOrganizationMembers(a, b) {
  const rank = { DEPT_HEAD: 0, TEAM_LEADER: 1, EMPLOYEE: 2 }
  return (rank[a.role] ?? 3) - (rank[b.role] ?? 3)
    || String(a.username ?? a.id ?? '').localeCompare(String(b.username ?? b.id ?? ''), 'zh-CN', { numeric: true })
    || Number(a.id ?? 0) - Number(b.id ?? 0)
}

// Keep old deployments/records readable without grouping leadership under fictitious units.
export function normalizeOrganization(person) {
  const result = { ...person }
  for (const key of Object.keys(result)) {
    const field = key.replaceAll('_', '').toLowerCase()
    if ((!hasDepartment(result.role) && ['departmentid', 'departmentname'].includes(field)) ||
        (!hasTeam(result.role) && ['teamid', 'teamname'].includes(field))) result[key] = null
  }
  for (const [name, key] of [['departmentId', 'department_id'], ['departmentName', 'department_name'], ['teamId', 'team_id'], ['teamName', 'team_name']]) {
    const value = field(result, name)
    if (value !== undefined) result[key] = value
  }
  return result
}

export function organizationPayload(form) {
  return { ...form, departmentId: hasDepartment(form.role) ? form.departmentId : null,
    teamId: hasTeam(form.role) ? form.teamId : null }
}

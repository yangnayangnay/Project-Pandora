import test from 'node:test'
import assert from 'node:assert/strict'
import { businessRoles, normalizeOrganization, organizationPayload, automaticSuperiors, departmentMembers, teamMembers, organizationError } from '../src/utils/organization.js'
import { accountPayload, emptyAccountForm } from '../src/utils/accountCreation.js'

test('founders have no department and heads have no team without mutating original records', () => {
  const original = { role: 'FOUNDER', department_id: 1, department_name: '研发', teamId: 2, teamName: '客户端' }
  const row = normalizeOrganization(original)
  assert.equal(row.department_id, null); assert.equal(row.teamId, null); assert.equal(original.department_id, 1)
  assert.deepEqual(organizationPayload({ role: 'DEPT_HEAD', departmentId: 1, teamId: 2 }), { role: 'DEPT_HEAD', departmentId: 1, teamId: null })
  assert.equal(normalizeOrganization({ role: 'EMPLOYEE', team_id: 2 }).team_id, 2)
})
test('account creation clears role-inapplicable stale form values', () => {
  const form = { ...emptyAccountForm(), role: 'FOUNDER', departmentId: 1, teamId: 2 }
  assert.equal(accountPayload(form).departmentId, null); assert.equal(accountPayload(form).teamId, null)
  form.role = 'DEPT_HEAD'; assert.equal(accountPayload(form).departmentId, 1); assert.equal(accountPayload(form).teamId, null)
})

test('department and team memberships ignore legacy leadership and administrator affiliations', () => {
  const people = ['ADMIN', 'FOUNDER', 'DEPT_HEAD', 'TEAM_LEADER', 'EMPLOYEE'].map((role, id) =>
    ({ id, role, departmentid: 10, teamid: 11 }))
  assert.deepEqual(departmentMembers(people, 10).map(person => person.role), ['DEPT_HEAD','TEAM_LEADER','EMPLOYEE'])
  assert.deepEqual(teamMembers(people, 11).map(person => person.role), ['TEAM_LEADER','EMPLOYEE'])
  assert.ok(!businessRoles.includes('ADMIN'))
  assert.equal(people[0].departmentid, 10)
})

test('automatic superior preview follows approved department and team posts for all response key styles', () => {
  const people = [
    { id: 1, role: 'ADMIN', approval_status: 'APPROVED' },
    { id: 2, role: 'FOUNDER', approval_status: 'APPROVED' },
    { id: 3, role: 'DEPT_HEAD', approvalstatus: 'APPROVED', department_id: 10 },
    { id: 4, role: 'TEAM_LEADER', approvalStatus: 'APPROVED', departmentId: 10, teamId: 11 },
    { id: 5, role: 'TEAM_LEADER', approval_status: 'PENDING', department_id: 10, team_id: 11 },
    { id: 6, role: 'TEAM_LEADER', approval_status: 'APPROVED', department_id: 20, team_id: 21 }
  ]
  const ids = form => automaticSuperiors(form, people, 9).map(person => person.id)
  assert.deepEqual(ids({ role: 'FOUNDER' }), [])
  assert.deepEqual(ids({ role: 'DEPT_HEAD', departmentId: 10 }), [2])
  assert.deepEqual(ids({ role: 'TEAM_LEADER', departmentId: '10', teamId: 11 }), [3])
  assert.deepEqual(ids({ role: 'EMPLOYEE', departmentId: 10, teamId: '11' }), [4])
  assert.deepEqual(ids({ role: 'EMPLOYEE', departmentId: 10, teamId: 21 }), [])
  assert.deepEqual(ids({ role: 'EMPLOYEE', departmentId: 10 }), [])
})

test('only fields applicable to the chosen post are required when saving an adjustment', () => {
  assert.equal(organizationError({ role: 'FOUNDER' }), '')
  assert.equal(organizationError({ role: 'DEPT_HEAD', departmentId: 10 }), '')
  assert.equal(organizationError({ role: 'EMPLOYEE', departmentId: 10, teamId: 11 }), '')
  assert.equal(organizationError({ role: 'DEPT_HEAD' }), '请选择部门')
  assert.equal(organizationError({ role: 'EMPLOYEE', departmentId: 10 }), '请选择团队')
  assert.equal(organizationError({ role: 'TEAM_LEADER', departmentId: 10 }), '请选择团队')
  assert.equal(organizationError({ role: 'ADMIN' }), '请选择有效公司岗位')
})

test('department and team summary and expanded members follow post order then employee number', () => {
  const people = [
    { id: 5, username: '000005', role: 'EMPLOYEE', department_id: 10, team_id: 11 },
    { id: 4, username: '000004', role: 'TEAM_LEADER', department_id: 10, team_id: 11 },
    { id: 3, username: '000003', role: 'EMPLOYEE', department_id: 10, team_id: 11 },
    { id: 9, username: '000009', role: 'DEPT_HEAD', department_id: 10, team_id: 11 },
    { id: 1, username: '000001', role: 'FOUNDER', department_id: 10, team_id: 11 }
  ]
  assert.deepEqual(departmentMembers(people, 10).map(p => p.id), [9, 4, 3, 5])
  assert.deepEqual(teamMembers(people, 11).map(p => p.id), [4, 3, 5])
  assert.deepEqual(people.map(p => p.id), [5, 4, 3, 9, 1])
})

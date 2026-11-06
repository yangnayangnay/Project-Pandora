import test from 'node:test'
import assert from 'node:assert/strict'
import { accountPayload, emptyAccountForm, teamsForDepartment, isFutureBirthday } from '../src/utils/accountCreation.js'

test('creation payload retains zeroes, raw password and all required personal and organization data', () => {
  const form = { employeeNo: ' 000123 ', password: ' secret ', role: 'EMPLOYEE', realName: ' 测试人员 ',
    email: ' employee@example.test ', phone: ' 13800000000 ', idCard: ' 11010119900101001x ', birthday: '1990-01-01', departmentId: 10, teamId: 11 }
  assert.deepEqual(accountPayload(form), { employeeNo: '000123', username: '000123', password: ' secret ', role: 'EMPLOYEE', realName: '测试人员',
    email: 'employee@example.test', phone: '13800000000', idCard: '11010119900101001X', birthday: '1990-01-01', departmentId: 10, teamId: 11 })
})
test('team options only include the selected department and support serialized IDs', () => {
  const teams = [{ id: 11, department_id: 10 }, { id: 12, departmentid: '10' }, { id: 21, departmentId: 20 }]
  assert.deepEqual(teamsForDepartment(teams, '10').map(team => team.id), [11, 12])
  assert.deepEqual(teamsForDepartment(teams, null), [])
  assert.deepEqual(teamsForDepartment(teams, 99), [])
})
test('reset provides a fresh complete form including cleared department and team', () => {
  const first = emptyAccountForm(); first.departmentId = 10; first.teamId = 11
  const second = emptyAccountForm()
  assert.equal(second.departmentId, null); assert.equal(second.teamId, null)
  assert.equal(second.employeeNo, ''); assert.equal(second.idCard, ''); assert.equal(second.birthday, '')
  assert.equal(Object.keys(second).length, 10)
})
test('birthday picker allows past dates and blocks future dates', () => {
  assert.equal(isFutureBirthday(new Date(1990, 0, 1)), false)
  assert.equal(isFutureBirthday(new Date(2999, 0, 1)), true)
})

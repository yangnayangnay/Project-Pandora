import test from 'node:test'
import assert from 'node:assert/strict'
import { accountProfileForm, profileChanges, profileFieldError } from '../src/utils/accountProfile.js'

test('editing makes a detached six-field form and preserves the employee number zeroes', () => {
  const row = { id: 2, username: '000123', real_name: '姓名甲', email: 'first@example.test', phone: '13800000000',
    id_card: '11010119900101001X', birthday: '1990-01-01', role: 'EMPLOYEE', password_hash: 'never-send' }
  const form = accountProfileForm(row)
  assert.equal(form.employeeNo, '000123'); assert.equal(Object.keys(form).length, 6)
  assert.equal(form.birthday, '1990-01-01'); form.realName = '姓名乙'; assert.equal(row.real_name, '姓名甲')
  assert.equal(form.password_hash, undefined)
})

test('payload only includes changes and leaves legacy missing fields intact', () => {
  const original = accountProfileForm({ username: 'legacy', real_name: '旧姓名' })
  const form = { ...original, realName: ' 新姓名 ', role: 'ADMIN', password: 'never-send' }
  assert.deepEqual(profileChanges(original, form), { realName: '新姓名' })
  assert.deepEqual(profileChanges(original, { ...original }), {})
})

test('edits trim contacts, uppercase identity and keep employee number as a string', () => {
  const changes = profileChanges(accountProfileForm({}), { employeeNo: ' 000789 ', realName: ' 姓名乙 ', email: ' new@example.test ',
    phone: ' 13900000000 ', idCard: ' 11010119900202003x ', birthday: '1992-02-29' })
  assert.deepEqual(changes, { employeeNo: '000789', realName: '姓名乙', email: 'new@example.test', phone: '13900000000',
    idCard: '11010119900202003X', birthday: '1992-02-29' })
  for (const [field, value] of Object.entries(changes)) assert.equal(profileFieldError(field, value), '', field)
})

test('changing a required field to empty remains a validation error', () => {
  for (const field of ['employeeNo', 'realName', 'email', 'phone', 'idCard', 'birthday']) {
    assert.ok(profileFieldError(field, ''))
    const changes = profileChanges({ [field]: 'original' }, { [field]: null })
    assert.equal(changes[field], '')
  }
})

test('format checks reject invalid contact, identity, date and oversized values', () => {
  for (const [field, value] of [['employeeNo', 'bad number'], ['employeeNo', 'x'.repeat(65)], ['realName', '名'.repeat(65)],
    ['email', 'invalid'], ['phone', '123'], ['idCard', '123'], ['birthday', '1990-02-30'], ['birthday', '2999-01-01'],
    ['birthday', '0000-01-01'], ['birthday', '1990-1-1']]) assert.ok(profileFieldError(field, value), `${field}: ${value}`)
})

test('reopening an editor does not retain the last account values', () => {
  const first = accountProfileForm({ username: '000123', email: 'first@example.test' }); first.phone = '13900000000'
  const second = accountProfileForm({ username: '000456' })
  assert.equal(second.employeeNo, '000456'); assert.equal(second.email, ''); assert.equal(second.phone, '')
})

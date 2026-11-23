import test from 'node:test'
import assert from 'node:assert/strict'
import { allocatedEmployeeNumber } from '../src/utils/accountCreation.js'

test('automatic number handles lowercase interceptor responses and raw server responses', () => {
  assert.equal(allocatedEmployeeNumber({ employeeno: '000002' }), '000002')
  assert.equal(allocatedEmployeeNumber({ employeeNo: '000003' }), '000003')
  assert.equal(allocatedEmployeeNumber({ employeeno: '1000000' }), '1000000')
})
test('invalid number response cannot silently create an undefined preview', () => {
  for (const data of [undefined, {}, { employeeno: 2 }, { employeeno: '' }, { employeeno: 'undefined' }]) {
    assert.throws(() => allocatedEmployeeNumber(data), /工号响应格式异常/)
  }
})

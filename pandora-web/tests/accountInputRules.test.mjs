import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { validRealName, validPassword, validIdCard } from '../src/utils/accountCreation.js'
import { identityRegionCodes } from '../src/utils/identityRegionCodes.js'

const identity = prefix => prefix + '10X98765432'[[...prefix].reduce((sum, digit, i) => sum + Number(digit) * [7,9,10,5,8,4,2,1,6,3,7,9,10,5,8,4,2][i], 0) % 11]
test('name accepts only 1 to 15 Han characters and password accepts 6 to 16 characters', () => {
  for (const value of ['吴六', '汉'.repeat(15), '𠮷野']) assert.equal(validRealName(value), true)
  for (const value of ['', '吴 六', 'Alice', '陈十1', '汉'.repeat(16), '吴·六']) assert.equal(validRealName(value), false)
  for (const value of ['123456', 'a'.repeat(16), '密'.repeat(16), '😀'.repeat(16)]) assert.equal(validPassword(value), true)
  for (const value of ['', '12345', 'a'.repeat(17), '😀'.repeat(17)]) assert.equal(validPassword(value), false)
})
test('identity validates county, historical county, date, retirement day, sequence and checksum', () => {
  const today = '2026-11-23'
  for (const prefix of ['11010119900101001', '11010319900101001', '11011519900101001', '11010119661124001', '11010120000229001'])
    assert.equal(validIdCard(identity(prefix), today), true, prefix)
  for (const prefix of ['11999919900101001', '11000019900101001', '11010119661123001', '11010119661122001', '11010120271123001', '11010120010229001', '11010119900101000'])
    assert.equal(validIdCard(identity(prefix), today), false, prefix)
  assert.equal(validIdCard('11010119900101001X', today), false)
  const lowercase = identity('11010119900203001').toLowerCase()
  assert.equal(validIdCard(lowercase, today), true)
})
test('web and backend use identical region codes', () => {
  const backend = readFileSync(new URL('../../pandora-backend/src/main/resources/identity/region-codes.txt', import.meta.url), 'utf8').trim().split(/\s+/)
  assert.deepEqual([...identityRegionCodes].sort(), backend.sort())
})

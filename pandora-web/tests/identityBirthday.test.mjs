import test from 'node:test'
import assert from 'node:assert/strict'
import { birthdayFromIdCard } from '../src/utils/accountCreation.js'
test('identity derives a real birthday and rejects malformed or impossible dates', () => {
  assert.equal(birthdayFromIdCard('11010119900203001x'), '1990-02-03')
  assert.equal(birthdayFromIdCard('110101200002290011'), '2000-02-29')
  for (const value of ['', '123', '110101190002290011', '110101199013010011', '110101299001010011']) assert.equal(birthdayFromIdCard(value), '')
})

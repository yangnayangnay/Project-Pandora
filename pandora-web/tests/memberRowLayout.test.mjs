import test from 'node:test'
import assert from 'node:assert/strict'
import { memberRowLayout } from '../src/utils/memberRowLayout.js'
test('shows all five members without an expand button when the actual cell is wide enough', () => {
  assert.deepEqual(memberRowLayout([124,124,140,100,100], 760, 60), { count:5, overflow:false })
  assert.deepEqual(memberRowLayout([100,100,100], 316, 60), { count:3, overflow:false })
})
test('reserves button space only when a row actually overflows and responds to resizing', () => {
  assert.deepEqual(memberRowLayout([100,100,100,100,100], 360, 60), { count:2, overflow:true })
  assert.deepEqual(memberRowLayout([180,80,80], 260, 60), { count:1, overflow:true })
  assert.deepEqual(memberRowLayout([100,100,100], 600, 60), { count:3, overflow:false })
  assert.deepEqual(memberRowLayout([200], 120, 60), { count:0, overflow:true })
})

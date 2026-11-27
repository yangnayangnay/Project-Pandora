import test from 'node:test'
import assert from 'node:assert/strict'
import { handoverAudit } from '../src/utils/handoverAudit.js'

test('handover displays different position and task recipients without merging them', () => {
  const row = { operation_type: 'USER_HANDOVER', operation_content: '离职工作交接：王八（工号000009）；岗位接任人：钱二（工号000003）；任务接收人：赵一（工号000002）；未完成任务：3项' }
  const result = handoverAudit(row)
  assert.equal(result.leaver, '王八（工号000009）')
  assert.equal(result.position, '钱二（工号000003）')
  assert.equal(result.tasks, '赵一（工号000002）')
  assert.equal(result.count, '3项')
  assert.equal(result.original, row.operation_content)
})

test('zero-task and old uppercase records remain meaningful', () => {
  const result = handoverAudit({ OPERATION_TYPE: 'USER_HANDOVER', OPERATION_CONTENT: '离职工作交接：王八；岗位接任人：钱二；任务接收人：无需交接；未完成任务：0项' })
  assert.equal(result.tasks, '无需交接')
  assert.equal(result.count, '0项')
})

test('missing, unrecognized and non-handover records use original text instead of invented fields', () => {
  assert.equal(handoverAudit({ operation_type: 'USER_HANDOVER', operation_content: '历史交接记录' }), null)
  assert.equal(handoverAudit({ operation_type: 'USER_HANDOVER', operation_content: '离职工作交接：王八；岗位接任人：钱二；任务接收人：赵一' }), null)
  assert.equal(handoverAudit({ operation_type: 'USER_DEPARTURE', operation_content: '办理离职：王八' }), null)
})

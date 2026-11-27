import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { mapOperationType, mapTargetType } from '../src/utils/auditLabelMap.js'

test('personnel audit labels match CSV export and the admin filter exposes lifecycle operations', () => {
  const backend = readFileSync(new URL('../../pandora-backend/src/main/java/com/pandora/backend/audit/AuditLabelMapper.java', import.meta.url), 'utf8')
  const page = readFileSync(new URL('../src/views/config/AuditLogView.vue', import.meta.url), 'utf8')
  for (const [type, label] of Object.entries({ USER_CREATE: '创建账号入职', USER_DEPARTURE: '办理离职', USER_HANDOVER: '离职工作交接', USER_RESTORE: '恢复账号' })) {
    assert.equal(mapOperationType(type), label)
    assert.ok(backend.includes(`Map.entry("${type}", "${label}")`))
    assert.ok(page.includes(`value: '${type}'`))
  }
  assert.equal(mapTargetType('DEPARTMENT'), '部门')
  assert.equal(mapTargetType('TEAM'), '团队')
})

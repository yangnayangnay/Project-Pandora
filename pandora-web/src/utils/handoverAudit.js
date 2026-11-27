/** Display historical audit text without rewriting records or guessing missing participants. */
export function handoverAudit(row) {
  if ((row.operation_type ?? row.OPERATION_TYPE) !== 'USER_HANDOVER') return null
  const text = String(row.operation_content ?? row.OPERATION_CONTENT ?? row.detail ?? '')
  const parts = text.split('；').map(part => part.trim())
  const field = label => parts.find(part => part.startsWith(`${label}：`))?.slice(label.length + 1).trim() || null
  const leaver = field('离职工作交接')
  const position = field('岗位接任人')
  const tasks = field('任务接收人')
  const count = field('未完成任务')
  if (!leaver || !position || !tasks || !/^\d+项$/.test(count ?? '')) return null
  return { leaver, position, tasks, count, original: text }
}

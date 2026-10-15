/**
 * 审计日志中文化映射工具（前端）。
 *
 * 与后端 AuditLabelMapper.java 保持一致的映射表内容。
 * 前端负责 UI 展示 + 筛选值转换，后端负责 CSV 导出，数据库存储英文不变。
 */

/**
 * 操作类型中英文映射表（共 58 个条目）。
 * 按 spec.md 6.1 节定义的 14 个分类组织，与后端 AuditLabelMapper.OPERATION_TYPE_MAP 完全一致。
 */
export const OPERATION_TYPE_MAP = {
  // 认证类（8个）
  LOGIN: '登录',
  LOGOUT: '登出',
  USER_REGISTER: '用户注册',
  AUTH_TOKEN_REFRESH: '令牌刷新',
  PASSWORD_RESET_REQUEST: '密码重置申请',
  PASSWORD_RESET_APPROVE: '密码重置审批通过',
  PASSWORD_RESET_REJECT: '密码重置审批拒绝',
  PASSWORD_RESET_EXECUTE: '密码重置执行',

  // 任务类（5个）
  TASK_CREATE: '任务创建',
  TASK_UPDATE: '任务更新',
  TASK_PROGRESS_UPDATE: '任务进度更新',
  TASK_TRANSITION: '任务状态流转',
  TASK_DELETE: '任务删除',

  // 工作日志类（7个）
  LOG_CREATE: '日志创建',
  LOG_UPDATE: '日志更新',
  LOG_DELETE: '日志删除',
  LOG_DRAFT_GENERATE: '日志草稿生成',
  LOG_AUTO_GENERATE: '日志自动生成',
  TOP10_UPDATE: 'Top10更新',
  TEST_DATA_CLEANUP: '测试数据清理',

  // 用户管理类（8个）
  USER_PROFILE_UPDATE: '用户资料更新',
  USER_AVATAR_UPDATE: '用户头像更新',
  USER_PASSWORD_CHANGE: '用户密码修改',
  USER_SETTINGS_UPDATE: '用户设置更新',
  USER_BIRTHDAY_UPDATE: '用户生日更新',
  USER_APPROVE: '用户审批通过',
  USER_REJECT: '用户审批拒绝',
  USER_BATCH_RENAME: '用户批量重命名',

  // 员工档案类（3个）
  EMPLOYEE_RECORD_CREATE: '员工档案创建',
  EMPLOYEE_RECORD_UPDATE: '员工档案更新',
  EMPLOYEE_RECORD_DELETE: '员工档案删除',

  // 层级管理类（2个）
  HIERARCHY_ADD: '层级新增',
  HIERARCHY_REMOVE: '层级移除',

  // 草稿与模板类（6个）
  DRAFT_SAVE: '草稿保存',
  DRAFT_DELETE: '草稿删除',
  TEMPLATE_SAVE: '模板保存',
  TEMPLATE_UPDATE: '模板更新',
  TEMPLATE_DELETE: '模板删除',
  TEMPLATE_MIGRATE: '模板迁移',

  // AI类（4个）
  AI_MBTI_SUBMIT: 'AI-MBTI提交',
  AI_MBTI_REPORT_GENERATE: 'AI-MBTI报告生成',
  AI_KEYWORD_ANALYSIS: 'AI关键词分析',
  AI_CONFIG_UPDATE: 'AI配置更新',

  // 数据同步类（1个）
  DATA_SYNC: '数据同步',

  // 公司重要事项类（3个，新增）
  COMPANY_IMPORTANT_CREATE: '公司重要事项创建',
  COMPANY_IMPORTANT_UPDATE: '公司重要事项更新',
  COMPANY_IMPORTANT_DELETE: '公司重要事项删除',

  // 公司派发类（4个，新增）
  COMPANY_DISPATCH_MARK: '公司派发标记',
  COMPANY_DISPATCH_UNMARK: '公司派发取消标记',
  COMPANY_DISPATCH_UPDATE: '公司派发更新',
  COMPANY_DISPATCH_CREATE: '公司派发创建',

  // 个人Top10类（3个，新增）
  PERSONAL_TOP10_INJECT: '个人Top10注入',
  PERSONAL_TOP10_UPDATE: '个人Top10更新',
  PERSONAL_TOP10_DELETE: '个人Top10删除',

  // 权限管理类（3个，新增）
  PERMISSION_SUBJECT_ADD: '权限主体新增',
  PERMISSION_SUBJECT_REMOVE: '权限主体移除',
  PERMISSION_SUBJECT_REPLACE: '权限主体替换',

  // 通知类（1个，新增）
  NOTIFICATION_MARK_READ: '通知标记已读'
}

/**
 * 目标类型中英文映射表（共 23 个条目）。
 * 包含 spec.md 6.2 节定义的 19 个目标类型 + 现有代码实际使用的 4 个扩展值，
 * 确保存量数据兼容。与后端 AuditLabelMapper.TARGET_TYPE_MAP 完全一致。
 */
export const TARGET_TYPE_MAP = {
  // spec.md 6.2 节定义的 19 个目标类型
  TASK: '任务',
  USER: '用户',
  LOG: '日志',
  HIERARCHY: '层级',
  DRAFT: '草稿',
  TEMPLATE: '模板',
  AI: 'AI',
  AI_CONFIG: 'AI配置',
  SYNC: '同步',
  APPROVAL: '审批',
  EMPLOYEE_RECORD: '员工档案',
  PASSWORD_RESET: '密码重置',
  COMPANY_IMPORTANT: '公司重要事项',
  COMPANY_DISPATCH: '公司派发',
  PERSONAL_TOP10: '个人Top10',
  PERMISSION: '权限',
  NOTIFICATION: '通知',
  TOKEN: '令牌',
  UNKNOWN: '未知',

  // 现有代码扩展的 4 个目标类型（存量数据兼容）
  WORK_LOG: '工作日志',
  TOP10_WORK: 'Top10工作',
  AI_ANALYSIS: 'AI分析',
  USER_HIERARCHY: '用户层级'
}

/**
 * 将英文操作类型标识转为中文显示文本，用于表格展示。
 *
 * 映射逻辑：
 * - en 为 null/undefined/空字符串 → 返回 "未知操作"（满足 REQ-AUDIT-CN-08）
 * - OPERATION_TYPE_MAP[en] 存在 → 返回中文
 * - 否则 → 返回 en（原始英文兜底，满足 REQ-AUDIT-CN-07）
 *
 * @param {string|null|undefined} en 英文操作类型标识
 * @returns {string} 中文显示文本或兜底值
 */
export function mapOperationType(en) {
  if (en === null || en === undefined || en === '') {
    return '未知操作'
  }
  const chinese = OPERATION_TYPE_MAP[en]
  return chinese !== undefined ? chinese : en
}

/**
 * 将英文目标类型标识转为中文显示文本，用于表格展示。
 *
 * 映射逻辑：
 * - en 为 null/undefined/空字符串 → 返回 "未知"
 * - en === "UNKNOWN" → 返回 "未知"（满足 REQ-AUDIT-CN-09）
 * - TARGET_TYPE_MAP[en] 存在 → 返回中文
 * - 否则 → 返回 en（原始英文兜底）
 *
 * @param {string|null|undefined} en 英文目标类型标识
 * @returns {string} 中文显示文本或兜底值
 */
export function mapTargetType(en) {
  if (en === null || en === undefined || en === '') {
    return '未知'
  }
  const chinese = TARGET_TYPE_MAP[en]
  return chinese !== undefined ? chinese : en
}

/**
 * 操作类型筛选下拉框选项数组（共 58 个 {label: 中文, value: 英文} 对象）。
 * 由 OPERATION_TYPE_MAP 转换生成，供下拉框使用（满足 REQ-AUDIT-CN-10，选项数 ≥ 55）。
 * label 显示中文（满足 REQ-AUDIT-CN-01），value 传英文给后端（满足 REQ-AUDIT-CN-02）。
 */
export const operationTypeOptions = Object.entries(OPERATION_TYPE_MAP).map(
  ([value, label]) => ({ label, value })
)
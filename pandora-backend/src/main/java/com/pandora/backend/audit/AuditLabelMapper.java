package com.pandora.backend.audit;

import java.util.Map;

/**
 * 审计日志中文化映射工具类。
 *
 * <p>持有 58 个操作类型 + 23 个目标类型的中英文映射表，
 * 供 CSV 导出调用，将英文标识转为中文显示文本。</p>
 *
 * <p>兜底策略：</p>
 * <ul>
 *   <li>未映射的英文标识 → 返回原始英文值（满足 REQ-AUDIT-CN-07）</li>
 *   <li>null/空字符串 → 返回空串 ""</li>
 *   <li>目标类型 "UNKNOWN" → 返回 "未知"</li>
 * </ul>
 */
public final class AuditLabelMapper {

    private AuditLabelMapper() {
        // 工具类，禁止实例化
    }

    /**
     * 操作类型中英文映射表（共 58 个条目）。
     * 按 spec.md 6.1 节定义的 14 个分类组织。
     */
    private static final Map<String, String> OPERATION_TYPE_MAP = Map.ofEntries(
            // 认证类（8个）
            Map.entry("LOGIN", "登录"),
            Map.entry("LOGOUT", "登出"),
            Map.entry("USER_REGISTER", "用户注册"),
            Map.entry("AUTH_TOKEN_REFRESH", "令牌刷新"),
            Map.entry("PASSWORD_RESET_REQUEST", "密码重置申请"),
            Map.entry("PASSWORD_RESET_APPROVE", "密码重置审批通过"),
            Map.entry("PASSWORD_RESET_REJECT", "密码重置审批拒绝"),
            Map.entry("PASSWORD_RESET_EXECUTE", "密码重置执行"),

            // 任务类（5个）
            Map.entry("TASK_CREATE", "任务创建"),
            Map.entry("TASK_UPDATE", "任务更新"),
            Map.entry("TASK_PROGRESS_UPDATE", "任务进度更新"),
            Map.entry("TASK_TRANSITION", "任务状态流转"),
            Map.entry("TASK_DELETE", "任务删除"),

            // 工作日志类（7个）
            Map.entry("LOG_CREATE", "日志创建"),
            Map.entry("LOG_UPDATE", "日志更新"),
            Map.entry("LOG_DELETE", "日志删除"),
            Map.entry("LOG_DRAFT_GENERATE", "日志草稿生成"),
            Map.entry("LOG_AUTO_GENERATE", "日志自动生成"),
            Map.entry("TOP10_UPDATE", "Top10更新"),
            Map.entry("TEST_DATA_CLEANUP", "测试数据清理"),

            // 用户管理类（8个）
            Map.entry("USER_PROFILE_UPDATE", "用户资料更新"),
            Map.entry("USER_AVATAR_UPDATE", "用户头像更新"),
            Map.entry("USER_PASSWORD_CHANGE", "用户密码修改"),
            Map.entry("USER_SETTINGS_UPDATE", "用户设置更新"),
            Map.entry("USER_BIRTHDAY_UPDATE", "用户生日更新"),
            Map.entry("USER_APPROVE", "用户审批通过"),
            Map.entry("USER_REJECT", "用户审批拒绝"),
            Map.entry("USER_BATCH_RENAME", "用户批量重命名"),

            // 员工档案类（3个）
            Map.entry("EMPLOYEE_RECORD_CREATE", "员工档案创建"),
            Map.entry("EMPLOYEE_RECORD_UPDATE", "员工档案更新"),
            Map.entry("EMPLOYEE_RECORD_DELETE", "员工档案删除"),

            // 层级管理类（2个）
            Map.entry("HIERARCHY_ADD", "层级新增"),
            Map.entry("HIERARCHY_REMOVE", "层级移除"),

            // 草稿与模板类（6个）
            Map.entry("DRAFT_SAVE", "草稿保存"),
            Map.entry("DRAFT_DELETE", "草稿删除"),
            Map.entry("TEMPLATE_SAVE", "模板保存"),
            Map.entry("TEMPLATE_UPDATE", "模板更新"),
            Map.entry("TEMPLATE_DELETE", "模板删除"),
            Map.entry("TEMPLATE_MIGRATE", "模板迁移"),

            // AI类（4个）
            Map.entry("AI_MBTI_SUBMIT", "AI-MBTI提交"),
            Map.entry("AI_MBTI_REPORT_GENERATE", "AI-MBTI报告生成"),
            Map.entry("AI_KEYWORD_ANALYSIS", "AI关键词分析"),
            Map.entry("AI_CONFIG_UPDATE", "AI配置更新"),

            // 数据同步类（1个）
            Map.entry("DATA_SYNC", "数据同步"),

            // 公司重要事项类（3个，新增）
            Map.entry("COMPANY_IMPORTANT_CREATE", "公司重要事项创建"),
            Map.entry("COMPANY_IMPORTANT_UPDATE", "公司重要事项更新"),
            Map.entry("COMPANY_IMPORTANT_DELETE", "公司重要事项删除"),

            // 公司派发类（4个，新增）
            Map.entry("COMPANY_DISPATCH_MARK", "公司派发标记"),
            Map.entry("COMPANY_DISPATCH_UNMARK", "公司派发取消标记"),
            Map.entry("COMPANY_DISPATCH_UPDATE", "公司派发更新"),
            Map.entry("COMPANY_DISPATCH_CREATE", "公司派发创建"),

            // 个人Top10类（3个，新增）
            Map.entry("PERSONAL_TOP10_INJECT", "个人Top10注入"),
            Map.entry("PERSONAL_TOP10_UPDATE", "个人Top10更新"),
            Map.entry("PERSONAL_TOP10_DELETE", "个人Top10删除"),

            // 权限管理类（3个，新增）
            Map.entry("PERMISSION_SUBJECT_ADD", "权限主体新增"),
            Map.entry("PERMISSION_SUBJECT_REMOVE", "权限主体移除"),
            Map.entry("PERMISSION_SUBJECT_REPLACE", "权限主体替换"),

            // 通知类（1个，新增）
            Map.entry("NOTIFICATION_MARK_READ", "通知标记已读")
    );

    /**
     * 目标类型中英文映射表（共 23 个条目）。
     * 包含 spec.md 6.2 节定义的 19 个目标类型 + 现有代码实际使用的 4 个扩展值，
     * 确保存量数据兼容。
     */
    private static final Map<String, String> TARGET_TYPE_MAP = Map.ofEntries(
            // spec.md 6.2 节定义的 19 个目标类型
            Map.entry("TASK", "任务"),
            Map.entry("USER", "用户"),
            Map.entry("LOG", "日志"),
            Map.entry("HIERARCHY", "层级"),
            Map.entry("DRAFT", "草稿"),
            Map.entry("TEMPLATE", "模板"),
            Map.entry("AI", "AI"),
            Map.entry("AI_CONFIG", "AI配置"),
            Map.entry("SYNC", "同步"),
            Map.entry("APPROVAL", "审批"),
            Map.entry("EMPLOYEE_RECORD", "员工档案"),
            Map.entry("PASSWORD_RESET", "密码重置"),
            Map.entry("COMPANY_IMPORTANT", "公司重要事项"),
            Map.entry("COMPANY_DISPATCH", "公司派发"),
            Map.entry("PERSONAL_TOP10", "个人Top10"),
            Map.entry("PERMISSION", "权限"),
            Map.entry("NOTIFICATION", "通知"),
            Map.entry("TOKEN", "令牌"),
            Map.entry("UNKNOWN", "未知"),

            // 现有代码扩展的 4 个目标类型（存量数据兼容）
            Map.entry("WORK_LOG", "工作日志"),
            Map.entry("TOP10_WORK", "Top10工作"),
            Map.entry("AI_ANALYSIS", "AI分析"),
            Map.entry("USER_HIERARCHY", "用户层级")
    );

    /**
     * 将英文操作类型标识转为中文显示文本，用于 CSV 导出。
     *
     * <p>映射逻辑：</p>
     * <ul>
     *   <li>输入 null 或空字符串 → 返回空字符串 ""</li>
     *   <li>输入在映射表中 → 返回对应中文（如 "TASK_CREATE" → "任务创建"）</li>
     *   <li>输入不在映射表中 → 返回原始英文值（兜底，满足 REQ-AUDIT-CN-07）</li>
     * </ul>
     *
     * @param en 英文操作类型标识，可能为 null
     * @return 中文字符串或兜底值
     */
    public static String toChineseOperationType(String en) {
        if (en == null || en.isEmpty()) {
            return "";
        }
        String chinese = OPERATION_TYPE_MAP.get(en);
        return chinese != null ? chinese : en;
    }

    /**
     * 将英文目标类型标识转为中文显示文本，用于 CSV 导出。
     *
     * <p>映射逻辑：</p>
     * <ul>
     *   <li>输入 null 或空字符串 → 返回空字符串 ""</li>
     *   <li>输入 "UNKNOWN" → 返回 "未知"</li>
     *   <li>输入在映射表中 → 返回对应中文（如 "TASK" → "任务"）</li>
     *   <li>输入不在映射表中 → 返回原始英文值（兜底）</li>
     * </ul>
     *
     * @param en 英文目标类型标识，可能为 null
     * @return 中文字符串或兜底值
     */
    public static String toChineseTargetType(String en) {
        if (en == null || en.isEmpty()) {
            return "";
        }
        String chinese = TARGET_TYPE_MAP.get(en);
        return chinese != null ? chinese : en;
    }
}
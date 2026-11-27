package com.pandora.backend.audit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuditService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public AuditService() {}
    public AuditService(JdbcTemplate db) { this.jdbcTemplate=db; }

    /**
     * System log visibility includes business edits and personnel administration.
     * Read acknowledgements and token-refresh noise remain excluded.
     */
    private static final List<String> DEFAULT_TYPE_WHITELIST = Arrays.asList(
            "TASK_CREATE", "TASK_UPDATE", "TASK_DELETE", "TASK_TRANSITION",
            "LOG_CREATE", "LOG_UPDATE", "LOG_DELETE",
            "USER_CREATE", "USER_DEPARTURE", "USER_HANDOVER", "USER_RESTORE", "USER_EMPLOYMENT_UPDATE",
            "USER_ORG_TRANSFER", "USER_PROFILE_ADMIN_UPDATE", "USER_APPROVE", "USER_REJECT", "USER_BATCH_RENAME",
            "DEPARTMENT_CREATE", "DEPARTMENT_UPDATE", "DEPARTMENT_DELETE", "TEAM_CREATE", "TEAM_UPDATE", "TEAM_DELETE",
            "EMPLOYEE_RECORD_CREATE", "EMPLOYEE_RECORD_UPDATE", "EMPLOYEE_RECORD_DELETE", "HIERARCHY_ADD", "HIERARCHY_REMOVE",
            "COMPANY_IMPORTANT_CREATE", "COMPANY_IMPORTANT_UPDATE", "COMPANY_IMPORTANT_DELETE",
            "COMPANY_DISPATCH_CREATE", "COMPANY_DISPATCH_UPDATE", "COMPANY_DISPATCH_MARK", "COMPANY_DISPATCH_UNMARK",
            "PERMISSION_SUBJECT_ADD", "PERMISSION_SUBJECT_REMOVE", "PERMISSION_SUBJECT_REPLACE"
    );

    public void record(Long operatorId, String action, String targetType, Long targetId, String detail) {
        record(operatorId, action, targetType, targetId, detail, null, null);
    }

    public void record(Long operatorId, String action, String targetType, Long targetId, String detail,
                       String operationType, String operationContent) {
        if (operationContent != null && operationContent.length() > 500) {
            operationContent = operationContent.substring(0, 500);
        }
        jdbcTemplate.update(
                "INSERT INTO audit_logs (operator_id, action, target_type, target_id, detail, operation_type, operation_content) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)",
                operatorId, action, targetType, targetId, detail, operationType, operationContent
        );
    }

    public Map<String, Object> queryAuditLogs(int page, int size, Long operatorId) {
        return queryAuditLogs(page, size, operatorId, null, null, null, null);
    }

    public Map<String, Object> queryAuditLogs(int page, int size, Long operatorId,
                                               String startTime, String endTime,
                                               String operationType, String keyword) {
        int offset = Math.max(0, (page - 1) * size);
        StringBuilder where = new StringBuilder();
        java.util.List<Object> params = new java.util.ArrayList<>();

        if (operatorId != null) {
            appendWhere(where, "a.operator_id = ?");
            params.add(operatorId);
        }
        if (startTime != null && !startTime.isEmpty()) {
            appendWhere(where, "a.operated_at >= ?");
            params.add(startTime);
        }
        if (endTime != null && !endTime.isEmpty()) {
            appendWhere(where, "a.operated_at <= ?");
            params.add(endTime);
        }
        // Round19 REQ-19-02: 默认三类白名单过滤
        // 当 operationType 未传入 → 附加白名单 IN 过滤
        // 当 operationType 传入但不在白名单内 → 仍按白名单过滤（防越权查看其他类型）
        // 当 operationType 传入且在白名单内 → 按指定类型精确过滤
        if (operationType != null && !operationType.isEmpty()
                && DEFAULT_TYPE_WHITELIST.contains(operationType)) {
            appendWhere(where, "a.operation_type = ?");
            params.add(operationType);
        } else {
            // 附加白名单 IN 过滤
            appendWhere(where, "a.operation_type IN (" + buildPlaceholders(DEFAULT_TYPE_WHITELIST.size()) + ")");
            params.addAll(DEFAULT_TYPE_WHITELIST);
        }
        if (keyword != null && !keyword.isEmpty()) {
            appendWhere(where, "a.operation_content LIKE ?");
            params.add("%" + keyword + "%");
        }

        String whereClause = where.toString();
        String sql = "SELECT a.*, u.username AS operator_name, u.real_name AS operator_real_name " +
                "FROM audit_logs a LEFT JOIN users u ON a.operator_id = u.id" +
                whereClause + " ORDER BY a.operated_at DESC LIMIT ? OFFSET ?";
        String countSql = "SELECT COUNT(*) FROM audit_logs a" + whereClause;

        java.util.List<Object> queryParams = new java.util.ArrayList<>(params);
        queryParams.add(size);
        queryParams.add(offset);

        java.util.List<Map<String, Object>> records = jdbcTemplate.queryForList(sql, queryParams.toArray());
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    public java.util.List<Map<String, Object>> exportAuditLogs(Long operatorId,
                                                                String startTime, String endTime,
                                                                String operationType, String keyword) {
        StringBuilder where = new StringBuilder();
        java.util.List<Object> params = new java.util.ArrayList<>();

        if (operatorId != null) {
            appendWhere(where, "a.operator_id = ?");
            params.add(operatorId);
        }
        if (startTime != null && !startTime.isEmpty()) {
            appendWhere(where, "a.operated_at >= ?");
            params.add(startTime);
        }
        if (endTime != null && !endTime.isEmpty()) {
            appendWhere(where, "a.operated_at <= ?");
            params.add(endTime);
        }
        // Round19 REQ-19-02: 导出同样应用白名单过滤
        if (operationType != null && !operationType.isEmpty()
                && DEFAULT_TYPE_WHITELIST.contains(operationType)) {
            appendWhere(where, "a.operation_type = ?");
            params.add(operationType);
        } else {
            appendWhere(where, "a.operation_type IN (" + buildPlaceholders(DEFAULT_TYPE_WHITELIST.size()) + ")");
            params.addAll(DEFAULT_TYPE_WHITELIST);
        }
        if (keyword != null && !keyword.isEmpty()) {
            appendWhere(where, "a.operation_content LIKE ?");
            params.add("%" + keyword + "%");
        }

        String whereClause = where.toString();
        String sql = "SELECT a.*, u.username AS operator_name, u.real_name AS operator_real_name " +
                "FROM audit_logs a LEFT JOIN users u ON a.operator_id = u.id" +
                whereClause + " ORDER BY a.operated_at DESC";

        return jdbcTemplate.queryForList(sql, params.toArray());
    }

    private String buildPlaceholders(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(",");
            sb.append("?");
        }
        return sb.toString();
    }

    private void appendWhere(StringBuilder where, String condition) {
        if (where.length() == 0) {
            where.append(" WHERE ").append(condition);
        } else {
            where.append(" AND ").append(condition);
        }
    }

    public java.util.List<Map<String, Object>> queryMyTodayAuditLogs(Long userId) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        String today = sdf.format(new java.util.Date());
        String startTime = today + " 00:00:00";
        String endTime = today + " 23:59:59";

        return jdbcTemplate.queryForList(
                "SELECT id, action, target_type, target_id, detail, operated_at, operation_type, operation_content " +
                "FROM audit_logs WHERE operator_id = ? AND operated_at >= ? AND operated_at <= ? ORDER BY operated_at ASC",
                userId, startTime, endTime
        );
    }
}

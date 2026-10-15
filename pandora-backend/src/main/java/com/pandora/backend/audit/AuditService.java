package com.pandora.backend.audit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuditService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
        if (operationType != null && !operationType.isEmpty()) {
            appendWhere(where, "a.operation_type = ?");
            params.add(operationType);
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
        if (operationType != null && !operationType.isEmpty()) {
            appendWhere(where, "a.operation_type = ?");
            params.add(operationType);
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
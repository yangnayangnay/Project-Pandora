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
        jdbcTemplate.update(
                "INSERT INTO audit_logs (operator_id, action, target_type, target_id, detail) VALUES (?, ?, ?, ?, ?)",
                operatorId, action, targetType, targetId, detail
        );
    }

    public Map<String, Object> queryAuditLogs(int page, int size, Long operatorId) {
        int offset = (page - 1) * size;
        StringBuilder sql = new StringBuilder("SELECT * FROM audit_logs");
        StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM audit_logs");

        java.util.List<Object> params = new java.util.ArrayList<>();
        if (operatorId != null) {
            sql.append(" WHERE operator_id = ?");
            countSql.append(" WHERE operator_id = ?");
            params.add(operatorId);
        }
        sql.append(" ORDER BY operated_at DESC LIMIT ? OFFSET ?");
        params.add(size);
        params.add(offset);

        java.util.List<Map<String, Object>> records = jdbcTemplate.queryForList(sql.toString(), params.toArray());
        Long total = jdbcTemplate.queryForObject(countSql.toString(), Long.class, operatorId != null ? new Object[]{operatorId} : new Object[]{});

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }
}
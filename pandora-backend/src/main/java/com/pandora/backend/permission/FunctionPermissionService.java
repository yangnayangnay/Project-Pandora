package com.pandora.backend.permission;

import com.pandora.backend.common.ErrorCode;
import com.pandora.backend.common.RoleChecker;
import com.pandora.backend.common.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FunctionPermissionService {

    private static final Logger log = LoggerFactory.getLogger(FunctionPermissionService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${pandora.permission.fallback-enabled:true}")
    private boolean fallbackEnabled;

    public boolean check(Long userId, String functionCode) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT 1 FROM function_permissions WHERE function_code = ? AND subject_user_id = ? AND deleted = 0",
                    functionCode, userId);
            if (!rows.isEmpty()) {
                return true;
            }
            if (fallbackEnabled) {
                return fallbackToRoleChecker(userId);
            }
            return false;
        } catch (Exception e) {
            log.warn("权限配置查询失败，降级回退RoleChecker: functionCode={}, userId={}, error={}", functionCode, userId, e.getMessage());
            if (fallbackEnabled) {
                return fallbackToRoleChecker(userId);
            }
            return false;
        }
    }

    private boolean fallbackToRoleChecker(Long userId) {
        try {
            Map<String, Object> user = jdbcTemplate.queryForMap("SELECT role FROM users WHERE id = ? AND deleted = 0", userId);
            String role = (String) user.get("ROLE");
            if (role == null) role = (String) user.get("role");
            return RoleChecker.isAdminOrFounder(role);
        } catch (Exception e) {
            log.warn("降级RoleChecker查询失败: userId={}", userId);
            return false;
        }
    }

    public List<Map<String, Object>> getSubjects(String functionCode) {
        return jdbcTemplate.queryForList(
                "SELECT fp.id, fp.subject_user_id, u.username, u.real_name, fp.configured_at " +
                "FROM function_permissions fp LEFT JOIN users u ON fp.subject_user_id = u.id " +
                "WHERE fp.function_code = ? AND fp.deleted = 0 ORDER BY fp.configured_at", functionCode);
    }

    public void addSubjects(String functionCode, List<Long> userIds, Long operatorId) {
        for (Long userId : userIds) {
            Integer exists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users WHERE id = ? AND deleted = 0", Integer.class, userId);
            if (exists == null || exists == 0) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND);
            }
            Integer already = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM function_permissions WHERE function_code = ? AND subject_user_id = ? AND deleted = 0",
                    Integer.class, functionCode, userId);
            if (already != null && already > 0) {
                continue;
            }
            jdbcTemplate.update(
                    "INSERT INTO function_permissions (function_code, subject_user_id, configured_by) VALUES (?, ?, ?)",
                    functionCode, userId, operatorId);
        }
    }

    public void removeSubjects(String functionCode, List<Long> userIds, Long operatorId) {
        for (Long userId : userIds) {
            jdbcTemplate.update(
                    "UPDATE function_permissions SET deleted = 1 WHERE function_code = ? AND subject_user_id = ? AND deleted = 0",
                    functionCode, userId);
        }
        validateAdminPreserved(functionCode);
    }

    public void replaceSubjects(String functionCode, List<Long> userIds, Long operatorId) {
        if (userIds == null || userIds.isEmpty()) {
            throw new BusinessException(ErrorCode.LAST_ADMIN_PROTECTION);
        }
        jdbcTemplate.update(
                "UPDATE function_permissions SET deleted = 1 WHERE function_code = ? AND deleted = 0", functionCode);
        addSubjects(functionCode, userIds, operatorId);
        validateAdminPreserved(functionCode);
    }

    private void validateAdminPreserved(String functionCode) {
        List<Map<String, Object>> subjects = getSubjects(functionCode);
        boolean hasAdmin = false;
        for (Map<String, Object> s : subjects) {
            Long uid = ((Number) s.get("SUBJECT_USER_ID")).longValue();
            if (uid == null) uid = ((Number) s.get("subject_user_id")).longValue();
            try {
                Map<String, Object> user = jdbcTemplate.queryForMap("SELECT role FROM users WHERE id = ?", uid);
                String role = (String) user.get("ROLE");
                if (role == null) role = (String) user.get("role");
                if ("ADMIN".equals(role)) {
                    hasAdmin = true;
                    break;
                }
            } catch (Exception ignored) {}
        }
        if (!hasAdmin) {
            throw new BusinessException(ErrorCode.LAST_ADMIN_PROTECTION);
        }
    }

    public List<Map<String, Object>> getHistory(String functionCode, String startDate, String endDate) {
        return jdbcTemplate.queryForList(
                "SELECT * FROM audit_logs WHERE operation_type LIKE 'PERMISSION_%' AND operation_content LIKE ? " +
                "AND operated_at BETWEEN ? AND ? ORDER BY operated_at DESC",
                "%" + functionCode + "%", startDate, endDate);
    }
}
package com.pandora.backend.permission;

import com.pandora.backend.common.ApiResponse;
import com.pandora.backend.common.BusinessException;

import com.pandora.backend.common.ErrorCode;
import com.pandora.backend.audit.Auditable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/approval")
public class ApprovalController {

    @Autowired
    private JdbcTemplate jdbcTemplate;


    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listPending() {
        List<Map<String, Object>> pending = jdbcTemplate.queryForList(
                "SELECT id, username, email, real_name, id_card, role, created_at " +
                        "FROM users WHERE approval_status = 'PENDING' AND deleted = 0 ORDER BY created_at");
        return ResponseEntity.ok(ApiResponse.success(pending));
    }

    @PostMapping("/approve/{userId}")
    @Auditable(operationType = "USER_APPROVE", targetType = "USER")
    public ResponseEntity<ApiResponse<Map<String, Object>>> approve(
            @PathVariable Long userId, @RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> user;
        try {
            user = jdbcTemplate.queryForMap(
                    "SELECT id, username, email, real_name, role, approval_status FROM users WHERE id = ? AND deleted = 0", userId);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        String currentStatus = (String) user.get("approval_status");
        if (!"PENDING".equals(currentStatus)) {
            throw new BusinessException(ErrorCode.INVALID_TRANSITION);
        }

        String role = (String) user.get("role");
        if (request != null && request.containsKey("role")) {
            role = (String) request.get("role");
        }

        jdbcTemplate.update("UPDATE users SET approval_status = 'APPROVED', role = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                role, userId);


        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("status", "APPROVED");
        result.put("role", role);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/reject/{userId}")
    @Auditable(operationType = "USER_REJECT", targetType = "USER")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reject(
            @PathVariable Long userId, @RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> user;
        try {
            user = jdbcTemplate.queryForMap(
                    "SELECT id, username, email, approval_status FROM users WHERE id = ? AND deleted = 0", userId);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        String currentStatus = (String) user.get("approval_status");
        if (!"PENDING".equals(currentStatus)) {
            throw new BusinessException(ErrorCode.INVALID_TRANSITION);
        }

        String reason = request != null ? (String) request.get("reason") : null;

        jdbcTemplate.update("UPDATE users SET approval_status = 'REJECTED', updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                userId);


        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("status", "REJECTED");
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/status/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatus(@PathVariable Long userId) {
        try {
            Map<String, Object> user = jdbcTemplate.queryForMap(
                    "SELECT id, username, approval_status, role FROM users WHERE id = ? AND deleted = 0", userId);
            return ResponseEntity.ok(ApiResponse.success(user));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }
}
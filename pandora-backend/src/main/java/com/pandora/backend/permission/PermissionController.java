package com.pandora.backend.permission;

import com.pandora.backend.common.ApiResponse;
import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/perm")
public class PermissionController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping("/invitation")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createInvitation(@RequestBody Map<String, Object> request) {
        Long fromUserId = getCurrentUserId();
        Object toUserIdObj = request.get("toUserId");
        String content = (String) request.get("content");

        if (toUserIdObj == null || content == null || content.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD);
        }

        Long toUserId = ((Number) toUserIdObj).longValue();

        jdbcTemplate.update(
                "INSERT INTO invitation_requests (from_user_id, to_user_id, content, status) VALUES (?, ?, ?, 'PENDING')",
                fromUserId, toUserId, content);

        Long invitationId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        Map<String, Object> result = new HashMap<>();
        result.put("id", invitationId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/invitation/list")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listInvitations() {
        Long userId = getCurrentUserId();
        List<Map<String, Object>> invitations = jdbcTemplate.queryForList(
                "SELECT * FROM invitation_requests WHERE to_user_id = ? AND deleted = 0 ORDER BY created_at DESC", userId);
        return ResponseEntity.ok(ApiResponse.success(invitations));
    }

    @PutMapping("/invitation/{invitationId}")
    public ResponseEntity<ApiResponse<Void>> respondInvitation(
            @PathVariable Long invitationId, @RequestBody Map<String, String> request) {
        String status = request.get("status");
        if (status == null || (!status.equals("APPROVED") && !status.equals("REJECTED"))) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD, "无效状态");
        }

        jdbcTemplate.update("UPDATE invitation_requests SET status = ? WHERE id = ?", status, invitationId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/audit-rule")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createAuditRule(@RequestBody Map<String, Object> request) {
        String triggerAction = (String) request.get("triggerAction");
        Integer sequence = (Integer) request.getOrDefault("sequence", 1);

        if (triggerAction == null) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD);
        }

        jdbcTemplate.update(
                "INSERT INTO audit_rules (trigger_action, sequence, enabled) VALUES (?, ?, 1)",
                triggerAction, sequence);

        Long ruleId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        @SuppressWarnings("unchecked")
        List<Integer> approverIds = (List<Integer>) request.get("approverIds");
        if (approverIds != null) {
            for (int i = 0; i < approverIds.size(); i++) {
                jdbcTemplate.update(
                        "INSERT INTO audit_rule_approvers (rule_id, approver_id, sequence) VALUES (?, ?, ?)",
                        ruleId, approverIds.get(i), i + 1);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", ruleId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/audit-rule/list")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listAuditRules() {
        List<Map<String, Object>> rules = jdbcTemplate.queryForList(
                "SELECT * FROM audit_rules WHERE deleted = 0 ORDER BY trigger_action, sequence");
        return ResponseEntity.ok(ApiResponse.success(rules));
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
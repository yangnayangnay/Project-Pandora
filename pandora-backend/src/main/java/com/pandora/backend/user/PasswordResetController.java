package com.pandora.backend.user;

import com.pandora.backend.audit.AuditService;
import com.pandora.backend.audit.Auditable;
import com.pandora.backend.auth.SessionStore;
import com.pandora.backend.common.ApiResponse;
import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 密码重置控制器——处理密码重置申请审批和执行。
 * 仅管理员可访问，包含申请列表、审批通过、审批拒绝、直接重置等功能。
 */
@RestController
@RequestMapping("/user")
public class PasswordResetController {

    // 默认重置密码
    private static final String DEFAULT_PASSWORD = "pandora123";

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    @Autowired
    private SessionStore sessionStore;
    @Autowired
    private AuditService auditService;

    /**
     * 获取密码重置申请列表。
     * 支持按状态筛选（PENDING/APPROVED/REJECTED）。
     *
     * @param status 状态筛选（可选）
     * @return 包含申请列表和总数的响应
     */
    @GetMapping("/reset-requests")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listResetRequests(
            @RequestParam(value = "status", required = false) String status) {
        requireAdmin();
        StringBuilder sql = new StringBuilder(
                "SELECT r.id, r.user_id, r.username, r.status, r.request_reason, r.reject_reason, " +
                        "r.processed_by, r.processed_at, r.created_at, " +
                        "u.real_name, u.email, u.role " +
                        "FROM password_reset_requests r LEFT JOIN users u ON r.user_id = u.id");
        List<Object> params = new ArrayList<>();
        if (status != null && !status.isEmpty()) {
            sql.append(" WHERE r.status = ?");
            params.add(status);
        }
        sql.append(" ORDER BY r.created_at DESC");
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql.toString(), params.toArray());
        Map<String, Object> data = new HashMap<>();
        data.put("records", list);
        data.put("total", list.size());
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 审批通过密码重置申请。
     * 业务规则：
     * - 仅待处理状态可审批
     * - 密码长度至少6位
     * - 移除目标用户会话（强制重新登录）
     *
     * @param requestId 申请ID
     * @param request 可选包含 newPassword 的请求体
     * @return 包含新密码的响应（前端需提示管理员告知用户）
     */
    @PostMapping("/reset-requests/{id}/approve")
    @Auditable(operationType = "PASSWORD_RESET_APPROVE", targetType = "USER")
    public ResponseEntity<ApiResponse<Map<String, Object>>> approveResetRequest(
            @PathVariable("id") Long requestId,
            @RequestBody(required = false) Map<String, String> request) {
        Long adminId = getCurrentUserId();
        requireAdmin();

        Map<String, Object> resetReq = findResetRequest(requestId);
        // 拦截已处理的申请
        if (!"PENDING".equals(resetReq.get("status"))) {
            throw new BusinessException(ErrorCode.RESET_REQUEST_ALREADY_PROCESSED);
        }
        Long targetUserId = resetReq.get("user_id") != null
                ? ((Number) resetReq.get("user_id")).longValue() : null;
        if (targetUserId == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 使用指定密码或默认密码
        String newPassword = (request != null && request.get("newPassword") != null
                && !request.get("newPassword").isEmpty())
                ? request.get("newPassword") : DEFAULT_PASSWORD;
        if (newPassword.length() < 6) {
            throw new BusinessException(ErrorCode.PASSWORD_TOO_SHORT);
        }

        String hash = passwordEncoder.encode(newPassword);
        jdbcTemplate.update("UPDATE users SET password_hash = ? WHERE id = ?", hash, targetUserId);
        jdbcTemplate.update(
                "UPDATE password_reset_requests SET status = 'APPROVED', processed_by = ?, processed_at = CURRENT_TIMESTAMP WHERE id = ?",
                adminId, requestId);
        // 移除会话，强制用户重新登录
        sessionStore.remove(targetUserId);
        auditService.record(adminId, "RESET_PASSWORD_APPROVE", "USER", targetUserId,
                "approved reset request #" + requestId + " for user " + resetReq.get("username"));

        Map<String, Object> data = new HashMap<>();
        data.put("newPassword", newPassword);
        data.put("message", "密码已重置为：" + newPassword + "，请告知用户使用新密码登录");
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 审批拒绝密码重置申请。
     *
     * @param requestId 申请ID
     * @param request 可选包含拒绝原因的请求体
     * @return 成功响应
     */
    @PostMapping("/reset-requests/{id}/reject")
    @Auditable(operationType = "PASSWORD_RESET_REJECT", targetType = "USER")
    public ResponseEntity<ApiResponse<Void>> rejectResetRequest(
            @PathVariable("id") Long requestId,
            @RequestBody(required = false) Map<String, String> request) {
        Long adminId = getCurrentUserId();
        requireAdmin();

        Map<String, Object> resetReq = findResetRequest(requestId);
        if (!"PENDING".equals(resetReq.get("status"))) {
            throw new BusinessException(ErrorCode.RESET_REQUEST_ALREADY_PROCESSED);
        }
        String reason = request != null ? request.get("reason") : null;
        jdbcTemplate.update(
                "UPDATE password_reset_requests SET status = 'REJECTED', reject_reason = ?, processed_by = ?, processed_at = CURRENT_TIMESTAMP WHERE id = ?",
                reason, adminId, requestId);
        auditService.record(adminId, "RESET_PASSWORD_REJECT", "USER",
                resetReq.get("user_id") != null ? ((Number) resetReq.get("user_id")).longValue() : null,
                "rejected reset request #" + requestId + " for " + resetReq.get("username"));
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 直接重置用户密码（无需申请）。
     * 业务规则：
     * - 管理员不能重置其他管理员密码（除非是本人）
     * - 密码长度至少6位
     *
     * @param targetUserId 目标用户ID
     * @param request 可选包含 newPassword 的请求体
     * @return 包含新密码的响应
     */
    @PostMapping("/{userId}/reset-password")
    @Auditable(operationType = "PASSWORD_RESET_EXECUTE", targetType = "USER")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resetUserPassword(
            @PathVariable("userId") Long targetUserId,
            @RequestBody(required = false) Map<String, String> request) {
        Long adminId = getCurrentUserId();
        requireAdmin();

        Map<String, Object> targetUser;
        try {
            targetUser = jdbcTemplate.queryForMap(
                    "SELECT id, username, role FROM users WHERE id = ? AND deleted = 0", targetUserId);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        // 保护机制：管理员不能重置其他管理员密码
        if ("ADMIN".equals(targetUser.get("role")) && !adminId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACTION);
        }

        String newPassword = (request != null && request.get("newPassword") != null
                && !request.get("newPassword").isEmpty())
                ? request.get("newPassword") : DEFAULT_PASSWORD;
        if (newPassword.length() < 6) {
            throw new BusinessException(ErrorCode.PASSWORD_TOO_SHORT);
        }

        String hash = passwordEncoder.encode(newPassword);
        jdbcTemplate.update("UPDATE users SET password_hash = ? WHERE id = ?", hash, targetUserId);
        sessionStore.remove(targetUserId);
        auditService.record(adminId, "RESET_PASSWORD_DIRECT", "USER", targetUserId,
                "directly reset password for user " + targetUser.get("username"));

        Map<String, Object> data = new HashMap<>();
        data.put("newPassword", newPassword);
        data.put("username", targetUser.get("username"));
        data.put("message", "用户 " + targetUser.get("username") + " 的密码已重置为：" + newPassword);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 查询密码重置申请详情。
     */
    private Map<String, Object> findResetRequest(Long id) {
        try {
            return jdbcTemplate.queryForMap(
                    "SELECT id, user_id, username, status, request_reason FROM password_reset_requests WHERE id = ?", id);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.RESET_REQUEST_NOT_FOUND);
        }
    }

    /**
     * 校验当前用户为管理员。
     */
    private void requireAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String role = auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        if (!"ADMIN".equals(role)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    /**
     * 获取当前用户ID。
     */
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
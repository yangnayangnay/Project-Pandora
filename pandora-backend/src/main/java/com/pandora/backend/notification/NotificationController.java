package com.pandora.backend.notification;

import com.pandora.backend.audit.Auditable;
import com.pandora.backend.common.ApiResponse;
import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/notification")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * TASK-R18-006: 5 类通知类型枚举（与 NotificationService 保持一致）
     */
    private static final List<String> NOTIFICATION_TYPES = Arrays.asList(
            "NEW_TASK", "STATUS_CHANGE", "LOG_COMPLETED",
            "TOP10_DISPATCH_CHANGED", "TOP10_IMPORTANT_CHANGED");

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listNotifications(
            @RequestParam(required = false) Boolean unreadOnly) {
        Long userId = getCurrentUserId();
        StringBuilder sql = new StringBuilder("SELECT * FROM notifications WHERE target_user_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(userId);
        if (Boolean.TRUE.equals(unreadOnly)) {
            sql.append(" AND is_read = 0");
        }
        sql.append(" ORDER BY created_at DESC");
        List<Map<String, Object>> notifications = jdbcTemplate.queryForList(sql.toString(), params.toArray());
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @GetMapping("/poll")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> pollNotifications(
            @RequestParam(required = false) Long lastId) {
        Long userId = getCurrentUserId();
        StringBuilder sql = new StringBuilder("SELECT * FROM notifications WHERE target_user_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(userId);
        if (lastId != null) {
            sql.append(" AND id > ?");
            params.add(lastId);
        }
        sql.append(" ORDER BY created_at DESC");
        List<Map<String, Object>> notifications = jdbcTemplate.queryForList(sql.toString(), params.toArray());
        ApiResponse<List<Map<String, Object>>> response = ApiResponse.success(notifications);
        // D-33-02 修复：填充全局通知 MAX(id) 基线，App 端游标超前时自适应重置（迁移基线差异防拉空）
        Long globalMaxId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM notifications", Long.class);
        response.setMaxNotificationId(globalMaxId != null ? globalMaxId : 0L);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/read")
    @Auditable(operationType = "NOTIFICATION_MARK_READ", targetType = "NOTIFICATION")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        int updated = jdbcTemplate.update(
                "UPDATE notifications SET is_read = 1 WHERE id = ? AND target_user_id = ?",
                id, userId);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "通知不存在或无权操作");
        }
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * TASK-R18-006: 查询通知偏好
     * 返回当前用户 5 类通知的开关状态；无偏好记录的类型默认返回 true（开启）。
     * 仅本人可查（沿用 JWT 鉴权）。
     */
    @GetMapping("/preferences")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> getPreferences() {
        Long userId = getCurrentUserId();
        Map<String, Boolean> result = new LinkedHashMap<>();
        // 默认全部开启
        for (String type : NOTIFICATION_TYPES) {
            result.put(type, true);
        }
        // 查询已有偏好记录覆盖默认值
        try {
            List<Map<String, Object>> prefs = jdbcTemplate.queryForList(
                    "SELECT type, enabled FROM notification_preferences WHERE user_id = ?", userId);
            for (Map<String, Object> pref : prefs) {
                Object typeObj = pref.get("type") != null ? pref.get("type") : pref.get("TYPE");
                Object enabledObj = pref.get("enabled") != null ? pref.get("enabled") : pref.get("ENABLED");
                if (typeObj != null && enabledObj != null) {
                    String type = typeObj.toString();
                    if (result.containsKey(type)) {
                        result.put(type, ((Number) enabledObj).intValue() == 1);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("查询通知偏好失败，返回默认全开：userId={}", userId, e);
        }
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * TASK-R18-006: 保存通知偏好
     * 接收 Map<String, Boolean>（5 类开关，Boolean 强约束），逐类型 upsert。
     * 每次保存加 @Auditable 审计（6.3-4），非法 type 键返回 MISSING_REQUIRED_FIELD（400），
     * 保存失败抛业务异常（触发 Android 开关回弹）。
     */
    @PutMapping("/preferences")
    @Auditable(operationType = "NOTIFICATION_PREFERENCES_UPDATE", targetType = "NOTIFICATION_PREFERENCE")
    public ResponseEntity<ApiResponse<Void>> updatePreferences(@RequestBody Map<String, Object> request) {
        Long userId = getCurrentUserId();

        for (Map.Entry<String, Object> entry : request.entrySet()) {
            String type = entry.getKey();
            // 非法 type 键校验
            if (!NOTIFICATION_TYPES.contains(type)) {
                throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD, "非法通知类型：" + type);
            }
            // Boolean 强约束
            Object valueObj = entry.getValue();
            if (!(valueObj instanceof Boolean)) {
                throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD, "通知开关值必须为布尔类型：" + type);
            }
            boolean enabled = (Boolean) valueObj;

            // 逐类型 upsert（先查后插/更新）
            try {
                Integer existing = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM notification_preferences WHERE user_id = ? AND type = ?",
                        Integer.class, userId, type);
                if (existing != null && existing > 0) {
                    jdbcTemplate.update(
                            "UPDATE notification_preferences SET enabled = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ? AND type = ?",
                            enabled ? 1 : 0, userId, type);
                } else {
                    jdbcTemplate.update(
                            "INSERT INTO notification_preferences (user_id, type, enabled) VALUES (?, ?, ?)",
                            userId, type, enabled ? 1 : 0);
                }
            } catch (Exception e) {
                log.error("保存通知偏好失败：userId={}, type={}", userId, type, e);
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "保存失败，请重试");
            }
        }
        return ResponseEntity.ok(ApiResponse.success());
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
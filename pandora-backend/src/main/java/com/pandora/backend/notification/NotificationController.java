package com.pandora.backend.notification;

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
@RequestMapping("/notification")
public class NotificationController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @PutMapping("/{id}/read")
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

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
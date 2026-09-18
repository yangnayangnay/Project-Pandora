package com.pandora.backend.user;

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
@RequestMapping("/user")
public class UserController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser() {
        Long userId = getCurrentUserId();
        try {
            Map<String, Object> user = jdbcTemplate.queryForMap(
                    "SELECT id, username, role, department_id, team_id, avatar, theme_mode, manual_theme " +
                    "FROM users WHERE id = ? AND deleted = 0", userId);
            return ResponseEntity.ok(ApiResponse.success(user));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long teamId) {
        StringBuilder sql = new StringBuilder("SELECT id, username, role, department_id, team_id, avatar FROM users WHERE deleted = 0");
        List<Object> params = new ArrayList<>();

        if (role != null) {
            sql.append(" AND role = ?");
            params.add(role);
        }
        if (departmentId != null) {
            sql.append(" AND department_id = ?");
            params.add(departmentId);
        }
        if (teamId != null) {
            sql.append(" AND team_id = ?");
            params.add(teamId);
        }
        sql.append(" ORDER BY username");

        List<Map<String, Object>> users = jdbcTemplate.queryForList(sql.toString(), params.toArray());
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/{userId}/panel")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserPanel(@PathVariable Long userId) {
        try {
            Map<String, Object> user = jdbcTemplate.queryForMap(
                    "SELECT id, username, role, department_id, team_id, avatar FROM users WHERE id = ? AND deleted = 0", userId);

            List<Map<String, Object>> tasks = jdbcTemplate.queryForList(
                    "SELECT * FROM tasks WHERE assignee_id = ? AND deleted = 0 ORDER BY created_at DESC LIMIT 20", userId);

            List<Map<String, Object>> logs = jdbcTemplate.queryForList(
                    "SELECT * FROM work_logs WHERE user_id = ? AND deleted = 0 ORDER BY log_date DESC LIMIT 20", userId);

            List<Map<String, Object>> top10 = jdbcTemplate.queryForList(
                    "SELECT * FROM top10_works WHERE user_id = ? AND deleted = 0 ORDER BY rank_order", userId);

            Map<String, Object> panel = new HashMap<>();
            panel.put("user", user);
            panel.put("tasks", tasks);
            panel.put("logs", logs);
            panel.put("top10Works", top10);

            return ResponseEntity.ok(ApiResponse.success(panel));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<Void>> updateProfile(@RequestBody Map<String, Object> request) {
        Long userId = getCurrentUserId();
        String username = (String) request.get("username");
        String avatar = (String) request.get("avatar");

        if (username != null) {
            jdbcTemplate.update("UPDATE users SET username = ? WHERE id = ?", username, userId);
        }
        if (avatar != null) {
            jdbcTemplate.update("UPDATE users SET avatar = ? WHERE id = ?", avatar, userId);
        }

        return ResponseEntity.ok(ApiResponse.success());
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
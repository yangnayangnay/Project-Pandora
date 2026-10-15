package com.pandora.backend.top10;

import com.pandora.backend.common.ApiResponse;
import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.ErrorCode;
import com.pandora.backend.common.RoleChecker;
import com.pandora.backend.permission.FunctionCode;
import com.pandora.backend.permission.FunctionPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/top10")
public class Top10Controller {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FunctionPermissionService functionPermissionService;

    @GetMapping("/company-important")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCompanyImportant() {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT * FROM top10_works WHERE work_type = 'COMPANY_IMPORTANT' AND deleted = 0 ORDER BY rank_order");
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PostMapping("/company-important")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createCompanyImportant(@RequestBody Map<String, Object> request) {
        Long userId = getCurrentUserId();
        if (!functionPermissionService.check(userId, FunctionCode.COMPANY_IMPORTANT_EDIT.getCode())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(403, "无权操作"));
        }

        String content = (String) request.get("content");
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.TOP10_CONTENT_EMPTY);
        }

        Integer rankOrder = (Integer) request.get("rankOrder");
        if (rankOrder == null || rankOrder < 1 || rankOrder > 10) {
            throw new BusinessException(ErrorCode.TOP10_LIMIT_EXCEEDED);
        }

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM top10_works WHERE work_type = 'COMPANY_IMPORTANT' AND deleted = 0",
                Integer.class);
        if (count != null && count >= 10) {
            throw new BusinessException(ErrorCode.TOP10_LIMIT_EXCEEDED);
        }

        jdbcTemplate.update(
                "INSERT INTO top10_works (user_id, work_type, rank_order, content) VALUES (?, 'COMPANY_IMPORTANT', ?, ?)",
                userId, rankOrder, content.trim());

        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PutMapping("/company-important/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateCompanyImportant(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Long userId = getCurrentUserId();
        if (!functionPermissionService.check(userId, FunctionCode.COMPANY_IMPORTANT_EDIT.getCode())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(403, "无权操作"));
        }

        List<Map<String, Object>> existing = jdbcTemplate.queryForList(
                "SELECT * FROM top10_works WHERE id = ? AND work_type = 'COMPANY_IMPORTANT' AND deleted = 0", id);
        if (existing.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "记录不存在");
        }

        String content = (String) request.get("content");
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.TOP10_CONTENT_EMPTY);
        }

        Integer rankOrder = (Integer) request.get("rankOrder");
        if (rankOrder == null || rankOrder < 1 || rankOrder > 10) {
            throw new BusinessException(ErrorCode.TOP10_LIMIT_EXCEEDED);
        }

        jdbcTemplate.update(
                "UPDATE top10_works SET content = ?, rank_order = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                content.trim(), rankOrder, id);

        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @DeleteMapping("/company-important/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCompanyImportant(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        if (!functionPermissionService.check(userId, FunctionCode.COMPANY_IMPORTANT_EDIT.getCode())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(403, "无权操作"));
        }

        int updated = jdbcTemplate.update(
                "UPDATE top10_works SET deleted = 1, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND work_type = 'COMPANY_IMPORTANT' AND deleted = 0", id);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "记录不存在");
        }
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/company-dispatch")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCompanyDispatch() {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT t.*, tk.name AS task_name, u.real_name AS assignee_name, u.username AS assignee_username " +
                "FROM top10_works t LEFT JOIN tasks tk ON t.related_task_id = tk.id " +
                "LEFT JOIN users u ON tk.assignee_id = u.id " +
                "WHERE t.work_type = 'COMPANY_DISPATCH' AND t.deleted = 0 ORDER BY t.rank_order");
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PostMapping("/company-dispatch/mark")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markCompanyDispatch(@RequestBody Map<String, Object> request) {
        Long userId = getCurrentUserId();
        if (!functionPermissionService.check(userId, FunctionCode.COMPANY_DISPATCH_EDIT.getCode())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(403, "无权操作"));
        }

        Object taskIdObj = request.get("taskId");
        if (taskIdObj == null) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD, "taskId不能为空");
        }
        Long taskId = ((Number) taskIdObj).longValue();

        List<Map<String, Object>> task = jdbcTemplate.queryForList(
                "SELECT id FROM tasks WHERE id = ? AND deleted = 0", taskId);
        if (task.isEmpty()) {
            throw new BusinessException(ErrorCode.TOP10_TASK_NOT_FOUND);
        }

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM top10_works WHERE work_type = 'COMPANY_DISPATCH' AND deleted = 0",
                Integer.class);
        if (count != null && count >= 10) {
            throw new BusinessException(ErrorCode.TOP10_LIMIT_EXCEEDED);
        }

        Integer rankOrder = (Integer) request.get("rankOrder");
        if (rankOrder == null) {
            rankOrder = (count != null ? count + 1 : 1);
        }

        jdbcTemplate.update(
                "INSERT INTO top10_works (user_id, work_type, rank_order, content, related_task_id) VALUES (?, 'COMPANY_DISPATCH', ?, ?, ?)",
                userId, rankOrder, "", taskId);

        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("taskId", taskId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @DeleteMapping("/company-dispatch/mark/{taskId}")
    public ResponseEntity<ApiResponse<Void>> unmarkCompanyDispatch(@PathVariable Long taskId) {
        Long userId = getCurrentUserId();
        if (!functionPermissionService.check(userId, FunctionCode.COMPANY_DISPATCH_EDIT.getCode())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(403, "无权操作"));
        }

        jdbcTemplate.update(
                "UPDATE top10_works SET deleted = 1, updated_at = CURRENT_TIMESTAMP WHERE work_type = 'COMPANY_DISPATCH' AND related_task_id = ? AND deleted = 0",
                taskId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PutMapping("/company-dispatch/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateCompanyDispatch(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Long userId = getCurrentUserId();
        if (!functionPermissionService.check(userId, FunctionCode.COMPANY_DISPATCH_EDIT.getCode())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(403, "无权操作"));
        }

        List<Map<String, Object>> existing = jdbcTemplate.queryForList(
                "SELECT * FROM top10_works WHERE id = ? AND work_type = 'COMPANY_DISPATCH' AND deleted = 0", id);
        if (existing.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "记录不存在");
        }

        String content = (String) request.get("content");
        Integer rankOrder = (Integer) request.get("rankOrder");

        StringBuilder sql = new StringBuilder("UPDATE top10_works SET updated_at = CURRENT_TIMESTAMP");
        List<Object> params = new ArrayList<>();

        if (content != null && !content.trim().isEmpty()) {
            sql.append(", content = ?");
            params.add(content.trim());
        }
        if (rankOrder != null && rankOrder >= 1 && rankOrder <= 10) {
            sql.append(", rank_order = ?");
            params.add(rankOrder);
        }

        sql.append(" WHERE id = ?");
        params.add(id);
        jdbcTemplate.update(sql.toString(), params.toArray());

        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/company-dispatch/create")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createAndMarkCompanyDispatch(@RequestBody Map<String, Object> request) {
        Long userId = getCurrentUserId();
        if (!functionPermissionService.check(userId, FunctionCode.COMPANY_DISPATCH_EDIT.getCode())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(403, "无权操作"));
        }

        String name = (String) request.get("name");
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD, "任务名称不能为空");
        }

        Object assigneeIdObj = request.get("assigneeId");
        if (assigneeIdObj == null) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD, "承接人不能为空");
        }
        Long assigneeId = ((Number) assigneeIdObj).longValue();

        String priority = (String) request.getOrDefault("priority", "MEDIUM");
        String endTimeStr = (String) request.get("endTime");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM top10_works WHERE work_type = 'COMPANY_DISPATCH' AND deleted = 0",
                Integer.class);
        if (count != null && count >= 10) {
            throw new BusinessException(ErrorCode.TOP10_LIMIT_EXCEEDED);
        }

        jdbcTemplate.update(
                "INSERT INTO tasks (name, priority, status, assignee_id, dispatcher_id, start_time, end_time) " +
                "VALUES (?, ?, 'PENDING', ?, ?, CURRENT_TIMESTAMP, ?)",
                name.trim(), priority, assigneeId, userId, endTimeStr);

        Long taskId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        Integer rankOrder = (Integer) request.get("rankOrder");
        if (rankOrder == null) {
            rankOrder = (count != null ? count + 1 : 1);
        }

        jdbcTemplate.update(
                "INSERT INTO top10_works (user_id, work_type, rank_order, content, related_task_id) VALUES (?, 'COMPANY_DISPATCH', ?, ?, ?)",
                userId, rankOrder, name.trim(), taskId);

        Long top10Id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        Map<String, Object> result = new HashMap<>();
        result.put("taskId", taskId);
        result.put("top10Id", top10Id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }

    private String getCurrentUserRole() {
        Long userId = getCurrentUserId();
        try {
            Map<String, Object> user = jdbcTemplate.queryForMap("SELECT role FROM users WHERE id = ?", userId);
            return (String) user.get("role");
        } catch (Exception e) {
            return null;
        }
    }
}
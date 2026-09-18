package com.pandora.backend.task;

import com.pandora.backend.common.ApiResponse;
import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.ErrorCode;
import com.pandora.backend.common.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/task")
public class TaskController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> createTask(@RequestBody Map<String, Object> taskRequest) {
        String name = (String) taskRequest.get("name");
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD, "任务名称不能为空");
        }

        String priority = (String) taskRequest.getOrDefault("priority", "MEDIUM");
        String assigneeStr = taskRequest.get("assigneeId") != null ? taskRequest.get("assigneeId").toString() : null;
        if (assigneeStr == null) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD, "责任人不能为空");
        }
        Long assigneeId = Long.parseLong(assigneeStr);

        Long dispatcherId = getCurrentUserId();
        Boolean isImportant = (Boolean) taskRequest.getOrDefault("isImportant", false);
        Boolean isUrgent = (Boolean) taskRequest.getOrDefault("isUrgent", false);

        String sql = "INSERT INTO tasks (name, priority, status, assignee_id, dispatcher_id, is_important, is_urgent) VALUES (?, ?, 'PENDING', ?, ?, ?, ?)";
        jdbcTemplate.update(sql, name, priority, assigneeId, dispatcherId, isImportant ? 1 : 0, isUrgent ? 1 : 0);

        Long taskId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        Map<String, Object> result = new HashMap<>();
        result.put("id", taskId);
        result.put("name", name);
        result.put("priority", priority);
        result.put("status", "PENDING");
        result.put("assigneeId", assigneeId);
        result.put("dispatcherId", dispatcherId);
        result.put("isImportant", isImportant);
        result.put("isUrgent", isUrgent);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listTasks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long assigneeId) {
        StringBuilder sql = new StringBuilder("SELECT * FROM tasks WHERE deleted = 0");
        List<Object> params = new ArrayList<>();

        if (status != null) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        if (assigneeId != null) {
            sql.append(" AND assignee_id = ?");
            params.add(assigneeId);
        }
        sql.append(" ORDER BY created_at DESC");

        List<Map<String, Object>> tasks = jdbcTemplate.queryForList(sql.toString(), params.toArray());
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTask(@PathVariable Long taskId) {
        try {
            Map<String, Object> task = jdbcTemplate.queryForMap("SELECT * FROM tasks WHERE id = ? AND deleted = 0", taskId);
            return ResponseEntity.ok(ApiResponse.success(task));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在");
        }
    }

    @PatchMapping("/{taskId}/progress")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateProgress(
            @PathVariable Long taskId, @RequestBody Map<String, Object> request) {
        String progressNote = (String) request.get("progressNote");
        String status = (String) request.get("status");

        Long currentUserId = getCurrentUserId();
        Map<String, Object> task = jdbcTemplate.queryForMap("SELECT assignee_id FROM tasks WHERE id = ? AND deleted = 0", taskId);
        Long assigneeId = ((Number) task.get("assignee_id")).longValue();

        if (!currentUserId.equals(assigneeId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACTION, "仅责任人可更新进度");
        }

        if (progressNote != null) {
            jdbcTemplate.update("UPDATE tasks SET progress_note = ? WHERE id = ?", progressNote, taskId);
        }
        if (status != null) {
            jdbcTemplate.update("UPDATE tasks SET status = ? WHERE id = ?", status, taskId);
        }

        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/quadrant")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getQuadrantTasks() {
        Long currentUserId = getCurrentUserId();

        List<Map<String, Object>> importantUrgent = jdbcTemplate.queryForList(
                "SELECT * FROM tasks WHERE assignee_id = ? AND is_important = 1 AND is_urgent = 1 AND deleted = 0", currentUserId);
        List<Map<String, Object>> importantNotUrgent = jdbcTemplate.queryForList(
                "SELECT * FROM tasks WHERE assignee_id = ? AND is_important = 1 AND is_urgent = 0 AND deleted = 0", currentUserId);
        List<Map<String, Object>> notImportantUrgent = jdbcTemplate.queryForList(
                "SELECT * FROM tasks WHERE assignee_id = ? AND is_important = 0 AND is_urgent = 1 AND deleted = 0", currentUserId);
        List<Map<String, Object>> notImportantNotUrgent = jdbcTemplate.queryForList(
                "SELECT * FROM tasks WHERE assignee_id = ? AND is_important = 0 AND is_urgent = 0 AND deleted = 0", currentUserId);

        Map<String, Object> result = new HashMap<>();
        result.put("importantUrgent", importantUrgent);
        result.put("importantNotUrgent", importantNotUrgent);
        result.put("notImportantUrgent", notImportantUrgent);
        result.put("notImportantNotUrgent", notImportantNotUrgent);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{taskId}/transition")
    public ResponseEntity<ApiResponse<Map<String, Object>>> transitionStatus(
            @PathVariable Long taskId, @RequestBody Map<String, String> request) {
        String targetStatus = request.get("status");
        if (targetStatus == null) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD, "目标状态不能为空");
        }

        jdbcTemplate.update("UPDATE tasks SET status = ? WHERE id = ? AND deleted = 0", targetStatus, taskId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
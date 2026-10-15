package com.pandora.backend.task;

import com.pandora.backend.common.ApiResponse;
import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.ErrorCode;
import com.pandora.backend.common.PageResult;
import com.pandora.backend.audit.Auditable;
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

    @Autowired
    private com.pandora.backend.notification.NotificationService notificationService;

    @Autowired
    private com.pandora.backend.draft.DraftService draftService;

    @Autowired
    private com.pandora.backend.hierarchy.HierarchyService hierarchyService;

    @PostMapping
    @Auditable(operationType = "TASK_CREATE", targetType = "TASK")
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
        if (!assigneeId.equals(dispatcherId) && !hierarchyService.getSubordinateIds(dispatcherId).contains(assigneeId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACTION, "无权分配给该用户");
        }

        Boolean isImportant = (Boolean) taskRequest.getOrDefault("isImportant", false);
        Boolean isUrgent = (Boolean) taskRequest.getOrDefault("isUrgent", false);

        Object endTimeObj = taskRequest.get("endTime");
        Object startTimeObj = taskRequest.get("startTime");

        String sql = "INSERT INTO tasks (name, priority, status, assignee_id, dispatcher_id, is_important, is_urgent" +
                (endTimeObj != null ? ", end_time" : "") +
                (startTimeObj != null ? ", start_time" : "") +
                ") VALUES (?, ?, 'PENDING', ?, ?, ?, ?" +
                (endTimeObj != null ? ", ?" : "") +
                (startTimeObj != null ? ", ?" : "") +
                ")";

        List<Object> params = new ArrayList<>();
        params.add(name);
        params.add(priority);
        params.add(assigneeId);
        params.add(dispatcherId);
        params.add(isImportant ? 1 : 0);
        params.add(isUrgent ? 1 : 0);
        if (endTimeObj != null) params.add(endTimeObj);
        if (startTimeObj != null) params.add(startTimeObj);

        jdbcTemplate.update(sql, params.toArray());

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

        try {
            notificationService.detectNewTask(taskId, assigneeId, dispatcherId);
        } catch (Exception ignored) {}

        try {
            draftService.deleteDraft(dispatcherId, "TASK");
        } catch (Exception ignored) {}

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listTasks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) Long dispatcherId,
            @RequestParam(required = false) Boolean showCompleted) {
        StringBuilder sql = new StringBuilder(
                "SELECT t.*, u.username AS assignee_name, u.real_name AS assignee_real_name, " +
                "d.username AS dispatcher_name, d.real_name AS dispatcher_real_name " +
                "FROM tasks t LEFT JOIN users u ON t.assignee_id = u.id " +
                "LEFT JOIN users d ON t.dispatcher_id = d.id WHERE t.deleted = 0");
        List<Object> params = new ArrayList<>();

        if (status != null) {
            sql.append(" AND t.status = ?");
            params.add(status);
        }
        if (assigneeId != null) {
            sql.append(" AND t.assignee_id = ?");
            params.add(assigneeId);
        }
        if (dispatcherId != null) {
            sql.append(" AND t.dispatcher_id = ?");
            params.add(dispatcherId);
        }
        if (Boolean.FALSE.equals(showCompleted)) {
            sql.append(" AND t.status != 'COMPLETED'");
        }
        sql.append(" ORDER BY t.created_at DESC");

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
    @Auditable(operationType = "TASK_PROGRESS_UPDATE", targetType = "TASK")
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
            jdbcTemplate.update("UPDATE tasks SET progress_note = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?", progressNote, taskId);
        }
        if (status != null) {
            jdbcTemplate.update("UPDATE tasks SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?", status, taskId);
        }

        return ResponseEntity.ok(ApiResponse.success());
    }

    @PatchMapping("/{taskId}")
    @Auditable(operationType = "TASK_UPDATE", targetType = "TASK")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateTask(
            @PathVariable Long taskId, @RequestBody Map<String, Object> request) {
        Long currentUserId = getCurrentUserId();

        Map<String, Object> task;
        try {
            task = jdbcTemplate.queryForMap("SELECT assignee_id, dispatcher_id FROM tasks WHERE id = ? AND deleted = 0", taskId);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在");
        }

        Long assigneeId = ((Number) task.get("assignee_id")).longValue();
        Long dispatcherId = ((Number) task.get("dispatcher_id")).longValue();

        if (!currentUserId.equals(assigneeId) && !currentUserId.equals(dispatcherId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACTION, "无权修改该任务");
        }

        StringBuilder sql = new StringBuilder("UPDATE tasks SET updated_at = CURRENT_TIMESTAMP");
        List<Object> params = new ArrayList<>();

        if (request.containsKey("isUrgent")) {
            sql.append(", is_urgent = ?");
            params.add(Boolean.TRUE.equals(request.get("isUrgent")) ? 1 : 0);
        }
        if (request.containsKey("endTime")) {
            sql.append(", end_time = ?");
            params.add(request.get("endTime"));
        }
        if (request.containsKey("progressNote")) {
            sql.append(", progress_note = ?");
            params.add(request.get("progressNote"));
        }
        if (request.containsKey("status")) {
            sql.append(", status = ?");
            params.add(request.get("status"));
        }

        sql.append(" WHERE id = ? AND deleted = 0");
        params.add(taskId);

        String oldStatus = null;
        if (request.containsKey("status")) {
            try {
                Map<String, Object> oldTask = jdbcTemplate.queryForMap("SELECT status FROM tasks WHERE id = ? AND deleted = 0", taskId);
                oldStatus = oldTask.get("status") != null ? oldTask.get("status").toString() : null;
            } catch (Exception ignored) {}
        }

        jdbcTemplate.update(sql.toString(), params.toArray());

        if (request.containsKey("status") && oldStatus != null) {
            try {
                notificationService.detectStatusChange(taskId, oldStatus, request.get("status").toString(), currentUserId);
            } catch (Exception ignored) {}
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", taskId);
        result.put("updated", true);
        return ResponseEntity.ok(ApiResponse.success(result));
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
    @Auditable(operationType = "TASK_TRANSITION", targetType = "TASK")
    public ResponseEntity<ApiResponse<Map<String, Object>>> transitionStatus(
            @PathVariable Long taskId, @RequestBody Map<String, String> request) {
        String targetStatus = request.get("status");
        if (targetStatus == null) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD, "目标状态不能为空");
        }

        jdbcTemplate.update("UPDATE tasks SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND deleted = 0", targetStatus, taskId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/subordinate")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSubordinateTasks() {
        Long currentUserId = getCurrentUserId();
        List<Long> subordinateIds = hierarchyService.getSubordinateIds(currentUserId);
        if (subordinateIds.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(Collections.emptyList()));
        }
        String placeholders = String.join(",", Collections.nCopies(subordinateIds.size(), "?"));
        List<Map<String, Object>> tasks = jdbcTemplate.queryForList(
                "SELECT t.*, u.username AS assignee_name, u.real_name AS assignee_real_name " +
                "FROM tasks t JOIN users u ON t.assignee_id = u.id " +
                "WHERE t.assignee_id IN (" + placeholders + ") AND t.deleted = 0 ORDER BY t.created_at DESC",
                subordinateIds.toArray());
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    @GetMapping("/subordinate/users")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSubordinateUsers() {
        Long currentUserId = getCurrentUserId();
        List<Map<String, Object>> subordinates = hierarchyService.getSubordinates(currentUserId);
        if (subordinates.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(Collections.emptyList()));
        }
        List<Long> subIds = new ArrayList<>();
        for (Map<String, Object> sub : subordinates) {
            subIds.add((Long) sub.get("id"));
        }
        String placeholders = String.join(",", Collections.nCopies(subIds.size(), "?"));
        List<Map<String, Object>> users = jdbcTemplate.queryForList(
                "SELECT id, username, role, real_name, department_id, team_id FROM users WHERE id IN (" + placeholders + ") AND deleted = 0",
                subIds.toArray());
        Map<Long, Integer> depthMap = new HashMap<>();
        for (Map<String, Object> sub : subordinates) {
            depthMap.put((Long) sub.get("id"), (Integer) sub.get("depth"));
        }
        for (Map<String, Object> user : users) {
            Long uid = ((Number) user.get("id")).longValue();
            user.put("depth", depthMap.getOrDefault(uid, 0));
        }
        users.sort((a, b) -> {
            int da = ((Number) a.get("depth")).intValue();
            int db = ((Number) b.get("depth")).intValue();
            if (da != db) return da - db;
            return ((Number) a.get("id")).intValue() - ((Number) b.get("id")).intValue();
        });
        return ResponseEntity.ok(ApiResponse.success(users));
    }


    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }

    @DeleteMapping("/{taskId}")
    @Auditable(operationType = "TASK_DELETE", targetType = "TASK")
    public ResponseEntity<ApiResponse<Void>> deleteTask(@PathVariable Long taskId) {
        Long userId = getCurrentUserId();

        Map<String, Object> task;
        try {
            task = jdbcTemplate.queryForMap("SELECT dispatcher_id, deleted FROM tasks WHERE id = ?", taskId);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在");
        }

        Object deletedObj = task.get("deleted");
        if (deletedObj == null) deletedObj = task.get("DELETED");
        if (deletedObj != null && ((Number) deletedObj).intValue() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在");
        }

        Object dispatcherIdObj = task.get("dispatcher_id");
        if (dispatcherIdObj == null) dispatcherIdObj = task.get("DISPATCHER_ID");
        Long dispatcherId = dispatcherIdObj != null ? ((Number) dispatcherIdObj).longValue() : null;

        String role = "";
        try {
            Map<String, Object> user = jdbcTemplate.queryForMap("SELECT role FROM users WHERE id = ?", userId);
            role = user.get("role") != null ? user.get("role").toString() : "";
        } catch (Exception ignored) {}

        if (!userId.equals(dispatcherId) && !"ADMIN".equals(role)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACTION, "无权删除该任务，仅派发者或管理员可删除");
        }

        jdbcTemplate.update("UPDATE tasks SET deleted = 1, updated_at = CURRENT_TIMESTAMP WHERE id = ?", taskId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
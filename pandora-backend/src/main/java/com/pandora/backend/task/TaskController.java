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
    private TaskChangeService taskChanges;

    @Autowired
    private com.pandora.backend.notification.NotificationService notificationService;

    @Autowired
    private com.pandora.backend.draft.DraftService draftService;

    @Autowired
    private com.pandora.backend.hierarchy.HierarchyService hierarchyService;

    @PostMapping
    @Auditable(operationType = "TASK_CREATE", targetType = "TASK")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createTask(@RequestBody Map<String, Object> taskRequest) {
        Map<String, Object> result = taskChanges.submit(getCurrentUserId(), "CREATE", null, taskRequest);
        try { draftService.deleteDraft(getCurrentUserId(), "TASK"); } catch (Exception ignored) {}
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
            Map<String, Object> task = jdbcTemplate.queryForMap(
                "SELECT t.*, u.username AS assignee_name, u.real_name AS assignee_real_name, " +
                "d.username AS dispatcher_name, d.real_name AS dispatcher_real_name " +
                "FROM tasks t LEFT JOIN users u ON t.assignee_id = u.id " +
                "LEFT JOIN users d ON t.dispatcher_id = d.id WHERE t.id = ? AND t.deleted = 0", taskId);
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

        taskChanges.updateExecution(getCurrentUserId(), taskId, status, progressNote);

        return ResponseEntity.ok(ApiResponse.success());
    }

    @PatchMapping("/{taskId}")
    @Auditable(operationType = "TASK_UPDATE", targetType = "TASK")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateTask(
            @PathVariable Long taskId, @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(ApiResponse.success(taskChanges.submit(getCurrentUserId(), "UPDATE", taskId, request)));
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

        taskChanges.updateExecution(getCurrentUserId(), taskId, targetStatus, null);
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

    private static Boolean toBool(Object v) {
        if (v == null) return false;
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof Number) return ((Number) v).intValue() != 0;
        return Boolean.parseBoolean(v.toString());
    }

    @DeleteMapping("/{taskId}")
    @Auditable(operationType = "TASK_DELETE", targetType = "TASK")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteTask(@PathVariable Long taskId) {
        return ResponseEntity.ok(ApiResponse.success(taskChanges.submit(getCurrentUserId(), "DELETE", taskId, Collections.emptyMap())));
    }
    @GetMapping("/assignees")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> assignees() {
        return ResponseEntity.ok(ApiResponse.success(jdbcTemplate.queryForList(
                "SELECT id, username, real_name, role FROM users WHERE deleted = 0 AND approval_status = 'APPROVED' ORDER BY real_name, username")));
    }


}

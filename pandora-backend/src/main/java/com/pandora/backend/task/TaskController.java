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
    private TaskTagService taskTags;

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
            @RequestParam(required = false) Boolean showCompleted,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        StringBuilder sql = new StringBuilder(
                "SELECT t.*, CASE WHEN u.role='ADMIN' THEN '系统' ELSE u.username END AS assignee_name, " +
                "CASE WHEN u.role='ADMIN' THEN '系统' ELSE u.real_name END AS assignee_real_name, " +
                "CASE WHEN d.role='ADMIN' THEN '系统' ELSE d.username END AS dispatcher_name, " +
                "CASE WHEN d.role='ADMIN' THEN '系统' ELSE d.real_name END AS dispatcher_real_name " +
                "FROM tasks t LEFT JOIN users u ON t.assignee_id = u.id " +
                "LEFT JOIN users d ON t.dispatcher_id = d.id WHERE t.deleted = 0");
        List<Object> params = new ArrayList<>();

        if (status != null) {
            if (!List.of("IN_PROGRESS", "COMPLETED").contains(status)) throw new BusinessException(ErrorCode.BAD_REQUEST, "任务状态无效");
            sql.append(" AND t.status = ?");
            params.add(status);
        }
        if (assigneeId != null) {
            sql.append(" AND (EXISTS (SELECT 1 FROM task_assignees ta WHERE ta.task_id=t.id AND ta.user_id=?) " +
                    "OR (NOT EXISTS (SELECT 1 FROM task_assignees tx WHERE tx.task_id=t.id) AND t.assignee_id=?))");
            params.add(assigneeId); params.add(assigneeId);
        }
        if (dispatcherId != null) {
            sql.append(" AND t.dispatcher_id = ?");
            params.add(dispatcherId);
        }
        if (Boolean.FALSE.equals(showCompleted)) {
            sql.append(" AND t.status != 'COMPLETED'");
        }
        if (keyword != null && !keyword.isBlank()) { sql.append(" AND (t.name LIKE ? OR t.content LIKE ?)"); params.add("%" + keyword.trim() + "%"); params.add("%" + keyword.trim() + "%"); }
        if (startDate != null && !startDate.isBlank()) { sql.append(" AND t.end_time >= ?"); params.add(startDate); }
        if (endDate != null && !endDate.isBlank()) { sql.append(" AND t.start_time <= ?"); params.add(endDate); }
        sql.append(" ORDER BY t.created_at DESC");

        List<Map<String, Object>> tasks = jdbcTemplate.queryForList(sql.toString(), params.toArray());
        return ResponseEntity.ok(ApiResponse.success(attachAssignees(taskTags.attach(tasks))));
    }

    /** Internal/test compatibility for callers that do not use the newer search and date filters. */
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listTasks(
            String status, Long assigneeId, Long dispatcherId, Boolean showCompleted) {
        return listTasks(status, assigneeId, dispatcherId, showCompleted, null, null, null);
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTask(@PathVariable Long taskId) {
        try {
            Map<String, Object> task = jdbcTemplate.queryForMap(
                "SELECT t.*, CASE WHEN u.role='ADMIN' THEN '系统' ELSE u.username END AS assignee_name, " +
                "CASE WHEN u.role='ADMIN' THEN '系统' ELSE u.real_name END AS assignee_real_name, " +
                "CASE WHEN d.role='ADMIN' THEN '系统' ELSE d.username END AS dispatcher_name, " +
                "CASE WHEN d.role='ADMIN' THEN '系统' ELSE d.real_name END AS dispatcher_real_name " +
                "FROM tasks t LEFT JOIN users u ON t.assignee_id = u.id " +
                "LEFT JOIN users d ON t.dispatcher_id = d.id WHERE t.id = ? AND t.deleted = 0", taskId);
            taskTags.attach(Collections.singletonList(task));
            attachAssignees(Collections.singletonList(task));
            return ResponseEntity.ok(ApiResponse.success(task));
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
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

        List<Map<String, Object>> urgent = jdbcTemplate.queryForList(
                "SELECT t.* FROM tasks t JOIN task_assignees ta ON ta.task_id=t.id WHERE ta.user_id=? AND t.is_urgent=1 AND t.deleted=0", currentUserId);
        List<Map<String, Object>> normal = jdbcTemplate.queryForList(
                "SELECT t.* FROM tasks t JOIN task_assignees ta ON ta.task_id=t.id WHERE ta.user_id=? AND t.is_urgent=0 AND t.deleted=0", currentUserId);

        Map<String, Object> result = new HashMap<>();
        result.put("urgent", attachAssignees(taskTags.attach(urgent)));
        result.put("normal", attachAssignees(taskTags.attach(normal)));

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
                "SELECT DISTINCT t.* FROM tasks t JOIN task_assignees ta ON ta.task_id=t.id " +
                "JOIN users u ON ta.user_id=u.id WHERE ta.user_id IN (" + placeholders + ") " +
                "AND t.deleted=0 AND u.role<>'ADMIN' ORDER BY t.created_at DESC",
                subordinateIds.toArray());
        return ResponseEntity.ok(ApiResponse.success(attachAssignees(taskTags.attach(tasks))));
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
                "SELECT id, username, role, real_name, department_id, team_id FROM users WHERE id IN (" + placeholders + ") AND deleted = 0 AND role <> 'ADMIN'",
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
        Long actor = getCurrentUserId();
        String role = jdbcTemplate.queryForObject("SELECT role FROM users WHERE id=? AND deleted=0", String.class, actor);
        List<Long> visible;
        if ("ADMIN".equals(role) || "FOUNDER".equals(role)) {
            visible = jdbcTemplate.queryForList(
                    "SELECT id FROM users WHERE deleted=0 AND approval_status='APPROVED' AND role<>'ADMIN'", Long.class);
        } else if ("DEPT_HEAD".equals(role) || "TEAM_LEADER".equals(role)) {
            LinkedHashSet<Long> ids = new LinkedHashSet<>(hierarchyService.getSubordinateIds(actor));
            ids.add(actor); visible = new ArrayList<>(ids);
        } else {
            visible = List.of(actor);
        }
        if (visible.isEmpty()) return ResponseEntity.ok(ApiResponse.success(Collections.emptyList()));
        String placeholders = String.join(",", Collections.nCopies(visible.size(), "?"));
        return ResponseEntity.ok(ApiResponse.success(jdbcTemplate.queryForList(
                "SELECT u.id,u.username,u.real_name,u.role,u.department_id,d.name AS department_name,u.team_id,t.name AS team_name " +
                "FROM users u LEFT JOIN departments d ON d.id=u.department_id AND d.deleted=0 " +
                "LEFT JOIN teams t ON t.id=u.team_id AND t.deleted=0 " +
                "WHERE u.id IN (" + placeholders + ") AND u.deleted=0 AND u.approval_status='APPROVED' " +
                "AND u.role<>'ADMIN' ORDER BY d.name,t.name,u.real_name,u.username", visible.toArray())));
    }

    private List<Map<String, Object>> attachAssignees(List<Map<String, Object>> tasks) {
        if (tasks.isEmpty()) return tasks;
        Map<Long, Map<String, Object>> byId = new LinkedHashMap<>();
        for (Map<String, Object> task : tasks) {
            Object raw = task.get("id"); if (raw == null) raw = task.get("ID");
            if (raw instanceof Number) { task.put("assignees", new ArrayList<>()); byId.put(((Number) raw).longValue(), task); }
        }
        if (byId.isEmpty()) return tasks;
        String placeholders = String.join(",", Collections.nCopies(byId.size(), "?"));
        for (Map<String, Object> user : jdbcTemplate.queryForList(
                "SELECT ta.task_id,u.id,u.username,u.real_name,u.avatar FROM task_assignees ta JOIN users u ON u.id=ta.user_id " +
                "WHERE ta.task_id IN (" + placeholders + ") ORDER BY ta.assigned_at,u.id", byId.keySet().toArray())) {
            Object rawTaskId = user.remove("task_id"); if (rawTaskId == null) rawTaskId = user.remove("TASK_ID");
            if (!(rawTaskId instanceof Number)) continue;
            long taskId = ((Number) rawTaskId).longValue();
            @SuppressWarnings("unchecked") List<Map<String, Object>> target = (List<Map<String, Object>>) byId.get(taskId).get("assignees");
            target.add(user);
        }
        return tasks;
    }


}

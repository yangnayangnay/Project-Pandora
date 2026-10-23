package com.pandora.backend.log;

import com.pandora.backend.common.ApiResponse;
import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.ErrorCode;
import com.pandora.backend.audit.Auditable;
import com.pandora.backend.ai.AiTransmitConfig;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/log")
public class LogController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private LogDraftGenerator logDraftGenerator;

    @Autowired
    private LogAnalysisTriggerService logAnalysisTriggerService;

    @Autowired
    private com.pandora.backend.draft.DraftService draftService;

    @Autowired
    private com.pandora.backend.hierarchy.HierarchyService hierarchyService;

    @Autowired
    private com.pandora.backend.notification.NotificationService notificationService;

    @PostMapping("/draft")
    @Auditable(operationType = "LOG_DRAFT_GENERATE", targetType = "WORK_LOG")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateDraft() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(logDraftGenerator.generateDraft(userId)));
    }

    @PostMapping
    @Auditable(operationType = "LOG_CREATE", targetType = "WORK_LOG")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createLog(@RequestBody Map<String, Object> request, HttpServletRequest servletRequest) {
        Long userId = getCurrentUserId();
        String workItem = (String) request.get("workItem");
        if (workItem == null || workItem.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD, "工作事项不能为空");
        }
        if (workItem.trim().length() > 512) {
            throw new BusinessException(ErrorCode.INVALID_PARAM_LENGTH, "工作事项长度不能超过512字符");
        }

        String completionStatus = (String) request.getOrDefault("completionStatus", "PENDING");
        Object timeCostObj = request.get("timeCost");
        String workContent = (String) request.get("workContent");
        String logDate = (String) request.getOrDefault("logDate", new java.sql.Date(System.currentTimeMillis()).toString());

        List<Map<String, Object>> existingLogs = jdbcTemplate.queryForList(
                "SELECT id, deleted FROM work_logs WHERE user_id = ? AND log_date = ? ORDER BY deleted ASC",
                userId, logDate);

        Long logId;
        if (!existingLogs.isEmpty()) {
            Map<String, Object> existing = existingLogs.get(0);
            Object deletedObj = existing.get("DELETED");
            int deleted = deletedObj != null ? ((Number) deletedObj).intValue() : 0;
            if (deleted == 0) {
                throw new BusinessException(ErrorCode.LOG_DATE_CONFLICT);
            }
            logId = ((Number) existing.get("ID")).longValue();
            jdbcTemplate.update(
                    "UPDATE work_logs SET work_item = ?, work_content = ?, completion_status = ?, time_cost = ?, deleted = 0, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                    workItem, workContent, completionStatus, timeCostObj, logId
            );
        } else {
            try {
                jdbcTemplate.update(
                        "INSERT INTO work_logs (user_id, work_item, work_content, completion_status, time_cost, log_date) VALUES (?, ?, ?, ?, ?, ?)",
                        userId, workItem, workContent, completionStatus, timeCostObj, logDate
                );
            } catch (DuplicateKeyException e) {
                throw new BusinessException(ErrorCode.LOG_DATE_CONFLICT);
            }
            logId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("id", logId);

        try {
            // Round28 REQ-28-02: 同步段解析透传配置传入自动分析链（异步线程不可用请求上下文）
            AiTransmitConfig aiConfig = AiTransmitConfig.fromHeaders(servletRequest);
            logAnalysisTriggerService.triggerAutoAnalysis(logId, userId, aiConfig);
        } catch (Exception ignored) {}

        try {
            draftService.deleteDraft(userId, "LOG");
        } catch (Exception ignored) {}

        // TASK-R18-009: 日志创建成功后触发 LOG_COMPLETED 通知（埋点，通知失败不影响主流程）
        try {
            notificationService.detectLogCompleted(userId, logDate);
        } catch (Exception ignored) {}

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listAllLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String keyword) {
        String role = getCurrentUserRole();
        if (!"ADMIN".equals(role) && !"FOUNDER".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(403, "仅管理员或创始人可查看全量日志"));
        }

        if (startDate != null && endDate != null && startDate.compareTo(endDate) > 0) {
            return ResponseEntity.ok(ApiResponse.success(Collections.emptyList()));
        }

        StringBuilder sql = new StringBuilder(
                "SELECT l.*, u.username, u.real_name FROM work_logs l JOIN users u ON l.user_id = u.id WHERE l.deleted = 0");
        List<Object> params = new ArrayList<>();
        if (userId != null) {
            sql.append(" AND l.user_id = ?");
            params.add(userId);
        }
        if (startDate != null) {
            sql.append(" AND l.log_date >= ?");
            params.add(startDate);
        }
        if (endDate != null) {
            sql.append(" AND l.log_date <= ?");
            params.add(endDate);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND l.work_item LIKE ?");
            params.add("%" + keyword + "%");
        }
        sql.append(" ORDER BY l.log_date DESC, l.created_at DESC");

        List<Map<String, Object>> logs = jdbcTemplate.queryForList(sql.toString(), params.toArray());
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/cleanup-preview")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> cleanupPreview() {
        String role = getCurrentUserRole();
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(403, "仅管理员可执行数据清理"));
        }
        List<Map<String, Object>> candidates = jdbcTemplate.queryForList(
                "SELECT l.*, u.username, u.real_name FROM work_logs l JOIN users u ON l.user_id = u.id " +
                "WHERE l.deleted = 0 AND (l.work_item LIKE '%测试%' OR l.work_item LIKE '%test%' OR l.work_item LIKE '%Test%' " +
                "OR l.work_item LIKE '%Round7%' OR l.work_item LIKE '%tmp%' OR l.work_item LIKE '%临时%' " +
                "OR l.work_item LIKE 'This is a test%') ORDER BY l.created_at DESC");
        return ResponseEntity.ok(ApiResponse.success(candidates));
    }

    @PostMapping("/cleanup-execute")
    @Auditable(operationType = "TEST_DATA_CLEANUP", targetType = "WORK_LOG")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cleanupExecute() {
        String role = getCurrentUserRole();
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(403, "仅管理员可执行数据清理"));
        }
        int deleted = jdbcTemplate.update(
                "UPDATE work_logs SET deleted = 1 WHERE deleted = 0 AND (work_item LIKE '%测试%' OR work_item LIKE '%test%' OR work_item LIKE '%Test%' " +
                "OR work_item LIKE '%Round7%' OR work_item LIKE '%tmp%' OR work_item LIKE '%临时%' " +
                "OR work_item LIKE 'This is a test%')");
        Map<String, Object> result = new HashMap<>();
        result.put("deletedCount", deleted);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{logId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLogDetail(@PathVariable Long logId) {
        Long userId = getCurrentUserId();
        String role = getCurrentUserRole();

        Map<String, Object> log = jdbcTemplate.queryForList(
                "SELECT l.*, u.username, u.real_name, u.avatar FROM work_logs l " +
                "JOIN users u ON l.user_id = u.id WHERE l.id = ? AND l.deleted = 0",
                logId).stream().findFirst().orElse(null);

        if (log == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "日志不存在");
        }

        Object logUserIdObj = log.get("USER_ID");
        Long logUserId = logUserIdObj != null ? ((Number) logUserIdObj).longValue() : null;
        boolean isOwner = logUserId != null && logUserId.equals(userId);
        boolean canViewOthers = "ADMIN".equals(role) || "FOUNDER".equals(role);
        if (!isOwner && !canViewOthers) {
            List<Long> subordinateIds = hierarchyService.getSubordinateIds(userId);
            if (!subordinateIds.contains(logUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error(403, "无权查看此日志"));
            }
        }

        List<Map<String, Object>> analyses = jdbcTemplate.queryForList(
                "SELECT id, analysis_type, result, status, generated_at FROM ai_analyses " +
                "WHERE user_id = ? AND deleted = 0 ORDER BY generated_at DESC LIMIT 5",
                logUserId);
        log.put("ai_analyses", analyses);

        return ResponseEntity.ok(ApiResponse.success(log));
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listLogs(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String keyword) {
        Long userId = getCurrentUserId();

        if (startDate != null && endDate != null && startDate.compareTo(endDate) > 0) {
            return ResponseEntity.ok(ApiResponse.success(Collections.emptyList()));
        }

        StringBuilder sql = new StringBuilder(
                "SELECT l.*, u.username, u.real_name, u.avatar FROM work_logs l JOIN users u ON l.user_id = u.id WHERE l.user_id = ? AND l.deleted = 0");
        List<Object> params = new ArrayList<>();
        params.add(userId);
        if (startDate != null) {
            sql.append(" AND l.log_date >= ?");
            params.add(startDate);
        }
        if (endDate != null) {
            sql.append(" AND l.log_date <= ?");
            params.add(endDate);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND l.work_item LIKE ?");
            params.add("%" + keyword + "%");
        }
        sql.append(" ORDER BY l.log_date DESC, l.created_at DESC");

        List<Map<String, Object>> logs = jdbcTemplate.queryForList(sql.toString(), params.toArray());
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/stat")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLogStats() {
        Long userId = getCurrentUserId();

        Map<String, Object> stats = new HashMap<>();

        List<Map<String, Object>> completionRate = jdbcTemplate.queryForList(
                "SELECT DATE(log_date) as date, COUNT(*) as total, " +
                "SUM(CASE WHEN completion_status = 'COMPLETED' THEN 1 ELSE 0 END) as completed " +
                "FROM work_logs WHERE user_id = ? AND deleted = 0 GROUP BY DATE(log_date) ORDER BY log_date DESC LIMIT 30", userId);

        // Round25 L7 修复: 补齐 App 统计弹窗期望的四个顶层键（total/pending/completed/totalTime）。
        // 口径与 completionRate 一致（user_id=当前用户 AND deleted=0）；
        // pending 含 NOT_STARTED+IN_PROGRESS（"待完成"语义）；COALESCE 防 null 透传（全 NULL 时归 0）
        Map<String, Object> summary = jdbcTemplate.queryForMap(
                "SELECT COUNT(*) AS total, " +
                "COALESCE(SUM(CASE WHEN completion_status = 'COMPLETED' THEN 1 ELSE 0 END), 0) AS completed, " +
                "COALESCE(SUM(CASE WHEN completion_status <> 'COMPLETED' THEN 1 ELSE 0 END), 0) AS pending, " +
                "COALESCE(SUM(time_cost), 0) AS totalTime " +
                "FROM work_logs WHERE user_id = ? AND deleted = 0", userId);
        stats.put("total", summary.get("total"));
        stats.put("pending", summary.get("pending"));
        stats.put("completed", summary.get("completed"));
        stats.put("totalTime", summary.get("totalTime"));

        stats.put("completionRate", completionRate);
        stats.put("importantRatio", Collections.emptyList());
        stats.put("analysisChart", Collections.emptyList());

        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @PutMapping("/top10")
    @Auditable(operationType = "TOP10_UPDATE", targetType = "TOP10_WORK")
    public ResponseEntity<ApiResponse<Void>> updateTop10(@RequestBody List<Map<String, Object>> top10List) {
        Long userId = getCurrentUserId();

        for (Map<String, Object> item : top10List) {
            String workType = (String) item.get("workType");
            Integer rankOrder = (Integer) item.get("rankOrder");
            String content = (String) item.get("content");

            jdbcTemplate.update(
                    "INSERT INTO top10_works (user_id, work_type, rank_order, content) VALUES (?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE content = VALUES(content)",
                    userId, workType, rankOrder, content
            );
        }

        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/top10")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTop10(@RequestParam(required = false) String workType) {
        Long userId = getCurrentUserId();
        StringBuilder sql = new StringBuilder("SELECT * FROM top10_works WHERE user_id = ? AND deleted = 0");
        List<Object> params = new ArrayList<>();
        params.add(userId);
        if (workType != null) {
            sql.append(" AND work_type = ?");
            params.add(workType);
        }
        sql.append(" ORDER BY rank_order");

        List<Map<String, Object>> top10 = jdbcTemplate.queryForList(sql.toString(), params.toArray());
        return ResponseEntity.ok(ApiResponse.success(top10));
    }

    @PutMapping("/{logId}")
    @Auditable(operationType = "LOG_UPDATE", targetType = "WORK_LOG")
    public ResponseEntity<ApiResponse<Map<String, Object>>> editLog(
            @PathVariable Long logId, @RequestBody Map<String, Object> request) {
        Long userId = getCurrentUserId();

        StringBuilder sql = new StringBuilder("UPDATE work_logs SET updated_at = CURRENT_TIMESTAMP");
        List<Object> params = new ArrayList<>();

        if (request.containsKey("workItem")) {
            sql.append(", work_item = ?");
            params.add(request.get("workItem"));
        }
        if (request.containsKey("workContent")) {
            sql.append(", work_content = ?");
            params.add(request.get("workContent"));
        }
        if (request.containsKey("completionStatus")) {
            sql.append(", completion_status = ?");
            params.add(request.get("completionStatus"));
        }
        if (request.containsKey("timeCost")) {
            sql.append(", time_cost = ?");
            params.add(request.get("timeCost"));
        }
        if (request.containsKey("logDate")) {
            sql.append(", log_date = ?");
            params.add(request.get("logDate"));
        }

        sql.append(" WHERE id = ? AND user_id = ? AND deleted = 0");
        params.add(logId);
        params.add(userId);

        int updated = jdbcTemplate.update(sql.toString(), params.toArray());
        if (updated == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "日志不存在或无权编辑");
        }

        // TASK-R18-009: 日志更新成功后触发 LOG_COMPLETED 通知（埋点）
        try {
            Object logDateObj = request.get("logDate");
            String logDate = logDateObj != null ? logDateObj.toString() : null;
            notificationService.detectLogCompleted(userId, logDate);
        } catch (Exception ignored) {}

        Map<String, Object> result = new HashMap<>();
        result.put("id", logId);
        result.put("updated", true);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @DeleteMapping("/{logId}")
    @Auditable(operationType = "LOG_DELETE", targetType = "WORK_LOG")
    public ResponseEntity<ApiResponse<Void>> deleteLog(@PathVariable Long logId) {
        Long userId = getCurrentUserId();
        int updated = jdbcTemplate.update(
                "UPDATE work_logs SET deleted = 1 WHERE id = ? AND user_id = ? AND deleted = 0",
                logId, userId);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "日志不存在或无权删除");
        }
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/subordinate")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSubordinateLogs(
            @RequestParam(required = false) Long subordinateId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String keyword) {
        Long userId = getCurrentUserId();
        String role = getCurrentUserRole();
        if ("EMPLOYEE".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(403, "无权访问"));
        }

        List<Long> subordinateIds = hierarchyService.getSubordinateIds(userId);
        if (subordinateIds.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(Collections.emptyList()));
        }

        if (subordinateId != null && !subordinateIds.contains(subordinateId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(403, "无权访问该下属日志"));
        }

        if (startDate != null && endDate != null && startDate.compareTo(endDate) > 0) {
            return ResponseEntity.ok(ApiResponse.success(Collections.emptyList()));
        }

        StringBuilder sql = new StringBuilder(
                "SELECT l.*, u.username, u.real_name, u.avatar FROM work_logs l JOIN users u ON l.user_id = u.id WHERE l.deleted = 0");
        List<Object> params = new ArrayList<>();

        if (subordinateId != null) {
            sql.append(" AND l.user_id = ?");
            params.add(subordinateId);
        } else {
            String placeholders = String.join(",", Collections.nCopies(subordinateIds.size(), "?"));
            sql.append(" AND l.user_id IN (").append(placeholders).append(")");
            params.addAll(subordinateIds);
        }
        if (startDate != null) {
            sql.append(" AND l.log_date >= ?");
            params.add(startDate);
        }
        if (endDate != null) {
            sql.append(" AND l.log_date <= ?");
            params.add(endDate);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND l.work_item LIKE ?");
            params.add("%" + keyword + "%");
        }
        sql.append(" ORDER BY l.log_date DESC, l.created_at DESC");

        List<Map<String, Object>> logs = jdbcTemplate.queryForList(sql.toString(), params.toArray());
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/subordinates")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSubordinates() {
        Long userId = getCurrentUserId();
        String role = getCurrentUserRole();
        if ("EMPLOYEE".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(403, "无权访问"));
        }

        List<Map<String, Object>> subordinates = hierarchyService.getSubordinates(userId);
        if (subordinates.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(Collections.emptyList()));
        }
        return ResponseEntity.ok(ApiResponse.success(subordinates));
    }

    @PostMapping("/auto-generate")
    @Deprecated
    @Auditable(operationType = "LOG_AUTO_GENERATE", targetType = "WORK_LOG")
    public ResponseEntity<ApiResponse<Map<String, Object>>> autoGenerateLogs() {
        Long userId = getCurrentUserId();
        String today = new java.sql.Date(System.currentTimeMillis()).toString();

        List<Map<String, Object>> todayTasks = jdbcTemplate.queryForList(
                "SELECT id, name, status, progress_note FROM tasks " +
                "WHERE assignee_id = ? AND deleted = 0 " +
                "AND DATE(updated_at) = ? AND status IN ('IN_PROGRESS', 'COMPLETED', 'CONFIRMED')",
                userId, today);

        int generated = 0;
        for (Map<String, Object> task : todayTasks) {
            String taskName = (String) task.get("NAME");
            String status = (String) task.get("STATUS");
            Object taskId = task.get("ID");

            Integer existing = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM work_logs WHERE user_id = ? AND log_date = ? AND work_item LIKE ? AND deleted = 0",
                    Integer.class, userId, today, "[自动]" + taskName + "%");

            if (existing == 0) {
                String completionStatus = "COMPLETED".equals(status) || "CONFIRMED".equals(status) ? "COMPLETED" : "PENDING";
                String workItem = "[自动]" + taskName + " (" + status + ")";
                jdbcTemplate.update(
                        "INSERT INTO work_logs (user_id, work_item, completion_status, log_date) VALUES (?, ?, ?, ?)",
                        userId, workItem, completionStatus, today);
                generated++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("generated", generated);
        result.put("date", today);
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
package com.pandora.backend.top10;

import com.pandora.backend.common.ApiResponse;
import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.ErrorCode;
import com.pandora.backend.common.RoleChecker;
import com.pandora.backend.permission.FunctionCode;
import com.pandora.backend.permission.FunctionPermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    private static final Logger log = LoggerFactory.getLogger(Top10Controller.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FunctionPermissionService functionPermissionService;

    @Value("${pandora.top10.cleanup-dirty-data:true}")
    private boolean cleanupDirtyData;

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
            throw new BusinessException(ErrorCode.TOP10_RANK_OUT_OF_RANGE);
        }

        // 内容唯一性校验（B3）
        Integer dupCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM top10_works WHERE work_type = 'COMPANY_IMPORTANT' AND TRIM(content) = TRIM(?) AND deleted = 0",
                Integer.class, content);
        if (dupCount != null && dupCount > 0) {
            throw new BusinessException(ErrorCode.TOP10_CONTENT_DUPLICATE);
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

        // 保存后验证（B3）
        List<Map<String, Object>> verify = jdbcTemplate.queryForList(
                "SELECT id FROM top10_works WHERE id = ? AND deleted = 0", id);
        if (verify.isEmpty()) {
            log.error("保存后验证失败：top10_works id={} 未找到", id);
            throw new BusinessException(ErrorCode.TOP10_SAVE_VERIFY_FAILED);
        }

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
            throw new BusinessException(ErrorCode.TOP10_RANK_OUT_OF_RANGE);
        }

        // 内容唯一性校验（排除自身）（B4）
        Integer dupCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM top10_works WHERE work_type = 'COMPANY_IMPORTANT' AND TRIM(content) = TRIM(?) AND deleted = 0 AND id != ?",
                Integer.class, content, id);
        if (dupCount != null && dupCount > 0) {
            throw new BusinessException(ErrorCode.TOP10_CONTENT_DUPLICATE);
        }

        jdbcTemplate.update(
                "UPDATE top10_works SET content = ?, rank_order = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                content.trim(), rankOrder, id);

        // 保存后验证（B4）
        List<Map<String, Object>> verify = jdbcTemplate.queryForList(
                "SELECT content FROM top10_works WHERE id = ? AND deleted = 0", id);
        if (verify.isEmpty()) {
            log.error("修改后验证失败：top10_works id={} 未找到", id);
            throw new BusinessException(ErrorCode.TOP10_SAVE_VERIFY_FAILED);
        }
        Object verifyContent = verify.get(0).get("CONTENT");
        if (verifyContent == null) verifyContent = verify.get(0).get("content");
        if (verifyContent == null || !verifyContent.toString().trim().equals(content.trim())) {
            log.error("修改后验证失败：top10_works id={} content 未更新为期望值", id);
            throw new BusinessException(ErrorCode.TOP10_SAVE_VERIFY_FAILED);
        }

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

        // 去重校验（B5）：查询同 taskId 是否已存在有效 COMPANY_DISPATCH 记录
        List<Map<String, Object>> existingMarks = jdbcTemplate.queryForList(
                "SELECT id, rank_order FROM top10_works WHERE work_type = 'COMPANY_DISPATCH' AND related_task_id = ? AND deleted = 0 ORDER BY rank_order ASC",
                taskId);
        if (!existingMarks.isEmpty()) {
            // 存量脏数据容忍策略（B5）：若存在多条记录，保留 rank_order 最小者，其余软删除
            if (existingMarks.size() > 1 && cleanupDirtyData) {
                log.warn("检测到 taskId={} 存在 {} 条 COMPANY_DISPATCH 脏数据，执行清理", taskId, existingMarks.size());
                Long keepId = (Long) (existingMarks.get(0).get("ID") != null ? existingMarks.get(0).get("ID") : existingMarks.get(0).get("id"));
                for (int i = 1; i < existingMarks.size(); i++) {
                    Long dirtyId = (Long) (existingMarks.get(i).get("ID") != null ? existingMarks.get(i).get("ID") : existingMarks.get(i).get("id"));
                    jdbcTemplate.update("UPDATE top10_works SET deleted = 1, updated_at = CURRENT_TIMESTAMP WHERE id = ?", dirtyId);
                }
                // 清理后重新查询
                existingMarks = jdbcTemplate.queryForList(
                        "SELECT id, rank_order FROM top10_works WHERE work_type = 'COMPANY_DISPATCH' AND related_task_id = ? AND deleted = 0",
                        taskId);
            }
            if (!existingMarks.isEmpty()) {
                Object rankObj = existingMarks.get(0).get("RANK_ORDER");
                if (rankObj == null) rankObj = existingMarks.get(0).get("rank_order");
                String currentRank = rankObj != null ? rankObj.toString() : "?";
                log.info("去重校验触发：taskId={} 已在十大派发中（当前排名{}）", taskId, currentRank);
                throw new BusinessException(ErrorCode.TOP10_TASK_ALREADY_MARKED,
                        "该任务已在十大派发中（当前排名" + currentRank + "），不可重复选择");
            }
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
        // rankOrder 范围校验（B5）：分离错误码
        if (rankOrder < 1 || rankOrder > 10) {
            throw new BusinessException(ErrorCode.TOP10_RANK_OUT_OF_RANGE);
        }

        jdbcTemplate.update(
                "INSERT INTO top10_works (user_id, work_type, rank_order, content, related_task_id) VALUES (?, 'COMPANY_DISPATCH', ?, ?, ?)",
                userId, rankOrder, "", taskId);

        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        // 保存后验证（B5）
        List<Map<String, Object>> verify = jdbcTemplate.queryForList(
                "SELECT id FROM top10_works WHERE id = ? AND deleted = 0", id);
        if (verify.isEmpty()) {
            log.error("保存后验证失败：top10_works id={} 未找到", id);
            throw new BusinessException(ErrorCode.TOP10_SAVE_VERIFY_FAILED);
        }

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
        // rankOrder 范围校验（B6）：分离错误码
        if (rankOrder < 1 || rankOrder > 10) {
            throw new BusinessException(ErrorCode.TOP10_RANK_OUT_OF_RANGE);
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
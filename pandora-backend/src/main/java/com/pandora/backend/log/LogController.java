package com.pandora.backend.log;

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
@RequestMapping("/log")
public class LogController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> createLog(@RequestBody Map<String, Object> request) {
        Long userId = getCurrentUserId();
        String workItem = (String) request.get("workItem");
        if (workItem == null || workItem.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD, "工作事项不能为空");
        }

        String completionStatus = (String) request.getOrDefault("completionStatus", "IN_PROGRESS");
        Object timeCostObj = request.get("timeCost");
        String logDate = (String) request.getOrDefault("logDate", new java.sql.Date(System.currentTimeMillis()).toString());

        jdbcTemplate.update(
                "INSERT INTO work_logs (user_id, work_item, completion_status, time_cost, log_date) VALUES (?, ?, ?, ?, ?)",
                userId, workItem, completionStatus, timeCostObj, logDate
        );

        Long logId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        Map<String, Object> result = new HashMap<>();
        result.put("id", logId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listLogs() {
        Long userId = getCurrentUserId();
        List<Map<String, Object>> logs = jdbcTemplate.queryForList(
                "SELECT * FROM work_logs WHERE user_id = ? AND deleted = 0 ORDER BY log_date DESC, created_at DESC", userId);
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

        stats.put("completionRate", completionRate);
        stats.put("importantRatio", Collections.emptyList());
        stats.put("analysisChart", Collections.emptyList());

        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @PutMapping("/top10")
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
    public ResponseEntity<ApiResponse<Map<String, Object>>> editLog(
            @PathVariable Long logId, @RequestBody Map<String, Object> request) {
        Long userId = getCurrentUserId();

        StringBuilder sql = new StringBuilder("UPDATE work_logs SET updated_at = CURRENT_TIMESTAMP");
        List<Object> params = new ArrayList<>();

        if (request.containsKey("workItem")) {
            sql.append(", work_item = ?");
            params.add(request.get("workItem"));
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

        Map<String, Object> result = new HashMap<>();
        result.put("id", logId);
        result.put("updated", true);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @DeleteMapping("/{logId}")
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

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
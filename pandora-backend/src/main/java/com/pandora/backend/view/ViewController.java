package com.pandora.backend.view;

import com.pandora.backend.common.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/view")
public class ViewController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/month")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMonthView(@RequestParam(required = false) String month) {
        Long userId = getCurrentUserId();
        Calendar cal = Calendar.getInstance();
        int currentMonth = cal.get(Calendar.MONTH) + 1;
        int currentYear = cal.get(Calendar.YEAR);

        List<Map<String, Object>> tasks = jdbcTemplate.queryForList(
                "SELECT * FROM tasks WHERE assignee_id = ? AND deleted = 0 AND MONTH(start_time) = ? OR MONTH(end_time) = ? ORDER BY start_time",
                userId, currentMonth, currentMonth);

        List<Map<String, Object>> logs = jdbcTemplate.queryForList(
                "SELECT * FROM work_logs WHERE user_id = ? AND deleted = 0 AND MONTH(log_date) = ? ORDER BY log_date",
                userId, currentMonth);

        Map<String, Object> result = new HashMap<>();
        result.put("tasks", tasks);
        result.put("logs", logs);
        result.put("month", currentMonth);
        result.put("year", currentYear);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/week")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getWeekView() {
        Long userId = getCurrentUserId();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        java.sql.Date weekStart = new java.sql.Date(cal.getTimeInMillis());
        cal.add(Calendar.DAY_OF_WEEK, 6);
        java.sql.Date weekEnd = new java.sql.Date(cal.getTimeInMillis());

        List<Map<String, Object>> tasks = jdbcTemplate.queryForList(
                "SELECT * FROM tasks WHERE assignee_id = ? AND deleted = 0 AND start_time BETWEEN ? AND ? ORDER BY start_time",
                userId, weekStart, weekEnd);

        Map<String, Object> result = new HashMap<>();
        result.put("tasks", tasks);
        result.put("weekStart", weekStart);
        result.put("weekEnd", weekEnd);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/day")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDayView(@RequestParam(required = false) String date) {
        Long userId = getCurrentUserId();
        if (date == null) {
            date = new java.sql.Date(System.currentTimeMillis()).toString();
        }

        List<Map<String, Object>> tasks = jdbcTemplate.queryForList(
                "SELECT * FROM tasks WHERE assignee_id = ? AND deleted = 0 AND DATE(start_time) = ? ORDER BY start_time",
                userId, date);

        List<Map<String, Object>> logs = jdbcTemplate.queryForList(
                "SELECT * FROM work_logs WHERE user_id = ? AND deleted = 0 AND log_date = ? ORDER BY created_at",
                userId, date);

        Map<String, Object> result = new HashMap<>();
        result.put("tasks", tasks);
        result.put("logs", logs);
        result.put("date", date);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/mindmap")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMindMap() {
        Long userId = getCurrentUserId();

        List<Map<String, Object>> companyImportant = jdbcTemplate.queryForList(
                "SELECT * FROM top10_works WHERE work_type = 'COMPANY_IMPORTANT' AND deleted = 0 ORDER BY rank_order LIMIT 10");
        List<Map<String, Object>> companyDispatch = jdbcTemplate.queryForList(
                "SELECT * FROM top10_works WHERE work_type = 'COMPANY_DISPATCH' AND deleted = 0 ORDER BY rank_order LIMIT 10");
        List<Map<String, Object>> personalImportant = jdbcTemplate.queryForList(
                "SELECT * FROM top10_works WHERE user_id = ? AND work_type = 'PERSONAL_IMPORTANT' AND deleted = 0 ORDER BY rank_order LIMIT 10", userId);
        List<Map<String, Object>> personalLog = jdbcTemplate.queryForList(
                "SELECT * FROM work_logs WHERE user_id = ? AND deleted = 0 ORDER BY log_date DESC LIMIT 10", userId);

        Map<String, Object> result = new HashMap<>();
        result.put("companyImportant", companyImportant);
        result.put("companyDispatch", companyDispatch);
        result.put("personalImportant", personalImportant);
        result.put("personalLog", personalLog);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/info-map")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getInfoMap() {
        Long userId = getCurrentUserId();
        List<Map<String, Object>> tasks = jdbcTemplate.queryForList(
                "SELECT id, name, status, priority FROM tasks WHERE assignee_id = ? AND deleted = 0", userId);

        Map<String, Object> result = new HashMap<>();
        result.put("nodes", tasks);
        result.put("edges", Collections.emptyList());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
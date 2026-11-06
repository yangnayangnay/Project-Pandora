package com.pandora.backend.view;

import com.pandora.backend.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
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

    @Autowired
    private com.pandora.backend.task.TaskTagService taskTags;

    @Autowired
    private PersonalImportantWorkService personalImportantWorks;

    @Autowired
    private com.pandora.backend.hierarchy.HierarchyService hierarchyService;

    @GetMapping("/month")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMonthView(@RequestParam(required = false) String month,
                                                                          @RequestParam(required = false) Boolean todayCoverage) {
        Long userId = getCurrentUserId();
        Calendar cal = Calendar.getInstance();
        int currentMonth = cal.get(Calendar.MONTH) + 1;
        int currentYear = cal.get(Calendar.YEAR);

        if (month != null && month.length() >= 7) {
            try {
                currentYear = Integer.parseInt(month.substring(0, 4));
                currentMonth = Integer.parseInt(month.substring(5, 7));
            } catch (Exception ignored) {}
        }

        Calendar monthStartCal = Calendar.getInstance();
        monthStartCal.set(currentYear, currentMonth - 1, 1, 0, 0, 0);
        monthStartCal.set(Calendar.MILLISECOND, 0);
        Calendar monthEndCal = Calendar.getInstance();
        monthEndCal.set(currentYear, currentMonth - 1, 1, 0, 0, 0);
        monthEndCal.set(Calendar.MILLISECOND, 0);
        monthEndCal.add(Calendar.MONTH, 1);
        monthEndCal.add(Calendar.MILLISECOND, -1);
        java.sql.Timestamp monthStart = new java.sql.Timestamp(monthStartCal.getTimeInMillis());
        java.sql.Timestamp monthEnd = new java.sql.Timestamp(monthEndCal.getTimeInMillis());

        List<Map<String, Object>> tasks;
        if (Boolean.TRUE.equals(todayCoverage)) {
            tasks = jdbcTemplate.queryForList(
                    "SELECT * FROM tasks WHERE assignee_id = ? AND deleted = 0 AND end_time >= CURRENT_DATE ORDER BY start_time",
                    userId);
        } else {
            tasks = jdbcTemplate.queryForList(
                    "SELECT * FROM tasks WHERE assignee_id = ? AND deleted = 0 AND " +
                    "start_time <= ? AND end_time >= ? ORDER BY start_time",
                    userId, monthEnd, monthStart);
        }

        List<Map<String, Object>> logs = jdbcTemplate.queryForList(
                "SELECT * FROM work_logs WHERE user_id = ? AND deleted = 0 AND YEAR(log_date) = ? AND MONTH(log_date) = ? ORDER BY log_date",
                userId, currentYear, currentMonth);

        sanitizeTaskDates(tasks);

        Map<String, Object> result = new HashMap<>();
        result.put("tasks", tasks);
        result.put("logs", logs);
        result.put("month", currentMonth);
        result.put("year", currentYear);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/week")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getWeekView(@RequestParam(required = false) Boolean todayCoverage) {
        Long userId = getCurrentUserId();
        Calendar cal = Calendar.getInstance();
        int diff = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        cal.add(Calendar.DAY_OF_MONTH, -diff);
        java.sql.Date weekStart = new java.sql.Date(cal.getTimeInMillis());
        cal.add(Calendar.DAY_OF_MONTH, 6);
        java.sql.Date weekEnd = new java.sql.Date(cal.getTimeInMillis());

        List<Map<String, Object>> tasks;
        if (Boolean.TRUE.equals(todayCoverage)) {
            tasks = jdbcTemplate.queryForList(
                    "SELECT * FROM tasks WHERE assignee_id = ? AND deleted = 0 AND end_time >= CURRENT_DATE ORDER BY start_time",
                    userId);
        } else {
            tasks = jdbcTemplate.queryForList(
                    "SELECT * FROM tasks WHERE assignee_id = ? AND deleted = 0 AND DATE(start_time) <= ? AND DATE(end_time) >= ? ORDER BY start_time",
                    userId, weekEnd, weekStart);
        }

        sanitizeTaskDates(tasks);

        Map<String, Object> result = new HashMap<>();
        result.put("tasks", tasks);
        result.put("weekStart", weekStart);
        result.put("weekEnd", weekEnd);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/day")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDayView(@RequestParam(required = false) String date,
                                                                        @RequestParam(required = false) Boolean todayCoverage) {
        Long userId = getCurrentUserId();
        if (date == null) {
            date = new java.sql.Date(System.currentTimeMillis()).toString();
        }

        List<Map<String, Object>> tasks;
        if (Boolean.TRUE.equals(todayCoverage)) {
            tasks = jdbcTemplate.queryForList(
                    "SELECT * FROM tasks WHERE assignee_id = ? AND deleted = 0 AND end_time >= CURRENT_DATE ORDER BY start_time",
                    userId);
        } else {
            tasks = jdbcTemplate.queryForList(
                    "SELECT * FROM tasks WHERE assignee_id = ? AND deleted = 0 AND " +
                    "DATE(start_time) <= ? AND DATE(end_time) >= ? ORDER BY start_time",
                    userId, date, date);
        }

        List<Map<String, Object>> logs = jdbcTemplate.queryForList(
                "SELECT * FROM work_logs WHERE user_id = ? AND deleted = 0 AND log_date = ? ORDER BY created_at",
                userId, date);

        sanitizeTaskDates(tasks);

        Map<String, Object> result = new HashMap<>();
        result.put("tasks", tasks);
        result.put("logs", logs);
        result.put("date", date);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/mindmap")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMindMap(HttpServletRequest servletRequest) {
        Long userId = getCurrentUserId();
        String role = getCurrentUserRole();

        List<Map<String, Object>> companyImportant = jdbcTemplate.queryForList(
                "SELECT * FROM top10_works WHERE work_type = 'COMPANY_IMPORTANT' AND deleted = 0 ORDER BY rank_order LIMIT 10");
        // REQ-31-08: 派发象限改为公司未完成任务自动聚合（优先级降序+创建时间降序 LIMIT 10），
        // 输出别名 task_name/assignee_name/assignee_username 与原查询对齐（App 端零改动兼容）；
        // 旧 top10_works COMPANY_DISPATCH 手动标记数据读取层面休眠（接口与存储保留）。
        List<Map<String, Object>> companyDispatch = jdbcTemplate.queryForList(
                "SELECT tk.id, tk.name AS task_name, tk.status, tk.priority, " +
                "tk.start_time, tk.end_time, tk.created_at, " +
                "CASE WHEN u.role='ADMIN' THEN '系统' ELSE u.real_name END AS assignee_name, " +
                "CASE WHEN u.role='ADMIN' THEN '系统' ELSE u.username END AS assignee_username " +
                "FROM tasks tk LEFT JOIN users u ON tk.assignee_id = u.id AND u.deleted = 0 " +
                "WHERE tk.deleted = 0 AND tk.status IN ('PENDING', 'IN_PROGRESS', 'REJECTED') " +
                "ORDER BY CASE tk.priority WHEN 'URGENT' THEN 4 WHEN 'HIGH' THEN 3 WHEN 'MEDIUM' THEN 2 ELSE 1 END DESC, " +
                "tk.created_at DESC LIMIT 10");

        List<Map<String, Object>> personalImportant = personalImportantWorks.list(userId);

        List<Map<String, Object>> personalLog = buildPersonalLog(userId, role);

        Map<String, Object> result = new HashMap<>();
        result.put("companyImportant", companyImportant);
        result.put("companyDispatch", companyDispatch);
        result.put("personalImportant", personalImportant);
        result.put("personalLog", personalLog);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private List<Map<String, Object>> buildPersonalLog(Long userId, String role) {
        // 优先展示今日日志，无今日日志则展示距离今日最近的上一次日志
        try {
            List<Map<String, Object>> todayLogs = jdbcTemplate.queryForList(
                    "SELECT l.*, u.username, u.real_name FROM work_logs l " +
                    "JOIN users u ON l.user_id = u.id " +
                    "WHERE l.user_id = ? AND l.deleted = 0 AND l.log_date = CURRENT_DATE " +
                    "ORDER BY l.created_at DESC " +
                    "LIMIT 1", userId);
            if (!todayLogs.isEmpty()) {
                return todayLogs;
            }
            return jdbcTemplate.queryForList(
                    "SELECT l.*, u.username, u.real_name FROM work_logs l " +
                    "JOIN users u ON l.user_id = u.id " +
                    "WHERE l.user_id = ? AND l.deleted = 0 " +
                    "ORDER BY l.log_date DESC, l.created_at DESC " +
                    "LIMIT 1", userId);
        } catch (Exception e) {
            return Collections.emptyList();
        }
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

    private void sanitizeTaskDates(List<Map<String, Object>> tasks) {
        if (tasks == null) return;
        taskTags.attach(tasks);
        java.text.SimpleDateFormat dateFmt = new java.text.SimpleDateFormat("yyyy-MM-dd");
        for (Map<String, Object> task : tasks) {
            for (String key : new String[]{"start_time", "end_time", "START_TIME", "END_TIME"}) {
                Object val = task.get(key);
                if (val instanceof java.util.Date) {
                    task.put(key, dateFmt.format((java.util.Date) val));
                }
            }
        }
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

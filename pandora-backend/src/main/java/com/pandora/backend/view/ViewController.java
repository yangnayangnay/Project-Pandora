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

    @Autowired
    private PersonalTop10Generator personalTop10Generator;

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
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        java.sql.Date weekStart = new java.sql.Date(cal.getTimeInMillis());
        cal.add(Calendar.DAY_OF_WEEK, 6);
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
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMindMap() {
        Long userId = getCurrentUserId();
        String role = getCurrentUserRole();

        List<Map<String, Object>> companyImportant = jdbcTemplate.queryForList(
                "SELECT * FROM top10_works WHERE work_type = 'COMPANY_IMPORTANT' AND deleted = 0 ORDER BY rank_order LIMIT 10");
        List<Map<String, Object>> companyDispatch = jdbcTemplate.queryForList(
                "SELECT t.*, tk.name AS task_name, u.real_name AS assignee_name, u.username AS assignee_username " +
                "FROM top10_works t LEFT JOIN tasks tk ON t.related_task_id = tk.id " +
                "LEFT JOIN users u ON tk.assignee_id = u.id " +
                "WHERE t.work_type = 'COMPANY_DISPATCH' AND t.deleted = 0 ORDER BY t.rank_order LIMIT 10");

        List<Map<String, Object>> personalImportant;
        try {
            personalImportant = personalTop10Generator.generate(userId);
        } catch (Exception e) {
            personalImportant = buildPersonalImportant(userId);
        }

        List<Map<String, Object>> personalLog = buildPersonalLog(userId, role);

        Map<String, Object> result = new HashMap<>();
        result.put("companyImportant", companyImportant);
        result.put("companyDispatch", companyDispatch);
        result.put("personalImportant", personalImportant);
        result.put("personalLog", personalLog);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private List<Map<String, Object>> buildPersonalImportant(Long userId) {
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            Map<String, Object> user = jdbcTemplate.queryForMap(
                    "SELECT birthday FROM users WHERE id = ? AND deleted = 0", userId);
            Object birthdayObj = user.get("birthday");
            if (birthdayObj != null) {
                String birthdayStr = birthdayObj.toString();
                java.sql.Date today = new java.sql.Date(System.currentTimeMillis());
                String todayStr = today.toString();
                String birthdayMd = birthdayStr.length() >= 10 ? birthdayStr.substring(5) : "";
                String todayMd = todayStr.length() >= 10 ? todayStr.substring(5) : "";
                if (!birthdayMd.isEmpty() && birthdayMd.equals(todayMd)) {
                    Map<String, Object> wish = new HashMap<>();
                    wish.put("rank_order", 1);
                    wish.put("content", "\u751F\u65E5\u5FEB\u4E50\uFF01");
                    wish.put("is_auto_filled", true);
                    wish.put("auto_type", "BIRTHDAY_WISH");
                    result.add(wish);
                }
            }
        } catch (Exception ignored) {}

        List<Map<String, Object>> importantTasks = jdbcTemplate.queryForList(
                "SELECT name, is_urgent, is_important, end_time FROM tasks " +
                "WHERE assignee_id = ? AND deleted = 0 AND status IN ('PENDING','IN_PROGRESS') " +
                "AND (is_urgent = 1 OR is_important = 1) " +
                "ORDER BY is_urgent DESC, is_important DESC, end_time ASC LIMIT 5", userId);
        int autoRank = result.isEmpty() ? 1 : 2;
        for (Map<String, Object> task : importantTasks) {
            Map<String, Object> item = new HashMap<>();
            item.put("rank_order", autoRank++);
            item.put("content", task.get("name"));
            item.put("is_auto_filled", true);
            item.put("auto_type", "IMPORTANT_TASK");
            result.add(item);
        }

        List<Map<String, Object>> manualItems = jdbcTemplate.queryForList(
                "SELECT * FROM top10_works WHERE user_id = ? AND work_type = 'PERSONAL_IMPORTANT' AND deleted = 0 ORDER BY rank_order LIMIT 10", userId);
        for (Map<String, Object> item : manualItems) {
            Map<String, Object> manual = new HashMap<>(item);
            manual.put("is_auto_filled", false);
            result.add(manual);
        }

        if (result.size() > 10) {
            result = result.subList(0, 10);
        }
        return result;
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
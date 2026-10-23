package com.pandora.backend.view;

import com.pandora.backend.common.ApiResponse;
import com.pandora.backend.ai.AiTransmitConfig;
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
                "u.real_name AS assignee_name, u.username AS assignee_username " +
                "FROM tasks tk LEFT JOIN users u ON tk.assignee_id = u.id AND u.deleted = 0 " +
                "WHERE tk.deleted = 0 AND tk.status IN ('PENDING', 'IN_PROGRESS', 'REJECTED') " +
                "ORDER BY CASE tk.priority WHEN 'URGENT' THEN 4 WHEN 'HIGH' THEN 3 WHEN 'MEDIUM' THEN 2 ELSE 1 END DESC, " +
                "tk.created_at DESC LIMIT 10");

        List<Map<String, Object>> personalImportant;
        try {
            // Round28 REQ-28-02: 同步段解析透传配置传入个人十大生成链（AI 点评走客户端配置端点）
            AiTransmitConfig aiConfig = AiTransmitConfig.fromHeaders(servletRequest);
            personalImportant = personalTop10Generator.generate(userId, aiConfig);
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

        // Round23 REQ-23-01: 兜底轻量账号条目（users 单查，空值跳过），替代原 IMPORTANT_TASK 聚合与全部手动条目查询；
        // 子查询异常被吞掉，不再导致接口 500
        int rank = result.isEmpty() ? 1 : 2;
        try {
            Map<String, Object> user = jdbcTemplate.queryForMap(
                    "SELECT real_name, username, role, phone, email FROM users WHERE id = ? AND deleted = 0", userId);
            rank = addFallbackAccountItem(result, rank, user.get("real_name"), "账号信息-真实姓名");
            rank = addFallbackAccountItem(result, rank, user.get("username"), "账号信息-用户名");
            Object roleObj = user.get("role");
            if (roleObj != null) {
                rank = addFallbackAccountItem(result, rank, toChineseRole(roleObj.toString()), "账号信息-角色");
            }
            rank = addFallbackAccountItem(result, rank, user.get("phone"), "账号信息-手机号");
            rank = addFallbackAccountItem(result, rank, user.get("email"), "账号信息-邮箱");
        } catch (Exception ignored) {}

        if (result.size() > 10) {
            result = result.subList(0, 10);
        }
        return result;
    }

    /**
     * Round23 REQ-23-01: 构造单条兜底账号信息条目并追加。值为 null 或空白时跳过返回原 rank；否则追加并返回 rank+1。
     * 键结构与主路径账号条目一致（work_type=ACCOUNT_INFO）。
     */
    private int addFallbackAccountItem(List<Map<String, Object>> result, int rank, Object value, String note) {
        if (value == null) return rank;
        String str = value.toString().trim();
        if (str.isEmpty()) return rank;
        Map<String, Object> item = new HashMap<>();
        item.put("rank_order", rank);
        item.put("content", str);
        item.put("is_auto_filled", true);
        item.put("work_type", "ACCOUNT_INFO");
        item.put("source_note", note);
        result.add(item);
        return rank + 1;
    }

    /**
     * Round23 REQ-23-01: 兜底路径角色英文转中文（映射与主路径 PersonalTop10Generator 一致；未知值原样返回）。
     */
    private String toChineseRole(String role) {
        switch (role) {
            case "FOUNDER": return "创始人";
            case "ADMIN": return "管理员";
            case "DEPT_HEAD": return "部门主管";
            case "TEAM_LEADER": return "团队负责人";
            case "EMPLOYEE": return "员工";
            default: return role;
        }
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
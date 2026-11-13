package com.pandora.backend.user;

import com.pandora.backend.common.ApiResponse;
import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.ErrorCode;
import com.pandora.backend.hierarchy.HierarchyService;
import com.pandora.backend.task.TaskTagService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/** Permission-scoped company directory for the company page. */
@RestController
@RequestMapping("/company/directory")
public class CompanyDirectoryController {
    private final JdbcTemplate db;
    private final HierarchyService hierarchy;
    private final TaskTagService tags;

    public CompanyDirectoryController(JdbcTemplate db, HierarchyService hierarchy, TaskTagService tags) {
        this.db = db; this.hierarchy = hierarchy; this.tags = tags;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        List<Long> ids = visibleIds(actor());
        if (ids.isEmpty()) return ApiResponse.success(Collections.emptyList());
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        return ApiResponse.success(OrganizationRules.normalize(db.queryForList(
                "SELECT u.id,u.username,u.real_name,u.role,u.phone,u.email,u.avatar,u.signature," +
                "u.department_id,d.name AS department_name,u.team_id,t.name AS team_name " +
                "FROM users u LEFT JOIN departments d ON d.id=u.department_id AND d.deleted=0 " +
                "LEFT JOIN teams t ON t.id=u.team_id AND t.deleted=0 " +
                "WHERE u.id IN (" + placeholders + ") AND u.deleted=0 AND u.approval_status='APPROVED' " +
                "AND u.role<>'ADMIN' ORDER BY d.name,t.name,u.real_name,u.username", ids.toArray())));
    }

    @GetMapping("/{userId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable long userId,
            @RequestParam(required = false) String taskStatus,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        requireVisible(userId);
        List<Map<String, Object>> people = db.queryForList(
                "SELECT u.id,u.username,u.real_name,u.role,u.phone,u.email,u.avatar,u.signature," +
                "u.department_id,d.name AS department_name,u.team_id,t.name AS team_name " +
                "FROM users u LEFT JOIN departments d ON d.id=u.department_id AND d.deleted=0 " +
                "LEFT JOIN teams t ON t.id=u.team_id AND t.deleted=0 WHERE u.id=? AND u.deleted=0", userId);
        if (people.isEmpty()) throw new BusinessException(ErrorCode.NOT_FOUND, "人员不存在");
        List<Object> taskArgs = new ArrayList<>(List.of(userId, userId));
        StringBuilder taskSql = new StringBuilder(
                "SELECT DISTINCT tk.*,CASE WHEN au.role='ADMIN' THEN '系统' ELSE au.username END AS assignee_name," +
                "CASE WHEN au.role='ADMIN' THEN '系统' ELSE au.real_name END AS assignee_real_name " +
                "FROM tasks tk LEFT JOIN task_assignees ta ON ta.task_id=tk.id " +
                "LEFT JOIN users au ON au.id=tk.assignee_id " +
                "WHERE (ta.user_id=? OR (ta.user_id IS NULL AND tk.assignee_id=?)) AND tk.deleted=0");
        if (taskStatus != null && !taskStatus.isBlank()) {
            if (!List.of("IN_PROGRESS", "COMPLETED").contains(taskStatus)) throw new BusinessException(ErrorCode.BAD_REQUEST, "任务状态无效");
            taskSql.append(" AND tk.status=?"); taskArgs.add(taskStatus);
        }
        if (startDate != null && !startDate.isBlank()) { taskSql.append(" AND tk.end_time>=?"); taskArgs.add(startDate); }
        if (endDate != null && !endDate.isBlank()) { taskSql.append(" AND tk.start_time<=?"); taskArgs.add(endDate); }
        taskSql.append(" ORDER BY tk.is_urgent DESC,tk.end_time,tk.created_at DESC");
        List<Map<String, Object>> tasks = tags.attach(db.queryForList(taskSql.toString(), taskArgs.toArray()));

        List<Object> logArgs = new ArrayList<>(List.of(userId));
        StringBuilder logSql = new StringBuilder("SELECT * FROM work_logs WHERE user_id=? AND deleted=0");
        if (startDate != null && !startDate.isBlank()) { logSql.append(" AND log_date>=?"); logArgs.add(startDate); }
        if (endDate != null && !endDate.isBlank()) { logSql.append(" AND log_date<=?"); logArgs.add(endDate); }
        logSql.append(" ORDER BY log_date DESC,created_at DESC");

        List<Object> itemArgs = new ArrayList<>(List.of(userId));
        StringBuilder itemSql = new StringBuilder("SELECT id,content,start_date,end_date,completed FROM daily_items WHERE user_id=? AND deleted=0");
        if (startDate != null && !startDate.isBlank()) { itemSql.append(" AND end_date>=?"); itemArgs.add(startDate); }
        if (endDate != null && !endDate.isBlank()) { itemSql.append(" AND start_date<=?"); itemArgs.add(endDate); }
        itemSql.append(" ORDER BY start_date,end_date,id");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("person", OrganizationRules.normalize(people.get(0)));
        result.put("tasks", tasks);
        result.put("logs", db.queryForList(logSql.toString(), logArgs.toArray()));
        result.put("calendar", db.queryForList(itemSql.toString(), itemArgs.toArray()));
        return ApiResponse.success(result);
    }

    private void requireVisible(long target) {
        if (!visibleIds(actor()).contains(target)) throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看该人员资料");
    }

    private List<Long> visibleIds(long actor) {
        String role = db.queryForObject("SELECT role FROM users WHERE id=? AND deleted=0", String.class, actor);
        if ("ADMIN".equals(role) || "FOUNDER".equals(role))
            return db.queryForList("SELECT id FROM users WHERE deleted=0 AND approval_status='APPROVED' AND role<>'ADMIN'", Long.class);
        if ("DEPT_HEAD".equals(role) || "TEAM_LEADER".equals(role)) {
            LinkedHashSet<Long> ids = new LinkedHashSet<>(hierarchy.getSubordinateIds(actor)); ids.add(actor);
            return new ArrayList<>(ids);
        }
        return List.of(actor);
    }

    private long actor() { return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal(); }
}

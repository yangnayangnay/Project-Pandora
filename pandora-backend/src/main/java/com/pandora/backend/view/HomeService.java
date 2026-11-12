package com.pandora.backend.view;

import com.pandora.backend.task.TaskTagService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class HomeService {
    private final JdbcTemplate db;
    private final TaskTagService tags;
    public HomeService(JdbcTemplate db, TaskTagService tags) { this.db = db; this.tags = tags; }

    public Map<String, Object> overview(long actor) {
        List<Map<String, Object>> events = normalized(db.queryForList("SELECT id, content, rank_order FROM top10_works WHERE work_type = 'COMPANY_IMPORTANT' AND deleted = 0 ORDER BY rank_order, id LIMIT 10"));
        List<Map<String, Object>> tasks = normalized(db.queryForList(
                "SELECT DISTINCT t.*, CASE WHEN d.role='ADMIN' THEN '系统' ELSE COALESCE(d.real_name,d.username) END AS dispatcher_name " +
                "FROM tasks t LEFT JOIN task_assignees ta ON ta.task_id=t.id LEFT JOIN users d ON d.id=t.dispatcher_id " +
                "WHERE (ta.user_id=? OR (ta.user_id IS NULL AND t.assignee_id=?)) AND t.deleted=0 AND t.status='IN_PROGRESS' " +
                "ORDER BY t.is_urgent DESC,t.created_at DESC,t.id DESC", actor, actor));
        attachAssignees(tasks);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("events", events); result.put("tasks", tags.attach(tasks));
        return result;
    }
    /** Compatibility for older callers; the completed switch is intentionally ignored. */
    public Map<String, Object> overview(long actor, boolean ignored) { return overview(actor); }

    private void attachAssignees(List<Map<String, Object>> tasks) {
        for (Map<String, Object> task : tasks) {
            long id = ((Number) task.get("id")).longValue();
            List<Map<String, Object>> people = normalized(db.queryForList(
                    "SELECT u.id,u.username,u.real_name FROM task_assignees ta JOIN users u ON u.id=ta.user_id WHERE ta.task_id=? ORDER BY ta.assigned_at,u.id", id));
            if (people.isEmpty() && task.get("assignee_id") instanceof Number) people = normalized(db.queryForList(
                    "SELECT id,username,real_name FROM users WHERE id=? AND deleted=0", task.get("assignee_id")));
            task.put("assignees", people);
            task.put("assignee_name", people.stream().map(person -> Objects.toString(person.get("real_name"), "").isBlank()
                    ? Objects.toString(person.get("username"), "") : Objects.toString(person.get("real_name"), "")).reduce((a,b) -> a + "、" + b).orElse(""));
        }
    }
    private static List<Map<String, Object>> normalized(List<Map<String, Object>> rows) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> normalized = new LinkedHashMap<>();
            row.forEach((key, value) -> normalized.put(key.toLowerCase(Locale.ROOT), value)); result.add(normalized);
        }
        return result;
    }
}

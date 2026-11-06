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

    public Map<String, Object> overview(long actor, boolean completed) {
        List<Map<String, Object>> events = normalized(db.queryForList("SELECT id, content, rank_order FROM top10_works WHERE work_type = 'COMPANY_IMPORTANT' AND deleted = 0 ORDER BY rank_order, id LIMIT 10"));
        List<Map<String, Object>> tasks = normalized(db.queryForList(
                "SELECT t.*, CASE WHEN d.role='ADMIN' THEN '系统' ELSE COALESCE(d.real_name, d.username) END AS dispatcher_name, " +
                "CASE WHEN u.role='ADMIN' THEN '系统' ELSE COALESCE(u.real_name, u.username) END AS assignee_name " +
                "FROM tasks t LEFT JOIN users d ON d.id = t.dispatcher_id LEFT JOIN users u ON u.id = t.assignee_id " +
                "WHERE t.assignee_id = ? AND t.deleted = 0 AND t.status " +
                (completed ? "IN ('COMPLETED', 'CONFIRMED') " : "IN ('PENDING', 'IN_PROGRESS', 'REJECTED') ") +
                "ORDER BY CASE t.priority WHEN 'URGENT' THEN 4 WHEN 'HIGH' THEN 3 WHEN 'MEDIUM' THEN 2 ELSE 1 END DESC, t.created_at DESC, t.id DESC", actor));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("events", events); result.put("tasks", tags.attach(tasks));
        return result;
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

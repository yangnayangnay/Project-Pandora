package com.pandora.backend.log;

import com.pandora.backend.hierarchy.HierarchyService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Returns organization labels only for people within the existing log visibility scope. */
@Service
public class LogDirectoryService {
    private final JdbcTemplate db;
    private final HierarchyService hierarchy;

    public LogDirectoryService(JdbcTemplate db, HierarchyService hierarchy) {
        this.db = db; this.hierarchy = hierarchy;
    }

    public List<Map<String, Object>> list(long viewerId) {
        List<Long> ids = hierarchy.getSubordinateIds(viewerId);
        if (ids.isEmpty()) return Collections.emptyList();
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        return db.queryForList("SELECT u.id, u.username, u.real_name, u.role, d.id AS department_id, " +
                "d.name AS department_name, t.id AS team_id, t.name AS team_name FROM users u " +
                "JOIN departments d ON d.id = u.department_id AND d.deleted = 0 " +
                "LEFT JOIN teams t ON t.id = u.team_id AND t.deleted = 0 " +
                "WHERE u.deleted = 0 AND u.role <> 'ADMIN' AND u.approval_status = 'APPROVED' AND u.id IN (" + placeholders + ") " +
                "ORDER BY d.name, t.name, u.real_name, u.username, u.id", ids.toArray());
    }
}

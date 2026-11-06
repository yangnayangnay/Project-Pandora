package com.pandora.backend.log;

import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.ErrorCode;
import com.pandora.backend.hierarchy.HierarchyService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class LogCommentService {
    private final JdbcTemplate db;
    private final HierarchyService hierarchy;

    public LogCommentService(JdbcTemplate db, HierarchyService hierarchy) {
        this.db = db; this.hierarchy = hierarchy;
    }

    public Map<String, Object> list(long logId, long viewerId) {
        boolean canComment = access(logId, viewerId);
        boolean administrator = "ADMIN".equals(db.queryForObject("SELECT role FROM users WHERE id=?", String.class, viewerId));
        List<Map<String, Object>> raw = db.queryForList(
                "SELECT c.id, c.author_id, c.content, c.created_at, " +
                "u.role AS author_role, " +
                "COALESCE(NULLIF(u.real_name, ''), u.username, '已注销用户') AS author_name " +
                "FROM log_comments c LEFT JOIN users u ON u.id = c.author_id " +
                "WHERE c.log_id = ? " + (administrator ? "" : "AND (u.role IS NULL OR u.role <> 'ADMIN') ")
                        + "ORDER BY c.created_at, c.id", logId);
        List<Map<String, Object>> comments = new ArrayList<>();
        for (Map<String, Object> row : raw) {
            Map<String, Object> normalized = new LinkedHashMap<>();
            row.forEach((key, value) -> normalized.put(key.toLowerCase(Locale.ROOT), value));
            comments.add(normalized);
        }
        return Map.of("comments", comments, "canComment", canComment);
    }

    @Transactional
    public Map<String, Object> add(long logId, long authorId, Object value) {
        if (!access(logId, authorId)) throw new BusinessException(ErrorCode.FORBIDDEN, "仅上级可以评论下级日志");
        if (!(value instanceof String)) throw new BusinessException(ErrorCode.BAD_REQUEST, "请输入评论内容");
        String content = ((String) value).strip();
        if (content.isEmpty()) throw new BusinessException(ErrorCode.BAD_REQUEST, "评论内容不能为空");
        if (content.length() > 2000) throw new BusinessException(ErrorCode.INVALID_PARAM_LENGTH, "评论不能超过2000字");
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        db.update(connection -> {
            var statement = connection.prepareStatement(
                    "INSERT INTO log_comments (log_id, author_id, content) VALUES (?, ?, ?)", new String[]{"id"});
            statement.setLong(1, logId); statement.setLong(2, authorId); statement.setString(3, content);
            return statement;
        }, key);
        return Map.of("id", key.getKey(), "content", content);
    }

    /** Checks the current database relationships on every read and write, including after transfers. */
    private boolean access(long logId, long viewerId) {
        List<Long> owners = db.queryForList("SELECT l.user_id FROM work_logs l JOIN users u ON u.id = l.user_id " +
                "WHERE l.id = ? AND l.deleted = 0 AND u.deleted = 0", Long.class, logId);
        if (owners.isEmpty()) throw new BusinessException(ErrorCode.NOT_FOUND, "日志不存在");
        List<String> roles = db.queryForList("SELECT role FROM users WHERE id = ? AND deleted = 0 " +
                "AND approval_status = 'APPROVED'", String.class, viewerId);
        if (roles.isEmpty()) throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问此日志评论");
        if (owners.get(0).equals(viewerId)) return false;
        String role = roles.get(0);
        String ownerRole = db.queryForObject("SELECT role FROM users WHERE id = ?", String.class, owners.get(0));
        boolean superior = Set.of("ADMIN", "FOUNDER", "DEPT_HEAD", "TEAM_LEADER").contains(role)
                && rank(role) > rank(ownerRole)
                && hierarchy.isSubordinate(viewerId, owners.get(0));
        if (!superior && !Set.of("ADMIN", "FOUNDER").contains(role))
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问此日志评论");
        return superior;
    }

    private int rank(String role) {
        if (role == null) return 0;
        return switch (role) {
            case "ADMIN" -> 5;
            case "FOUNDER" -> 4;
            case "DEPT_HEAD" -> 3;
            case "TEAM_LEADER" -> 2;
            case "EMPLOYEE" -> 1;
            default -> 0;
        };
    }
}

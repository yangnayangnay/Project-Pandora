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

/**
 * 日志评论服务——处理日志评论的创建、查询。
 * 权限控制：仅上级可评论下级日志。
 * 被 LogCommentController 调用。
 */
@Service
public class LogCommentService {
    private final JdbcTemplate db;
    private final HierarchyService hierarchy;
    private final com.pandora.backend.notification.NotificationService notifications;

    public LogCommentService(JdbcTemplate db, HierarchyService hierarchy,
            com.pandora.backend.notification.NotificationService notifications) {
        this.db = db; this.hierarchy = hierarchy; this.notifications = notifications;
    }

    /**
     * 获取日志评论列表。
     * 包含评论权限判断。
     *
     * @param logId 日志ID
     * @param viewerId 查看者ID
     * @return 包含评论列表和评论权限的响应
     */
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

    /**
     * 添加评论。
     * 业务规则：
     * - 仅上级可评论下级日志
     * - 评论内容不能为空
     * - 评论内容不超过2000字
     * - 添加成功后触发通知
     *
     * @param logId 日志ID
     * @param authorId 评论者ID
     * @param value 评论内容
     * @return 包含评论ID和内容的响应
     */
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
        notifications.detectLogComment(logId, authorId, content);
        return Map.of("id", key.getKey(), "content", content);
    }

    /**
     * 校验评论权限。
     * 规则：
     * - 日志所有者不能评论自己的日志
     * - 仅上级（ADMIN/FOUNDER/DEPT_HEAD/TEAM_LEADER）可评论下级日志
     * - 管理员/创始人可评论所有日志
     *
     * @param logId 日志ID
     * @param viewerId 查看者ID
     * @return true 表示有评论权限
     */
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

    /**
     * 计算角色权限等级（用于排序）。
     * 数字越大权限越高。
     */
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
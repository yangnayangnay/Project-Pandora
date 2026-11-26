package com.pandora.backend.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 通知服务——处理系统通知的创建和发送。
 * 包括日志评论通知、新任务通知、状态变更通知、日志完成通知、十大事项变更通知等。
 * 被各个业务模块调用。
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public NotificationService() {}
    public NotificationService(JdbcTemplate db) { this.jdbcTemplate = db; }

    /**
     * 检测日志评论并创建通知。
     * 在评论事务中调用，评论失败不会留下通知。
     * 管理员评论不会触发通知。
     *
     * @param logId 日志ID
     * @param authorId 评论者ID
     * @param comment 评论内容
     */
    public void detectLogComment(long logId, long authorId, String comment) {
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT l.user_id,l.log_date,u.role,COALESCE(NULLIF(u.real_name,''),u.username) AS author_name "
                + "FROM work_logs l JOIN users u ON u.id=? WHERE l.id=? AND l.deleted=0", authorId, logId);
        long owner = ((Number) row.get("user_id")).longValue();
        // 管理员评论不触发通知，自己评论自己也不触发
        if (owner == authorId || "ADMIN".equals(row.get("role"))) return;
        String excerpt = comment.replaceAll("\\s+", " ");
        if (excerpt.codePointCount(0, excerpt.length()) > 100)
            excerpt = excerpt.substring(0, excerpt.offsetByCodePoints(0, 100)) + "…";
        String content = row.get("author_name") + "评论了你" + row.get("log_date") + "的日志：" + excerpt;
        jdbcTemplate.update("INSERT INTO notifications(target_user_id,type,log_id,content) VALUES(?,'LOG_COMMENTED',?,?)", owner, logId, content);
    }

    /**
     * 记录事件通知。
     *
     * @param type 通知类型
     * @param taskId 任务ID（可选）
     * @param targetUserId 目标用户ID
     * @param content 通知内容
     */
    public void recordEvent(String type, Long taskId, Long targetUserId, String content) {
        jdbcTemplate.update(
                "INSERT INTO notifications (target_user_id, type, task_id, content) VALUES (?, ?, ?, ?)",
                targetUserId, type, taskId, content
        );
    }

    /**
     * 检测新任务并创建通知。
     * 派发给自己时不触发通知。
     *
     * @param taskId 任务ID
     * @param assigneeId 负责人ID
     * @param dispatcherId 派发人ID
     */
    public void detectNewTask(Long taskId, Long assigneeId, Long dispatcherId) {
        if (assigneeId.equals(dispatcherId)) return;
        try {
            Map<String, Object> task = jdbcTemplate.queryForMap("SELECT name FROM tasks WHERE id = ?", taskId);
            String taskName = task.get("name") != null ? task.get("name").toString() : task.get("NAME").toString();
            recordEvent("NEW_TASK", taskId, assigneeId, "新任务：" + taskName);
        } catch (Exception error) { log.error("New-task notification creation failed: task={}",taskId,error); }
    }

    /**
     * 检测任务状态变更并创建通知。
     * 状态未变更时不触发。
     *
     * @param taskId 任务ID
     * @param oldStatus 旧状态
     * @param newStatus 新状态
     * @param modifierId 修改人ID
     */
    public void detectStatusChange(Long taskId, String oldStatus, String newStatus, Long modifierId) {
        if (newStatus == null || newStatus.equals(oldStatus)) return;
        try {
            Map<String, Object> task = jdbcTemplate.queryForMap(
                    "SELECT name, assignee_id, dispatcher_id FROM tasks WHERE id = ?", taskId);
            String taskName = task.get("name") != null ? task.get("name").toString() : task.get("NAME").toString();
            Long assigneeId = ((Number) task.get("assignee_id")).longValue();
            Long dispatcherId = ((Number) task.get("dispatcher_id")).longValue();

            Long targetUserId = modifierId.equals(assigneeId) ? dispatcherId : assigneeId;
            String content = "COMPLETED".equals(newStatus)
                    ? "任务已完成：" + taskName
                    : "任务状态已更新：" + taskName;
            recordEvent("STATUS_CHANGE", taskId, targetUserId, content);
        } catch (Exception error) { log.error("Task-status notification creation failed: task={}",taskId,error); }
    }

    /**
     * TASK-R18-007: 下属完成日志通知（LOG_COMPLETED）。
     * 查 user_hierarchy 表找直属上级（superior_id，仅直属一级、不上溯）。
     * 通知失败不影响日志主流程。
     *
     * @param logUserId 日志所属用户ID
     * @param logDate 日志日期
     */
    public void detectLogCompleted(Long logUserId, String logDate) {
        try {
            List<Map<String, Object>> superiors = jdbcTemplate.queryForList(
                    "SELECT superior_id FROM user_hierarchy WHERE subordinate_id = ?", logUserId);
            if (superiors.isEmpty()) return;

            Object supIdObj = superiors.get(0).get("SUPERIOR_ID");
            if (supIdObj == null) supIdObj = superiors.get(0).get("superior_id");
            if (supIdObj == null) return;
            Long superiorId = ((Number) supIdObj).longValue();
            if (superiorId.equals(logUserId)) return;

            Map<String, Object> user = jdbcTemplate.queryForMap("SELECT real_name, username FROM users WHERE id = ?", logUserId);
            Object realName = user.get("real_name") != null ? user.get("real_name") : user.get("REAL_NAME");
            Object username = user.get("username") != null ? user.get("username") : user.get("USERNAME");
            String displayName = realName != null ? realName.toString() : (username != null ? username.toString() : "未知");

            String content = "下属已完成日志：" + displayName + " " + (logDate != null ? logDate : "");
            recordEvent("LOG_COMPLETED", null, superiorId, content);
        } catch (Exception error) { log.error("Log-completion notification creation failed: user={}",logUserId,error); }
    }

    /**
     * TASK-R18-008: 公司十大重要事项变更通知（TOP10_IMPORTANT_CHANGED）。
     * 查询 ADMIN+FOUNDER 角色用户（除操作者本人）逐一生成通知。
     *
     * @param operatorUserId 操作者ID
     * @param action 动作描述
     */
    public void detectTop10ImportantChanged(Long operatorUserId, String action) {
        detectTop10Changed(operatorUserId, action, "公司十大重要事项", "TOP10_IMPORTANT_CHANGED");
    }

    /**
     * TASK-R18-008: 公司十大任务派发变更通知（TOP10_DISPATCH_CHANGED）。
     *
     * @param operatorUserId 操作者ID
     * @param action 动作描述
     */
    public void detectTop10DispatchChanged(Long operatorUserId, String action) {
        detectTop10Changed(operatorUserId, action, "公司十大任务派发", "TOP10_DISPATCH_CHANGED");
    }

    /**
     * 通用的十大事项变更通知。
     *
     * @param operatorUserId 操作者ID
     * @param action 动作描述
     * @param bizName 业务名称
     * @param notifType 通知类型
     */
    private void detectTop10Changed(Long operatorUserId, String action, String bizName, String notifType) {
        try {
            Map<String, Object> opUser = jdbcTemplate.queryForMap("SELECT real_name, username FROM users WHERE id = ?", operatorUserId);
            Object opRealName = opUser.get("real_name") != null ? opUser.get("real_name") : opUser.get("REAL_NAME");
            Object opUsername = opUser.get("username") != null ? opUser.get("username") : opUser.get("USERNAME");
            String opDisplayName = opRealName != null ? opRealName.toString() : (opUsername != null ? opUsername.toString() : "未知");

            List<Map<String, Object>> targets = jdbcTemplate.queryForList(
                    "SELECT id FROM users WHERE role IN ('ADMIN', 'FOUNDER') AND deleted = 0 AND id != ?", operatorUserId);

            String content = bizName + "已" + action + "（操作者：" + opDisplayName + "）";
            for (Map<String, Object> target : targets) {
                Object idObj = target.get("id") != null ? target.get("id") : target.get("ID");
                if (idObj == null) continue;
                Long targetId = ((Number) idObj).longValue();
                recordEvent(notifType, null, targetId, content);
            }
        } catch (Exception error) { log.error("Top-ten notification creation failed: operator={},type={}",operatorUserId,notifType,error); }
    }
}
package com.pandora.backend.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void recordEvent(String type, Long taskId, Long targetUserId, String content) {
        jdbcTemplate.update(
                "INSERT INTO notifications (target_user_id, type, task_id, content) VALUES (?, ?, ?, ?)",
                targetUserId, type, taskId, content
        );
    }

    public void detectNewTask(Long taskId, Long assigneeId, Long dispatcherId) {
        if (assigneeId.equals(dispatcherId)) return;
        try {

            Map<String, Object> task = jdbcTemplate.queryForMap("SELECT name FROM tasks WHERE id = ?", taskId);
            String taskName = task.get("name") != null ? task.get("name").toString() : task.get("NAME").toString();
            recordEvent("NEW_TASK", taskId, assigneeId, "新任务：" + taskName);
        } catch (Exception ignored) {}
    }

    public void detectStatusChange(Long taskId, String oldStatus, String newStatus, Long modifierId) {
        if (newStatus == null || newStatus.equals(oldStatus)) return;
        try {
            Map<String, Object> task = jdbcTemplate.queryForMap(
                    "SELECT name, assignee_id, dispatcher_id FROM tasks WHERE id = ?", taskId);
            String taskName = task.get("name") != null ? task.get("name").toString() : task.get("NAME").toString();
            Long assigneeId = ((Number) task.get("assignee_id")).longValue();
            Long dispatcherId = ((Number) task.get("dispatcher_id")).longValue();

            Long targetUserId = modifierId.equals(assigneeId) ? dispatcherId : assigneeId;
            String content = "COMPLETED".equals(newStatus) || "CONFIRMED".equals(newStatus)
                    ? "任务已完成：" + taskName
                    : "任务状态已更新：" + taskName;
            recordEvent("STATUS_CHANGE", taskId, targetUserId, content);
        } catch (Exception ignored) {}
    }

    /**
     * TASK-R18-007: 下属完成日志通知（LOG_COMPLETED）
     * 查 user_hierarchy 表找直属上级（superior_id，仅直属一级、不上溯），
     * 组装"下属已完成日志：{姓名} {日志日期}"，为直属上级调用 recordEvent 单条插入。
     * 接收方不含操作者本人；沿用现有 try-catch 吞异常模式（通知失败不影响日志主流程）。
     */
    public void detectLogCompleted(Long logUserId, String logDate) {
        try {
            // 查直属上级（仅直属一级）
            List<Map<String, Object>> superiors = jdbcTemplate.queryForList(
                    "SELECT superior_id FROM user_hierarchy WHERE subordinate_id = ?", logUserId);
            if (superiors.isEmpty()) return;

            Object supIdObj = superiors.get(0).get("SUPERIOR_ID");
            if (supIdObj == null) supIdObj = superiors.get(0).get("superior_id");
            if (supIdObj == null) return;
            Long superiorId = ((Number) supIdObj).longValue();
            if (superiorId.equals(logUserId)) return; // 排除自指

            // 查下属姓名
            Map<String, Object> user = jdbcTemplate.queryForMap("SELECT real_name, username FROM users WHERE id = ?", logUserId);
            Object realName = user.get("real_name") != null ? user.get("real_name") : user.get("REAL_NAME");
            Object username = user.get("username") != null ? user.get("username") : user.get("USERNAME");
            String displayName = realName != null ? realName.toString() : (username != null ? username.toString() : "未知");

            String content = "下属已完成日志：" + displayName + " " + (logDate != null ? logDate : "");
            recordEvent("LOG_COMPLETED", null, superiorId, content);
        } catch (Exception ignored) {}
    }

    /**
     * TASK-R18-008: 公司十大重要事项变更通知（TOP10_IMPORTANT_CHANGED）
     * 查询 ADMIN+FOUNDER 角色用户（除操作者本人）逐一生成通知，
     * 内容组装"公司十大重要事项已{? 动作}（操作者：{姓名}）"。
     * 复用 recordEvent，沿用 try-catch 模式；每次业务动作仅单次调用（天然满足 REQ-R18-04 规则 7 无重复通知）。
     */
    public void detectTop10ImportantChanged(Long operatorUserId, String action) {
        detectTop10Changed(operatorUserId, action, "公司十大重要事项", "TOP10_IMPORTANT_CHANGED");
    }

    /**
     * TASK-R18-008: 公司十大任务派发变更通知（TOP10_DISPATCH_CHANGED）
     * 查询 ADMIN+FOUNDER 角色用户（除操作者本人）逐一生成通知，
     * 内容组装"公司十大任务派发已{动作}（操作者：{姓名}）"。
     */
    public void detectTop10DispatchChanged(Long operatorUserId, String action) {
        detectTop10Changed(operatorUserId, action, "公司十大任务派发", "TOP10_DISPATCH_CHANGED");
    }

    private void detectTop10Changed(Long operatorUserId, String action, String bizName, String notifType) {
        try {
            // 查操作者姓名
            Map<String, Object> opUser = jdbcTemplate.queryForMap("SELECT real_name, username FROM users WHERE id = ?", operatorUserId);
            Object opRealName = opUser.get("real_name") != null ? opUser.get("real_name") : opUser.get("REAL_NAME");
            Object opUsername = opUser.get("username") != null ? opUser.get("username") : opUser.get("USERNAME");
            String opDisplayName = opRealName != null ? opRealName.toString() : (opUsername != null ? opUsername.toString() : "未知");

            // 查 ADMIN+FOUNDER 角色用户（除操作者本人）
            List<Map<String, Object>> targets = jdbcTemplate.queryForList(
                    "SELECT id FROM users WHERE role IN ('ADMIN', 'FOUNDER') AND deleted = 0 AND id != ?", operatorUserId);

            String content = bizName + "已" + action + "（操作者：" + opDisplayName + "）";
            for (Map<String, Object> target : targets) {
                Object idObj = target.get("id") != null ? target.get("id") : target.get("ID");
                if (idObj == null) continue;
                Long targetId = ((Number) idObj).longValue();
                recordEvent(notifType, null, targetId, content);
            }
        } catch (Exception ignored) {}
    }
}
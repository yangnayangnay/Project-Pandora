package com.pandora.backend.notification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class NotificationService {

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
}
package com.example.project_pandora.notification;

import com.example.project_pandora.core.util.NumberCompat;
import com.example.project_pandora.core.util.TaskTags;
import java.util.Map;
import java.util.Objects;

/** Wire-format labels and navigation shared by the list and system notification. */
public final class NotificationContent {
    private NotificationContent() {}
    public static final String[] TYPES = {"NEW_TASK", "STATUS_CHANGE", "LOG_COMPLETED", "LOG_COMMENTED",
            "TOP10_DISPATCH_CHANGED", "TOP10_IMPORTANT_CHANGED"};
    public static Long logId(Map<String, Object> row) {
        if (!"LOG_COMMENTED".equals(TaskTags.value(row, "type"))) return null;
        Long id = NumberCompat.toLong(TaskTags.value(row, "log_id"));
        return id != null && id > 0 ? id : null;
    }
    public static String listTitle(String type) {
        switch (Objects.toString(type, "")) {
            case "NEW_TASK": return "任务派发";
            case "STATUS_CHANGE": return "任务更新";
            case "LOG_COMPLETED": return "日志动态";
            case "LOG_COMMENTED": return "上级评论日志";
            case "TOP10_DISPATCH_CHANGED": return "任务看板";
            case "TOP10_IMPORTANT_CHANGED": return "事件纪";
            default: return "工作通知";
        }
    }
    public static String systemTitle(String type) {
        switch (Objects.toString(type, "")) {
            case "NEW_TASK": case "STATUS_CHANGE": return "任务通知";
            case "LOG_COMPLETED": return "日志通知";
            case "LOG_COMMENTED": return "上级评论了你的日志";
            case "TOP10_DISPATCH_CHANGED": case "TOP10_IMPORTANT_CHANGED": return "导图变更通知";
            default: return "通知";
        }
    }
}

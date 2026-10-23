package com.example.project_pandora.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.project_pandora.MainActivity;
import com.example.project_pandora.R;
import com.example.project_pandora.ui.task.TaskDetailActivity;

/**
 * 任务/通知栏服务。
 *
 * <p>TASK-R18-016: 扩展按通知类型映射标题与点击跳转路由：
 * <ul>
 *   <li>NEW_TASK/STATUS_CHANGE → 标题"任务通知"，跳转 TaskDetailActivity</li>
 *   <li>LOG_COMPLETED → 标题"日志通知"，跳转主界面（无独立日志详情页）</li>
 *   <li>TOP10_DISPATCH_CHANGED/TOP10_IMPORTANT_CHANGED → 标题"导图变更通知"，跳转主界面（导图页由 Fragment 承载）</li>
 *   <li>未知类型 → 标题"通知"，跳转主界面兜底</li>
 * </ul>
 * 沿用单一渠道 task_notification + notify(notificationId) 系统级去重，避免渠道爆炸。
 * 未授予通知权限时不因弹通知失败而崩溃（try-catch 包裹）。</p>
 */
public class TaskNotificationService {

    private static final String CHANNEL_ID = "task_notification";
    private static final String CHANNEL_NAME = "任务通知";

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * TASK-R18-016: 按通知类型映射标题
     */
    public static String titleOfType(String type) {
        if (type == null) return "通知";
        switch (type) {
            case "NEW_TASK":
            case "STATUS_CHANGE":
                return "任务通知";
            case "LOG_COMPLETED":
                return "日志通知";
            case "TOP10_DISPATCH_CHANGED":
            case "TOP10_IMPORTANT_CHANGED":
                return "导图变更通知";
            default:
                return "通知";
        }
    }

    /**
     * TASK-R18-016: 按通知类型路由跳转目标
     *
     * @return 跳转 Intent（任务类跳 TaskDetailActivity，其余跳主界面兜底）
     */
    private static Intent intentOfType(Context context, String type, Long taskId) {
        if (type != null && (type.equals("NEW_TASK") || type.equals("STATUS_CHANGE")) && taskId != null) {
            Intent intent = new Intent(context, TaskDetailActivity.class);
            intent.putExtra("taskId", taskId);
            return intent;
        }
        // 日志/导图类通知及兜底跳转主界面
        return new Intent(context, MainActivity.class);
    }

    /**
     * 弹出通知栏通知（兼容旧调用，默认类型任务通知）
     */
    public static void showNotification(Context context, long notificationId, String title, String content, Long taskId) {
        showNotification(context, notificationId, "STATUS_CHANGE", title, content, taskId);
    }

    /**
     * TASK-R18-016: 弹出通知栏通知（按类型标题与跳转路由）
     *
     * @param context       上下文
     * @param notificationId 通知 ID（用于系统级去重）
     * @param type          通知类型（NEW_TASK/STATUS_CHANGE/LOG_COMPLETED/TOP10_*）
     * @param fallbackTitle 兜底标题（type 为 null 时使用）
     * @param content       通知正文
     * @param taskId        任务 ID（任务类通知跳转用，可为 null）
     */
    public static void showNotification(Context context, long notificationId, String type,
                                        String fallbackTitle, String content, Long taskId) {
        try {
            String title = titleOfType(type);
            if ("通知".equals(title) && fallbackTitle != null) {
                title = fallbackTitle;
            }

            Intent intent = intentOfType(context, type, taskId);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(
                    context, (int) notificationId, intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(title)
                    .setContentText(content != null ? content : "")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent);

            // R45: lint MissingPermission 显式防线（权限拒绝时静默跳过，语义与原 catch 一致）
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(context).notify((int) notificationId, builder.build());
            }
        } catch (Exception ignored) {
            // TASK-R18-016 规则 6：未授予通知权限时不因弹通知失败而崩溃
        }
    }
}
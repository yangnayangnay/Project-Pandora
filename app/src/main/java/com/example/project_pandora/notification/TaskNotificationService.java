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

    public static final String CHANNEL_ID = "work_updates_v2";
    private static final String CHANNEL_NAME = "工作动态提醒";

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null && manager.getNotificationChannel(CHANNEL_ID)==null) {
                NotificationChannel old=manager.getNotificationChannel("task_notification");
                // Migrate the old default-level channel, while respecting an explicit user block.
                int importance=NotificationManager.IMPORTANCE_HIGH;
                if(old!=null && (old.getImportance()==NotificationManager.IMPORTANCE_NONE
                        || (Build.VERSION.SDK_INT>=29 && old.hasUserSetImportance()))) importance=old.getImportance();
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance);
                channel.setDescription("任务、日志评论及公司十大事项变更提醒");
                channel.enableVibration(true);
                channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PRIVATE);
                manager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * TASK-R18-016: 按通知类型映射标题
     */
    public static String titleOfType(String type) {
        return NotificationContent.systemTitle(type);
    }

    /**
     * TASK-R18-016: 按通知类型路由跳转目标
     *
     * @return 跳转 Intent（任务类跳 TaskDetailActivity，其余跳主界面兜底）
     */
    private static Intent intentOfType(Context context, String type, Long taskId, Long logId) {
        if ("LOG_COMMENTED".equals(type) && logId != null && logId > 0) {
            return new Intent(context, com.example.project_pandora.ui.log.LogDetailActivity.class)
                    .putExtra(com.example.project_pandora.ui.log.LogDetailActivity.EXTRA_LOG_ID, logId.longValue());
        }
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
        showNotification(context, notificationId, type, fallbackTitle, content, taskId, null);
    }

    public static void showNotification(Context context, long notificationId, String type,
                                        String fallbackTitle, String content, Long taskId, Long logId) {
        postNotification(context,notificationId,type,fallbackTitle,content,taskId,logId);
    }

    public static boolean alertsAllowed(Context context) {
        createNotificationChannel(context);
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false;
        if (Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,android.Manifest.permission.POST_NOTIFICATIONS)
                !=android.content.pm.PackageManager.PERMISSION_GRANTED) return false;
        if (Build.VERSION.SDK_INT>=26) {
            NotificationManager manager=context.getSystemService(NotificationManager.class);
            NotificationChannel channel=manager==null ? null : manager.getNotificationChannel(CHANNEL_ID);
            return channel!=null && channel.getImportance()!=NotificationManager.IMPORTANCE_NONE;
        }
        return true;
    }

    public static String accountTag(Context context) {
        return "work_user_"+com.example.project_pandora.core.network.TokenManager.getInstance(context).getUserId();
    }

    public static boolean postNotification(Context context, long notificationId, String type,
                                        String fallbackTitle, String content, Long taskId, Long logId) {
        try {
            if(!alertsAllowed(context)) return false;
            String title = titleOfType(type);
            if ("通知".equals(title) && fallbackTitle != null) {
                title = fallbackTitle;
            }

            Intent intent = intentOfType(context, type, taskId, logId);
            intent.putExtra("notificationId",notificationId);
            intent.putExtra("notificationUserId",com.example.project_pandora.core.network.TokenManager.getInstance(context).getUserId().longValue());
            intent.setData(android.net.Uri.parse("pandora://notification/"+accountTag(context)+"/"+notificationId));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(
                    context, (int) notificationId, intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(title)
                    .setContentText(content != null ? content : "")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_EVENT)
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(content != null ? content : ""))
                    .setOnlyAlertOnce(true)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent);

            // R45: lint MissingPermission 显式防线（权限拒绝时静默跳过，语义与原 catch 一致）
            if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(context).notify(accountTag(context),(int) notificationId, builder.build());
                return true;
            }
        } catch (Exception error) {
            android.util.Log.w("WorkNotification","Notification posting failed",error);
            // TASK-R18-016 规则 6：未授予通知权限时不因弹通知失败而崩溃
        }
        return false;
    }
}

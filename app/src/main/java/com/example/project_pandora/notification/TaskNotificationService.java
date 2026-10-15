package com.example.project_pandora.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.project_pandora.R;
import com.example.project_pandora.ui.task.TaskDetailActivity;

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

    public static void showNotification(Context context, long notificationId, String title, String content, Long taskId) {
        Intent intent = new Intent(context, TaskDetailActivity.class);
        if (taskId != null) {
            intent.putExtra("taskId", taskId);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(
                context, (int) notificationId, intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title != null ? title : "任务通知")
                .setContentText(content != null ? content : "")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat.from(context).notify((int) notificationId, builder.build());
    }
}
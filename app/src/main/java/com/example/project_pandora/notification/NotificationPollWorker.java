package com.example.project_pandora.notification;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.data.remote.NotificationApi;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.core.Observable;

public class NotificationPollWorker extends Worker {

    private static final String PREF_NAME = "notification_prefs";
    private static final String KEY_LAST_ID = "last_notification_id";

    public NotificationPollWorker(Context context, WorkerParameters params) {
        super(context, params);
    }

    @Override
    public Result doWork() {
        try {
            SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            long lastId = prefs.getLong(KEY_LAST_ID, 0);

            NotificationApi api = NetworkClient.getInstance(getApplicationContext()).createService(NotificationApi.class);

            AtomicReference<Result> resultRef = new AtomicReference<>(Result.success());
            api.pollNotifications(lastId)
                    .subscribe(
                            response -> {
                                if (response.isSuccess() && response.getData() != null) {
                                    List<Map<String, Object>> notifications = response.getData();
                                    long maxId = lastId;
                                    for (Map<String, Object> notification : notifications) {
                                        Object idObj = notification.get("id");
                                        if (idObj == null) idObj = notification.get("ID");
                                        long notifId = 0;
                                        if (idObj != null) {
                                            try { notifId = Long.parseLong(idObj.toString()); } catch (NumberFormatException ignored) {}
                                        }

                                        Object contentObj = notification.get("content");
                                        if (contentObj == null) contentObj = notification.get("CONTENT");
                                        Object taskIdObj = notification.get("task_id");
                                        if (taskIdObj == null) taskIdObj = notification.get("TASK_ID");
                                        Long taskId = null;
                                        if (taskIdObj != null) {
                                            try { taskId = Long.parseLong(taskIdObj.toString()); } catch (NumberFormatException ignored) {}
                                        }

                                        TaskNotificationService.showNotification(
                                                getApplicationContext(), notifId,
                                                "任务通知",
                                                contentObj != null ? contentObj.toString() : "",
                                                taskId);

                                        if (notifId > maxId) maxId = notifId;
                                    }
                                    if (maxId > lastId) {
                                        prefs.edit().putLong(KEY_LAST_ID, maxId).apply();
                                    }
                                }
                            },
                            error -> resultRef.set(Result.retry())
                    );

            return resultRef.get();
        } catch (Exception e) {
            return Result.retry();
        }
    }

    public static void enqueuePeriodicWork(Context context) {
        androidx.work.PeriodicWorkRequest workRequest =
                new androidx.work.PeriodicWorkRequest.Builder(NotificationPollWorker.class, 15, java.util.concurrent.TimeUnit.MINUTES)
                        .build();
        androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "notification_poll",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                workRequest
        );
    }
}
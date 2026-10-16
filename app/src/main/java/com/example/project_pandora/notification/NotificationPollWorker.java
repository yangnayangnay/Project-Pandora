package com.example.project_pandora.notification;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.data.remote.NotificationApi;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.core.Observable;

/**
 * 通知轮询 Worker。
 *
 * <p>TASK-R18-015: 修复异步竞争缺陷 + 偏好过滤：
 * <ol>
 *   <li>同步阻塞模式：CountDownLatch 等待 RxJava 回调落地再返回 Result，确保成功/失败状态真实返回给 Worker 重试机制</li>
 *   <li>轮询后读取通知偏好做弹窗过滤：本地 SharedPreferences 缓存优先，缓存缺失时调偏好接口拉取并写缓存，接口失败默认全部开启</li>
 *   <li>被关闭类型的通知仅保留记录不弹窗</li>
 *   <li>保留 SharedPreferences 持久化 last_notification_id 增量游标机制，重启后依据游标继续增量轮询不重复弹出历史通知</li>
 * </ol></p>
 *
 * <p>TASK-R18-016: 按通知类型映射标题与跳转路由（调用 TaskNotificationService.showNotification 的类型重载）。</p>
 */
public class NotificationPollWorker extends Worker {

    private static final String PREF_NAME = "notification_prefs";
    private static final String KEY_LAST_ID = "last_notification_id";
    /** 偏好缓存 key 前缀（pref_type_ + 通知类型） */
    private static final String KEY_PREF_PREFIX = "pref_type_";
    /** 偏好缓存时间戳 key（用于判断缓存是否过期，1 小时过期） */
    private static final String KEY_PREF_TIMESTAMP = "pref_timestamp";
    private static final long PREF_CACHE_TTL_MS = 60 * 60 * 1000L;

    /** 5 类通知类型（与后端保持一致） */
    private static final String[] NOTIFICATION_TYPES = {
            "NEW_TASK", "STATUS_CHANGE", "LOG_COMPLETED",
            "TOP10_DISPATCH_CHANGED", "TOP10_IMPORTANT_CHANGED"
    };

    public NotificationPollWorker(Context context, WorkerParameters params) {
        super(context, params);
    }

    @Override
    public Result doWork() {
        try {
            SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            long lastId = prefs.getLong(KEY_LAST_ID, 0);

            NotificationApi api = NetworkClient.getInstance(getApplicationContext()).createService(NotificationApi.class);

            // TASK-R18-015: 同步阻塞模式——CountDownLatch 等待 RxJava 回调落地再返回
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<Result> resultRef = new AtomicReference<>(Result.success());

            api.pollNotifications(lastId)
                    .subscribe(
                            response -> {
                                try {
                                    if (response.isSuccess() && response.getData() != null) {
                                        List<Map<String, Object>> notifications = response.getData();

                                        // TASK-R18-015: 读取通知偏好做弹窗过滤
                                        Set<String> disabledTypes = loadDisabledTypes(api, prefs);

                                        long maxId = lastId;
                                        for (Map<String, Object> notification : notifications) {
                                            Object idObj = notification.get("id");
                                            if (idObj == null) idObj = notification.get("ID");
                                            long notifId = 0;
                                            if (idObj != null) {
                                                try { notifId = Long.parseLong(idObj.toString()); } catch (NumberFormatException ignored) {}
                                            }

                                            Object typeObj = notification.get("type");
                                            if (typeObj == null) typeObj = notification.get("TYPE");
                                            String type = typeObj != null ? typeObj.toString() : null;

                                            Object contentObj = notification.get("content");
                                            if (contentObj == null) contentObj = notification.get("CONTENT");
                                            Object taskIdObj = notification.get("task_id");
                                            if (taskIdObj == null) taskIdObj = notification.get("TASK_ID");
                                            Long taskId = null;
                                            if (taskIdObj != null) {
                                                try { taskId = Long.parseLong(taskIdObj.toString()); } catch (NumberFormatException ignored) {}
                                            }

                                            // TASK-R18-015: 被关闭类型的通知仅保留记录不弹窗
                                            if (type == null || !disabledTypes.contains(type)) {
                                                TaskNotificationService.showNotification(
                                                        getApplicationContext(), notifId, type,
                                                        "通知",
                                                        contentObj != null ? contentObj.toString() : "",
                                                        taskId);
                                            }

                                            if (notifId > maxId) maxId = notifId;
                                        }
                                        if (maxId > lastId) {
                                            prefs.edit().putLong(KEY_LAST_ID, maxId).apply();
                                        }
                                    }
                                    resultRef.set(Result.success());
                                } finally {
                                    latch.countDown();
                                }
                            },
                            error -> {
                                try {
                                    resultRef.set(Result.retry());
                                } finally {
                                    latch.countDown();
                                }
                            }
                    );

            // 等待回调落地（最长 30 秒超时，避免 Worker 卡死）
            if (!latch.await(30, TimeUnit.SECONDS)) {
                return Result.retry();
            }
            return resultRef.get();
        } catch (Exception e) {
            return Result.retry();
        }
    }

    /**
     * TASK-R18-015: 读取被关闭的通知类型集合
     * 本地 SharedPreferences 缓存优先，缓存缺失或过期时调偏好接口拉取并写缓存，接口失败默认全部开启（返回空集合）
     */
    private Set<String> loadDisabledTypes(NotificationApi api, SharedPreferences prefs) {
        Set<String> disabled = new HashSet<>();

        // 判断缓存是否过期
        long cacheTimestamp = prefs.getLong(KEY_PREF_TIMESTAMP, 0);
        boolean cacheExpired = (System.currentTimeMillis() - cacheTimestamp) > PREF_CACHE_TTL_MS;

        // 缓存有效时直接读本地
        if (!cacheExpired && cacheTimestamp > 0) {
            for (String type : NOTIFICATION_TYPES) {
                if (!prefs.getBoolean(KEY_PREF_PREFIX + type, true)) {
                    disabled.add(type);
                }
            }
            return disabled;
        }

        // 缓存缺失或过期，调偏好接口拉取
        try {
            final CountDownLatch latch = new CountDownLatch(1);
            final boolean[] success = {false};
            final Map<String, Object>[] prefsMap = new Map[]{null};

            api.getPreferences()
                    .subscribe(
                            response -> {
                                try {
                                    if (response.isSuccess() && response.getData() != null) {
                                        prefsMap[0] = response.getData();
                                        success[0] = true;
                                    }
                                } finally {
                                    latch.countDown();
                                }
                            },
                            error -> latch.countDown()
                    );

            if (latch.await(10, TimeUnit.SECONDS) && success[0] && prefsMap[0] != null) {
                // 写入本地缓存
                SharedPreferences.Editor editor = prefs.edit();
                for (String type : NOTIFICATION_TYPES) {
                    Object value = prefsMap[0].get(type);
                    boolean enabled = true;
                    if (value instanceof Boolean) {
                        enabled = (Boolean) value;
                    } else if (value != null) {
                        try { enabled = Boolean.parseBoolean(value.toString()); } catch (Exception ignored) {}
                    }
                    editor.putBoolean(KEY_PREF_PREFIX + type, enabled);
                    if (!enabled) disabled.add(type);
                }
                editor.putLong(KEY_PREF_TIMESTAMP, System.currentTimeMillis());
                editor.apply();
            }
            // 接口失败默认全部开启（disabled 为空集合）
        } catch (Exception ignored) {
            // 异常时默认全部开启
        }

        return disabled;
    }

    /**
     * 周期轮询（15 分钟，WorkManager 系统下限）
     */
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

    /**
     * TASK-R18-017: 前台即时轮询触发（OneTimeWorkRequest，REPLACE 防重复入队）
     * 在 MainActivity onStart 与导图页/任务页 onResume 调用
     */
    public static void enqueueOneTimeWork(Context context) {
        androidx.work.OneTimeWorkRequest workRequest =
                new androidx.work.OneTimeWorkRequest.Builder(NotificationPollWorker.class)
                        .build();
        androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
                "notification_poll_onetime",
                androidx.work.ExistingWorkPolicy.REPLACE,
                workRequest
        );
    }
}
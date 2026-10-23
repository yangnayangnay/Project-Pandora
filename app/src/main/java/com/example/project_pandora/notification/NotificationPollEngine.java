package com.example.project_pandora.notification;

import android.content.Context;
import android.content.SharedPreferences;

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
 * R45 REQ-D: 通知轮询共享内核——Worker（WorkManager 15min 周期兜底）与
 * PollingForegroundService（前台服务保活）双入口的唯一轮询实现。
 * <p>防重复通知双保险：
 * <ol>
 *   <li>游标键唯一：PREF_NAME/KEY_LAST_ID 与既有 Worker 逐字一致（双入口写同一 SharedPreferences 游标）</li>
 *   <li>notify(notificationId) 系统级去重：同一通知 ID 重复 notify 仅刷新不新增</li>
 * </ol></p>
 * <p>逻辑自 NotificationPollWorker.doWork 完整迁移（TASK-R18-015 同步阻塞/偏好过滤/
 * D-33-02 游标基线自适应语义逐条保持）。</p>
 */
public final class NotificationPollEngine {

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

    private NotificationPollEngine() {}

    /**
     * 单次轮询（同步阻塞，最长 30s）。
     *
     * @return true=本轮成功（含空结果）；false=失败（游标未推进，调用方可重试）
     */
    public static boolean pollOnce(Context context) {
        try {
            Context app = context.getApplicationContext();
            SharedPreferences prefs = app.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            long lastId = prefs.getLong(KEY_LAST_ID, 0);

            NotificationApi api = NetworkClient.getInstance(app).createService(NotificationApi.class);

            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<Boolean> resultRef = new AtomicReference<>(true);

            api.pollNotifications(lastId)
                    .subscribe(
                            response -> {
                                try {
                                    // D-33-02 修复：游标基线自适应——本地游标超前服务端全局 MAX(id)
                                    // （单设备迁移到新库的基线差异）时重置为服务端基线，仅影响后续增量
                                    // 拉取（不回溯弹历史通知），根治"永远拉空"
                                    Long serverMaxId = response.getMaxNotificationId();
                                    long effectiveLastId = lastId;
                                    if (serverMaxId != null && effectiveLastId > serverMaxId) {
                                        prefs.edit().putLong(KEY_LAST_ID, serverMaxId).apply();
                                        effectiveLastId = serverMaxId;
                                    }
                                    if (response.isSuccess() && response.getData() != null) {
                                        List<Map<String, Object>> notifications = response.getData();

                                        Set<String> disabledTypes = loadDisabledTypes(api, prefs);

                                        long maxId = effectiveLastId;
                                        for (Map<String, Object> notification : notifications) {
                                            Object idObj = notification.get("id");
                                            if (idObj == null) idObj = notification.get("ID");
                                            long notifId = com.example.project_pandora.core.util.NumberCompat.toLongOrDefault(idObj, 0L);

                                            Object typeObj = notification.get("type");
                                            if (typeObj == null) typeObj = notification.get("TYPE");
                                            String type = typeObj != null ? typeObj.toString() : null;

                                            Object contentObj = notification.get("content");
                                            if (contentObj == null) contentObj = notification.get("CONTENT");
                                            Object taskIdObj = notification.get("task_id");
                                            if (taskIdObj == null) taskIdObj = notification.get("TASK_ID");
                                            Long taskId = com.example.project_pandora.core.util.NumberCompat.toLong(taskIdObj);

                                            // 被关闭类型的通知仅保留记录不弹窗
                                            if (type == null || !disabledTypes.contains(type)) {
                                                TaskNotificationService.showNotification(
                                                        app, notifId, type,
                                                        "通知",
                                                        contentObj != null ? contentObj.toString() : "",
                                                        taskId);
                                            }

                                            if (notifId > maxId) maxId = notifId;
                                        }
                                        if (maxId > effectiveLastId) {
                                            prefs.edit().putLong(KEY_LAST_ID, maxId).apply();
                                        }
                                    }
                                    resultRef.set(true);
                                } finally {
                                    latch.countDown();
                                }
                            },
                            error -> {
                                try {
                                    resultRef.set(false);
                                } finally {
                                    latch.countDown();
                                }
                            }
                    );

            // 等待回调落地（最长 30 秒超时，避免调用方卡死）
            if (!latch.await(30, TimeUnit.SECONDS)) {
                return false;
            }
            return resultRef.get();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 读取被关闭的通知类型集合。
     * 本地 SharedPreferences 缓存优先，缓存缺失或过期时调偏好接口拉取并写缓存，接口失败默认全部开启（返回空集合）。
     */
    private static Set<String> loadDisabledTypes(NotificationApi api, SharedPreferences prefs) {
        Set<String> disabled = new HashSet<>();

        long cacheTimestamp = prefs.getLong(KEY_PREF_TIMESTAMP, 0);
        boolean cacheExpired = (System.currentTimeMillis() - cacheTimestamp) > PREF_CACHE_TTL_MS;

        if (!cacheExpired && cacheTimestamp > 0) {
            for (String type : NOTIFICATION_TYPES) {
                if (!prefs.getBoolean(KEY_PREF_PREFIX + type, true)) {
                    disabled.add(type);
                }
            }
            return disabled;
        }

        try {
            final CountDownLatch latch = new CountDownLatch(1);
            final boolean[] success = {false};
            @SuppressWarnings("unchecked")
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
        } catch (Exception ignored) {
            // 异常时默认全部开启
        }

        return disabled;
    }
}
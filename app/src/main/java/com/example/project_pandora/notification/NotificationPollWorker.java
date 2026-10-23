package com.example.project_pandora.notification;

import android.content.Context;

import androidx.work.Worker;
import androidx.work.WorkerParameters;



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
        // R45 REQ-D: 薄壳化——轮询内核迁移至 NotificationPollEngine（与前台服务共享）
        return NotificationPollEngine.pollOnce(getApplicationContext())
                ? Result.success() : Result.retry();
    }

    /**
     * TASK-R18-015: 读取被关闭的通知类型集合
     * 本地 SharedPreferences 缓存优先，缓存缺失或过期时调偏好接口拉取并写缓存，接口失败默认全部开启（返回空集合）
     */
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
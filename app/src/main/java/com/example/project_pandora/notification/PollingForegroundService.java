package com.example.project_pandora.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.project_pandora.R;
import com.example.project_pandora.core.network.TokenManager;

/**
 * R45 REQ-D: 息屏通知保活前台服务。
 * <p>根因（R30 实测）：ColorOS 息屏约 10 分钟后将 cached 进程冻结，WorkManager 周期
 * 任务被 JobScheduler 强制延迟最长 45 分钟——息屏期间通知不可达。</p>
 * <p>机制：startForeground 将进程提升至前台服务优先级（系统不冻结），服务内守护线程
 * 每 15 分钟调用 NotificationPollEngine.pollOnce（与 Worker 共享游标键+notify(id)
 * 系统去重双保险防重复通知）。Android 15 dataSync 类型 6 小时限额到期 onTimeout
 * 优雅退出交 WorkManager 兜底（禁止超时后立即自启）。</p>
 * <p>启动链：登录成功/已登录冷启（前台 Activity 生命周期内，规避 Android 12+ 后台
 * 启动限制）；停止链：登出。回前台补收兜底（enqueueOneTimeWork）原样保留。</p>
 */
public class PollingForegroundService extends Service {

    private static final String TAG = "PollFgService";
    public static final String CHANNEL_KEEP_ALIVE = "keep_alive";
    private static final int NOTIF_ID_KEEP_ALIVE = 2001;

    /** 守护轮询节拍（15 分钟，DFX 约束：不比 WorkManager 更频繁） */
    private static final long POLL_INTERVAL_MS = 15 * 60 * 1000L;

    private volatile boolean running = false;
    private Thread pollThread;

    @Override
    public void onCreate() {
        super.onCreate();
        createKeepAliveChannel();
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        // START_STICKY 重建时 intent 可能为 null——容忍判空
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_KEEP_ALIVE)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(getString(R.string.keep_alive_notif_title))
                .setContentText(getString(R.string.keep_alive_notif_text))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .build();
        startForeground(NOTIF_ID_KEEP_ALIVE, notification);

        if (!running) {
            running = true;
            pollThread = new Thread(this::pollLoop, "PollingForegroundService");
            pollThread.setDaemon(true);
            pollThread.start();
            Log.i(TAG, "foreground polling started (interval=15min)");
        }
        return START_STICKY;
    }

    private void pollLoop() {
        while (running) {
            try {
                // 登录态前置校验（兜住 TokenInterceptor 401 被动登出路径——未登录不空转轮询）
                if (TokenManager.getInstance(this).isLoggedIn()) {
                    boolean ok = NotificationPollEngine.pollOnce(this);
                    if (!ok) {
                        Log.w(TAG, "pollOnce failed（游标未推进，下轮重试）");
                    }
                } else {
                    Log.i(TAG, "not logged in, skip poll");
                }
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                Log.w(TAG, "pollLoop error: " + e.getMessage());
                try {
                    Thread.sleep(POLL_INTERVAL_MS);
                } catch (InterruptedException ie) {
                    break;
                }
            }
        }
    }

    /**
     * Android 15+（API 35）dataSync 前台服务 6 小时限额到期回调：
     * 优雅退出交 WorkManager 兜底——禁止超时后立即自启。
     */
    @Override
    public void onTimeout(int startId) {
        Log.i(TAG, "dataSync 6h limit reached, graceful shutdown（WorkManager 兜底接管）");
        running = false;
        stopSelf();
    }

    @Override
    public void onDestroy() {
        running = false;
        if (pollThread != null) {
            pollThread.interrupt();
        }
        Log.i(TAG, "foreground polling stopped");
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createKeepAliveChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null && nm.getNotificationChannel(CHANNEL_KEEP_ALIVE) == null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_KEEP_ALIVE,
                        getString(R.string.keep_alive_channel_name),
                        NotificationManager.IMPORTANCE_LOW); // 低优先级：无声、无横幅
                channel.setDescription(getString(R.string.keep_alive_channel_desc));
                nm.createNotificationChannel(channel);
            }
        }
    }

    /** 启动助手（幂等；须在前台 Activity 生命周期内调用——规避 Android 12+ 后台启动限制） */
    public static void startIfLoggedIn(Context context) {
        try {
            if (TokenManager.getInstance(context).isLoggedIn()) {
                ContextCompat.startForegroundService(
                        context, new Intent(context, PollingForegroundService.class));
            }
        } catch (Exception e) {
            Log.w(TAG, "startIfLoggedIn failed: " + e.getMessage());
        }
    }

    /** 停止助手（空操作安全） */
    public static void stop(Context context) {
        try {
            context.stopService(new Intent(context, PollingForegroundService.class));
        } catch (Exception ignored) {
        }
    }
}
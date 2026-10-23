package com.example.project_pandora;

import android.app.Application;
import android.os.Build;
import android.util.Log;

import com.example.project_pandora.core.network.NetworkStateMonitor;
import com.example.project_pandora.core.network.ServerConnectivityMonitor;
import com.example.project_pandora.core.network.ServerUrlManager;
import com.example.project_pandora.core.sync.SyncManager;
import com.example.project_pandora.core.util.StartupTracer;
import com.example.project_pandora.notification.NotificationPollWorker;
import com.example.project_pandora.notification.TaskNotificationService;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class PandoraApplication extends Application {
    private static final String TAG = "PandoraApp";

    @Override
    public void onCreate() {
        super.onCreate();

        // R37 REQ-37-B5: 主题首帧前应用（三态持久化；无记录=跟随系统，升级无感）
        com.example.project_pandora.core.config.ThemePrefs.applyTheme(this);

        StartupTracer.begin("notificationChannel");
        TaskNotificationService.createNotificationChannel(this);

        // R45 REQ-D: keep_alive 低优先级渠道（前台服务常驻通知专用，无声无横幅）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.NotificationChannel keepAlive = new android.app.NotificationChannel(
                    com.example.project_pandora.notification.PollingForegroundService.CHANNEL_KEEP_ALIVE,
                    getString(com.example.project_pandora.R.string.keep_alive_channel_name),
                    android.app.NotificationManager.IMPORTANCE_LOW);
            keepAlive.setDescription(getString(com.example.project_pandora.R.string.keep_alive_channel_desc));
            ((android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                    .createNotificationChannel(keepAlive);
        }
        StartupTracer.end("notificationChannel");

        Completable.fromAction(() -> {
            StartupTracer.begin("enqueuePeriodicWork");
            NotificationPollWorker.enqueuePeriodicWork(this);
            StartupTracer.end("enqueuePeriodicWork");

            // R44 REQ-D 规则4: 同步周期兜底任务（5min；即时链保留）
            StartupTracer.begin("enqueueSyncPeriodicWork");
            com.example.project_pandora.core.sync.SyncWorker.enqueuePeriodicWork(this);
            StartupTracer.end("enqueueSyncPeriodicWork");

            StartupTracer.begin("networkStateRegister");
            NetworkStateMonitor.getInstance().register(this);
            StartupTracer.end("networkStateRegister");

            // Round29: 服务器地址配置预热（先于健康检查启动，保证首轮即读到生效地址）
            StartupTracer.begin("serverUrlInit");
            ServerUrlManager.getInstance(this);
            StartupTracer.end("serverUrlInit");

            StartupTracer.begin("serverConnStart");
            ServerConnectivityMonitor.getInstance().startMonitoring(this);
            StartupTracer.end("serverConnStart");

            StartupTracer.begin("syncManagerInit");
            SyncManager.getInstance().init(this);
            StartupTracer.end("syncManagerInit");
        })
        .subscribeOn(Schedulers.io())
        .subscribe(
                () -> Log.i(TAG, "async init completed"),
                error -> Log.e(TAG, "async init failed", error)
        );
    }
}
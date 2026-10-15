package com.example.project_pandora;

import android.app.Application;
import android.util.Log;

import com.example.project_pandora.core.network.NetworkStateMonitor;
import com.example.project_pandora.core.network.ServerConnectivityMonitor;
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

        StartupTracer.begin("notificationChannel");
        TaskNotificationService.createNotificationChannel(this);
        StartupTracer.end("notificationChannel");

        Completable.fromAction(() -> {
            StartupTracer.begin("enqueuePeriodicWork");
            NotificationPollWorker.enqueuePeriodicWork(this);
            StartupTracer.end("enqueuePeriodicWork");

            StartupTracer.begin("networkStateRegister");
            NetworkStateMonitor.getInstance().register(this);
            StartupTracer.end("networkStateRegister");

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
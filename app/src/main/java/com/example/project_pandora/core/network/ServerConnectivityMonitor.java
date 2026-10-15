package com.example.project_pandora.core.network;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.project_pandora.core.sync.ServerConnectivity;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.subjects.BehaviorSubject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ServerConnectivityMonitor {

    private static final String CHANNEL_ID = "pandora_notifications";
    private static final int NOTIF_SERVER_RECONNECTED = 1001;

    private static volatile ServerConnectivityMonitor instance;

    private final BehaviorSubject<ServerConnectivity> stateSubject;
    private volatile ServerConnectivity currentState;
    private final AtomicBoolean monitoring;
    private final AtomicBoolean isForeground;
    private volatile int pollIntervalMs;
    private Thread monitorThread;
    private Context appContext;

    private static final int FOREGROUND_INTERVAL_MS = 15000;
    private static final int BACKGROUND_INTERVAL_MS = 60000;
    private static final int HEALTH_TIMEOUT_S = 5;

    private ServerConnectivityMonitor() {
        stateSubject = BehaviorSubject.createDefault(ServerConnectivity.CHECKING);
        currentState = ServerConnectivity.CHECKING;
        monitoring = new AtomicBoolean(false);
        isForeground = new AtomicBoolean(true);
        pollIntervalMs = FOREGROUND_INTERVAL_MS;
    }

    public static ServerConnectivityMonitor getInstance() {
        if (instance == null) {
            synchronized (ServerConnectivityMonitor.class) {
                if (instance == null) {
                    instance = new ServerConnectivityMonitor();
                }
            }
        }
        return instance;
    }

    public void startMonitoring(Context context) {
        this.appContext = context.getApplicationContext();
        if (monitoring.getAndSet(true)) return;
        Log.i("ServerConnMon", "startMonitoring() starting monitor thread");
        monitorThread = new Thread(this::monitorLoop, "ServerConnectivityMonitor");
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    public void stopMonitoring() {
        monitoring.set(false);
        if (monitorThread != null) {
            monitorThread.interrupt();
        }
    }

    private void monitorLoop() {
        while (monitoring.get()) {
            checkHealth();
            try {
                Thread.sleep(pollIntervalMs);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void checkHealth() {
        boolean connected = false;
        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(HEALTH_TIMEOUT_S, TimeUnit.SECONDS)
                    .readTimeout(HEALTH_TIMEOUT_S, TimeUnit.SECONDS)
                    .build();
            String healthUrl = NetworkClient.getBaseUrl() + "actuator/health";
            Log.d("ServerConnMon", "checkHealth() url=" + healthUrl);
            Request request = new Request.Builder().url(healthUrl).get().build();
            try (Response response = client.newCall(request).execute()) {
                connected = response.isSuccessful();
                Log.d("ServerConnMon", "checkHealth() response code=" + response.code() + " connected=" + connected);
            }
        } catch (Exception e) {
            Log.w("ServerConnMon", "checkHealth() failed: " + e.getMessage());
            connected = false;
        }

        ServerConnectivity newState = connected ? ServerConnectivity.CONNECTED : ServerConnectivity.DISCONNECTED;
        updateState(newState);
    }

    private void updateState(ServerConnectivity newState) {
        Log.i("ServerConnMon", "updateState: " + currentState + " -> " + newState);
        if (newState != currentState) {
            ServerConnectivity oldState = currentState;
            currentState = newState;
            stateSubject.onNext(newState);
            Log.i("ServerConnMon", "stateSubject.onNext(" + newState + ")");

            if (oldState == ServerConnectivity.DISCONNECTED && newState == ServerConnectivity.CONNECTED) {
                sendReconnectedNotification();
            }
        }
    }

    private void sendReconnectedNotification() {
        if (appContext == null) return;
        try {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(appContext, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("服务器已恢复连接")
                    .setContentText("您可以正常使用应用了")
                    .setAutoCancel(true);
            NotificationManager nm = (NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(NOTIF_SERVER_RECONNECTED, builder.build());
        } catch (Exception ignored) {}
    }

    public ServerConnectivity checkNow() {
        boolean connected = false;
        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(HEALTH_TIMEOUT_S, TimeUnit.SECONDS)
                    .readTimeout(HEALTH_TIMEOUT_S, TimeUnit.SECONDS)
                    .build();
            String healthUrl = NetworkClient.getBaseUrl() + "actuator/health";
            Request request = new Request.Builder().url(healthUrl).get().build();
            try (Response response = client.newCall(request).execute()) {
                connected = response.isSuccessful();
            }
        } catch (Exception e) {
            connected = false;
        }
        ServerConnectivity newState = connected ? ServerConnectivity.CONNECTED : ServerConnectivity.DISCONNECTED;
        updateState(newState);
        return newState;
    }

    public ServerConnectivity getCurrentState() {
        return currentState;
    }

    public io.reactivex.rxjava3.core.Observable<ServerConnectivity> stateStream() {
        return stateSubject.hide();
    }

    public void onAppForeground() {
        isForeground.set(true);
        pollIntervalMs = FOREGROUND_INTERVAL_MS;
    }

    public void onAppBackground() {
        isForeground.set(false);
        pollIntervalMs = BACKGROUND_INTERVAL_MS;
    }
}
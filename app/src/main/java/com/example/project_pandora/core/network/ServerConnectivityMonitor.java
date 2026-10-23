package com.example.project_pandora.core.network;

import android.app.NotificationManager;
import android.content.Context;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.project_pandora.R;
import com.example.project_pandora.core.sync.ServerConnectivity;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.subjects.BehaviorSubject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Round29 起：单服务器健康检查（15s/60s 节拍+断连/恢复通知）。
 * R44 REQ-B/C: 升级为多服务端故障转移状态机（内部六态语义）：
 * - STEADY：活跃端健康（failCount=0）
 * - DEGRADING：活跃端探测失败 1 次（防抖窗口，外部仍 CONNECTED）
 * - SCANNING：连续第 2 次失败=断连确认 → 同步扫描列表（跳过停用条目）逐台 probe，
 *   首台健康者 setActiveUrl（唯一切换动作，BaseUrlInterceptor 次请求即生效）+ "已切换至"通知
 * - ALL_DISCONNECTED：扫描无可达者 → 外部 DISCONNECTED；每轮探测全部启用条目，
 *   任一成功 → 接回（按序首个）+ 既有"服务器已恢复连接"通知
 * - RECOVERING/SWITCHBACK（D1 自动回切）：STEADY 且活跃端≠列表头时每轮附随探测列表头，
 *   连续 2 次成功 → setActiveUrl(列表头)+"已切回云服务端"通知；任一次失败 recoverCount 清零（双向防抖）
 * 外部接口（stateStream/ServerConnectivity/前台后台节拍/DEF-36-01 每轮无条件回调）保持不变。
 */
public class ServerConnectivityMonitor {

    private static final String TAG = "ServerConnMon";
    private static final String CHANNEL_ID = "pandora_notifications";
    private static final int NOTIF_SERVER_RECONNECTED = 1001;
    private static final int NOTIF_SERVER_SWITCHED = 1002;

    private static volatile ServerConnectivityMonitor instance;

    private final BehaviorSubject<ServerConnectivity> stateSubject;
    private volatile ServerConnectivity currentState;
    private final AtomicBoolean monitoring;
    private final AtomicBoolean isForeground;
    private volatile int pollIntervalMs;
    private Thread monitorThread;
    private Context appContext;
    private ServerUrlManager serverUrlManager;

    /** R44: 活跃端断连防抖计数（0-2；任一次成功清零） */
    private int failCount = 0;
    /** R44: 回切目标恢复计数（0-2；任一次失败清零——双向防抖抑制振荡） */
    private int recoverCount = 0;

    private static final int FOREGROUND_INTERVAL_MS = 15000;
    private static final int BACKGROUND_INTERVAL_MS = 60000;
    private static final int HEALTH_TIMEOUT_S = 5;
    /** R44 REQ-B 规则 1: 连续 2 次探测失败才确认断连 */
    private static final int FAIL_CONFIRM_THRESHOLD = 2;
    /** R44 REQ-C: 回切目标连续 2 次探测成功才执行切换 */
    private static final int RECOVER_CONFIRM_THRESHOLD = 2;

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
        this.serverUrlManager = ServerUrlManager.getInstance(appContext);
        if (monitoring.getAndSet(true)) return;
        Log.i(TAG, "startMonitoring() starting monitor thread");
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
            try {
                handleProbeCycle();
            } catch (Exception e) {
                Log.w(TAG, "probe cycle error: " + e.getMessage());
            }
            try {
                Thread.sleep(pollIntervalMs);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    /**
     * R44: 单轮探测周期（状态机主入口；checkNow() 同入口避免旁路）。
     */
    private synchronized void handleProbeCycle() {
        if (serverUrlManager == null) {
            // 防御：未初始化（startMonitoring 未调）时退化为单地址探测
            legacyProbe();
            return;
        }
        String activeUrl = serverUrlManager.getActiveUrl();
        List<ServerEntry> list = serverUrlManager.getServerList();

        // 外部 DISCONNECTED（全断兜底）：探测全部启用条目，任一成功接回
        if (currentState == ServerConnectivity.DISCONNECTED) {
            probeAllDisconnected(list);
            notifyNetworkStateMonitor();
            return;
        }

        boolean activeHealthy = probe(activeUrl);

        if (activeHealthy) {
            failCount = 0;
            updateState(ServerConnectivity.CONNECTED);
            attemptSwitchback(list, activeUrl);
        } else {
            failCount++;
            Log.w(TAG, "FAIL_COUNT:" + failCount + " target=" + activeUrl);
            if (failCount >= FAIL_CONFIRM_THRESHOLD) {
                Log.w(TAG, "DISCONNECT_CONFIRMED target=" + activeUrl);
                failoverScan(list, activeUrl);
            }
            // DEGRADING（failCount=1）外部仍 CONNECTED——不误报断连横幅
        }
        notifyNetworkStateMonitor();
    }

    /** 全断兜底：探测全部启用条目，按序首个成功者接回（REQ-B 规则 5） */
    private void probeAllDisconnected(List<ServerEntry> list) {
        for (ServerEntry e : list) {
            if (!e.isEnabled()) continue;
            if (probe(e.getUrl())) {
                Log.i(TAG, "RECONNECTED target=" + e.getUrl());
                serverUrlManager.setActiveUrl(e.getUrl());
                failCount = 0;
                recoverCount = 0;
                updateState(ServerConnectivity.CONNECTED); // 触发既有"服务器已恢复连接"通知+SyncManager 联动
                return;
            }
        }
        // 全部仍不可达：保持 DISCONNECTED
        updateState(ServerConnectivity.DISCONNECTED);
    }

    /** 断连确认后依序扫描（跳过停用条目与刚断连条目；REQ-B 规则 2/3/4） */
    private void failoverScan(List<ServerEntry> list, String failedUrl) {
        for (ServerEntry e : list) {
            if (!e.isEnabled()) continue;
            if (e.getUrl().equals(failedUrl)) continue; // 跳过刚断连条目
            if (probe(e.getUrl())) {
                Log.i(TAG, "FAILOVER_SWITCHED from=" + failedUrl + " to=" + e.getUrl());
                serverUrlManager.setActiveUrl(e.getUrl());
                failCount = 0;
                sendSwitchedNotification(
                        appContext != null ? appContext.getString(R.string.server_switched_to, e.getUrl()) : e.getUrl());
                updateState(ServerConnectivity.CONNECTED);
                return;
            }
        }
        Log.w(TAG, "ALL_SERVERS_DOWN（" + list.size() + " 台均不可达）");
        updateState(ServerConnectivity.DISCONNECTED);
    }

    /** D1 自动回切：STEADY 且活跃端≠列表头时每轮附随探测列表头（双向防抖） */
    private void attemptSwitchback(List<ServerEntry> list, String activeUrl) {
        String headUrl = list.get(0).getUrl();
        if (headUrl.equals(activeUrl)) {
            recoverCount = 0;
            return;
        }
        if (probe(headUrl)) {
            recoverCount++;
            Log.i(TAG, "RECOVER_COUNT:" + recoverCount + " target=" + headUrl);
            if (recoverCount >= RECOVER_CONFIRM_THRESHOLD) {
                Log.i(TAG, "SWITCHBACK to=" + headUrl);
                serverUrlManager.setActiveUrl(headUrl);
                recoverCount = 0;
                sendSwitchedNotification(
                        appContext != null ? appContext.getString(R.string.server_switchback_cloud) : headUrl);
                updateState(ServerConnectivity.CONNECTED);
            }
        } else {
            recoverCount = 0; // 双向防抖：列表头任一次失败清零（REQ-C 异常场景 3 振荡抑制）
        }
    }

    /**
     * R44: 参数化探测内核（活跃端轮询/扫描/全断轮询/回切判定四路共用）。
     */
    private boolean probe(String baseUrl) {
        boolean connected = false;
        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(HEALTH_TIMEOUT_S, TimeUnit.SECONDS)
                    .readTimeout(HEALTH_TIMEOUT_S, TimeUnit.SECONDS)
                    .build();
            String healthUrl = baseUrl + "actuator/health";
            Log.d(TAG, "probe() url=" + healthUrl);
            Request request = new Request.Builder().url(healthUrl).get().build();
            try (Response response = client.newCall(request).execute()) {
                connected = response.isSuccessful();
                Log.d(TAG, "probe() response code=" + response.code() + " connected=" + connected);
            }
        } catch (Exception e) {
            Log.w(TAG, "probe() failed: " + e.getMessage());
            connected = false;
        }
        return connected;
    }

    /** 防御回退：ServerUrlManager 未初始化时保持既有单地址行为 */
    private void legacyProbe() {
        boolean connected = probe(NetworkClient.getBaseUrl());
        ServerConnectivity newState = connected ? ServerConnectivity.CONNECTED : ServerConnectivity.DISCONNECTED;
        updateState(newState);
        notifyNetworkStateMonitor();
    }

    /**
     * DEF-36-01 修复保留：每轮探测后无条件通知 NetworkStateMonitor（幂等自愈兜底）。
     */
    private void notifyNetworkStateMonitor() {
        NetworkStateMonitor.getInstance().onServerConnectivityChanged(currentState);
    }

    private void updateState(ServerConnectivity newState) {
        Log.i(TAG, "updateState: " + currentState + " -> " + newState);
        if (newState != currentState) {
            ServerConnectivity oldState = currentState;
            currentState = newState;
            stateSubject.onNext(newState);
            Log.i(TAG, "stateSubject.onNext(" + newState + ")");

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
            // R45: lint 显式权限防线（与外层 catch ignored 语义一致）
            if (androidx.core.content.ContextCompat.checkSelfPermission(appContext,
                    android.Manifest.permission.POST_NOTIFICATIONS)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                NotificationManager nm = (NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE);
                nm.notify(NOTIF_SERVER_RECONNECTED, builder.build());
            }
        } catch (Exception ignored) {}
    }

    /** R44: 切换/回切通知（沿既有渠道模式） */
    private void sendSwitchedNotification(String text) {
        if (appContext == null || text == null) return;
        try {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(appContext, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(appContext.getString(R.string.app_name))
                    .setContentText(text)
                    .setAutoCancel(true);
            if (androidx.core.content.ContextCompat.checkSelfPermission(appContext,
                    android.Manifest.permission.POST_NOTIFICATIONS)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                NotificationManager nm = (NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE);
                nm.notify(NOTIF_SERVER_SWITCHED, builder.build());
            }
        } catch (Exception ignored) {}
    }

    /** 手动"重新检测服务器"：走同一状态机入口（避免旁路绕过故障转移逻辑） */
    public synchronized ServerConnectivity checkNow() {
        handleProbeCycle();
        return currentState;
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
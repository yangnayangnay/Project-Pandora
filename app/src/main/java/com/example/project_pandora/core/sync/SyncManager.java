package com.example.project_pandora.core.sync;

import android.content.Context;

import com.example.project_pandora.core.network.NetworkStateMonitor;
import com.example.project_pandora.core.network.ServerConnectivityMonitor;
import com.example.project_pandora.core.network.TokenManager;
import com.example.project_pandora.repository.SyncRepository;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import android.util.Log;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

public class SyncManager {

    private static volatile SyncManager instance;
    private static final int MAX_RETRY_COUNT = 3;

    private final AtomicBoolean syncing;
    private final AtomicInteger retryCount;
    private final BehaviorSubject<SyncProgress> progressSubject;
    private final BehaviorSubject<SyncState> statusSubject;
    private final CompositeDisposable disposables;

    private SyncService syncService;
    private SyncRepository syncRepository;
    private NetworkStateMonitor networkStateMonitor;
    private ServerConnectivityMonitor serverConnectivityMonitor;
    private TokenManager tokenManager;
    private volatile boolean initialized;

    private SyncManager() {
        syncing = new AtomicBoolean(false);
        retryCount = new AtomicInteger(0);
        progressSubject = BehaviorSubject.createDefault(SyncProgress.empty());
        statusSubject = BehaviorSubject.createDefault(SyncState.IDLE);
        disposables = new CompositeDisposable();
    }

    public static SyncManager getInstance() {
        if (instance == null) {
            synchronized (SyncManager.class) {
                if (instance == null) {
                    instance = new SyncManager();
                }
            }
        }
        return instance;
    }

    public void init(Context context) {
        if (initialized) return;
        initialized = true;
        this.syncService = new SyncService(context);
        this.syncRepository = new SyncRepository(context);
        this.networkStateMonitor = NetworkStateMonitor.getInstance();
        this.serverConnectivityMonitor = ServerConnectivityMonitor.getInstance();
        this.tokenManager = TokenManager.getInstance(context);
        Log.i("SyncManager", "init() called, subscribing to streams");

        disposables.add(networkStateMonitor.modeStream()
                .subscribe(mode -> {
                    Log.i("SyncManager", "modeStream emitted: " + mode + ", isLoggedIn=" + tokenManager.isLoggedIn());
                    if (mode == NetworkMode.ONLINE && tokenManager.isLoggedIn()) {
                        triggerSync().subscribe(progress -> {}, Throwable::printStackTrace);
                    }
                }, Throwable::printStackTrace));

        disposables.add(serverConnectivityMonitor.stateStream()
                .subscribe(state -> {
                    Log.i("SyncManager", "stateStream emitted: " + state + ", isLoggedIn=" + tokenManager.isLoggedIn());
                    if (state == ServerConnectivity.CONNECTED && tokenManager.isLoggedIn()) {
                        networkStateMonitor.onServerConnectivityChanged(state);
                        if (retryCount.get() < MAX_RETRY_COUNT) {
                            triggerSync().subscribe(progress -> {}, Throwable::printStackTrace);
                        } else {
                            Log.w("SyncManager", "retryCount=" + MAX_RETRY_COUNT + ", skip auto sync");
                        }
                    } else if (state == ServerConnectivity.DISCONNECTED) {
                        networkStateMonitor.onServerConnectivityChanged(state);
                        retryCount.set(0);
                        Log.i("SyncManager", "connection lost, retryCount reset");
                    }
                }, Throwable::printStackTrace));
    }

    public Observable<SyncProgress> triggerSync() {
        if (!initialized) {
            Log.w("SyncManager", "triggerSync() not initialized");
            return Observable.empty();
        }
        if (!syncing.compareAndSet(false, true)) {
            Log.w("SyncManager", "triggerSync() already syncing, skipping");
            return Observable.empty();
        }

        if (!tokenManager.isLoggedIn()) {
            Log.w("SyncManager", "triggerSync() not logged in");
            syncing.set(false);
            return Observable.empty();
        }

        Log.i("SyncManager", "triggerSync() starting sync");
        statusSubject.onNext(SyncState.SYNCING);

        return syncService.syncAll()
                .doOnNext(progress -> {
                    Log.i("SyncManager", "sync progress: total=" + progress.getTotal() + " success=" + progress.getSuccessCount() + " failed=" + progress.getFailedCount() + " completed=" + progress.isCompleted());
                    progressSubject.onNext(progress);
                    if (progress.isCompleted()) {
                        if (progress.getFailedCount() > 0) {
                            statusSubject.onNext(SyncState.PARTIAL_FAILED);
                            retryCount.incrementAndGet();
                            Log.i("SyncManager", "sync had failures, retryCount=" + retryCount.get());
                        } else if (progress.getTotal() == 0) {
                            statusSubject.onNext(SyncState.SYNCED);
                        } else {
                            statusSubject.onNext(SyncState.SYNCED);
                        }
                    }
                })
                .doOnError(error -> {
                    Log.e("SyncManager", "sync error", error);
                    if (isTokenExpired(error)) {
                        statusSubject.onNext(SyncState.TOKEN_EXPIRED);
                    }
                })
                .doFinally(() -> {
                    Log.i("SyncManager", "sync finished, resetting syncing flag");
                    syncing.set(false);
                })
                .subscribeOn(Schedulers.io());
    }

    public Observable<SyncProgress> manualResync() {
        if (!initialized) return Observable.empty();
        return syncRepository.resetFailedToPending()
                .toObservable()
                .flatMap(ignored -> triggerSync())
                .subscribeOn(Schedulers.io());
    }

    public Observable<SyncProgress> progressStream() {
        return progressSubject.hide();
    }

    public Observable<SyncState> statusStream() {
        return statusSubject.hide();
    }

    public boolean isSyncing() {
        return syncing.get();
    }

    public SyncState getCurrentStatus() {
        return statusSubject.getValue();
    }

    public SyncProgress getCurrentProgress() {
        return progressSubject.getValue();
    }

    public void destroy() {
        disposables.clear();
        initialized = false;
    }

    private boolean isTokenExpired(Throwable error) {
        if (error instanceof retrofit2.HttpException) {
            return ((retrofit2.HttpException) error).code() == 401;
        }
        String msg = error.getMessage();
        return msg != null && (msg.contains("过期") || msg.contains("expired") || msg.contains("401"));
    }
}
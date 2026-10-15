package com.example.project_pandora.core.sync;

import android.content.Context;

import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.project_pandora.core.network.ServerConnectivityMonitor;
import com.example.project_pandora.core.network.TokenManager;
import com.example.project_pandora.repository.SyncRepository;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class SyncWorker extends Worker {

    private static final String UNIQUE_WORK_NAME = "sync_one_time";

    public SyncWorker(Context context, WorkerParameters params) {
        super(context, params);
    }

    @Override
    public Result doWork() {
        try {
            TokenManager tokenManager = TokenManager.getInstance(getApplicationContext());
            if (!tokenManager.isLoggedIn()) {
                return Result.success();
            }

            SyncRepository syncRepository = new SyncRepository(getApplicationContext());
            AtomicReference<Boolean> hasUnsynced = new AtomicReference<>(false);
            CountDownLatch checkLatch = new CountDownLatch(1);
            syncRepository.hasUnsyncedData()
                    .subscribe(
                            has -> {
                                hasUnsynced.set(has);
                                checkLatch.countDown();
                            },
                            error -> checkLatch.countDown()
                    );
            checkLatch.await(5, TimeUnit.SECONDS);

            if (!hasUnsynced.get()) {
                return Result.success();
            }

            ServerConnectivity connectivity = ServerConnectivityMonitor.getInstance().getCurrentState();
            if (connectivity != ServerConnectivity.CONNECTED) {
                return Result.success();
            }

            SyncManager syncManager = SyncManager.getInstance();
            CountDownLatch syncLatch = new CountDownLatch(1);
            AtomicReference<Result> resultRef = new AtomicReference<>(Result.success());

            syncManager.triggerSync()
                    .subscribe(
                            progress -> {
                                if (progress.isCompleted()) {
                                    syncLatch.countDown();
                                }
                            },
                            error -> {
                                resultRef.set(Result.success());
                                syncLatch.countDown();
                            },
                            syncLatch::countDown
                    );

            syncLatch.await(60, TimeUnit.SECONDS);
            return resultRef.get();
        } catch (Exception e) {
            return Result.success();
        }
    }

    public static void enqueueOneTime(Context context) {
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(SyncWorker.class).build();
        WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                workRequest
        );
    }
}
package com.example.project_pandora.repository;

import android.content.Context;

import com.example.project_pandora.core.network.TokenManager;
import com.example.project_pandora.core.sync.SyncStatus;
import com.example.project_pandora.data.local.AppDatabase;
import com.example.project_pandora.data.local.PendingData;
import com.example.project_pandora.data.local.PendingDataDao;

import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class SyncRepository {

    private final PendingDataDao dao;
    private final TokenManager tokenManager;

    public SyncRepository(Context context) {
        dao = AppDatabase.getInstance(context).pendingDataDao();
        tokenManager = TokenManager.getInstance(context);
    }

    public Single<Long> savePending(String dataType, String content) {
        PendingData data = new PendingData();
        data.userId = tokenManager.getUserId();
        data.dataType = dataType;
        data.content = content;
        data.syncStatus = SyncStatus.PENDING.name();
        data.createdAt = System.currentTimeMillis();
        return dao.insert(data).subscribeOn(Schedulers.io());
    }

    public Observable<List<PendingData>> queryPendingByOrder() {
        return dao.queryPendingByOrder(tokenManager.getUserId())
                .subscribeOn(Schedulers.io());
    }

    public Single<Integer> countByStatus(SyncStatus status) {
        return dao.countByStatus(tokenManager.getUserId(), status.name())
                .subscribeOn(Schedulers.io());
    }

    public Single<Integer> updateSynced(long dataId) {
        return dao.updateSynced(dataId, System.currentTimeMillis())
                .subscribeOn(Schedulers.io());
    }

    public Single<Integer> updateFailed(long dataId, String failReason) {
        return dao.updateFailed(dataId, failReason, System.currentTimeMillis())
                .subscribeOn(Schedulers.io());
    }

    public Single<Integer> resetFailedToPending() {
        return dao.resetFailedToPending(tokenManager.getUserId())
                .subscribeOn(Schedulers.io());
    }

    public Single<Boolean> hasUnsyncedData() {
        return dao.hasUnsyncedData(tokenManager.getUserId())
                .subscribeOn(Schedulers.io());
    }
}
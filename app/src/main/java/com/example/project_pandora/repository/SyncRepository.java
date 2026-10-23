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

    /** R44 REQ-D: 直发非权威端成功后生成中继记录（初始即 RELAY_PENDING+源端业务 id） */
    public Single<Long> saveRelayPending(String dataType, String content, long sourceServerId) {
        PendingData data = new PendingData();
        data.userId = tokenManager.getUserId();
        data.dataType = dataType;
        data.content = content;
        data.syncStatus = SyncStatus.RELAY_PENDING.name();
        data.createdAt = System.currentTimeMillis();
        data.source_server_id = sourceServerId;
        return dao.insert(data).subscribeOn(Schedulers.io());
    }

    /** R44: 云中继全量面（PENDING+RELAY_PENDING+SYNC_FAILED） */
    public Observable<java.util.List<PendingData>> queryPendingAndRelayPending() {
        return dao.queryPendingAndRelayPending(tokenManager.getUserId())
                .subscribeOn(Schedulers.io());
    }

    /** R44: 本地重放面（PENDING+SYNC_FAILED，RELAY_PENDING 不重推） */
    public Observable<java.util.List<PendingData>> queryPendingForLocal() {
        return dao.queryPendingForLocal(tokenManager.getUserId())
                .subscribeOn(Schedulers.io());
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

    /** R44: 双参重载（中继批量回调带时间戳） */
    public Single<Integer> updateSynced(long dataId, long syncTime) {
        return dao.updateSynced(dataId, syncTime)
                .subscribeOn(Schedulers.io());
    }

    /** R44: 推本地成功 → RELAY_PENDING+源端 id（中继态标记） */
    public Single<Integer> markRelayed(long dataId, long sourceServerId, long syncTime) {
        return dao.markRelayed(dataId, sourceServerId, syncTime)
                .subscribeOn(Schedulers.io());
    }

    /** R44: 双参重载（NO_MAPPING 失败带时间戳） */
    public Single<Integer> updateFailed(long dataId, String failReason, long syncTime) {
        return dao.updateFailed(dataId, failReason, syncTime)
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
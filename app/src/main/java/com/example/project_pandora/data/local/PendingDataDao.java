package com.example.project_pandora.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface PendingDataDao {

    @Insert
    Single<Long> insert(PendingData pendingData);

    @Update
    Single<Integer> update(PendingData pendingData);

    @Query("UPDATE pending_data SET syncStatus = 'SYNCED', lastSyncTime = :syncTime WHERE dataId = :dataId")
    Single<Integer> updateSynced(long dataId, long syncTime);

    @Query("UPDATE pending_data SET syncStatus = 'SYNC_FAILED', failReason = :failReason, failCount = failCount + 1, lastSyncTime = :syncTime WHERE dataId = :dataId")
    Single<Integer> updateFailed(long dataId, String failReason, long syncTime);

    @Query("UPDATE pending_data SET syncStatus = 'PENDING' WHERE userId = :userId AND syncStatus = 'SYNC_FAILED'")
    Single<Integer> resetFailedToPending(long userId);

    @Query("SELECT * FROM pending_data WHERE userId = :userId AND syncStatus = 'PENDING' ORDER BY createdAt ASC")
    Observable<List<PendingData>> queryPendingByOrder(long userId);

    @Query("SELECT COUNT(*) FROM pending_data WHERE userId = :userId AND syncStatus = :status")
    Single<Integer> countByStatus(long userId, String status);

    @Query("SELECT EXISTS(SELECT 1 FROM pending_data WHERE userId = :userId AND syncStatus IN ('PENDING', 'SYNC_FAILED'))")
    Single<Boolean> hasUnsyncedData(long userId);

    @Query("SELECT * FROM pending_data WHERE userId = :userId AND syncStatus = 'PENDING' ORDER BY createdAt ASC LIMIT 1")
    Single<PendingData> queryFirstPending(long userId);
}
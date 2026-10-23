package com.example.project_pandora.data.local;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.example.project_pandora.core.sync.SyncStatus;

@Entity(tableName = "pending_data",
        indices = {
            @Index(value = {"userId", "syncStatus"}),
            @Index(value = {"userId", "createdAt"})
        })
public class PendingData {

    @PrimaryKey(autoGenerate = true)
    public long dataId;

    public long userId;

    public String dataType;

    public String content;

    public String syncStatus;

    public long createdAt;

    public Long lastSyncTime;

    public String failReason;

    public int failCount;

    /** R44 REQ-D: 源服务端业务 id（直发本地成功后回填；云中继时用于 UPDATE 定位映射） */
    public Long source_server_id;

    public PendingData() {
        this.syncStatus = SyncStatus.PENDING.name();
        this.failCount = 0;
    }
}
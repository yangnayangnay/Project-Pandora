package com.example.project_pandora.core.sync;

public enum SyncStatus {
    PENDING,
    /** R44: 已落非权威服务端（本地），待活跃端=云时中继推送 */
    RELAY_PENDING,
    SYNCED,
    SYNC_FAILED
}
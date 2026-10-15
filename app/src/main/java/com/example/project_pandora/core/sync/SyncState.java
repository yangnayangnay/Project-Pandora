package com.example.project_pandora.core.sync;

public enum SyncState {
    IDLE,
    SYNCING,
    SYNCED,
    PARTIAL_FAILED,
    TOKEN_EXPIRED,
    OFFLINE
}
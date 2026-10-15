package com.example.project_pandora.core.sync;

public class UploadResult {

    public enum Status {
        SUCCESS,
        FAILED,
        TOKEN_EXPIRED
    }

    private final Status status;
    private final String failReason;
    private final long dataId;

    private UploadResult(Status status, String failReason, long dataId) {
        this.status = status;
        this.failReason = failReason;
        this.dataId = dataId;
    }

    public static UploadResult success(long dataId) {
        return new UploadResult(Status.SUCCESS, null, dataId);
    }

    public static UploadResult failed(long dataId, String reason) {
        return new UploadResult(Status.FAILED, reason, dataId);
    }

    public static UploadResult tokenExpired(long dataId) {
        return new UploadResult(Status.TOKEN_EXPIRED, "登录已过期", dataId);
    }

    public Status getStatus() { return status; }
    public String getFailReason() { return failReason; }
    public long getDataId() { return dataId; }

    public boolean isSuccess() { return status == Status.SUCCESS; }
    public boolean isTokenExpired() { return status == Status.TOKEN_EXPIRED; }
    public boolean isFailed() { return status == Status.FAILED; }
}
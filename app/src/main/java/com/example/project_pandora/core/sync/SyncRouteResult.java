package com.example.project_pandora.core.sync;

public class SyncRouteResult {
    private final RoutePath routePath;
    private final Long dataId;
    private final String message;

    public SyncRouteResult(RoutePath routePath, Long dataId, String message) {
        this.routePath = routePath;
        this.dataId = dataId;
        this.message = message;
    }

    public RoutePath getRoutePath() { return routePath; }
    public Long getDataId() { return dataId; }
    public String getMessage() { return message; }
}
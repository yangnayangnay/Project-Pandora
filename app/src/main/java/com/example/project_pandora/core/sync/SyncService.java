package com.example.project_pandora.core.sync;

import android.content.Context;

import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.core.network.TokenManager;
import com.example.project_pandora.data.local.PendingData;
import com.example.project_pandora.data.remote.LogApi;
import com.example.project_pandora.data.remote.TaskApi;
import com.example.project_pandora.repository.SyncRepository;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class SyncService {

    private final SyncRepository syncRepository;
    private final LogApi logApi;
    private final TaskApi taskApi;
    private final TokenManager tokenManager;
    private final Gson gson;

    public SyncService(Context context) {
        this.syncRepository = new SyncRepository(context);
        this.logApi = NetworkClient.getInstance(context).createService(LogApi.class);
        this.taskApi = NetworkClient.getInstance(context).createService(TaskApi.class);
        this.tokenManager = TokenManager.getInstance(context);
        this.gson = new Gson();
    }

    public Observable<SyncProgress> syncAll() {
        return syncRepository.queryPendingByOrder()
                .firstOrError()
                .toObservable()
                .flatMap(pendingList -> {
                    if (pendingList == null || pendingList.isEmpty()) {
                        return Observable.just(SyncProgress.empty());
                    }

                    int total = pendingList.size();
                    int[] successCount = {0};
                    int[] failedCount = {0};

                    Observable<UploadResult> chain = Observable.empty();
                    for (PendingData data : pendingList) {
                        final PendingData currentData = data;
                        chain = chain.concatWith(uploadSingle(currentData)
                                .doOnNext(result -> {
                                    if (result.isSuccess()) {
                                        successCount[0]++;
                                    } else {
                                        failedCount[0]++;
                                    }
                                }));
                    }

                    return chain.map(result -> {
                        int processed = successCount[0] + failedCount[0];
                        boolean done = processed >= total;
                        return new SyncProgress(total, successCount[0], failedCount[0], done);
                    })
                    .concatWith(Observable.defer(() -> Observable.just(
                            new SyncProgress(total, successCount[0], failedCount[0], true))));
                })
                .subscribeOn(Schedulers.io());
    }

    @SuppressWarnings("unchecked")
    public Observable<UploadResult> uploadSingle(PendingData data) {
        if (data.dataType == null) {
            return Observable.defer(() -> syncRepository.updateFailed(data.dataId, "未知数据类型")
                    .toObservable().map(r -> UploadResult.failed(data.dataId, "未知数据类型")));
        }

        switch (data.dataType) {
            case "LOG":
                return uploadLog(data);
            case "TASK_STATUS":
                return uploadTaskStatus(data);
            case "TASK_PROGRESS":
                return uploadTaskProgress(data);
            default:
                return Observable.defer(() -> syncRepository.updateFailed(data.dataId, "不支持的数据类型: " + data.dataType)
                        .toObservable().map(r -> UploadResult.failed(data.dataId, "不支持的数据类型: " + data.dataType)));
        }
    }

    @SuppressWarnings("unchecked")
    private Observable<UploadResult> uploadLog(PendingData data) {
        Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
        Map<String, Object> content;
        try {
            content = gson.fromJson(data.content, mapType);
        } catch (Exception e) {
            return Observable.defer(() -> syncRepository.updateFailed(data.dataId, "内容解析失败")
                    .toObservable().map(r -> UploadResult.failed(data.dataId, "内容解析失败")));
        }
        if (content == null) content = new HashMap<>();

        String operation = content.get("operation") != null ? content.get("operation").toString() : "CREATE";
        String workItem = content.get("workItem") != null ? content.get("workItem").toString() : "";
        String completionStatus = content.get("completionStatus") != null ? content.get("completionStatus").toString() : "PENDING";
        Double timeCost = null;
        Object timeCostObj = content.get("timeCost");
        if (timeCostObj != null) {
            try {
                timeCost = Double.parseDouble(timeCostObj.toString());
            } catch (NumberFormatException ignored) {}
        }
        String logDate = content.get("logDate") != null ? content.get("logDate").toString() : null;

        Map<String, Object> request = new HashMap<>();
        request.put("workItem", workItem);
        request.put("completionStatus", completionStatus);
        request.put("timeCost", timeCost);
        request.put("logDate", logDate);

        Observable<Map<String, Object>> apiCall;
        if ("EDIT".equals(operation)) {
            Long logId = null;
            Object logIdObj = content.get("logId");
            if (logIdObj != null) {
                try { logId = Long.parseLong(logIdObj.toString()); } catch (NumberFormatException ignored) {}
            }
            if (logId == null) {
                return Observable.defer(() -> syncRepository.updateFailed(data.dataId, "编辑日志缺少ID")
                        .toObservable().map(r -> UploadResult.failed(data.dataId, "编辑日志缺少ID")));
            }
            apiCall = logApi.editLog(logId, request)
                    .map(response -> {
                        if (response.isSuccess() && response.getData() != null) {
                            return response.getData();
                        }
                        throw new RuntimeException(response.getMessage());
                    });
        } else {
            apiCall = logApi.createLog(request)
                    .map(response -> {
                        if (response.isSuccess() && response.getData() != null) {
                            return response.getData();
                        }
                        throw new RuntimeException(response.getMessage());
                    });
        }

        return apiCall
                .flatMap(result -> syncRepository.updateSynced(data.dataId)
                        .toObservable().map(r -> UploadResult.success(data.dataId)))
                .onErrorResumeNext(error -> {
                    if (isTokenExpired(error)) {
                        return Observable.just(UploadResult.tokenExpired(data.dataId));
                    }
                    String reason = error.getMessage() != null ? error.getMessage() : "上传失败";
                    if (isNetworkError(error)) {
                        reason = "网络错误";
                    }
                    final String failReason = reason;
                    return syncRepository.updateFailed(data.dataId, failReason)
                            .toObservable().map(r -> UploadResult.failed(data.dataId, failReason));
                })
                .delay(100, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io());
    }

    private boolean isTokenExpired(Throwable error) {
        if (error instanceof retrofit2.HttpException) {
            return ((retrofit2.HttpException) error).code() == 401;
        }
        String msg = error.getMessage();
        return msg != null && (msg.contains("过期") || msg.contains("expired") || msg.contains("401"));
    }

    @SuppressWarnings("unchecked")
    private Observable<UploadResult> uploadTaskStatus(PendingData data) {
        Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
        Map<String, Object> content;
        try {
            content = gson.fromJson(data.content, mapType);
        } catch (Exception e) {
            return Observable.defer(() -> syncRepository.updateFailed(data.dataId, "内容解析失败")
                    .toObservable().map(r -> UploadResult.failed(data.dataId, "内容解析失败")));
        }
        if (content == null) content = new HashMap<>();

        Long taskId = extractLong(content.get("taskId"));
        String targetStatus = content.get("targetStatus") != null ? content.get("targetStatus").toString() : null;

        if (taskId == null || targetStatus == null) {
            return Observable.defer(() -> syncRepository.updateFailed(data.dataId, "缺少任务ID或状态")
                    .toObservable().map(r -> UploadResult.failed(data.dataId, "缺少任务ID或状态")));
        }

        Map<String, String> request = new HashMap<>();
        request.put("status", targetStatus);

        return taskApi.transitionStatus(taskId, request)
                .flatMap(response -> {
                    if (response.isSuccess()) {
                        return syncRepository.updateSynced(data.dataId)
                                .toObservable().map(r -> UploadResult.success(data.dataId));
                    }
                    throw new RuntimeException(response.getMessage());
                })
                .onErrorResumeNext(error -> handleUploadError(data, error))
                .delay(100, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io());
    }

    @SuppressWarnings("unchecked")
    private Observable<UploadResult> uploadTaskProgress(PendingData data) {
        Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
        Map<String, Object> content;
        try {
            content = gson.fromJson(data.content, mapType);
        } catch (Exception e) {
            return Observable.defer(() -> syncRepository.updateFailed(data.dataId, "内容解析失败")
                    .toObservable().map(r -> UploadResult.failed(data.dataId, "内容解析失败")));
        }
        if (content == null) content = new HashMap<>();

        Long taskId = extractLong(content.get("taskId"));
        String progressNote = content.get("progressNote") != null ? content.get("progressNote").toString() : null;
        String status = content.get("status") != null ? content.get("status").toString() : null;

        if (taskId == null) {
            return Observable.defer(() -> syncRepository.updateFailed(data.dataId, "缺少任务ID")
                    .toObservable().map(r -> UploadResult.failed(data.dataId, "缺少任务ID")));
        }

        Map<String, Object> request = new HashMap<>();
        if (progressNote != null) request.put("progressNote", progressNote);
        if (status != null) request.put("status", status);

        return taskApi.updateProgress(taskId, request)
                .flatMap(response -> {
                    if (response.isSuccess()) {
                        return syncRepository.updateSynced(data.dataId)
                                .toObservable().map(r -> UploadResult.success(data.dataId));
                    }
                    throw new RuntimeException(response.getMessage());
                })
                .onErrorResumeNext(error -> handleUploadError(data, error))
                .delay(100, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io());
    }

    private Observable<UploadResult> handleUploadError(PendingData data, Throwable error) {
        if (isTokenExpired(error)) {
            return Observable.just(UploadResult.tokenExpired(data.dataId));
        }
        String reason = error.getMessage() != null ? error.getMessage() : "上传失败";
        if (isNetworkError(error)) {
            reason = "网络错误";
        }
        final String failReason = reason;
        return syncRepository.updateFailed(data.dataId, failReason)
                .toObservable().map(r -> UploadResult.failed(data.dataId, failReason));
    }

    private Long extractLong(Object obj) {
        if (obj == null) return null;
        try { return Long.parseLong(obj.toString()); } catch (NumberFormatException e) { return null; }
    }

    private boolean isNetworkError(Throwable error) {
        if (error instanceof java.io.IOException) return true;
        if (error instanceof retrofit2.HttpException) {
            return ((retrofit2.HttpException) error).code() >= 500;
        }
        return false;
    }
}
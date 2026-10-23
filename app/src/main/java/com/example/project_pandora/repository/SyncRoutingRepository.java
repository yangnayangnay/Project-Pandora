package com.example.project_pandora.repository;

import android.content.Context;

import com.example.project_pandora.core.network.NetworkStateMonitor;
import com.example.project_pandora.core.sync.NetworkMode;
import com.example.project_pandora.core.sync.RoutePath;
import com.example.project_pandora.core.sync.SyncRouteResult;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class SyncRoutingRepository {

    private final LogRepository logRepository;
    private final TaskRepository taskRepository;
    private final SyncRepository syncRepository;
    private final NetworkStateMonitor networkStateMonitor;
    private final Gson gson;

    public SyncRoutingRepository(Context context) {
        this.logRepository = new LogRepository(context);
        this.taskRepository = new TaskRepository(context);
        this.syncRepository = new SyncRepository(context);
        this.networkStateMonitor = NetworkStateMonitor.getInstance();
        this.gson = new Gson();
    }

    public Observable<SyncRouteResult> createLog(String workItem, String workContent, String completionStatus,
                                                    Double timeCost, String logDate) {
        Map<String, Object> content = new HashMap<>();
        content.put("operation", "CREATE");
        content.put("workItem", workItem);
        content.put("workContent", workContent);
        content.put("completionStatus", completionStatus);
        content.put("timeCost", timeCost);
        content.put("logDate", logDate);
        String jsonContent = gson.toJson(content);

        return routeLog(jsonContent, () -> logRepository.createLog(workItem, workContent, completionStatus, timeCost, logDate));
    }

    public Observable<SyncRouteResult> editLog(Long logId, String workItem, String workContent, String completionStatus,
                                                 Double timeCost, String logDate) {
        Map<String, Object> content = new HashMap<>();
        content.put("operation", "EDIT");
        content.put("logId", logId);
        content.put("workItem", workItem);
        content.put("workContent", workContent);
        content.put("completionStatus", completionStatus);
        content.put("timeCost", timeCost);
        content.put("logDate", logDate);
        String jsonContent = gson.toJson(content);

        return routeLog(jsonContent, () -> logRepository.editLog(logId, workItem, workContent, completionStatus, timeCost, logDate));
    }

    public Observable<SyncRouteResult> transitionTaskStatus(Long taskId, String targetStatus) {
        Map<String, Object> content = new HashMap<>();
        content.put("taskId", taskId);
        content.put("targetStatus", targetStatus);
        String jsonContent = gson.toJson(content);

        return routeTask("TASK_STATUS", jsonContent, "操作成功",
                () -> taskRepository.transitionStatus(taskId, targetStatus).map(v -> new HashMap<>()));
    }

    public Observable<SyncRouteResult> updateTaskProgress(Long taskId, String progressNote, String status) {
        Map<String, Object> content = new HashMap<>();
        content.put("taskId", taskId);
        content.put("progressNote", progressNote);
        content.put("status", status);
        String jsonContent = gson.toJson(content);

        return routeTask("TASK_PROGRESS", jsonContent, "操作成功",
                () -> taskRepository.updateProgress(taskId, progressNote, status).map(v -> new HashMap<>()));
    }

    private interface LogOperation {
        Observable<Map<String, Object>> execute();
    }

    private Observable<SyncRouteResult> routeLog(String jsonContent, LogOperation operation) {
        return route("LOG", jsonContent, "日志保存成功", operation);
    }

    private Observable<SyncRouteResult> routeTask(String dataType, String jsonContent, String successMsg,
                                                   LogOperation operation) {
        return route(dataType, jsonContent, successMsg, operation);
    }

    private Observable<SyncRouteResult> route(String dataType, String jsonContent, String successMsg,
                                               LogOperation operation) {
        NetworkMode mode = networkStateMonitor.getCurrentMode();

        if (mode == NetworkMode.OFFLINE) {
            return syncRepository.savePending(dataType, jsonContent)
                    .toObservable()
                    .map(id -> new SyncRouteResult(RoutePath.OFFLINE_PENDING, id, "已保存至本地，待同步"))
                    .subscribeOn(Schedulers.io());
        }

        return operation.execute()
                .map(result -> new SyncRouteResult(RoutePath.ONLINE_DIRECT, null, successMsg))
                .onErrorResumeNext(error -> {
                    if (isBusinessError(error)) {
                        // DEF-31-01: HttpException 统一解析为 BusinessException（携带后端 code/message）
                        return Observable.error(com.example.project_pandora.core.network.HttpErrorParser.parse(error));
                    }
                    return syncRepository.savePending(dataType, jsonContent)
                            .toObservable()
                            .map(id -> new SyncRouteResult(RoutePath.ONLINE_FAILED_FALLBACK, id, "已保存至本地，待同步"));
                })
                .subscribeOn(Schedulers.io());
    }

    private boolean isBusinessError(Throwable error) {
        if (error instanceof java.io.IOException) return false;
        if (error instanceof retrofit2.HttpException) {
            retrofit2.HttpException httpEx = (retrofit2.HttpException) error;
            int code = httpEx.code();
            if (code == 401) return true;
            if (code >= 500) return false;
            return true;
        }
        if (error instanceof RuntimeException) {
            String msg = error.getMessage();
            if (msg == null) return true;
            if (msg.contains("网络") || msg.contains("timeout") || msg.contains("连接")) return false;
            return true;
        }
        return false;
    }
}
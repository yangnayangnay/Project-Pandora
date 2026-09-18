package com.example.project_pandora.repository;

import android.content.Context;

import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.data.remote.LogApi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class LogRepository {

    private final LogApi logApi;

    public LogRepository(Context context) {
        logApi = NetworkClient.getInstance(context).createService(LogApi.class);
    }

    public Observable<Map<String, Object>> createLog(String workItem, String completionStatus,
                                                      Double timeCost, String logDate) {
        Map<String, Object> request = new HashMap<>();
        request.put("workItem", workItem);
        request.put("completionStatus", completionStatus);
        request.put("timeCost", timeCost);
        request.put("logDate", logDate);

        return logApi.createLog(request)
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        return response.getData();
                    }
                    throw new RuntimeException(response.getMessage());
                });
    }

    public Observable<List<Map<String, Object>>> listLogs() {
        return logApi.listLogs()
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        return response.getData();
                    }
                    throw new RuntimeException(response.getMessage());
                });
    }

    public Observable<Map<String, Object>> getLogStats() {
        return logApi.getLogStats()
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        return response.getData();
                    }
                    throw new RuntimeException(response.getMessage());
                });
    }

    public Observable<Void> updateTop10(List<Map<String, Object>> top10List) {
        return logApi.updateTop10(top10List)
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess()) return null;
                    throw new RuntimeException(response.getMessage());
                });
    }

    public Observable<List<Map<String, Object>>> getTop10(String workType) {
        return logApi.getTop10(workType)
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        return response.getData();
                    }
                    throw new RuntimeException(response.getMessage());
                });
    }

    public Observable<Map<String, Object>> editLog(Long logId, String workItem, String completionStatus,
                                                     Double timeCost, String logDate) {
        Map<String, Object> request = new HashMap<>();
        if (workItem != null) request.put("workItem", workItem);
        if (completionStatus != null) request.put("completionStatus", completionStatus);
        if (timeCost != null) request.put("timeCost", timeCost);
        if (logDate != null) request.put("logDate", logDate);

        return logApi.editLog(logId, request)
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        return response.getData();
                    }
                    throw new RuntimeException(response.getMessage());
                });
    }

    public Observable<Void> deleteLog(Long logId) {
        return logApi.deleteLog(logId)
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess()) return null;
                    throw new RuntimeException(response.getMessage());
                });
    }
}
package com.example.project_pandora.repository;

import android.content.Context;

import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.data.remote.TaskApi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class TaskRepository {

    private final TaskApi taskApi;

    public TaskRepository(Context context) {
        taskApi = NetworkClient.getInstance(context).createService(TaskApi.class);
    }

    public Observable<Map<String, Object>> createTask(String name, String priority, Long assigneeId,
                                                       Boolean isImportant, Boolean isUrgent,
                                                       String startTime, String endTime) {
        Map<String, Object> request = new HashMap<>();
        request.put("name", name);
        request.put("priority", priority);
        request.put("assigneeId", assigneeId);
        request.put("isImportant", isImportant);
        request.put("isUrgent", isUrgent);
        if (startTime != null) request.put("startTime", startTime);
        if (endTime != null) request.put("endTime", endTime);

        return taskApi.createTask(request)
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        return response.getData();
                    }
                    throw new RuntimeException(response.getMessage());
                });
    }

    public Observable<List<Map<String, Object>>> listTasks(String status, Long assigneeId) {
        return taskApi.listTasks(status, assigneeId)
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        return response.getData();
                    }
                    throw new RuntimeException(response.getMessage());
                });
    }

    public Observable<Map<String, Object>> getTask(Long taskId) {
        return taskApi.getTask(taskId)
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        return response.getData();
                    }
                    throw new RuntimeException(response.getMessage());
                });
    }

    public Observable<Void> updateProgress(Long taskId, String progressNote, String status) {
        Map<String, Object> request = new HashMap<>();
        if (progressNote != null) request.put("progressNote", progressNote);
        if (status != null) request.put("status", status);

        return taskApi.updateProgress(taskId, request)
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess()) return null;
                    throw new RuntimeException(response.getMessage());
                });
    }

    public Observable<Map<String, Object>> getQuadrantTasks() {
        return taskApi.getQuadrantTasks()
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        return response.getData();
                    }
                    throw new RuntimeException(response.getMessage());
                });
    }

    public Observable<Void> transitionStatus(Long taskId, String targetStatus) {
        Map<String, String> request = new HashMap<>();
        request.put("status", targetStatus);

        return taskApi.transitionStatus(taskId, request)
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess()) return null;
                    throw new RuntimeException(response.getMessage());
                });
    }
}
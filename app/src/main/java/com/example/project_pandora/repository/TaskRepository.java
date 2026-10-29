package com.example.project_pandora.repository;

import android.content.Context;

import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.data.remote.TaskApi;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
                        List<Map<String, Object>> list = response.getData();
                        for (int i = 0; i < list.size(); i++) {
                            list.set(i, normalizeKeys(list.get(i)));
                        }
                        return list;
                    }
                    throw new RuntimeException(response.getMessage());
                });
    }

    public Observable<List<Map<String, Object>>> listTasksExtended(String status, Long assigneeId,
                                                                     Long dispatcherId, Boolean showCompleted) {
        return taskApi.listTasksExtended(status, assigneeId, dispatcherId, showCompleted)
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        List<Map<String, Object>> list = response.getData();
                        for (int i = 0; i < list.size(); i++) {
                            list.set(i, normalizeKeys(list.get(i)));
                        }
                        return list;
                    }
                    throw new RuntimeException(response.getMessage());
                });
    }

    public Observable<Map<String, Object>> updateTask(Long taskId, Map<String, Object> request) {
        return taskApi.updateTask(taskId, request)
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
                        return normalizeKeys(response.getData());
                    }
                    throw new RuntimeException(response.getMessage());
                });
    }

    public Observable<Map<String, Object>> updateProgress(Long taskId, String progressNote, String status) {
        Map<String, Object> request = new HashMap<>();
        if (progressNote != null) request.put("progressNote", progressNote);
        if (status != null) request.put("status", status);

        return taskApi.updateProgress(taskId, request)
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess()) return Collections.emptyMap();
                    throw new RuntimeException(response.getMessage());
                });
    }

    @SuppressWarnings("unchecked")
    public Observable<Map<String, Object>> getQuadrantTasks() {
        return taskApi.getQuadrantTasks()
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        Map<String, Object> data = response.getData();
                        Map<String, Object> result = new LinkedHashMap<>();
                        for (Map.Entry<String, Object> entry : data.entrySet()) {
                            Object value = entry.getValue();
                            if (value instanceof List) {
                                List<Object> list = (List<Object>) value;
                                for (int i = 0; i < list.size(); i++) {
                                    if (list.get(i) instanceof Map) {
                                        list.set(i, normalizeKeys((Map<String, Object>) list.get(i)));
                                    }
                                }
                                result.put(entry.getKey(), list);
                            } else {
                                result.put(entry.getKey(), value);
                            }
                        }
                        return result;
                    }
                    throw new RuntimeException(response.getMessage());
                });
    }

    public Observable<Map<String, Object>> transitionStatus(Long taskId, String targetStatus) {
        Map<String, String> request = new HashMap<>();
        request.put("status", targetStatus);

        return taskApi.transitionStatus(taskId, request)
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess()) return Collections.emptyMap();
                    throw new RuntimeException(response.getMessage());
                });
    }

    public Observable<List<Map<String, Object>>> getSubordinateTasks() {
        return taskApi.getSubordinateTasks()
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        List<Map<String, Object>> list = response.getData();
                        for (int i = 0; i < list.size(); i++) {
                            list.set(i, normalizeKeys(list.get(i)));
                        }
                        return list;
                    }
                    throw new RuntimeException(response.getMessage());
                });
    }

    public Observable<List<Map<String, Object>>> getSubordinateUsers() {
        return taskApi.getSubordinateUsers()
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        List<Map<String, Object>> list = response.getData();
                        for (int i = 0; i < list.size(); i++) {
                            list.set(i, normalizeKeys(list.get(i)));
                        }
                        return list;
                    }
                    throw new RuntimeException(response.getMessage());
                });
    }

    public Observable<Map<String, Object>> deleteTask(Long taskId) {
        return taskApi.deleteTask(taskId)
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) return response.getData();
                    throw new RuntimeException(response.getMessage());
                });
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> normalizeKeys(Map<String, Object> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                value = normalizeKeys((Map<String, Object>) value);
            } else if (value instanceof List) {
                List<Object> list = (List<Object>) value;
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i) instanceof Map) {
                        list.set(i, normalizeKeys((Map<String, Object>) list.get(i)));
                    }
                }
            }
            result.put(entry.getKey().toLowerCase(), value);
        }
        return result;
    }
}
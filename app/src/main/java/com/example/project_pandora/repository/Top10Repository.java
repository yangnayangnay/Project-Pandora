package com.example.project_pandora.repository;

import android.content.Context;

import com.example.project_pandora.core.exception.BusinessException;
import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.data.remote.Top10Api;

import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class Top10Repository {

    private final Top10Api top10Api;

    public Top10Repository(Context context) {
        top10Api = NetworkClient.getInstance(context).createService(Top10Api.class);
    }

    public Observable<List<Map<String, Object>>> getCompanyImportant() {
        return top10Api.getCompanyImportant()
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        return response.getData();
                    }
                    throw new BusinessException(response.getCode(), response.getMessage());
                });
    }

    public Observable<Map<String, Object>> createCompanyImportant(String content, int rankOrder) {
        return top10Api.createCompanyImportant(Map.of("content", content, "rankOrder", rankOrder))
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        return response.getData();
                    }
                    throw new BusinessException(response.getCode(), response.getMessage());
                });
    }

    public Observable<Map<String, Object>> updateCompanyImportant(long id, String content, int rankOrder) {
        return top10Api.updateCompanyImportant(id, Map.of("content", content, "rankOrder", rankOrder))
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        return response.getData();
                    }
                    throw new BusinessException(response.getCode(), response.getMessage());
                });
    }

    public Observable<Void> deleteCompanyImportant(long id) {
        return top10Api.deleteCompanyImportant(id)
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess()) return null;
                    throw new BusinessException(response.getCode(), response.getMessage());
                });
    }

    public Observable<List<Map<String, Object>>> getCompanyDispatch() {
        return top10Api.getCompanyDispatch()
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        return response.getData();
                    }
                    throw new BusinessException(response.getCode(), response.getMessage());
                });
    }

    public Observable<Map<String, Object>> markCompanyDispatch(long taskId, int rankOrder) {
        return top10Api.markCompanyDispatch(Map.of("taskId", taskId, "rankOrder", rankOrder))
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        return response.getData();
                    }
                    throw new BusinessException(response.getCode(), response.getMessage());
                });
    }

    public Observable<Void> unmarkCompanyDispatch(long taskId) {
        return top10Api.unmarkCompanyDispatch(taskId)
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess()) return null;
                    throw new BusinessException(response.getCode(), response.getMessage());
                });
    }

    public Observable<Map<String, Object>> createAndMarkCompanyDispatch(String name, long assigneeId, String priority, int rankOrder, String endTime) {
        return top10Api.createAndMarkCompanyDispatch(Map.of(
                "name", name,
                "assigneeId", assigneeId,
                "priority", priority != null ? priority : "MEDIUM",
                "rankOrder", rankOrder,
                "endTime", endTime != null ? endTime : ""
        ))
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        return response.getData();
                    }
                    throw new BusinessException(response.getCode(), response.getMessage());
                });
    }
}
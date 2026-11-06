package com.example.project_pandora.repository;

import android.content.Context;
import com.example.project_pandora.core.exception.BusinessException;
import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.data.remote.DailyItemApi;
import java.util.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public final class DailyItemRepository {
    private final DailyItemApi api;
    public DailyItemRepository(Context context) { api = NetworkClient.getInstance(context).createService(DailyItemApi.class); }
    public Observable<List<Map<String, Object>>> list(String month) {
        return api.list(month).subscribeOn(Schedulers.io()).map(response -> {
            if (!response.isSuccess() || response.getData() == null) throw new BusinessException(response.getCode(), response.getMessage());
            return response.getData();
        });
    }
    public Observable<Map<String, Object>> save(Long id, String content, String date) {
        Map<String, Object> body = new HashMap<>(); body.put("content", content); body.put("itemDate", date);
        return (id == null ? api.create(body) : api.update(id, body)).subscribeOn(Schedulers.io()).map(response -> {
            if (!response.isSuccess() || response.getData() == null) throw new BusinessException(response.getCode(), response.getMessage());
            return response.getData();
        });
    }
    public Observable<Map<String, Object>> complete(long id, boolean completed) {
        Map<String, Object> body = new HashMap<>(); body.put("completed", completed);
        return api.update(id, body).subscribeOn(Schedulers.io()).map(response -> {
            if (!response.isSuccess() || response.getData() == null) throw new BusinessException(response.getCode(), response.getMessage());
            return response.getData();
        });
    }
    public Observable<Boolean> delete(long id) {
        return api.delete(id).subscribeOn(Schedulers.io()).map(response -> {
            if (!response.isSuccess()) throw new BusinessException(response.getCode(), response.getMessage()); return true;
        });
    }
}

package com.example.project_pandora.repository;

import android.content.Context;
import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.data.remote.TaskTagApi;
import java.util.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public final class TaskTagRepository {
    private final TaskTagApi api;
    public TaskTagRepository(Context context) { api = NetworkClient.getInstance(context).createService(TaskTagApi.class); }
    public Observable<List<Map<String, Object>>> list() {
        return api.list().subscribeOn(Schedulers.io()).map(response -> {
            if (!response.isSuccess() || response.getData() == null) throw new IllegalStateException(response.getMessage());
            return response.getData();
        });
    }
    public Observable<Map<String, Object>> save(Long id, String name, int color) {
        Map<String, Object> body = new HashMap<>(); body.put("name", name); body.put("colorIndex", color);
        return (id == null ? api.create(body) : api.update(id, body)).subscribeOn(Schedulers.io()).map(response -> {
            if (!response.isSuccess() || response.getData() == null) throw new IllegalStateException(response.getMessage());
            return response.getData();
        });
    }
    public Observable<Boolean> delete(long id) {
        return api.delete(id).subscribeOn(Schedulers.io()).map(response -> {
            if (!response.isSuccess()) throw new IllegalStateException(response.getMessage()); return true;
        });
    }
}

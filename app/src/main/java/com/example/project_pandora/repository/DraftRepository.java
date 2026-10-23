package com.example.project_pandora.repository;

import android.content.Context;

import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.data.remote.DraftApi;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class DraftRepository {

    private final DraftApi draftApi;

    public DraftRepository(Context context) {
        draftApi = NetworkClient.getInstance(context).createService(DraftApi.class);
    }

    public Observable<Map<String, Object>> saveDraft(String draftType, Map<String, Object> content) {
        Map<String, Object> request = new HashMap<>();
        request.put("draftType", draftType);
        request.put("content", content);
        return draftApi.saveDraft(request)
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        return response.getData();
                    }
                    throw new RuntimeException(response.getMessage());
                });
    }

    public Observable<Map<String, Object>> getDraft(String draftType) {
        return draftApi.getDraft(draftType)
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        return response.getData();
                    }
                    if (response.isSuccess()) {
                        return null;
                    }
                    throw new RuntimeException(response.getMessage());
                });
    }

    public Observable<Map<String, Object>> deleteDraft(String draftType) {
        return draftApi.deleteDraft(draftType)
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess()) return Collections.emptyMap();
                    throw new RuntimeException(response.getMessage());
                });
    }
}
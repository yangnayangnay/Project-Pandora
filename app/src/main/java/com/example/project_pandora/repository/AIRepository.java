package com.example.project_pandora.repository;

import android.content.Context;

import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.data.remote.AIApi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class AIRepository {

    private final AIApi aiApi;

    public AIRepository(Context context) {
        aiApi = NetworkClient.getInstance(context).createService(AIApi.class);
    }

    public Observable<Map<String, Object>> submitMbti(List<Integer> answers) {
        Map<String, Object> request = new HashMap<>();
        request.put("answers", answers);

        return aiApi.submitMbti(request)
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        return response.getData();
                    }
                    throw new RuntimeException(response.getMessage());
                });
    }

    public Observable<Map<String, Object>> getMbtiReport() {
        return aiApi.getMbtiReport()
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        return response.getData();
                    }
                    throw new RuntimeException(response.getMessage());
                });
    }

    public Observable<List<Map<String, Object>>> keywordAnalysis(String text) {
        Map<String, Object> request = new HashMap<>();
        request.put("text", text);

        return aiApi.keywordAnalysis(request)
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        return response.getData();
                    }
                    throw new RuntimeException(response.getMessage());
                });
    }

    public Observable<Map<String, Object>> getSuggestion() {
        return aiApi.getSuggestion()
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        return response.getData();
                    }
                    throw new RuntimeException(response.getMessage());
                });
    }

    public Observable<Map<String, Object>> getDivination() {
        return aiApi.getDivination()
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        return response.getData();
                    }
                    throw new RuntimeException(response.getMessage());
                });
    }
}
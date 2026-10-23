package com.example.project_pandora.repository;

import android.content.Context;

import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.data.remote.TemplateApi;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class TemplateRepository {

    private final TemplateApi templateApi;

    public TemplateRepository(Context context) {
        templateApi = NetworkClient.getInstance(context).createService(TemplateApi.class);
    }

    public Observable<Map<String, Object>> saveTemplate(String templateType, String name, Map<String, Object> content) {
        Map<String, Object> request = new HashMap<>();
        request.put("templateType", templateType);
        request.put("name", name);
        request.put("content", content);
        return templateApi.saveTemplate(request)
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        return response.getData();
                    }
                    throw new RuntimeException(response.getMessage());
                });
    }

    public Observable<List<Map<String, Object>>> listTemplates(String templateType) {
        return templateApi.listTemplates(templateType)
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        return response.getData();
                    }
                    throw new RuntimeException(response.getMessage());
                });
    }

    public Observable<Map<String, Object>> updateTemplate(Long templateId, String name, Map<String, Object> content) {
        Map<String, Object> request = new HashMap<>();
        request.put("name", name);
        request.put("content", content);
        return templateApi.updateTemplate(templateId, request)
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess()) return Collections.emptyMap();
                    throw new RuntimeException(response.getMessage());
                });
    }

    public Observable<Map<String, Object>> deleteTemplate(Long templateId) {
        return templateApi.deleteTemplate(templateId)
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess()) return Collections.emptyMap();
                    throw new RuntimeException(response.getMessage());
                });
    }

    public Observable<Map<String, Object>> migrateTemplates(String templateType, List<Map<String, Object>> templates) {
        Map<String, Object> request = new HashMap<>();
        request.put("templateType", templateType);
        request.put("templates", templates);
        return templateApi.migrateTemplates(request)
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        return response.getData();
                    }
                    throw new RuntimeException(response.getMessage());
                });
    }
}
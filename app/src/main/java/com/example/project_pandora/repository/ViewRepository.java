package com.example.project_pandora.repository;

import android.content.Context;

import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.data.remote.ViewApi;

import java.util.Map;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ViewRepository {

    private final ViewApi viewApi;

    public ViewRepository(Context context) {
        viewApi = NetworkClient.getInstance(context).createService(ViewApi.class);
    }

    public Observable<Map<String, Object>> getMonthView(String month) {
        return viewApi.getMonthView(month)
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        return response.getData();
                    }
                    throw new RuntimeException(response.getMessage());
                });
    }

    public Observable<Map<String, Object>> getWeekView() {
        return viewApi.getWeekView()
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        return response.getData();
                    }
                    throw new RuntimeException(response.getMessage());
                });
    }

    public Observable<Map<String, Object>> getDayView(String date) {
        return viewApi.getDayView(date)
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        return response.getData();
                    }
                    throw new RuntimeException(response.getMessage());
                });
    }

    public Observable<Map<String, Object>> getMindMap() {
        return viewApi.getMindMap()
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        return response.getData();
                    }
                    throw new RuntimeException(response.getMessage());
                });
    }

    public Observable<Map<String, Object>> getInfoMap() {
        return viewApi.getInfoMap()
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        return response.getData();
                    }
                    throw new RuntimeException(response.getMessage());
                });
    }
}
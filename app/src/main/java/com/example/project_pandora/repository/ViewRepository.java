package com.example.project_pandora.repository;

import android.content.Context;

import com.example.project_pandora.core.exception.BusinessException;
import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.data.remote.LogApi;
import com.example.project_pandora.data.remote.ViewApi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ViewRepository {

    private final ViewApi viewApi;
    private final LogRepository logRepository;

    public ViewRepository(Context context) {
        this(NetworkClient.getInstance(context).createService(ViewApi.class),
                NetworkClient.getInstance(context).createService(LogApi.class));
    }

    ViewRepository(ViewApi viewApi, LogApi logApi) {
        this.viewApi = viewApi;
        this.logRepository = new LogRepository(logApi);
    }

    public Observable<Map<String, Object>> getMonthView(String month, Boolean todayCoverage) {
        return viewApi.getMonthView(month, todayCoverage)
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        return response.getData();
                    }
                    throw new BusinessException(response.getCode(), response.getMessage());
                });
    }

    public Observable<Map<String, Object>> getWeekView(Boolean todayCoverage) {
        return viewApi.getWeekView(todayCoverage)
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        return response.getData();
                    }
                    throw new BusinessException(response.getCode(), response.getMessage());
                });
    }

    public Observable<Map<String, Object>> getDayView(String date, Boolean todayCoverage) {
        return viewApi.getDayView(date, todayCoverage)
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        return response.getData();
                    }
                    throw new BusinessException(response.getCode(), response.getMessage());
                });
    }

    public Observable<Map<String, Object>> getMindMap() {
        return viewApi.getMindMap()
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        // Older servers generate profile rows under this key. The personal card
                        // must use the editor's saved-work endpoint, even with such a server.
                        Map<String, Object> overview = new HashMap<>(response.getData());
                        overview.remove("personalImportant");
                        return overview;
                    }
                    throw new BusinessException(response.getCode(), response.getMessage());
                });
    }

    public Observable<Map<String, Object>> getHome() {
        return viewApi.getHome().subscribeOn(Schedulers.io()).map(response -> {
            if (response.isSuccess() && response.getData() != null) return response.getData();
            throw new BusinessException(response.getCode(), response.getMessage());
        });
    }

    public Observable<List<Map<String, Object>>> getPersonalImportantWork() {
        return logRepository.getTop10("PERSONAL_IMPORTANT");
    }

    public Observable<Map<String, Object>> getInfoMap() {
        return viewApi.getInfoMap()
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        return response.getData();
                    }
                    throw new BusinessException(response.getCode(), response.getMessage());
                });
    }

    public Observable<Map<String, Object>> injectPersonalTop10(Map<String, Object> request) {
        return viewApi.injectPersonalTop10(request)
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        return response.getData();
                    }
                    throw new BusinessException(response.getCode(), response.getMessage());
                });
    }

    public Observable<Void> deletePersonalTop10(Long id) {
        return viewApi.deletePersonalTop10(id)
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (response.isSuccess()) {
                        return null;
                    }
                    throw new BusinessException(response.getCode(), response.getMessage());
                });
    }
}

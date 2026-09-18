package com.example.project_pandora.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.project_pandora.repository.ViewRepository;

import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

public class ViewViewModel extends BaseViewModel {

    private final ViewRepository repository;

    private final MutableLiveData<Map<String, Object>> monthViewData = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>> weekViewData = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>> dayViewData = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>> mindMapData = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>> infoMapData = new MutableLiveData<>();

    public ViewViewModel(@NonNull Application application) {
        super();
        repository = new ViewRepository(application);
    }

    public LiveData<Map<String, Object>> getMonthViewData() { return monthViewData; }
    public LiveData<Map<String, Object>> getWeekViewData() { return weekViewData; }
    public LiveData<Map<String, Object>> getDayViewData() { return dayViewData; }
    public LiveData<Map<String, Object>> getMindMapData() { return mindMapData; }
    public LiveData<Map<String, Object>> getInfoMapData() { return infoMapData; }

    public void loadMonthView(String month) {
        showLoading();
        compositeDisposable.add(
                repository.getMonthView(month)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                data -> {
                                    hideLoading();
                                    monthViewData.setValue(data);
                                },
                                error -> {
                                    hideLoading();
                                    showError(error.getMessage());
                                }
                        )
        );
    }

    public void loadWeekView() {
        showLoading();
        compositeDisposable.add(
                repository.getWeekView()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                data -> {
                                    hideLoading();
                                    weekViewData.setValue(data);
                                },
                                error -> {
                                    hideLoading();
                                    showError(error.getMessage());
                                }
                        )
        );
    }

    public void loadDayView(String date) {
        showLoading();
        compositeDisposable.add(
                repository.getDayView(date)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                data -> {
                                    hideLoading();
                                    dayViewData.setValue(data);
                                },
                                error -> {
                                    hideLoading();
                                    showError(error.getMessage());
                                }
                        )
        );
    }

    public void loadMindMap() {
        showLoading();
        compositeDisposable.add(
                repository.getMindMap()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                data -> {
                                    hideLoading();
                                    mindMapData.setValue(data);
                                },
                                error -> {
                                    hideLoading();
                                    showError(error.getMessage());
                                }
                        )
        );
    }

    public void loadInfoMap() {
        showLoading();
        compositeDisposable.add(
                repository.getInfoMap()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                data -> {
                                    hideLoading();
                                    infoMapData.setValue(data);
                                },
                                error -> {
                                    hideLoading();
                                    showError(error.getMessage());
                                }
                        )
        );
    }
}
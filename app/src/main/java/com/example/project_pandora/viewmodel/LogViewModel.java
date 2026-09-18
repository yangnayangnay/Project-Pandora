package com.example.project_pandora.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.project_pandora.repository.LogRepository;

import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

public class LogViewModel extends BaseViewModel {

    private final LogRepository repository;

    private final MutableLiveData<List<Map<String, Object>>> logList = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>> logStats = new MutableLiveData<>();
    private final MutableLiveData<List<Map<String, Object>>> top10Works = new MutableLiveData<>();
    private final MutableLiveData<Boolean> operationSuccess = new MutableLiveData<>();

    public LogViewModel(@NonNull Application application) {
        super();
        repository = new LogRepository(application);
    }

    public LiveData<List<Map<String, Object>>> getLogList() { return logList; }
    public LiveData<Map<String, Object>> getLogStats() { return logStats; }
    public LiveData<List<Map<String, Object>>> getTop10Works() { return top10Works; }
    public LiveData<Boolean> getOperationSuccess() { return operationSuccess; }

    public void createLog(String workItem, String completionStatus, Double timeCost, String logDate) {
        showLoading();
        compositeDisposable.add(
                repository.createLog(workItem, completionStatus, timeCost, logDate)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                result -> {
                                    hideLoading();
                                    operationSuccess.setValue(true);
                                },
                                error -> {
                                    hideLoading();
                                    showError(error.getMessage());
                                }
                        )
        );
    }

    public void loadLogList() {
        showLoading();
        compositeDisposable.add(
                repository.listLogs()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                list -> {
                                    hideLoading();
                                    logList.setValue(list);
                                    if (list == null || list.isEmpty()) showEmpty(); else hideEmpty();
                                },
                                error -> {
                                    hideLoading();
                                    showError(error.getMessage());
                                }
                        )
        );
    }

    public void loadLogStats() {
        showLoading();
        compositeDisposable.add(
                repository.getLogStats()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                stats -> {
                                    hideLoading();
                                    logStats.setValue(stats);
                                },
                                error -> {
                                    hideLoading();
                                    showError(error.getMessage());
                                }
                        )
        );
    }

    public void loadTop10(String workType) {
        showLoading();
        compositeDisposable.add(
                repository.getTop10(workType)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                list -> {
                                    hideLoading();
                                    top10Works.setValue(list);
                                },
                                error -> {
                                    hideLoading();
                                    showError(error.getMessage());
                                }
                        )
        );
    }

    public void updateTop10(List<Map<String, Object>> top10List) {
        showLoading();
        compositeDisposable.add(
                repository.updateTop10(top10List)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                ignored -> {
                                    hideLoading();
                                    operationSuccess.setValue(true);
                                },
                                error -> {
                                    hideLoading();
                                    showError(error.getMessage());
                                }
                        )
        );
    }
}
package com.example.project_pandora.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.project_pandora.repository.AIRepository;

import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

public class AIViewModel extends BaseViewModel {

    private final AIRepository repository;

    private final MutableLiveData<Map<String, Object>> mbtiReport = new MutableLiveData<>();
    private final MutableLiveData<List<Map<String, Object>>> keywords = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>> suggestion = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>> divination = new MutableLiveData<>();

    public AIViewModel(@NonNull Application application) {
        super();
        repository = new AIRepository(application);
    }

    public LiveData<Map<String, Object>> getMbtiReport() { return mbtiReport; }
    public LiveData<List<Map<String, Object>>> getKeywords() { return keywords; }
    public LiveData<Map<String, Object>> getSuggestion() { return suggestion; }
    public LiveData<Map<String, Object>> getDivination() { return divination; }

    public void submitMbti(List<Integer> answers) {
        showLoading();
        compositeDisposable.add(
                repository.submitMbti(answers)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                report -> {
                                    hideLoading();
                                    mbtiReport.setValue(report);
                                },
                                error -> {
                                    hideLoading();
                                    showError(error.getMessage());
                                }
                        )
        );
    }

    public void loadMbtiReport() {
        showLoading();
        compositeDisposable.add(
                repository.getMbtiReport()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                report -> {
                                    hideLoading();
                                    mbtiReport.setValue(report);
                                },
                                error -> {
                                    hideLoading();
                                    showError(error.getMessage());
                                }
                        )
        );
    }

    public void analyzeKeywords(String text) {
        showLoading();
        compositeDisposable.add(
                repository.keywordAnalysis(text)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                list -> {
                                    hideLoading();
                                    keywords.setValue(list);
                                },
                                error -> {
                                    hideLoading();
                                    showError(error.getMessage());
                                }
                        )
        );
    }

    public void loadSuggestion() {
        showLoading();
        compositeDisposable.add(
                repository.getSuggestion()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                s -> {
                                    hideLoading();
                                    suggestion.setValue(s);
                                },
                                error -> {
                                    hideLoading();
                                    showError(error.getMessage());
                                }
                        )
        );
    }

    public void loadDivination() {
        showLoading();
        compositeDisposable.add(
                repository.getDivination()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                d -> {
                                    hideLoading();
                                    divination.setValue(d);
                                },
                                error -> {
                                    hideLoading();
                                    showError(error.getMessage());
                                }
                        )
        );
    }
}
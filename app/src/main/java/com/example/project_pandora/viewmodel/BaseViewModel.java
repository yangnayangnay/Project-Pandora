package com.example.project_pandora.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class BaseViewModel extends ViewModel {

    protected final CompositeDisposable compositeDisposable = new CompositeDisposable();

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> empty = new MutableLiveData<>(false);

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getEmpty() { return empty; }

    protected void showLoading() { loading.setValue(true); }
    protected void hideLoading() { loading.setValue(false); }
    protected void showError(String message) { error.setValue(message); }
    protected void showEmpty() { empty.setValue(true); }
    protected void hideEmpty() { empty.setValue(false); }

    @Override
    protected void onCleared() {
        super.onCleared();
        compositeDisposable.dispose();
    }
}
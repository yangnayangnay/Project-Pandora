package com.example.project_pandora.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.project_pandora.repository.TaskRepository;

import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

public class TaskViewModel extends BaseViewModel {

    private final TaskRepository repository;

    private final MutableLiveData<List<Map<String, Object>>> taskList = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>> taskDetail = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>> quadrantTasks = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>> createdTask = new MutableLiveData<>();
    private final MutableLiveData<Boolean> operationSuccess = new MutableLiveData<>();

    public TaskViewModel(@NonNull Application application) {
        super();
        repository = new TaskRepository(application);
    }

    public LiveData<List<Map<String, Object>>> getTaskList() { return taskList; }
    public LiveData<Map<String, Object>> getTaskDetail() { return taskDetail; }
    public LiveData<Map<String, Object>> getQuadrantTasks() { return quadrantTasks; }
    public LiveData<Map<String, Object>> getCreatedTask() { return createdTask; }
    public LiveData<Boolean> getOperationSuccess() { return operationSuccess; }

    public void createTask(String name, String priority, Long assigneeId,
                           Boolean isImportant, Boolean isUrgent,
                           String startTime, String endTime) {
        showLoading();
        compositeDisposable.add(
                repository.createTask(name, priority, assigneeId, isImportant, isUrgent, startTime, endTime)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                result -> {
                                    hideLoading();
                                    createdTask.setValue(result);
                                    operationSuccess.setValue(true);
                                },
                                error -> {
                                    hideLoading();
                                    showError(error.getMessage());
                                }
                        )
        );
    }

    public void loadTaskList(String status, Long assigneeId) {
        showLoading();
        compositeDisposable.add(
                repository.listTasks(status, assigneeId)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                list -> {
                                    hideLoading();
                                    taskList.setValue(list);
                                    if (list == null || list.isEmpty()) showEmpty(); else hideEmpty();
                                },
                                error -> {
                                    hideLoading();
                                    showError(error.getMessage());
                                }
                        )
        );
    }

    public void loadTaskDetail(Long taskId) {
        showLoading();
        compositeDisposable.add(
                repository.getTask(taskId)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                detail -> {
                                    hideLoading();
                                    taskDetail.setValue(detail);
                                },
                                error -> {
                                    hideLoading();
                                    showError(error.getMessage());
                                }
                        )
        );
    }

    public void loadQuadrantTasks() {
        showLoading();
        compositeDisposable.add(
                repository.getQuadrantTasks()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                quadrant -> {
                                    hideLoading();
                                    quadrantTasks.setValue(quadrant);
                                },
                                error -> {
                                    hideLoading();
                                    showError(error.getMessage());
                                }
                        )
        );
    }

    public void updateProgress(Long taskId, String progressNote, String status) {
        showLoading();
        compositeDisposable.add(
                repository.updateProgress(taskId, progressNote, status)
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

    public void transitionStatus(Long taskId, String targetStatus) {
        showLoading();
        compositeDisposable.add(
                repository.transitionStatus(taskId, targetStatus)
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
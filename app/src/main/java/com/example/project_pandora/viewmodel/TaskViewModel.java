package com.example.project_pandora.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.project_pandora.repository.TaskRepository;

import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

/**
 * 任务 ViewModel——管理任务相关的 UI 数据和业务逻辑。
 * 使用 LiveData 暴露数据状态，供 UI 层观察更新。
 * 被任务列表、任务详情、象限任务等页面使用。
 */
public class TaskViewModel extends BaseViewModel {

    private final TaskRepository repository;

    // 任务列表数据
    private final MutableLiveData<List<Map<String, Object>>> taskList = new MutableLiveData<>();
    // 任务详情数据
    private final MutableLiveData<Map<String, Object>> taskDetail = new MutableLiveData<>();
    // 象限任务数据（紧急/普通）
    private final MutableLiveData<Map<String, Object>> quadrantTasks = new MutableLiveData<>();
    // 新创建的任务数据
    private final MutableLiveData<Map<String, Object>> createdTask = new MutableLiveData<>();
    // 操作成功状态
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

    /**
     * 创建任务。
     *
     * @param name 任务名称
     * @param assigneeId 负责人ID
     * @param isUrgent 是否紧急
     * @param startTime 开始时间
     * @param endTime 结束时间
     */
    public void createTask(String name, Long assigneeId, Boolean isUrgent,
                           String startTime, String endTime) {
        showLoading();
        compositeDisposable.add(
                repository.createTask(name, assigneeId, isUrgent, startTime, endTime)
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

    /**
     * 加载任务列表。
     *
     * @param status 任务状态筛选
     * @param assigneeId 负责人ID筛选
     */
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

    /**
     * 加载任务详情。
     *
     * @param taskId 任务ID
     */
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

    /**
     * 加载象限任务（紧急/普通）。
     */
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

    /**
     * 更新任务进度。
     *
     * @param taskId 任务ID
     * @param progressNote 进度备注
     * @param status 状态
     */
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

    /**
     * 任务状态流转。
     *
     * @param taskId 任务ID
     * @param targetStatus 目标状态
     */
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
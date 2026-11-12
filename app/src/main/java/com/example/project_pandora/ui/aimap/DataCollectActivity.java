package com.example.project_pandora.ui.aimap;

import android.content.Intent;
import android.os.Bundle;

import com.example.project_pandora.ui.PandoraActivity;

import com.example.project_pandora.core.util.BackToolbarHelper;
import com.example.project_pandora.databinding.ActivityDataCollectBinding;
import com.example.project_pandora.repository.LogRepository;
import com.example.project_pandora.repository.TaskRepository;
import com.example.project_pandora.ui.ai.KeywordAnalysisActivity;

import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class DataCollectActivity extends PandoraActivity {

    private ActivityDataCollectBinding binding;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private boolean tasksFailed, logsFailed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDataCollectBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BackToolbarHelper.setupBack(binding.toolbar, this::finish);
        com.example.project_pandora.ui.widget.CompanyBackgrounds.bind(binding.getRoot(), com.example.project_pandora.ui.widget.CompanyBackgrounds.Scene.AI);
        binding.toolbar.setBackgroundColor(android.graphics.Color.TRANSPARENT);

        loadTaskData();
        loadLogCount();

        binding.btnKeywordAnalysis.setOnClickListener(v ->
                startActivity(new Intent(this, KeywordAnalysisActivity.class)));
    }

    private void loadTaskData() {
        TaskRepository taskRepository = new TaskRepository(this);
        Long userId = com.example.project_pandora.core.network.TokenManager.getInstance(this).getUserId();
        disposables.add(
                taskRepository.listTasks(null, userId)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                this::updateTaskStats,
                                error -> { tasksFailed = true; binding.textTaskCount.setText("—"); binding.textCompletedCount.setText("—"); renderState(); }
                        )
        );
    }

    private void updateTaskStats(List<Map<String, Object>> tasks) {
        tasksFailed = false; renderState();
        if (tasks == null) {
            binding.textTaskCount.setText("0");
            binding.textCompletedCount.setText("0");
            return;
        }
        binding.textTaskCount.setText(String.valueOf(tasks.size()));
        int completed = 0;
        for (Map<String, Object> task : tasks) {
            String status = task.get("status") != null ? task.get("status").toString() : "";
            if ("COMPLETED".equals(status)) {
                completed++;
            }
        }
        binding.textCompletedCount.setText(String.valueOf(completed));
    }

    private void loadLogCount() {
        LogRepository logRepository = new LogRepository(this);
        disposables.add(
                logRepository.listLogs(null, null, null)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                logs -> binding.textLogCount.setText(String.valueOf(logs.size())),
                                error -> { logsFailed = true; binding.textLogCount.setText("—"); renderState(); }
                        )
        );
    }

    private void renderState() {
        binding.textDataState.setText(tasksFailed || logsFailed ? "部分统计暂未加载成功，不影响进入关键词分析。" : "统计来自当前账号已有的任务与日志。");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }
}

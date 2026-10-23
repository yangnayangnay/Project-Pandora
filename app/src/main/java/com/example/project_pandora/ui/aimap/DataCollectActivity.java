package com.example.project_pandora.ui.aimap;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project_pandora.core.util.BackToolbarHelper;
import com.example.project_pandora.databinding.ActivityDataCollectBinding;
import com.example.project_pandora.repository.LogRepository;
import com.example.project_pandora.repository.TaskRepository;
import com.example.project_pandora.ui.ai.KeywordAnalysisActivity;

import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class DataCollectActivity extends AppCompatActivity {

    private ActivityDataCollectBinding binding;
    private final CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDataCollectBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BackToolbarHelper.setupBack(binding.toolbar, this::finish);

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
                                error -> binding.textTaskCount.setText("0")
                        )
        );
    }

    private void updateTaskStats(List<Map<String, Object>> tasks) {
        if (tasks == null) {
            binding.textTaskCount.setText("0");
            binding.textCompletedCount.setText("0");
            return;
        }
        binding.textTaskCount.setText(String.valueOf(tasks.size()));
        int completed = 0;
        for (Map<String, Object> task : tasks) {
            String status = task.get("status") != null ? task.get("status").toString() : "";
            if ("COMPLETED".equals(status) || "CONFIRMED".equals(status)) {
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
                                error -> binding.textLogCount.setText("0")
                        )
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }
}
package com.example.project_pandora.ui.task;

import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.core.network.TokenManager;
import com.example.project_pandora.data.remote.TaskApi;
import com.example.project_pandora.databinding.ActivityTaskCreateBinding;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class TaskCreateActivity extends AppCompatActivity {

    private ActivityTaskCreateBinding binding;
    private TaskApi taskApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTaskCreateBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        taskApi = NetworkClient.getInstance(this).createService(TaskApi.class);

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        setupPrioritySpinner();
        binding.btnCreateTask.setOnClickListener(v -> handleCreateTask());
    }

    private void setupPrioritySpinner() {
        String[] priorities = {"LOW - 低", "MEDIUM - 中", "HIGH - 高", "URGENT - 紧急"};
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, priorities);
        binding.spinnerPriority.setAdapter(adapter);
    }

    private void handleCreateTask() {
        String name = binding.editTaskName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "请输入任务名称", Toast.LENGTH_SHORT).show();
            return;
        }

        String priority = "MEDIUM";
        int priorityPos = binding.spinnerPriority.getSelectedItemPosition();
        switch (priorityPos) {
            case 0: priority = "LOW"; break;
            case 1: priority = "MEDIUM"; break;
            case 2: priority = "HIGH"; break;
            case 3: priority = "URGENT"; break;
        }

        boolean isImportant = binding.checkImportant.isChecked();
        boolean isUrgent = binding.checkUrgent.isChecked();

        Long assigneeId = TokenManager.getInstance(this).getUserId();

        Map<String, Object> request = new HashMap<>();
        request.put("name", name);
        request.put("priority", priority);
        request.put("assigneeId", assigneeId);
        request.put("isImportant", isImportant);
        request.put("isUrgent", isUrgent);

        binding.btnCreateTask.setEnabled(false);
        binding.btnCreateTask.setText("创建中...");

        taskApi.createTask(request)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        response -> {
                            if (response.isSuccess()) {
                                Toast.makeText(this, "任务创建成功", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(this, response.getMessage(), Toast.LENGTH_SHORT).show();
                                binding.btnCreateTask.setEnabled(true);
                                binding.btnCreateTask.setText("创建任务");
                            }
                        },
                        error -> {
                            Toast.makeText(this, "创建失败: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            binding.btnCreateTask.setEnabled(true);
                            binding.btnCreateTask.setText("创建任务");
                        }
                );
    }
}
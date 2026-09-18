package com.example.project_pandora.ui.task;

import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.project_pandora.core.network.TokenManager;
import com.example.project_pandora.databinding.ActivityTaskDispatchBinding;
import com.example.project_pandora.repository.TaskRepository;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

public class TaskDispatchActivity extends AppCompatActivity {

    private ActivityTaskDispatchBinding binding;
    private TaskRepository taskRepository;
    private Long selectedAssigneeId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTaskDispatchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        taskRepository = new TaskRepository(this);

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        setupPrioritySpinner();
        binding.btnDispatch.setOnClickListener(v -> handleDispatch());
    }

    private void setupPrioritySpinner() {
        String[] priorities = {"LOW - 低", "MEDIUM - 中", "HIGH - 高", "URGENT - 紧急"};
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, priorities);
        binding.spinnerPriority.setAdapter(adapter);
    }

    private void handleDispatch() {
        String name = binding.editTaskName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "请输入任务名称", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedAssigneeId == null) {
            Toast.makeText(this, "请选择责任人", Toast.LENGTH_SHORT).show();
            return;
        }

        String priority = "MEDIUM";
        switch (binding.spinnerPriority.getSelectedItemPosition()) {
            case 0: priority = "LOW"; break;
            case 1: priority = "MEDIUM"; break;
            case 2: priority = "HIGH"; break;
            case 3: priority = "URGENT"; break;
        }

        boolean isImportant = binding.checkImportant.isChecked();
        boolean isUrgent = binding.checkUrgent.isChecked();
        Long dispatcherId = TokenManager.getInstance(this).getUserId();

        binding.btnDispatch.setEnabled(false);
        binding.btnDispatch.setText("分发中...");

        taskRepository.createTask(name, priority, selectedAssigneeId, isImportant, isUrgent, null, null)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            Toast.makeText(this, "任务分发成功", Toast.LENGTH_SHORT).show();
                            finish();
                        },
                        error -> {
                            Toast.makeText(this, "分发失败: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            binding.btnDispatch.setEnabled(true);
                            binding.btnDispatch.setText("分发任务");
                        }
                );
    }
}
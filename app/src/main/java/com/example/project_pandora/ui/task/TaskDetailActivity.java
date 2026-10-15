package com.example.project_pandora.ui.task;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.project_pandora.R;
import com.example.project_pandora.core.network.TokenManager;
import com.example.project_pandora.core.sync.SyncRouteResult;
import com.example.project_pandora.databinding.ActivityTaskDetailBinding;
import com.example.project_pandora.repository.SyncRoutingRepository;
import com.example.project_pandora.repository.TaskRepository;


import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import com.example.project_pandora.core.util.BackToolbarHelper;

public class TaskDetailActivity extends AppCompatActivity {

    private ActivityTaskDetailBinding binding;
    private TaskRepository taskRepository;
    private SyncRoutingRepository syncRoutingRepository;
    private Long taskId;
    private Map<String, Object> currentTask;
    private android.content.SharedPreferences urgencyPrefs;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityTaskDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        taskRepository = new TaskRepository(this);
        syncRoutingRepository = new SyncRoutingRepository(this);
        taskId = getIntent().getLongExtra("taskId", -1);
        urgencyPrefs = getSharedPreferences("task_urgency", MODE_PRIVATE);

        if (taskId == -1) {
            finish();
            return;
        }

        BackToolbarHelper.setupBack(binding.toolbar, this::finish);
        setupButtons();
        loadTaskDetail();
    }

    private void loadTaskDetail() {
        binding.progressBar.setVisibility(View.VISIBLE);
        taskRepository.getTask(taskId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        task -> {
                            binding.progressBar.setVisibility(View.GONE);
                            currentTask = task;
                            displayTask(task);
                        },
                        error -> {
                            binding.progressBar.setVisibility(View.GONE);
                            Toast.makeText(this, error.getMessage(), Toast.LENGTH_SHORT).show();
                            finish();
                        }
                );
    }

    @SuppressWarnings("unchecked")
    private void displayTask(Map<String, Object> task) {
        binding.textTaskName.setText((String) task.get("name"));

        Object endTime = task.get("end_time");
        binding.textSummaryDeadline.setText(formatDateTime(endTime));

        String priority = (String) task.get("priority");
        boolean isUrgent = "URGENT".equals(priority) || "HIGH".equals(priority);
        binding.textSummaryUrgency.setText(isUrgent ? "紧急" : "不紧急");
        binding.textSummaryUrgency.setBackgroundColor(isUrgent ? 0xFFF44336 : 0xFF4CAF50);

        String status = (String) task.get("status");
        boolean isCompleted = "COMPLETED".equals(status) || "CONFIRMED".equals(status);
        binding.textSummaryCompletion.setText(isCompleted ? "已完成" : "未完成");
        binding.textSummaryCompletion.setBackgroundColor(isCompleted ? 0xFF2196F3 : 0xFFFFEB3B);
        binding.textSummaryCompletion.setTextColor(isCompleted ? 0xFFFFFFFF : 0xFF333333);

        Object progressNote = task.get("progress_note");
        binding.textSummaryProgress.setText(progressNote != null ? progressNote.toString() : "-");

        Object assigneeName = task.get("assignee_name");
        if (assigneeName == null) assigneeName = task.get("assigneeName");
        binding.textSummaryAssignee.setText(assigneeName != null ? assigneeName.toString() : "-");

        binding.textStatus.setText(getStatusDisplayName(status));

        binding.textPriority.setText(getPriorityDisplayName(priority));

        Object isImportantObj = task.get("is_important");
        Object isUrgentObj = task.get("is_urgent");
        boolean isImportant = toBool(isImportantObj);
        boolean urgentFlag = toBool(isUrgentObj);
        String quadrant;
        if (isImportant && urgentFlag) quadrant = "重要且紧急";
        else if (isImportant) quadrant = "重要不紧急";
        else if (urgentFlag) quadrant = "紧急不重要";
        else quadrant = "不重要不紧急";
        binding.textQuadrant.setText(quadrant);

        Object startTime = task.get("start_time");
        binding.textStartTime.setText("开始: " + formatDateTime(startTime));
        binding.textEndTime.setText("截止: " + formatDateTime(endTime));

        setupUrgencyAdjustment(task);

        binding.textProgressNote.setText(progressNote != null ? progressNote.toString() : "暂无进度备注");

        updateButtonVisibility(status);
    }

    private void setupUrgencyAdjustment(Map<String, Object> task) {
        Object isUrgentObj = task.get("is_urgent");
        boolean defaultUrgent = toBool(isUrgentObj);

        boolean savedOverride = urgencyPrefs.getBoolean("task_" + taskId, defaultUrgent);
        boolean hasOverride = urgencyPrefs.contains("task_" + taskId);

        if (hasOverride) {
            binding.textUrgencyHint.setText("已手动设置紧急程度，可随时调整");
        } else {
            binding.textUrgencyHint.setText("请设置紧急程度（红色为紧急，绿色为不紧急）");
        }

        binding.radioUrgencyUrgent.setChecked(savedOverride);
        binding.radioUrgencyNotUrgent.setChecked(!savedOverride);

        binding.radioGroupUrgency.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isUrgentNow = checkedId == R.id.radioUrgencyUrgent;
            urgencyPrefs.edit().putBoolean("task_" + taskId, isUrgentNow).apply();
            binding.textUrgencyHint.setText("已手动设置紧急程度，可随时调整");
            Toast.makeText(this, "紧急程度已调整为：" + (isUrgentNow ? "紧急" : "不紧急"), Toast.LENGTH_SHORT).show();
        });
    }

    private String formatDateTime(Object dateTime) {
        if (dateTime == null) return "未设置";
        String raw = dateTime.toString();
        if (raw.length() >= 16) {
            return raw.substring(0, 10) + " " + raw.substring(11, 16);
        }
        if (raw.length() >= 10) {
            return raw.substring(0, 10);
        }
        return raw;
    }

    private void updateButtonVisibility(String status) {
        Long currentUserId = TokenManager.getInstance(this).getUserId();
        Long assigneeId = currentTask != null && currentTask.get("assignee_id") != null
                ? ((Number) currentTask.get("assignee_id")).longValue() : null;
        Long dispatcherId = currentTask != null && currentTask.get("dispatcher_id") != null
                ? ((Number) currentTask.get("dispatcher_id")).longValue() : null;

        boolean isAssignee = currentUserId.equals(assigneeId);
        boolean isDispatcher = currentUserId.equals(dispatcherId);

        binding.btnStart.setVisibility(isAssignee && "PENDING".equals(status) ? View.VISIBLE : View.GONE);
        binding.btnComplete.setVisibility(isAssignee && "IN_PROGRESS".equals(status) ? View.VISIBLE : View.GONE);
        binding.btnConfirm.setVisibility(isDispatcher && "COMPLETED".equals(status) ? View.VISIBLE : View.GONE);
        binding.btnReject.setVisibility(isDispatcher && "COMPLETED".equals(status) ? View.VISIBLE : View.GONE);

        String role = TokenManager.getInstance(this).getRole();
        boolean canDelete = isDispatcher || "ADMIN".equals(role);
        binding.btnDelete.setVisibility(canDelete ? View.VISIBLE : View.GONE);
    }

    private void setupButtons() {
        binding.btnStart.setOnClickListener(v -> transitionStatus("IN_PROGRESS"));
        binding.btnComplete.setOnClickListener(v -> {
            String progressNote = binding.editProgressNote.getText().toString().trim();
            updateProgress(progressNote, "COMPLETED");
        });
        binding.btnConfirm.setOnClickListener(v -> transitionStatus("CONFIRMED"));
        binding.btnReject.setOnClickListener(v -> transitionStatus("REJECTED"));
        binding.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("确认删除")
                    .setMessage("确定要删除此任务吗？此操作不可撤销。")
                    .setPositiveButton("删除", (d, w) -> confirmDeleteTask())
                    .setNegativeButton("取消", null)
                    .show();
        });
    }

    private void confirmDeleteTask() {
        binding.progressBar.setVisibility(View.VISIBLE);
        taskRepository.deleteTask(taskId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        ignored -> {
                            binding.progressBar.setVisibility(View.GONE);
                            Toast.makeText(this, "任务已删除", Toast.LENGTH_SHORT).show();
                            finish();
                        },
                        error -> {
                            binding.progressBar.setVisibility(View.GONE);
                            Toast.makeText(this, error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                );
    }

    private void transitionStatus(String targetStatus) {
        binding.progressBar.setVisibility(View.VISIBLE);
        syncRoutingRepository.transitionTaskStatus(taskId, targetStatus)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            binding.progressBar.setVisibility(View.GONE);
                            Toast.makeText(this, result.getMessage(), Toast.LENGTH_SHORT).show();
                            loadTaskDetail();
                        },
                        error -> {
                            binding.progressBar.setVisibility(View.GONE);
                            Toast.makeText(this, error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                );
    }

    private void updateProgress(String progressNote, String status) {
        binding.progressBar.setVisibility(View.VISIBLE);
        syncRoutingRepository.updateTaskProgress(taskId, progressNote, status)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            binding.progressBar.setVisibility(View.GONE);
                            Toast.makeText(this, result.getMessage(), Toast.LENGTH_SHORT).show();
                            loadTaskDetail();
                        },
                        error -> {
                            binding.progressBar.setVisibility(View.GONE);
                            Toast.makeText(this, error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                );
    }

    private String getStatusDisplayName(String status) {
        if (status == null) return "待接收";
        switch (status) {
            case "PENDING": return "待接收";
            case "IN_PROGRESS": return "进行中";
            case "COMPLETED": return "已完成";
            case "CONFIRMED": return "已确认";
            case "REJECTED": return "已退回";
            default: return status;
        }
    }

    private String getPriorityDisplayName(String priority) {
        if (priority == null) return "中";
        switch (priority) {
            case "LOW": return "低";
            case "MEDIUM": return "中";
            case "HIGH": return "高";
            case "URGENT": return "紧急";
            default: return priority;
        }
    }

    private static boolean toBool(Object obj) {
        if (obj == null) return false;
        if (obj instanceof Boolean) return (Boolean) obj;
        if (obj instanceof Number) return ((Number) obj).intValue() != 0;
        return false;
    }
}
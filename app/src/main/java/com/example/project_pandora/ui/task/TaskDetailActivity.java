package com.example.project_pandora.ui.task;

import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project_pandora.R;
import com.example.project_pandora.core.network.TokenManager;
import com.example.project_pandora.databinding.ActivityTaskDetailBinding;
import com.example.project_pandora.repository.TaskRepository;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

public class TaskDetailActivity extends AppCompatActivity {

    private ActivityTaskDetailBinding binding;
    private TaskRepository taskRepository;
    private Long taskId;
    private Map<String, Object> currentTask;
    private android.content.SharedPreferences urgencyPrefs;
    private final SimpleDateFormat urgencyDateFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTaskDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        taskRepository = new TaskRepository(this);
        taskId = getIntent().getLongExtra("taskId", -1);
        urgencyPrefs = getSharedPreferences("task_urgency", MODE_PRIVATE);

        if (taskId == -1) {
            finish();
            return;
        }

        binding.toolbar.setNavigationOnClickListener(v -> finish());
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

        String status = (String) task.get("status");
        binding.textStatus.setText(getStatusDisplayName(status));

        String priority = (String) task.get("priority");
        binding.textPriority.setText(getPriorityDisplayName(priority));

        Object isImportantObj = task.get("is_important");
        Object isUrgentObj = task.get("is_urgent");
        boolean isImportant = isImportantObj != null && (Boolean) isImportantObj;
        boolean isUrgent = isUrgentObj != null && (Boolean) isUrgentObj;
        String quadrant;
        if (isImportant && isUrgent) quadrant = "重要且紧急";
        else if (isImportant) quadrant = "重要不紧急";
        else if (isUrgent) quadrant = "紧急不重要";
        else quadrant = "不重要不紧急";
        binding.textQuadrant.setText(quadrant);

        Object startTime = task.get("start_time");
        binding.textStartTime.setText("开始: " + (startTime != null ? startTime.toString() : "未设置"));
        Object endTime = task.get("end_time");
        binding.textEndTime.setText("截止: " + (endTime != null ? endTime.toString() : "未设置"));

        setupUrgencyAdjustment(task);

        Object progressNote = task.get("progress_note");
        binding.textProgressNote.setText(progressNote != null ? progressNote.toString() : "暂无进度备注");

        updateButtonVisibility(status);
    }

    private void setupUrgencyAdjustment(Map<String, Object> task) {
        int defaultUrgency = calculateDeadlineUrgency(task);
        int savedOverride = urgencyPrefs.getInt("task_" + taskId, 0);
        int currentUrgency = savedOverride > 0 ? savedOverride : defaultUrgency;

        String[] labels = {"", "最高（深红色）", "高（淡红色）", "中（黄色）", "低（绿色）"};
        String[] hints = {
            "",
            "当天交付或已逾期",
            "3天内交付",
            "10天内交付",
            "10天以上交付"
        };

        if (savedOverride > 0) {
            binding.textUrgencyHint.setText("默认紧急程度：" + labels[defaultUrgency] + "（" + hints[defaultUrgency] + "），已手动调整");
        } else {
            binding.textUrgencyHint.setText("默认紧急程度：" + labels[defaultUrgency] + "（" + hints[defaultUrgency] + "），可手动调整");
        }

        RadioButton[] radios = {
            binding.radioUrgencyCritical,
            binding.radioUrgencyHigh,
            binding.radioUrgencyMedium,
            binding.radioUrgencyLow
        };
        for (int i = 0; i < 4; i++) {
            radios[i].setChecked(currentUrgency == i + 1);
        }

        binding.radioGroupUrgency.setOnCheckedChangeListener((group, checkedId) -> {
            int newLevel = 0;
            if (checkedId == R.id.radioUrgencyCritical) newLevel = 1;
            else if (checkedId == R.id.radioUrgencyHigh) newLevel = 2;
            else if (checkedId == R.id.radioUrgencyMedium) newLevel = 3;
            else if (checkedId == R.id.radioUrgencyLow) newLevel = 4;

            if (newLevel > 0) {
                urgencyPrefs.edit().putInt("task_" + taskId, newLevel).apply();
                binding.textUrgencyHint.setText("默认紧急程度：" + labels[defaultUrgency] + "（" + hints[defaultUrgency] + "），已手动调整");
                Toast.makeText(this, "紧急程度已调整为：" + labels[newLevel], Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int calculateDeadlineUrgency(Map<String, Object> task) {
        Object endTime = task.get("end_time");
        if (endTime == null) return 4;

        String dateStr = endTime.toString();
        if (dateStr.length() >= 10) dateStr = dateStr.substring(0, 10);
        Date deadline;
        try {
            deadline = urgencyDateFmt.parse(dateStr);
        } catch (ParseException e) {
            return 4;
        }

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        Calendar dl = Calendar.getInstance();
        dl.setTime(deadline);
        dl.set(Calendar.HOUR_OF_DAY, 0);
        dl.set(Calendar.MINUTE, 0);
        dl.set(Calendar.SECOND, 0);
        dl.set(Calendar.MILLISECOND, 0);

        long diffDays = (dl.getTimeInMillis() - today.getTimeInMillis()) / (1000 * 60 * 60 * 24);

        if (diffDays <= 0) return 1;
        if (diffDays < 3) return 2;
        if (diffDays <= 10) return 3;
        return 4;
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
    }

    private void setupButtons() {
        binding.btnStart.setOnClickListener(v -> transitionStatus("IN_PROGRESS"));
        binding.btnComplete.setOnClickListener(v -> {
            String progressNote = binding.editProgressNote.getText().toString().trim();
            updateProgress(progressNote, "COMPLETED");
        });
        binding.btnConfirm.setOnClickListener(v -> transitionStatus("CONFIRMED"));
        binding.btnReject.setOnClickListener(v -> transitionStatus("REJECTED"));
    }

    private void transitionStatus(String targetStatus) {
        binding.progressBar.setVisibility(View.VISIBLE);
        taskRepository.transitionStatus(taskId, targetStatus)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        ignored -> {
                            binding.progressBar.setVisibility(View.GONE);
                            Toast.makeText(this, "操作成功", Toast.LENGTH_SHORT).show();
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
        taskRepository.updateProgress(taskId, progressNote, status)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        ignored -> {
                            binding.progressBar.setVisibility(View.GONE);
                            Toast.makeText(this, "操作成功", Toast.LENGTH_SHORT).show();
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
}
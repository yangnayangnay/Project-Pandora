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

        // Round26 观察K: 摘要位判定口径由 priority 改为 is_urgent（与象限判定/手动调整区统一，消除双口径）
        boolean isUrgent = com.example.project_pandora.core.util.NumberCompat.toBool(task.get("is_urgent"));
        binding.textSummaryUrgency.setText(isUrgent ? "紧急" : "不紧急");
        binding.textSummaryUrgency.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, isUrgent ? com.example.project_pandora.R.color.color_urgent_bg : com.example.project_pandora.R.color.color_not_urgent_bg));

        String status = (String) task.get("status");
        boolean isCompleted = "COMPLETED".equals(status) || "CONFIRMED".equals(status);
        binding.textSummaryCompletion.setText(isCompleted ? "已完成" : "未完成");
        binding.textSummaryCompletion.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, isCompleted ? com.example.project_pandora.R.color.color_status_completed_bg : com.example.project_pandora.R.color.color_status_pending_bg));
        binding.textSummaryCompletion.setTextColor(androidx.core.content.ContextCompat.getColor(this, isCompleted ? com.example.project_pandora.R.color.color_status_completed_text : com.example.project_pandora.R.color.color_status_pending_text));


        Object assigneeName = task.get("assignee_name");
        if (assigneeName == null) assigneeName = task.get("assigneeName");
        binding.textSummaryAssignee.setText(assigneeName != null ? assigneeName.toString() : "-");

        binding.textStatus.setText(getStatusDisplayName(status));

        // Round26 观察K: priority 保留字段区展示（摘要位已改 is_urgent）
        String priority = (String) task.get("priority");
        binding.textPriority.setText(getPriorityDisplayName(priority));

        Object isImportantObj = task.get("is_important");
        Object isUrgentObj = task.get("is_urgent");
        // Round26: toBool 收敛至 NumberCompat（原私有方法语义一致：Boolean 直取/Number 非零/其余 false）
        boolean isImportant = com.example.project_pandora.core.util.NumberCompat.toBool(isImportantObj);
        boolean urgentFlag = com.example.project_pandora.core.util.NumberCompat.toBool(isUrgentObj);
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


        // R39 REQ-C08: 正文展示（存量任务 null 占位）
        Object taskContent = task.get("content");
        binding.textTaskContent.setText(taskContent != null && !taskContent.toString().trim().isEmpty()
                ? taskContent.toString() : getString(com.example.project_pandora.R.string.task_content_placeholder));

        updateButtonVisibility(status);
    }

    private void setupUrgencyAdjustment(Map<String, Object> task) {
        Object isUrgentObj = task.get("is_urgent");
        // Round26: toBool 收敛至 NumberCompat（原私有方法语义一致）
        boolean defaultUrgent = com.example.project_pandora.core.util.NumberCompat.toBool(isUrgentObj);

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
        // Round25 R25-02: UTC ISO 串按本地时区解析（原 substring 会把 00:00 截成前一天）
        String local = com.example.project_pandora.core.util.DateCompat.toLocalDateTime(dateTime);
        return local != null ? local : "未设置";
    }

    private void updateButtonVisibility(String status) {
        Long currentUserId = TokenManager.getInstance(this).getUserId();
        // Round26: 解析统一收敛至 NumberCompat（原裸强转 (Number)，串形态会 ClassCastException）
        Long assigneeId = currentTask != null
                ? com.example.project_pandora.core.util.NumberCompat.toLong(currentTask.get("assignee_id")) : null;
        Long dispatcherId = currentTask != null
                ? com.example.project_pandora.core.util.NumberCompat.toLong(currentTask.get("dispatcher_id")) : null;

        boolean isAssignee = currentUserId.equals(assigneeId);
        boolean isDispatcher = currentUserId.equals(dispatcherId);

        binding.btnStart.setVisibility(isAssignee && "PENDING".equals(status) ? View.VISIBLE : View.GONE);
        binding.btnComplete.setVisibility(isAssignee && "IN_PROGRESS".equals(status) ? View.VISIBLE : View.GONE);
        // R39 REQ-B02: 切回未完成（责任人+COMPLETED；二元口径）
        binding.btnReopen.setVisibility(isAssignee && "COMPLETED".equals(status) ? View.VISIBLE : View.GONE);
        binding.btnConfirm.setVisibility(isDispatcher && "COMPLETED".equals(status) ? View.VISIBLE : View.GONE);
        binding.btnReject.setVisibility(isDispatcher && "COMPLETED".equals(status) ? View.VISIBLE : View.GONE);

        String role = TokenManager.getInstance(this).getRole();
        boolean canDelete = isDispatcher || "ADMIN".equals(role);
        binding.btnDelete.setVisibility(canDelete ? View.VISIBLE : View.GONE);
    }

    private void setupButtons() {
        binding.btnStart.setOnClickListener(v -> transitionStatus("IN_PROGRESS"));
        binding.btnComplete.setOnClickListener(v -> {
            // REQ-31-01: 完成按钮二次确认（取消/框外不发起请求）
            String taskName = currentTask != null && currentTask.get("name") != null
                    ? currentTask.get("name").toString() : "";
            com.example.project_pandora.ui.common.StatusConfirmDialog.show(this, taskName, "已完成",
                    () -> transitionStatus("COMPLETED"), // R39 REQ-B01/B06: 二元化收敛至 transition 端点
                    null);
        });
        // R39 REQ-B02: 切回未完成——确认弹窗沿用，transition 收敛
        binding.btnReopen.setOnClickListener(v -> {
            String taskName = currentTask != null && currentTask.get("name") != null
                    ? currentTask.get("name").toString() : "";
            com.example.project_pandora.ui.common.StatusConfirmDialog.show(this, taskName, "未完成",
                    () -> transitionStatus("IN_PROGRESS"),
                    null);
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
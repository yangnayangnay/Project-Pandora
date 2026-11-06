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
    private final io.reactivex.rxjava3.disposables.CompositeDisposable tagActions = new io.reactivex.rxjava3.disposables.CompositeDisposable();
    private androidx.appcompat.app.AlertDialog tagDialog;
    private TaskTagPickerView tagPicker;



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
        displayTags(task);

        Object endTime = task.get("end_time");

        String status = (String) task.get("status");

        Object assigneeName = task.get("assignee_name");
        if (assigneeName == null) assigneeName = task.get("assigneeName");
        binding.textAssignee.setText("负责人：" + (assigneeName != null ? assigneeName.toString() : "-"));

        binding.textStatus.setText(getStatusDisplayName(status));

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

        binding.radioGroupUrgency.setOnCheckedChangeListener(null);
        binding.radioUrgencyUrgent.setChecked(defaultUrgent);
        binding.radioUrgencyNotUrgent.setChecked(!defaultUrgent);
        String role = TokenManager.getInstance(this).getRole();
        Long actor = TokenManager.getInstance(this).getUserId();
        boolean canEdit = "ADMIN".equals(role) || "FOUNDER".equals(role)
                || java.util.Objects.equals(actor, com.example.project_pandora.core.util.NumberCompat.toLong(task.get("dispatcher_id")))
                || java.util.Objects.equals(actor, com.example.project_pandora.core.util.NumberCompat.toLong(task.get("assignee_id")));
        binding.radioUrgencyUrgent.setEnabled(canEdit);
        binding.radioUrgencyNotUrgent.setEnabled(canEdit);
        binding.textUrgencyHint.setText(!canEdit ? "仅创建人、责任人或管理职级可调整" : "ADMIN".equals(role) || "FOUNDER".equals(role)
                ? "调整后立即生效（当前职级免审）" : "调整将提交上级审核，通过后生效");
        binding.radioGroupUrgency.setOnCheckedChangeListener((group, checkedId) -> {
            boolean urgent = checkedId == R.id.radioUrgencyUrgent;
            if (urgent == defaultUrgent) return;
            binding.radioUrgencyUrgent.setEnabled(false); binding.radioUrgencyNotUrgent.setEnabled(false);
            Map<String, Object> request = new java.util.HashMap<>(); request.put("isUrgent", urgent);
            taskRepository.updateTask(taskId, request).observeOn(AndroidSchedulers.mainThread()).subscribe(result -> {
                Toast.makeText(this, java.util.Objects.toString(result.get("message"), "操作成功"), Toast.LENGTH_LONG).show();
                loadTaskDetail();
            }, error -> { Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show(); loadTaskDetail(); });
        });
    }

    private String formatDateTime(Object dateTime) {
        // Round25 R25-02: UTC ISO 串按本地时区解析（原 substring 会把 00:00 截成前一天）
        String local = com.example.project_pandora.core.util.DateCompat.toLocalDateTime(dateTime);
        return local != null ? local : "未设置";
    }

    private void displayTags(Map<String, Object> task) {
        binding.taskTagChips.removeAllViews();
        for (Map<String, Object> tag : com.example.project_pandora.core.util.TaskTags.of(task)) {
            com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
            chip.setText("#" + com.example.project_pandora.core.util.TaskTags.value(tag, "name"));
            chip.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.color_text_primary));
            Long index = com.example.project_pandora.core.util.NumberCompat.toLong(com.example.project_pandora.core.util.TaskTags.value(tag, "color_index"));
            chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(TaskTagColors.color(this, index == null ? 0 : index.intValue())));
            chip.setClickable(false); binding.taskTagChips.addView(chip);
        }
        String role = TokenManager.getInstance(this).getRole(); Long actor = TokenManager.getInstance(this).getUserId();
        boolean canEdit = "ADMIN".equals(role) || "FOUNDER".equals(role)
                || java.util.Objects.equals(actor, com.example.project_pandora.core.util.NumberCompat.toLong(task.get("dispatcher_id")))
                || java.util.Objects.equals(actor, com.example.project_pandora.core.util.NumberCompat.toLong(task.get("assignee_id")));
        binding.btnEditTaskTags.setVisibility(canEdit ? View.VISIBLE : View.GONE);
        binding.btnEditTaskTags.setOnClickListener(v -> editTags());
    }

    private void editTags() {
        if (tagDialog != null && tagDialog.isShowing()) return;
        tagPicker = new TaskTagPickerView(this);
        tagPicker.setSelectedIds(com.example.project_pandora.core.util.TaskTags.ids(currentTask));
        android.widget.ScrollView scroll = new android.widget.ScrollView(this); scroll.addView(tagPicker);
        tagDialog = new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("调整任务标签").setView(scroll)
                .setNegativeButton("取消", null).setPositiveButton("保存", null).create();
        tagDialog.setOnShowListener(ignored -> tagDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (!tagPicker.isLoaded()) { Toast.makeText(this, "请先加载标签列表", Toast.LENGTH_SHORT).show(); return; }
            Map<String, Object> body = new java.util.HashMap<>(); body.put("tagIds", tagPicker.getSelectedIds());
            tagDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            tagDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).setEnabled(false);
            tagDialog.setCancelable(false);
            tagActions.add(taskRepository.updateTask(taskId, body).observeOn(AndroidSchedulers.mainThread()).subscribe(result -> {
                tagDialog.dismiss(); Toast.makeText(this, java.util.Objects.toString(result.get("message"), "操作成功"), Toast.LENGTH_LONG).show(); loadTaskDetail();
            }, error -> { tagDialog.setCancelable(true); tagDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setEnabled(true); tagDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).setEnabled(true); Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show(); }));
        })); tagDialog.show();
    }

    @Override protected void onResume() {
        super.onResume();
        if (tagDialog != null && tagDialog.isShowing() && tagPicker != null) tagPicker.reload();
        if (currentTask != null) loadTaskDetail();
    }

    @Override protected void onDestroy() {
        tagActions.dispose(); if (tagDialog != null) tagDialog.dismiss(); super.onDestroy();
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

        String role = TokenManager.getInstance(this).getRole();
        boolean canDelete = isDispatcher || "ADMIN".equals(role) || "FOUNDER".equals(role);
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
                        result -> {
                            binding.progressBar.setVisibility(View.GONE);
                            Toast.makeText(this, java.util.Objects.toString(result.get("message"), "操作成功"), Toast.LENGTH_LONG).show();
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

}

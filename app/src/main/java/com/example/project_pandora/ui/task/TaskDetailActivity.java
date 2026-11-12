package com.example.project_pandora.ui.task;

import android.app.AlertDialog;
import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import com.example.project_pandora.ui.PandoraActivity;
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

public class TaskDetailActivity extends PandoraActivity {

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
        binding.textTaskName.setText(java.util.Objects.toString(com.example.project_pandora.core.util.TaskTags.value(task, "name"), ""));
        displayTags(task);

        Object endTime = com.example.project_pandora.core.util.TaskTags.value(task, "end_time");

        String status = java.util.Objects.toString(com.example.project_pandora.core.util.TaskTags.value(task, "status"), "");

        binding.textAssignee.setText("负责人：" + TaskDetailFilter.assigneeNames(task));

        binding.textStatus.setText(getStatusDisplayName(status));

        Object isUrgentObj = com.example.project_pandora.core.util.TaskTags.value(task, "is_urgent");
        boolean urgentFlag = com.example.project_pandora.core.util.NumberCompat.toBool(isUrgentObj);
        binding.textUrgencyBadge.setText(urgentFlag ? "紧急" : "不紧急");

        Object startTime = com.example.project_pandora.core.util.TaskTags.value(task, "start_time");
        binding.textCreatedTime.setText("创建: " + formatDateTime(com.example.project_pandora.core.util.TaskTags.value(task, "created_at")));
        binding.textStartTime.setText("开始: " + formatDateTime(startTime));
        binding.textEndTime.setText("截止: " + formatDateTime(endTime));

        setupUrgencyAdjustment(task);


        // R39 REQ-C08: 正文展示（存量任务 null 占位）
        Object taskContent = com.example.project_pandora.core.util.TaskTags.value(task, "content");
        binding.textTaskContent.setText(taskContent != null && !taskContent.toString().trim().isEmpty()
                ? taskContent.toString() : getString(com.example.project_pandora.R.string.task_content_placeholder));

        updateButtonVisibility(status);
    }

    private void setupUrgencyAdjustment(Map<String, Object> task) {
        Object isUrgentObj = com.example.project_pandora.core.util.TaskTags.value(task, "is_urgent");
        // Round26: toBool 收敛至 NumberCompat（原私有方法语义一致）
        boolean defaultUrgent = com.example.project_pandora.core.util.NumberCompat.toBool(isUrgentObj);

        binding.radioGroupUrgency.setOnCheckedChangeListener(null);
        binding.radioUrgencyUrgent.setChecked(defaultUrgent);
        binding.radioUrgencyNotUrgent.setChecked(!defaultUrgent);
        String role = TokenManager.getInstance(this).getRole();
        Long actor = TokenManager.getInstance(this).getUserId();
        boolean canEdit = "ADMIN".equals(role) || "FOUNDER".equals(role)
                || java.util.Objects.equals(actor, com.example.project_pandora.core.util.NumberCompat.toLong(com.example.project_pandora.core.util.TaskTags.value(task, "dispatcher_id")))
                || TaskDetailFilter.assigneeIds(task).contains(actor);
        binding.radioUrgencyUrgent.setEnabled(canEdit);
        binding.radioUrgencyNotUrgent.setEnabled(canEdit);
        binding.textUrgencyHint.setText(!canEdit ? "仅创建人、责任人或管理职级可调整" : "ADMIN".equals(role) || "FOUNDER".equals(role)
                ? "此处选择后立即生效；修改其他信息请使用底部“修改”" : "此处选择后提交上级审核，通过后生效；其他信息请使用底部“修改”");
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
            Long index = com.example.project_pandora.core.util.NumberCompat.toLong(com.example.project_pandora.core.util.TaskTags.value(tag, "color_index"));
            int fill = TaskTagColors.color(this, index == null ? 0 : index.intValue());
            chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(fill));
            chip.setTextColor(TaskTagColors.text(this, fill));
            chip.setClickable(false); binding.taskTagChips.addView(chip);
        }
        String role = TokenManager.getInstance(this).getRole(); Long actor = TokenManager.getInstance(this).getUserId();
        boolean canEdit = "ADMIN".equals(role) || "FOUNDER".equals(role)
                || java.util.Objects.equals(actor, com.example.project_pandora.core.util.NumberCompat.toLong(com.example.project_pandora.core.util.TaskTags.value(task, "dispatcher_id")))
                || TaskDetailFilter.assigneeIds(task).contains(actor);
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
        Long dispatcherId = currentTask != null
                ? com.example.project_pandora.core.util.NumberCompat.toLong(currentTask.get("dispatcher_id")) : null;

        boolean isAssignee = currentTask != null && TaskDetailFilter.assigneeIds(currentTask).contains(currentUserId);
        boolean isDispatcher = java.util.Objects.equals(currentUserId, dispatcherId);

        binding.btnComplete.setVisibility(isAssignee && "IN_PROGRESS".equals(status) ? View.VISIBLE : View.GONE);

        String role = TokenManager.getInstance(this).getRole();
        boolean canEdit = "IN_PROGRESS".equals(status) && (isAssignee || isDispatcher || "ADMIN".equals(role) || "FOUNDER".equals(role));
        binding.btnEdit.setVisibility(canEdit ? View.VISIBLE : View.GONE);
        boolean canDelete = isDispatcher || "ADMIN".equals(role) || "FOUNDER".equals(role);
        binding.btnDelete.setVisibility(canDelete ? View.VISIBLE : View.GONE);
    }

    private void setupButtons() {
        binding.btnEdit.setOnClickListener(v -> startActivity(new Intent(this, TaskCreateActivity.class)
                .putExtra("editMode", true).putExtra("taskId", taskId.longValue())));
        binding.btnComplete.setOnClickListener(v -> {
            // REQ-31-01: 完成按钮二次确认（取消/框外不发起请求）
            String taskName = currentTask != null && currentTask.get("name") != null
                    ? currentTask.get("name").toString() : "";
            com.example.project_pandora.ui.common.StatusConfirmDialog.show(this, taskName, "已完成",
                    () -> transitionStatus("COMPLETED"), // R39 REQ-B01/B06: 二元化收敛至 transition 端点
                    null);
        });
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
        if (status == null) return "进行中";
        switch (status) {
            case "IN_PROGRESS": return "进行中";
            case "COMPLETED": return "已完成";
            default: return status;
        }
    }

}

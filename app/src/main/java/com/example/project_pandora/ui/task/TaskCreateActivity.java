package com.example.project_pandora.ui.task;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project_pandora.R;
import com.example.project_pandora.core.autosave.AutoSaveManager;
import com.example.project_pandora.core.autosave.DraftRestoreDialog;
import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.core.network.TokenManager;
import com.example.project_pandora.core.util.BackToolbarHelper;
import com.example.project_pandora.data.remote.TaskApi;
import com.example.project_pandora.databinding.ActivityTaskCreateBinding;
import com.example.project_pandora.repository.DraftRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class TaskCreateActivity extends AppCompatActivity {

    private ActivityTaskCreateBinding binding;
    private TaskApi taskApi;
    private DraftRepository draftRepository;
    private AutoSaveManager autoSaveManager;
    private String selectedEndTime = null;
    private final List<Long> assigneeIds = new ArrayList<>();
    private final List<String> assigneeNames = new ArrayList<>();
    private final CompositeDisposable disposables = new CompositeDisposable();
    private boolean isRestoringDraft = false;
    // Round19 REQ-19-03: 视图页FAB预设分配人
    private Long presetAssigneeId = null;
    private boolean assigneeLocked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTaskCreateBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        taskApi = NetworkClient.getInstance(this).createService(TaskApi.class);
        draftRepository = new DraftRepository(this);

        BackToolbarHelper.setupBack(binding.toolbar, this::finish);

        // Round19 REQ-19-03: 读取 Intent 预设分配人参数
        presetAssigneeId = getIntent().getLongExtra("assigneeId", -1);
        if (presetAssigneeId <= 0) presetAssigneeId = null;
        assigneeLocked = getIntent().getBooleanExtra("assigneeLocked", false);

        loadAssigneeOptions();

        binding.textEndTime.setOnClickListener(v -> {
            showDatePicker();
            onContentChanged();
        });

        binding.btnCreateTask.setOnClickListener(v -> handleCreateTask());

        setupAutoSave();
        checkDraftRestore();
    }

    private void setupAutoSave() {
        autoSaveManager = new AutoSaveManager(this, "TASK",
                () -> {
                    Map<String, Object> content = new HashMap<>();
                    content.put("name", binding.editTaskName.getText().toString());
                    content.put("endTime", selectedEndTime);
                    boolean isUrgent = binding.rgUrgency.getCheckedRadioButtonId() == R.id.rb_urgent;
                    content.put("isUrgent", isUrgent);
                    boolean isImportant = binding.rgImportance.getCheckedRadioButtonId() == R.id.rb_important;
                    content.put("isImportant", isImportant);
                    int pos = binding.spinnerAssignee.getSelectedItemPosition();
                    if (pos >= 0 && pos < assigneeIds.size()) {
                        content.put("assigneeId", assigneeIds.get(pos));
                    }
                    return content;
                },
                status -> {
                    switch (status) {
                        case SAVING:
                            binding.textAutoSaveStatus.setVisibility(android.view.View.VISIBLE);
                            binding.textAutoSaveStatus.setText("保存中...");
                            break;
                        case SAVED:
                            binding.textAutoSaveStatus.setVisibility(android.view.View.VISIBLE);
                            String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
                            binding.textAutoSaveStatus.setText("已保存 " + time);
                            break;
                        case SAVE_FAILED:
                            binding.textAutoSaveStatus.setVisibility(android.view.View.VISIBLE);
                            binding.textAutoSaveStatus.setText("保存失败");
                            break;
                        default:
                            break;
                    }
                });

        binding.editTaskName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (!isRestoringDraft) onContentChanged();
            }
        });
    }

    public void onContentChanged() {
        if (autoSaveManager != null) {
            autoSaveManager.onContentChanged();
        }
    }

    private void checkDraftRestore() {
        disposables.add(draftRepository.getDraft("TASK")
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        draftData -> {
                            if (draftData != null && draftData.get("content") != null) {
                                DraftRestoreDialog.show(this, draftData,
                                        content -> {
                                            isRestoringDraft = true;
                                            restoreDraft(content);
                                            isRestoringDraft = false;
                                        },
                                        () -> disposables.add(draftRepository.deleteDraft("TASK")
                                                .subscribe(r -> {}, Throwable::printStackTrace)));
                            }
                        },
                        error -> {}
                ));
    }

    @SuppressWarnings("unchecked")
    private void restoreDraft(Map<String, Object> content) {
        if (content == null) return;
        Object name = content.get("name");
        if (name != null) binding.editTaskName.setText(name.toString());
        Object endTime = content.get("endTime");
        if (endTime != null) {
            selectedEndTime = endTime.toString();
            binding.textEndTime.setText(selectedEndTime);
        }
        Object isUrgent = content.get("isUrgent");
        if (isUrgent instanceof Boolean) {
            if ((Boolean) isUrgent) {
                binding.rgUrgency.check(R.id.rb_urgent);
            } else {
                binding.rgUrgency.check(R.id.rb_not_urgent);
            }
        }
        Object isImportant = content.get("isImportant");
        if (isImportant instanceof Boolean) {
            if ((Boolean) isImportant) {
                binding.rgImportance.check(R.id.rb_important);
            } else {
                binding.rgImportance.check(R.id.rb_not_important);
            }
        }
        Object assigneeIdObj = content.get("assigneeId");
        if (assigneeIdObj != null) {
            Long assigneeId = Long.parseLong(assigneeIdObj.toString());
            int idx = assigneeIds.indexOf(assigneeId);
            if (idx >= 0) binding.spinnerAssignee.setSelection(idx);
        }
    }

    private void loadAssigneeOptions() {
        Long currentUserId = TokenManager.getInstance(this).getUserId();
        String currentUserName = "自己";

        assigneeIds.clear();
        assigneeNames.clear();
        assigneeIds.add(currentUserId);
        assigneeNames.add(currentUserName);

        disposables.add(taskApi.getSubordinateUsers()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        response -> {
                            if (response.isSuccess() && response.getData() != null) {
                                for (Map<String, Object> user : response.getData()) {
                                    Object id = user.get("id");
                                    if (id == null) id = user.get("ID");
                                    Object name = user.get("real_name");
                                    if (name == null) name = user.get("REAL_NAME");
                                    if (name == null || name.toString().isEmpty()) {
                                        name = user.get("username");
                                        if (name == null) name = user.get("USERNAME");
                                    }
                                    if (id != null) {
                                        assigneeIds.add(Long.parseLong(id.toString()));
                                        assigneeNames.add(name != null ? name.toString() : "用户" + id);
                                    }
                                }
                            }
                            setupAssigneeSpinner();
                        },
                        error -> setupAssigneeSpinner()
                ));
    }

    private void setupAssigneeSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, assigneeNames);
        binding.spinnerAssignee.setAdapter(adapter);

        // Round19 REQ-19-03: 预设分配人定位与锁定
        if (presetAssigneeId != null) {
            int idx = assigneeIds.indexOf(presetAssigneeId);
            if (idx >= 0) {
                binding.spinnerAssignee.setSelection(idx);
            }
        }
        if (assigneeLocked) {
            // 锁定分配人，禁用 Spinner 选择
            binding.spinnerAssignee.setEnabled(false);
            binding.spinnerAssignee.setAlpha(0.6f);
        }
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            selectedEndTime = String.format("%04d-%02d-%02d", year, month + 1, day);
            binding.textEndTime.setText(selectedEndTime);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void handleCreateTask() {
        String name = binding.editTaskName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "请输入任务名称", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedEndTime == null) {
            Toast.makeText(this, "请选择截止日期", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isUrgent = binding.rgUrgency.getCheckedRadioButtonId() == R.id.rb_urgent;
        boolean isImportant = binding.rgImportance.getCheckedRadioButtonId() == R.id.rb_important;
        int assigneePos = binding.spinnerAssignee.getSelectedItemPosition();
        Long assigneeId = assigneeIds.get(assigneePos);

        Map<String, Object> request = new HashMap<>();
        request.put("name", name);
        request.put("priority", isUrgent ? "URGENT" : "MEDIUM");
        request.put("assigneeId", assigneeId);
        request.put("isImportant", isImportant);
        request.put("isUrgent", isUrgent);
        request.put("endTime", selectedEndTime);

        binding.btnCreateTask.setEnabled(false);
        binding.btnCreateTask.setText("创建中...");

        disposables.add(taskApi.createTask(request)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        response -> {
                            if (response.isSuccess()) {
                                Toast.makeText(this, "任务创建成功", Toast.LENGTH_SHORT).show();
                                disposables.add(draftRepository.deleteDraft("TASK")
                                        .subscribe(r -> {}, Throwable::printStackTrace));
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
                ));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (autoSaveManager != null) {
            autoSaveManager.triggerImmediateSave();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (autoSaveManager != null) {
            autoSaveManager.stop();
        }
        disposables.clear();
    }
}
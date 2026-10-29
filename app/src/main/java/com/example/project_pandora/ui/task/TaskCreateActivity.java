package com.example.project_pandora.ui.task;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
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
import com.example.project_pandora.core.util.TaskDateTimes;
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
    private String selectedStartTime;
    private String selectedEndTime;
    private final List<Long> assigneeIds = new ArrayList<>();
    private final List<String> assigneeNames = new ArrayList<>();
    private final CompositeDisposable disposables = new CompositeDisposable();
    private boolean isRestoringDraft = false;
    private boolean saveCompleted = false;
    // Round19 REQ-19-03: 视图页FAB预设分配人
    private Long presetAssigneeId = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTaskCreateBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        taskApi = NetworkClient.getInstance(this).createService(TaskApi.class);
        draftRepository = new DraftRepository(this);

        BackToolbarHelper.setupBack(binding.toolbar, this::finish);

        // Round19 REQ-19-03: 读取 Intent 预设分配人参数
        // Round23 REQ-23-04: 废止锁定语义，仅保留预设定位（默认自己、可选改）
        presetAssigneeId = getIntent().getLongExtra("assigneeId", -1);
        if (presetAssigneeId <= 0) presetAssigneeId = null;

        // Round23 REQ-23-07: 候选加载期间禁用 Spinner，避免误导性下拉
        binding.spinnerAssignee.setEnabled(false);
        loadAssigneeOptions();
        String role = TokenManager.getInstance(this).getRole();
        binding.textTaskReviewHint.setText("ADMIN".equals(role) || "FOUNDER".equals(role)
                ? "可派发给全公司任意人员；当前职级的任务操作免审。"
                : "可派发给全公司任意人员；创建、编辑和删除须经对应上级审核后生效。");

        selectedStartTime = savedInstanceState == null ? TaskDateTimes.format(new Date())
                : savedInstanceState.getString("taskStartTime", TaskDateTimes.format(new Date()));
        selectedEndTime = savedInstanceState == null ? null : savedInstanceState.getString("taskEndTime");
        renderTimes();
        binding.textStartTime.setOnClickListener(v -> showDateTimePicker(false));
        binding.textEndTime.setOnClickListener(v -> showDateTimePicker(true));

        binding.btnCreateTask.setOnClickListener(v -> handleCreateTask());

        setupAutoSave();
        if (savedInstanceState == null) checkDraftRestore();
    }

    private void setupAutoSave() {
        autoSaveManager = new AutoSaveManager(this, "TASK",
                () -> {
                    Map<String, Object> content = new HashMap<>();
                    content.put("name", binding.editTaskName.getText().toString());
                    content.put("content", binding.editTaskContent.getText().toString()); // R39 REQ-C07: 草稿覆盖正文
                    content.put("startTime", selectedStartTime);
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
        // R39 REQ-C07: 草稿恢复回显正文（旧草稿无 content 键 null 守卫天然兼容）
        Object draftContent = content.get("content");
        if (draftContent != null) binding.editTaskContent.setText(draftContent.toString());
        String start = TaskDateTimes.restore(content.get("startTime"), false);
        if (start != null) selectedStartTime = start;
        selectedEndTime = TaskDateTimes.restore(content.get("endTime"), true);
        renderTimes();
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
            // Round25 缺陷⑫: 草稿 JSON 数字反序列化为 Double（"5.0"）——Round26 统一收敛至 NumberCompat
            Long assigneeId = com.example.project_pandora.core.util.NumberCompat.toLong(assigneeIdObj);
            if (assigneeId != null) {
                int idx = assigneeIds.indexOf(assigneeId);
                if (idx >= 0) binding.spinnerAssignee.setSelection(idx);
            }
        }
    }

    private void loadAssigneeOptions() {
        Long currentUserId = TokenManager.getInstance(this).getUserId();
        String currentUserName = "自己";

        assigneeIds.clear();
        assigneeNames.clear();
        assigneeIds.add(currentUserId);
        assigneeNames.add(currentUserName);
        setupAssigneeSpinner();

        disposables.add(taskApi.getAssignees()
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
                                        // Round26: Gson 数字串统一收敛至 NumberCompat（原 parseDouble 裸调无捕获）
                                        Long idLong = com.example.project_pandora.core.util.NumberCompat.toLong(id);
                                        if (idLong != null && !assigneeIds.contains(idLong)) {
                                            assigneeIds.add(idLong);
                                            assigneeNames.add(name != null ? name.toString() : "用户" + idLong);
                                        }
                                    }
                                }
                                setupAssigneeSpinner();
                            } else {
                                showAssigneeLoadError();
                            }
                        },
                        error -> showAssigneeLoadError()
                ));
    }

    private void showAssigneeLoadError() {
        if (isFinishing() || isDestroyed()) return;
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("人员列表加载失败")
                .setMessage("暂时只能分配给自己。请重试以加载公司人员列表。")
                .setPositiveButton("重试", (dialog, which) -> loadAssigneeOptions())
                .setNegativeButton("分配给自己", null)
                .show();
    }

    private void setupAssigneeSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, assigneeNames);
        binding.spinnerAssignee.setAdapter(adapter);

        // Round19 REQ-19-03: 预设分配人定位（Round23 REQ-23-04 起不再锁定）
        if (presetAssigneeId != null) {
            int idx = assigneeIds.indexOf(presetAssigneeId);
            if (idx >= 0) {
                binding.spinnerAssignee.setSelection(idx);
            }
        }

        // Round23 REQ-23-07: 候选仅"自己"（无下属或加载失败）时禁用选择器并降透明度提示
        boolean single = assigneeIds.size() <= 1;
        binding.spinnerAssignee.setEnabled(!single);
        binding.spinnerAssignee.setClickable(!single);
        binding.spinnerAssignee.setAlpha(single ? 0.6f : 1.0f);
    }

    private void renderTimes() {
        binding.textStartTime.setText(selectedStartTime == null ? "点击选择日期和时间" : selectedStartTime);
        binding.textEndTime.setText(selectedEndTime == null ? "点击选择日期和时间" : selectedEndTime);
    }

    private void showDateTimePicker(boolean end) {
        Calendar cal = Calendar.getInstance();
        Date previous = TaskDateTimes.parse(end ? selectedEndTime : selectedStartTime);
        if (previous == null && end) previous = TaskDateTimes.parse(selectedStartTime);
        if (previous != null) cal.setTime(previous);
        new DatePickerDialog(this, (view, year, month, day) -> {
            cal.set(year, month, day);
            TimePickerDialog picker = new TimePickerDialog(this, (timeView, hour, minute) -> {
                cal.set(Calendar.HOUR_OF_DAY, hour);
                cal.set(Calendar.MINUTE, minute);
                String value = TaskDateTimes.format(cal.getTime());
                if (end) selectedEndTime = value; else selectedStartTime = value;
                renderTimes();
                onContentChanged();
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true);
            picker.setTitle(end ? "选择截止时间" : "选择开始时间");
            picker.show();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        outState.putString("taskStartTime", selectedStartTime);
        outState.putString("taskEndTime", selectedEndTime);
        super.onSaveInstanceState(outState);
    }

    private void handleCreateTask() {
        String name = binding.editTaskName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "请输入任务名称", Toast.LENGTH_SHORT).show();
            return;
        }
        if (name.length() > 256) {
            Toast.makeText(this, "任务名称长度不能超过256字符", Toast.LENGTH_SHORT).show();
            return;
        }
        // R39 REQ-C05/C10: 正文必填+长度防呆
        String content = binding.editTaskContent.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, getString(com.example.project_pandora.R.string.task_content_empty), Toast.LENGTH_SHORT).show();
            return;
        }
        if (content.length() > 5000) {
            Toast.makeText(this, getString(com.example.project_pandora.R.string.task_content_too_long), Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedEndTime == null) {
            Toast.makeText(this, "请选择截止日期和时间", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!TaskDateTimes.validRange(selectedStartTime, selectedEndTime)) {
            Toast.makeText(this, "截止时间不能早于开始时间，请检查任务排期", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isUrgent = binding.rgUrgency.getCheckedRadioButtonId() == R.id.rb_urgent;
        boolean isImportant = binding.rgImportance.getCheckedRadioButtonId() == R.id.rb_important;
        int assigneePos = binding.spinnerAssignee.getSelectedItemPosition();
        Long assigneeId = assigneeIds.get(assigneePos);

        Map<String, Object> request = new HashMap<>();
        request.put("name", name);
        request.put("content", content); // R39 REQ-C06: 正文落库
        request.put("priority", isUrgent ? "URGENT" : "MEDIUM");
        request.put("assigneeId", assigneeId);
        request.put("isImportant", isImportant);
        request.put("isUrgent", isUrgent);
        request.put("startTime", selectedStartTime + ":00");
        request.put("endTime", selectedEndTime + ":00");

        binding.btnCreateTask.setEnabled(false);
        binding.btnCreateTask.setText("创建中...");

        disposables.add(taskApi.createTask(request)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        response -> {
                            if (response.isSuccess()) {
                                Toast.makeText(this, response.getData() == null ? response.getMessage() : java.util.Objects.toString(response.getData().get("message"), "操作成功"), Toast.LENGTH_LONG).show();
                                saveCompleted = true;
                                if (autoSaveManager != null) {
                                    autoSaveManager.stop();
                                }
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
        if (autoSaveManager != null && !saveCompleted) {
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

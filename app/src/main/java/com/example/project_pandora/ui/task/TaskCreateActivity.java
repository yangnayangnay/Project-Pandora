package com.example.project_pandora.ui.task;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import com.example.project_pandora.ui.PandoraActivity;
import androidx.appcompat.app.AlertDialog;

import com.example.project_pandora.R;
import com.example.project_pandora.core.autosave.AutoSaveManager;
import com.example.project_pandora.core.autosave.DraftRestoreDialog;
import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.core.network.TokenManager;
import com.example.project_pandora.core.util.BackToolbarHelper;
import com.example.project_pandora.core.util.TaskDateTimes;
import com.example.project_pandora.core.util.TaskTags;
import com.example.project_pandora.data.remote.TaskApi;
import com.example.project_pandora.databinding.ActivityTaskCreateBinding;
import com.example.project_pandora.repository.DraftRepository;
import com.example.project_pandora.ui.widget.QuickDatePickerDialog;
import com.example.project_pandora.ui.widget.WheelTimePickerDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashSet;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class TaskCreateActivity extends PandoraActivity {

    private ActivityTaskCreateBinding binding;
    private TaskApi taskApi;
    private DraftRepository draftRepository;
    private AutoSaveManager autoSaveManager;
    private String selectedStartTime;
    private String selectedEndTime;
    private AlertDialog datePicker;
    private WheelTimePickerDialog timePicker;
    private final List<Long> assigneeIds = new ArrayList<>();
    private final List<String> assigneeNames = new ArrayList<>();
    private final List<Map<String, Object>> assigneePeople = new ArrayList<>();
    private final LinkedHashSet<Long> selectedAssigneeIds = new LinkedHashSet<>();
    private final CompositeDisposable disposables = new CompositeDisposable();
    private boolean isRestoringDraft = false;
    private boolean saveCompleted = false;
    private boolean taskLoaded;
    // Round19 REQ-19-03: 视图页FAB预设分配人
    private Long presetAssigneeId = null;
    private boolean editMode;
    private long editTaskId = -1;
    private TaskFormState originalEditState;
    private AlertDialog exitDialog;

    private final androidx.activity.result.ActivityResultLauncher<android.content.Intent> assigneesLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != android.app.Activity.RESULT_OK || result.getData() == null) return;
                long[] ids = result.getData().getLongArrayExtra("selectedIds");
                if (ids == null || ids.length == 0) return;
                ArrayList<String> names = result.getData().getStringArrayListExtra("selectedNames");
                selectedAssigneeIds.clear();
                for (int i = 0; i < ids.length; i++) {
                    selectedAssigneeIds.add(ids[i]);
                    int existing = assigneeIds.indexOf(ids[i]);
                    if (existing < 0) { assigneeIds.add(ids[i]); assigneeNames.add(names != null && i < names.size() ? names.get(i) : Long.toString(ids[i])); }
                    else if (names != null && i < names.size()) assigneeNames.set(existing, names.get(i));
                }
                renderAssignees(); onContentChanged();
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTaskCreateBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        taskApi = NetworkClient.getInstance(this).createService(TaskApi.class);
        draftRepository = new DraftRepository(this);

        binding.getRoot().setFocusableInTouchMode(true);
        binding.getRoot().requestFocus();
        BackToolbarHelper.setupBack(binding.toolbar, this::requestExit);
        editMode = getIntent().getBooleanExtra("editMode", false);
        editTaskId = getIntent().getLongExtra("taskId", -1);
        if (savedInstanceState != null) originalEditState = (TaskFormState) savedInstanceState.getSerializable("originalEditState");
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { requestExit(); }
        });
        if (editMode) { binding.toolbar.setTitle("修改任务"); binding.btnCreateTask.setText("提交修改审核"); }

        // Round19 REQ-19-03: 读取 Intent 预设分配人参数
        // Round23 REQ-23-04: 废止锁定语义，仅保留预设定位（默认自己、可选改）
        presetAssigneeId = getIntent().getLongExtra("assigneeId", -1);
        if (presetAssigneeId <= 0) presetAssigneeId = null;

        // 候选加载期间禁用负责人入口，避免展示未加载的选择页。
        binding.buttonAssignees.setEnabled(false);
        binding.buttonAssignees.setOnClickListener(v -> showAssigneePicker());
        loadAssigneeOptions();
        String role = TokenManager.getInstance(this).getRole();
        binding.textTaskReviewHint.setText("ADMIN".equals(role) || "FOUNDER".equals(role)
                ? "可派发给公司内非管理员人员；当前职级的任务操作免审。"
                : "可派发给公司内非管理员人员；创建、编辑和删除须经对应上级审核后生效。");

        selectedStartTime = savedInstanceState == null ? TaskDateTimes.format(new Date())
                : savedInstanceState.getString("taskStartTime", TaskDateTimes.format(new Date()));
        selectedEndTime = savedInstanceState == null ? null : savedInstanceState.getString("taskEndTime");
        renderTimes();
        binding.textStartTime.setOnClickListener(v -> showDateTimePicker(false));
        binding.textEndTime.setOnClickListener(v -> showDateTimePicker(true));

        binding.btnCreateTask.setOnClickListener(v -> handleCreateTask());

        setupAutoSave();
        binding.taskTags.setOnChanged(this::onContentChanged);
        binding.rgUrgency.setOnCheckedChangeListener((group, checkedId) -> onContentChanged());
        if (savedInstanceState != null) {
            taskLoaded = savedInstanceState.getBoolean("taskLoaded");
            Object ids = savedInstanceState.getSerializable("taskTagIds");
            if (ids instanceof java.util.ArrayList) binding.taskTags.setSelectedIds((java.util.ArrayList<Long>) ids);
            selectedAssigneeIds.clear();
            Object people = savedInstanceState.getSerializable("taskAssigneeIds");
            if (people instanceof java.util.ArrayList<?>) for (Object value : (java.util.ArrayList<?>) people) { Long id = com.example.project_pandora.core.util.NumberCompat.toLong(value); if (id != null) selectedAssigneeIds.add(id); }
            ArrayList<String> names = savedInstanceState.getStringArrayList("taskAssigneeNames");
            int index = 0;
            for (Long id : selectedAssigneeIds) {
                if (!assigneeIds.contains(id)) {
                    assigneeIds.add(id);
                    assigneeNames.add(names != null && index < names.size() ? names.get(index) : "用户 " + id);
                }
                index++;
            }
            renderAssignees();
        }
        if (savedInstanceState == null && !editMode) checkDraftRestore();
        if (editMode && editTaskId > 0 && (savedInstanceState == null || !savedInstanceState.getBoolean("taskLoaded") || originalEditState == null)) loadTaskForEdit();
    }

    private void setupAutoSave() {
        if (!editMode) autoSaveManager = new AutoSaveManager(this, "TASK",
                () -> {
                    Map<String, Object> content = new HashMap<>();
                    content.put("editorMode", "CREATE");
                    content.put("name", binding.editTaskName.getText().toString());
                    content.put("content", binding.editTaskContent.getText().toString()); // R39 REQ-C07: 草稿覆盖正文
                    content.put("startTime", selectedStartTime);
                    content.put("endTime", selectedEndTime);
                    content.put("tagIds", binding.taskTags.getSelectedIds());
                    boolean isUrgent = binding.rgUrgency.getCheckedRadioButtonId() == R.id.rb_urgent;
                    content.put("isUrgent", isUrgent);
                    content.put("assigneeIds", new ArrayList<>(selectedAssigneeIds));
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

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (!isRestoringDraft) onContentChanged();
            }
        };
        binding.editTaskName.addTextChangedListener(watcher);
        binding.editTaskContent.addTextChangedListener(watcher);
    }

    public void onContentChanged() {
        if (!isRestoringDraft && autoSaveManager != null) {
            autoSaveManager.onContentChanged();
        }
    }

    private void checkDraftRestore() {
        disposables.add(draftRepository.getDraft("TASK")
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        draftData -> {
                            if (draftData != null && TaskFormState.isCreationDraft(draftData.get("content"))) {
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
        Object tagIds = content.get("tagIds");
        if (tagIds instanceof List) {
            List<Long> ids = new ArrayList<>();
            for (Object item : (List<?>) tagIds) {
                Long id = com.example.project_pandora.core.util.NumberCompat.toLong(item);
                if (id != null) ids.add(id);
            }
            binding.taskTags.setSelectedIds(ids);
        }
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
        Object selected = content.get("assigneeIds");
        if (selected instanceof List<?>) for (Object value : (List<?>) selected) { Long id = com.example.project_pandora.core.util.NumberCompat.toLong(value); if (id != null) selectedAssigneeIds.add(id); }
        Object legacy = content.get("assigneeId"); Long legacyId = com.example.project_pandora.core.util.NumberCompat.toLong(legacy); if (legacyId != null) selectedAssigneeIds.add(legacyId);
        renderAssignees();
    }

    private void loadAssigneeOptions() {
        Long currentUserId = TokenManager.getInstance(this).getUserId();
        String currentRole = TokenManager.getInstance(this).getRole();
        String currentUserName = "自己";

        assigneeIds.clear();
        assigneeNames.clear();
        assigneePeople.clear();
        if (!"ADMIN".equals(currentRole) && currentUserId != null && currentUserId > 0) {
            assigneeIds.add(currentUserId);
            assigneeNames.add(currentUserName);
            Map<String, Object> self = new HashMap<>(); self.put("id", currentUserId); self.put("real_name", currentUserName); self.put("department_name", "我的部门"); self.put("role", currentRole); assigneePeople.add(self);
            if (!editMode && selectedAssigneeIds.isEmpty()) selectedAssigneeIds.add(currentUserId);
        }
        updateAssigneeControl();

        disposables.add(taskApi.getAssignees()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        response -> {
                            if (response.isSuccess() && response.getData() != null) {
                                for (Map<String, Object> user : response.getData()) {
                                    if (!com.example.project_pandora.ui.people.PeopleDirectory.isMobilePerson(user)) continue;
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
                                        if (idLong != null) {
                                            int existing = assigneeIds.indexOf(idLong);
                                            if (existing < 0) {
                                                assigneeIds.add(idLong);
                                                assigneeNames.add(name != null ? name.toString() : "用户" + idLong);
                                            } else if (name != null) {
                                                assigneeNames.set(existing, name.toString());
                                            }
                                            boolean listed = false;
                                            for (Map<String, Object> person : assigneePeople) {
                                                if (idLong.equals(com.example.project_pandora.core.util.NumberCompat.toLong(TaskTags.value(person, "id")))) {
                                                    listed = true; break;
                                                }
                                            }
                                            if (!listed) assigneePeople.add(user);
                                        }
                                    }
                                }
                                updateAssigneeControl();
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

    private void updateAssigneeControl() {
        if (!editMode && presetAssigneeId != null && assigneeIds.contains(presetAssigneeId)) {
            selectedAssigneeIds.clear(); selectedAssigneeIds.add(presetAssigneeId); presetAssigneeId = null;
        }
        binding.buttonAssignees.setEnabled(!assigneePeople.isEmpty());
        renderAssignees();
    }

    private void renderAssignees() {
        if (binding == null) return;
        List<String> names = new ArrayList<>();
        for (Long id : selectedAssigneeIds) {
            int index = assigneeIds.indexOf(id);
            names.add(index >= 0 && index < assigneeNames.size() ? assigneeNames.get(index) : "用户 " + id);
        }
        binding.buttonAssignees.setText(names.isEmpty() ? "按部门搜索并选择负责人" : "已选 " + names.size() + " 人，点击调整");
        binding.selectedAssigneeChips.removeAllViews();
        for (String name : names) {
            com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
            chip.setText(name); chip.setClickable(false); chip.setCheckable(false);
            binding.selectedAssigneeChips.addView(chip);
        }
    }

    private void showAssigneePicker() {
        leaveTextEditing();
        if (assigneePeople.isEmpty()) return;
        long[] ids = new long[selectedAssigneeIds.size()]; int index = 0;
        for (long id : selectedAssigneeIds) ids[index++] = id;
        assigneesLauncher.launch(new android.content.Intent(this, TaskAssigneesActivity.class)
                .putExtra("selectedIds", ids).putExtra("taskId", editMode ? editTaskId : -1));
    }

    @SuppressWarnings("unchecked")
    private void loadTaskForEdit() {
        binding.btnCreateTask.setEnabled(false);
        disposables.add(taskApi.getTask(editTaskId).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(response -> {
            if (!response.isSuccess() || response.getData() == null) { Toast.makeText(this, response.getMessage(), Toast.LENGTH_LONG).show(); finish(); return; }
            Map<String, Object> task = response.getData();
            binding.editTaskName.setText(java.util.Objects.toString(TaskTags.value(task, "name"), ""));
            binding.editTaskContent.setText(java.util.Objects.toString(TaskTags.value(task, "content"), ""));
            selectedStartTime = TaskDateTimes.restore(TaskTags.value(task, "start_time"), false);
            selectedEndTime = TaskDateTimes.restore(TaskTags.value(task, "end_time"), true); renderTimes();
            binding.rgUrgency.check(com.example.project_pandora.core.util.NumberCompat.toBool(TaskTags.value(task, "is_urgent")) ? R.id.rb_urgent : R.id.rb_not_urgent);
            binding.taskTags.setSelectedIds(com.example.project_pandora.core.util.TaskTags.ids(task));
            selectedAssigneeIds.clear();
            selectedAssigneeIds.addAll(com.example.project_pandora.core.util.TaskAssignees.ids(task));
            for (Map<String, Object> person : com.example.project_pandora.core.util.TaskAssignees.people(task)) {
                Long id = com.example.project_pandora.core.util.NumberCompat.toLong(TaskTags.value(person, "id"));
                if (id != null && !assigneeIds.contains(id)) {
                    assigneeIds.add(id); assigneeNames.add(com.example.project_pandora.ui.people.PeopleDirectory.name(person)); assigneePeople.add(person);
                }
            }
            taskLoaded = true; renderAssignees();
            originalEditState = formState();
            binding.btnCreateTask.setEnabled(true);
        }, error -> { Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show(); finish(); }));
    }

    private void renderTimes() {
        binding.textStartTime.setText(selectedStartTime == null ? "点击选择日期和时间" : selectedStartTime);
        binding.textEndTime.setText(selectedEndTime == null ? "点击选择日期和时间" : selectedEndTime);
    }

    private void showDateTimePicker(boolean end) {
        leaveTextEditing();
        if ((datePicker != null && datePicker.isShowing()) || (timePicker != null && timePicker.isShowing())) return;
        Calendar cal = Calendar.getInstance();
        Date previous = TaskDateTimes.parse(end ? selectedEndTime : selectedStartTime);
        if (previous == null && end) previous = TaskDateTimes.parse(selectedStartTime);
        if (previous != null) cal.setTime(previous);
        datePicker = QuickDatePickerDialog.create(this, cal, date -> {
            cal.set(date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH));
            timePicker = new WheelTimePickerDialog(this, end ? "结束时间" : "开始时间",
                    cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), (hour, minute) -> {
                cal.set(Calendar.HOUR_OF_DAY, hour);
                cal.set(Calendar.MINUTE, minute);
                String value = TaskDateTimes.format(cal.getTime());
                if (end) selectedEndTime = value; else selectedStartTime = value;
                renderTimes();
                onContentChanged();
            });
            timePicker.show();
        });
        datePicker.show();
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable("originalEditState", originalEditState);
        outState.putBoolean("taskLoaded", taskLoaded);
        outState.putString("taskStartTime", selectedStartTime);
        outState.putString("taskEndTime", selectedEndTime);
        outState.putSerializable("taskTagIds", binding.taskTags.getSelectedIds());
        outState.putSerializable("taskAssigneeIds", new ArrayList<>(selectedAssigneeIds));
        ArrayList<String> names = new ArrayList<>();
        for (Long id : selectedAssigneeIds) {
            int index = assigneeIds.indexOf(id);
            names.add(index >= 0 ? assigneeNames.get(index) : "用户 " + id);
        }
        outState.putStringArrayList("taskAssigneeNames", names);
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
            Toast.makeText(this, "截止时间必须晚于开始时间，请检查任务排期", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isUrgent = binding.rgUrgency.getCheckedRadioButtonId() == R.id.rb_urgent;
        if (selectedAssigneeIds.isEmpty()) {
            Toast.makeText(this, "请至少选择一位负责人", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> request = new HashMap<>();
        request.put("name", name);
        request.put("content", content); // R39 REQ-C06: 正文落库
        request.put("assigneeIds", new ArrayList<>(selectedAssigneeIds));
        request.put("isUrgent", isUrgent);
        request.put("startTime", selectedStartTime + ":00");
        request.put("endTime", selectedEndTime + ":00");
        request.put("tagIds", binding.taskTags.getSelectedIds());

        binding.btnCreateTask.setEnabled(false);
        binding.btnCreateTask.setText(editMode ? "提交中..." : "创建中...");

        disposables.add((editMode ? taskApi.updateTask(editTaskId, request) : taskApi.createTask(request))
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
                                if (!editMode) disposables.add(draftRepository.deleteDraft("TASK")
                                        .subscribe(r -> {}, Throwable::printStackTrace));
                                finish();
                            } else {
                                Toast.makeText(this, response.getMessage(), Toast.LENGTH_SHORT).show();
                                binding.btnCreateTask.setEnabled(true);
                                binding.btnCreateTask.setText(editMode ? "提交修改审核" : "创建任务");
                            }
                        },
                        error -> {
                            Toast.makeText(this, (editMode ? "修改提交失败: " : "创建失败: ") + error.getMessage(), Toast.LENGTH_SHORT).show();
                            binding.btnCreateTask.setEnabled(true);
                            binding.btnCreateTask.setText(editMode ? "提交修改审核" : "创建任务");
                        }
                ));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding != null) binding.taskTags.reload();
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
        if (exitDialog != null) exitDialog.dismiss();
        if (datePicker != null) datePicker.dismiss();
        if (timePicker != null) timePicker.dismiss();
        super.onDestroy();
        if (autoSaveManager != null) {
            autoSaveManager.stop();
        }
        disposables.clear();
    }

    private TaskFormState formState() {
        return new TaskFormState(binding.editTaskName.getText().toString(), binding.editTaskContent.getText().toString(),
                selectedStartTime, selectedEndTime, binding.rgUrgency.getCheckedRadioButtonId() == R.id.rb_urgent,
                binding.taskTags.getSelectedIds(), selectedAssigneeIds);
    }

    private void requestExit() {
        leaveTextEditing();
        if (editMode && !saveCompleted && originalEditState != null && !originalEditState.equals(formState())) {
            if (exitDialog != null && exitDialog.isShowing()) return;
            exitDialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setMessage("当前修改未保存，是否退出？")
                    .setNegativeButton("继续编辑", null)
                    .setPositiveButton("退出", (dialog, which) -> finish()).create();
            exitDialog.show();
        } else finish();
    }

    private void leaveTextEditing() {
        android.view.View focused = getCurrentFocus();
        if (!(focused instanceof android.widget.EditText)) return;
        android.view.inputmethod.InputMethodManager keyboard = (android.view.inputmethod.InputMethodManager)
                getSystemService(INPUT_METHOD_SERVICE);
        if (keyboard != null) keyboard.hideSoftInputFromWindow(focused.getWindowToken(), 0);
        focused.clearFocus();
        // Keep focus outside ScrollView so window restoration cannot scroll back to an input.
        binding.getRoot().requestFocus();
    }

    @Override public boolean dispatchTouchEvent(android.view.MotionEvent event) {
        if (binding != null && event.getActionMasked() == android.view.MotionEvent.ACTION_DOWN) {
            android.view.View focused = getCurrentFocus();
            if (focused instanceof android.widget.EditText) {
                android.graphics.Rect bounds = new android.graphics.Rect();
                focused.getGlobalVisibleRect(bounds);
                if (!bounds.contains((int) event.getRawX(), (int) event.getRawY())) leaveTextEditing();
            }
        }
        return super.dispatchTouchEvent(event);
    }
}

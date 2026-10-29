package com.example.project_pandora.ui.log;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project_pandora.core.autosave.AutoSaveManager;
import com.example.project_pandora.core.autosave.DraftRestoreDialog;
import com.example.project_pandora.core.util.BackToolbarHelper;
import com.example.project_pandora.core.util.LogDateWindow;
import com.example.project_pandora.R;
import com.example.project_pandora.core.sync.SyncRouteResult;
import com.example.project_pandora.databinding.ActivityLogCreateBinding;
import com.example.project_pandora.repository.DraftRepository;
import com.example.project_pandora.repository.LogRepository;
import com.example.project_pandora.repository.SyncRoutingRepository;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class LogCreateActivity extends AppCompatActivity {

    private ActivityLogCreateBinding binding;
    private LogRepository logRepository;
    private SyncRoutingRepository syncRoutingRepository;
    private DraftRepository draftRepository;
    private AutoSaveManager autoSaveManager;
    private String selectedDate;
    private boolean syncingDateControls = false;
    private boolean isEditMode = false;
    private long editLogId = -1;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private boolean isRestoringDraft = false;
    private boolean saveCompleted = false;

    /** DEF-32-02 修复：提交超 5s 的中间反馈定时器（成功/失败回调取消，onDestroy 兜底清理） */
    private final android.os.Handler saveHintHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable saveHintRunnable = () ->
            Toast.makeText(this, getString(com.example.project_pandora.R.string.log_save_retrying_hint),
                    Toast.LENGTH_LONG).show();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLogCreateBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        logRepository = new LogRepository(this);
        syncRoutingRepository = new SyncRoutingRepository(this);
        draftRepository = new DraftRepository(this);

        BackToolbarHelper.setupBack(binding.toolbar, this::finish);

        selectedDate = LogDateWindow.daysAgo(0);
        binding.textLogDate.setText(selectedDate);
        binding.textLogDate.setOnClickListener(v -> showDatePicker());

        checkEditMode();
        setupDateControls();
        setupAutoSave();
        checkDraftRestore();

        binding.btnSave.setOnClickListener(v -> handleSave());
    }

    private void setupAutoSave() {
        autoSaveManager = new AutoSaveManager(this, "LOG",
                () -> {
                    Map<String, Object> content = new HashMap<>();
                    content.put("workItem", binding.editWorkItem.getText().toString());
                    content.put("workContent", binding.editWorkContent.getText().toString());
                    content.put("logDate", selectedDate);
                    return content;
                },
                status -> {
                    switch (status) {
                        case SAVING:
                            showAutoSaveStatus("保存中...");
                            break;
                        case SAVED:
                            String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
                            showAutoSaveStatus("已保存 " + time);
                            break;
                        case SAVE_FAILED:
                            showAutoSaveStatus("保存失败");
                            break;
                        default:
                            break;
                    }
                });

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (!isRestoringDraft) onContentChanged();
            }
        };
        binding.editWorkItem.addTextChangedListener(textWatcher);
        binding.editWorkContent.addTextChangedListener(textWatcher);

    }

    public void onContentChanged() {
        if (autoSaveManager != null) {
            autoSaveManager.onContentChanged();
        }
    }

    private void showAutoSaveStatus(String text) {
        binding.textAutoSaveStatus.setText(text);
        if (binding.textAutoSaveStatus.getVisibility() != android.view.View.VISIBLE) {
            binding.textAutoSaveStatus.setAlpha(0f);
            binding.textAutoSaveStatus.setVisibility(android.view.View.VISIBLE);
            binding.textAutoSaveStatus.animate().alpha(1f).setDuration(200).start();
        }
    }

    private void checkDraftRestore() {
        if (isEditMode) return;
        disposables.add(draftRepository.getDraft("LOG")
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
                                        () -> disposables.add(draftRepository.deleteDraft("LOG")
                                                .subscribe(r -> {}, Throwable::printStackTrace)));
                            }
                        },
                        error -> {}
                ));
    }

    @SuppressWarnings("unchecked")
    private void restoreDraft(Map<String, Object> content) {
        if (content == null) return;
        Object workItem = content.get("workItem");
        if (workItem != null) binding.editWorkItem.setText(workItem.toString());

        Object workContent = content.get("workContent");
        if (workContent != null) binding.editWorkContent.setText(workContent.toString());

        Object logDate = content.get("logDate");
        if (logDate != null) {
            selectedDate = LogDateWindow.isWritable(logDate.toString()) ? logDate.toString() : LogDateWindow.daysAgo(0);
            binding.textLogDate.setText(selectedDate);
            updateDateControls();
        }
    }

    private void checkEditMode() {
        Intent intent = getIntent();
        isEditMode = intent.getBooleanExtra("editMode", false);
        if (isEditMode) {
            editLogId = intent.getLongExtra("logId", -1);
            binding.toolbar.setTitle("编辑日志");
            binding.btnSave.setText("更新");

            String workItem = intent.getStringExtra("workItem");
            if (workItem != null) binding.editWorkItem.setText(workItem);

            String workContent = intent.getStringExtra("workContent");
            if (workContent != null) binding.editWorkContent.setText(workContent);

            String logDate = intent.getStringExtra("logDate");
            if (logDate != null) {
                selectedDate = logDate;
                binding.textLogDate.setText(selectedDate);
            }
        }
    }

    private void setupDateControls() {
        updateDateControls();
        binding.chipGroupDate.setOnCheckedStateChangeListener((group, checked) -> {
            if (syncingDateControls || isEditMode || checked.isEmpty()) return;
            int days = checked.get(0) == R.id.chipYesterday ? 1 : checked.get(0) == R.id.chipTomorrow ? -1 : 0;
            selectDate(LogDateWindow.daysAgo(days));
        });
    }

    private void updateDateControls() {
        syncingDateControls = true;
        binding.chipGroupDate.check(selectedDate.equals(LogDateWindow.daysAgo(1)) ? R.id.chipYesterday
                : selectedDate.equals(LogDateWindow.daysAgo(-1)) ? R.id.chipTomorrow : R.id.chipToday);
        binding.chipToday.setEnabled(!isEditMode);
        binding.chipYesterday.setEnabled(!isEditMode);
        binding.chipTomorrow.setEnabled(!isEditMode);
        binding.textLogDate.setText(selectedDate + (isEditMode ? " · 编辑时日期锁定" : ""));
        binding.textLogDate.setEnabled(!isEditMode);
        if (!LogDateWindow.isWritable(selectedDate)) {
            binding.btnSave.setEnabled(false);
            binding.btnSave.setText("已超出可编辑日期范围");
        }
        syncingDateControls = false;
    }

    private void selectDate(String date) {
        if (!LogDateWindow.isWritable(date) || isEditMode) return;
        String previous = selectedDate;
        selectedDate = date;
        updateDateControls();
        onContentChanged();
        disposables.add(logRepository.listLogs(date, date, null)
                .observeOn(AndroidSchedulers.mainThread()).subscribe(logs -> {
                    if (!date.equals(selectedDate) || isFinishing()) return;
                    if (logs.isEmpty()) return;
                    new android.app.AlertDialog.Builder(this).setTitle("该日期已有日志")
                            .setMessage("每人每天一篇日志。是否打开已有日志进行编辑？当前未提交内容不会覆盖原日志。")
                            .setPositiveButton("打开编辑", (dialog, which) -> {
                                Map<String, Object> existing = logs.get(0);
                                Long id = com.example.project_pandora.core.util.NumberCompat.toLong(existing.get("id"));
                                if (id == null) return;
                                editLogId = id; isEditMode = true;
                                binding.editWorkItem.setText(existing.get("work_item") == null ? "" : existing.get("work_item").toString());
                                binding.editWorkContent.setText(existing.get("work_content") == null ? "" : existing.get("work_content").toString());
                                binding.toolbar.setTitle("编辑日志"); binding.btnSave.setText("更新日志"); updateDateControls();
                            }).setNegativeButton("取消", (dialog, which) -> { selectedDate = previous; updateDateControls(); })
                            .setOnCancelListener(dialog -> { selectedDate = previous; updateDateControls(); }).show();
                }, error -> { /* Offline saves still go through the queue; the server enforces date uniqueness. */ }));
    }

    private void showDatePicker() {
        if (isEditMode) return;
        java.util.TimeZone zone = java.util.TimeZone.getTimeZone("Asia/Shanghai");
        Calendar selected = Calendar.getInstance(zone);
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
            format.setTimeZone(zone);
            Date parsed = format.parse(selectedDate);
            if (parsed != null) selected.setTime(parsed);
        } catch (java.text.ParseException ignored) {
        }
        Calendar today = Calendar.getInstance(zone);
        today.set(Calendar.HOUR_OF_DAY, 0); today.set(Calendar.MINUTE, 0); today.set(Calendar.SECOND, 0); today.set(Calendar.MILLISECOND, 0);
        Calendar min = (Calendar) today.clone();
        min.add(Calendar.DAY_OF_YEAR, -1);
        Calendar max = (Calendar) today.clone();
        max.set(Calendar.HOUR_OF_DAY, 23); max.set(Calendar.MINUTE, 59); max.set(Calendar.SECOND, 59); max.set(Calendar.MILLISECOND, 999);
        max.add(Calendar.DAY_OF_YEAR, 1);
        DatePickerDialog picker = new DatePickerDialog(this, (view, year, month, day) ->
                selectDate(String.format(Locale.ROOT, "%04d-%02d-%02d", year, month + 1, day)),
                selected.get(Calendar.YEAR), selected.get(Calendar.MONTH), selected.get(Calendar.DAY_OF_MONTH));
        picker.getDatePicker().setMinDate(min.getTimeInMillis());
        picker.getDatePicker().setMaxDate(max.getTimeInMillis());
        picker.show();
    }

    private void handleSave() {
        if (!LogDateWindow.isWritable(selectedDate)) {
            Toast.makeText(this, "仅可填写或编辑昨天、今天、明天的日志", Toast.LENGTH_LONG).show(); return;
        }
        String workItem = binding.editWorkItem.getText().toString().trim();
        if (workItem.isEmpty()) {
            Toast.makeText(this, "请输入日志标题", Toast.LENGTH_SHORT).show();
            return;
        }
        String workContent = binding.editWorkContent.getText().toString();

        binding.btnSave.setEnabled(false);
        binding.btnSave.setText("保存中...");

        // DEF-32-02 修复：提交 5s 未完成给出中间反馈（不可达/重试窗口不再无声锁死）
        saveHintHandler.removeCallbacks(saveHintRunnable);
        saveHintHandler.postDelayed(saveHintRunnable, 5000);

        if (isEditMode && editLogId > 0) {
            disposables.add(syncRoutingRepository.editLog(editLogId, workItem, workContent, null, null, selectedDate)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            result -> {
                                saveHintHandler.removeCallbacks(saveHintRunnable);
                                Toast.makeText(this, result.getMessage(), Toast.LENGTH_SHORT).show();
                                saveCompleted = true;
                                if (autoSaveManager != null) {
                                    autoSaveManager.stop();
                                }
                                disposables.add(draftRepository.deleteDraft("LOG")
                                        .subscribe(r -> {}, Throwable::printStackTrace));
                                finish();
                            },
                            error -> {
                                saveHintHandler.removeCallbacks(saveHintRunnable);
                                Toast.makeText(this, "更新失败: " + mapError(error), Toast.LENGTH_SHORT).show();
                                binding.btnSave.setEnabled(true);
                                binding.btnSave.setText("更新");
                            }
                    ));
        } else {
            disposables.add(syncRoutingRepository.createLog(workItem, workContent, "PENDING", null, selectedDate)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            result -> {
                                saveHintHandler.removeCallbacks(saveHintRunnable);
                                Toast.makeText(this, result.getMessage(), Toast.LENGTH_SHORT).show();
                                saveCompleted = true;
                                if (autoSaveManager != null) {
                                    autoSaveManager.stop();
                                }
                                disposables.add(draftRepository.deleteDraft("LOG")
                                        .subscribe(r -> {}, Throwable::printStackTrace));
                                finish();
                            },
                            error -> {
                                saveHintHandler.removeCallbacks(saveHintRunnable);
                                Toast.makeText(this, "保存失败: " + mapError(error), Toast.LENGTH_SHORT).show();
                                binding.btnSave.setEnabled(true);
                                binding.btnSave.setText("保存");
                            }
                    ));
        }
    }

    /**
     * DEF-31-01: 错误文案统一走 ErrorMessageMapper 映射。
     * BusinessException（含 HttpErrorParser 转换产物）按 code 精确映射，其余保留原始信息。
     */
    private String mapError(Throwable error) {
        if (error instanceof com.example.project_pandora.core.exception.BusinessException) {
            com.example.project_pandora.core.exception.BusinessException be =
                    (com.example.project_pandora.core.exception.BusinessException) error;
            return com.example.project_pandora.core.util.ErrorMessageMapper.map(be.getCode(), be.getMessage());
        }
        return error.getMessage() != null ? error.getMessage() : "未知错误";
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
        saveHintHandler.removeCallbacks(saveHintRunnable);
        if (autoSaveManager != null) {

            autoSaveManager.stop();
        }
        disposables.clear();
    }
}

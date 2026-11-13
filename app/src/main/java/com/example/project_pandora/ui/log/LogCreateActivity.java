package com.example.project_pandora.ui.log;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import com.example.project_pandora.ui.PandoraActivity;

import com.example.project_pandora.core.autosave.AutoSaveManager;
import com.example.project_pandora.core.autosave.DraftRestoreDialog;
import com.example.project_pandora.core.util.BackToolbarHelper;
import com.example.project_pandora.core.util.LogDateWindow;
import com.example.project_pandora.R;
import com.example.project_pandora.core.sync.SyncRouteResult;
import com.example.project_pandora.databinding.ActivityLogCreateBinding;
import com.example.project_pandora.repository.DraftRepository;
import com.example.project_pandora.repository.DailyItemRepository;
import com.example.project_pandora.repository.LogRepository;
import com.example.project_pandora.ui.home.DailyItems;
import com.example.project_pandora.repository.SyncRoutingRepository;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class LogCreateActivity extends PandoraActivity {

    private ActivityLogCreateBinding binding;
    private LogRepository logRepository;
    private SyncRoutingRepository syncRoutingRepository;
    private DraftRepository draftRepository;
    private DailyItemRepository dailyItemRepository;
    private AutoSaveManager autoSaveManager;
    private String selectedDate;
    private boolean syncingDateControls = false;
    private boolean isEditMode = false;
    private long editLogId = -1;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private boolean isRestoringDraft = false;
    private boolean saveCompleted = false;
    private String dailyPrefill;

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
        dailyItemRepository = new DailyItemRepository(this);

        BackToolbarHelper.setupBack(binding.toolbar, this::finish);

        selectedDate = LogDateWindow.daysAgo(0);
        binding.textLogDate.setText(selectedDate);
        binding.textLogDate.setOnClickListener(v -> showDatePicker());

        checkEditMode();
        setupDateControls();
        setupAutoSave();
        checkDraftRestore();
        // Do not make daily-item prefill wait for the independent draft request.
        // Whichever response arrives first still preserves any non-empty user/draft text.
        loadDailyItemsForDate(selectedDate);

        binding.btnSave.setOnClickListener(v -> handleSave());
    }

    private void setupAutoSave() {
        autoSaveManager = new AutoSaveManager(this, "LOG",
                () -> {
                    Map<String, Object> content = new HashMap<>();
                    content.put("workItem", com.example.project_pandora.core.util.LogEntryRules.title(selectedDate));
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
                                                .subscribe(r -> loadDailyItemsForDate(selectedDate), error -> loadDailyItemsForDate(selectedDate))));
                            } else loadDailyItemsForDate(selectedDate);
                        },
                        error -> loadDailyItemsForDate(selectedDate)
                ));
    }

    @SuppressWarnings("unchecked")
    private void restoreDraft(Map<String, Object> content) {
        if (content == null) return;
        Object workContent = content.get("workContent");
        if (workContent != null) binding.editWorkContent.setText(workContent.toString());

        Object logDate = content.get("logDate");
        if (logDate != null) {
            selectedDate = LogDateWindow.isWritable(logDate.toString()) ? logDate.toString() : LogDateWindow.daysAgo(0);
            binding.textLogDate.setText(selectedDate);
            updateDateControls();
        }
        dailyPrefill = null;
        // A restored empty draft must still receive the selected day's items. The loader
        // keeps non-empty draft/user text intact, so this cannot overwrite their content.
        loadDailyItemsForDate(selectedDate);
    }

    private void checkEditMode() {
        Intent intent = getIntent();
        isEditMode = intent.getBooleanExtra("editMode", false);
        if (isEditMode) {
            editLogId = intent.getLongExtra("logId", -1);
            binding.toolbar.setTitle("编辑日志");
            binding.btnSave.setText("更新");

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
            int days = checked.get(0) == R.id.chipDayBeforeYesterday ? 2 : checked.get(0) == R.id.chipYesterday ? 1 : 0;
            selectDate(LogDateWindow.daysAgo(days));
        });
    }

    private void updateDateControls() {
        syncingDateControls = true;
        binding.textLogHeading.setText(com.example.project_pandora.core.util.LogEntryRules.title(selectedDate));
        binding.chipGroupDate.check(selectedDate.equals(LogDateWindow.daysAgo(2)) ? R.id.chipDayBeforeYesterday
                : selectedDate.equals(LogDateWindow.daysAgo(1)) ? R.id.chipYesterday : R.id.chipToday);
        binding.chipToday.setEnabled(!isEditMode);
        binding.chipYesterday.setEnabled(!isEditMode);
        binding.chipDayBeforeYesterday.setEnabled(!isEditMode);
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
        if (!date.equals(previous) && dailyPrefill != null && dailyPrefill.equals(binding.editWorkContent.getText().toString())) {
            binding.editWorkContent.setText("");
            dailyPrefill = null;
        }
        selectedDate = date;
        updateDateControls();
        onContentChanged();
        disposables.add(logRepository.listLogs(date, date, null)
                .observeOn(AndroidSchedulers.mainThread()).subscribe(logs -> {
                    if (!date.equals(selectedDate) || isFinishing()) return;
                    if (logs.isEmpty()) { loadDailyItemsForDate(date); return; }
                    new android.app.AlertDialog.Builder(this).setTitle("该日期已有日志")
                            .setMessage("每人每天一篇日志。是否打开已有日志进行编辑？当前未提交内容不会覆盖原日志。")
                            .setPositiveButton("打开编辑", (dialog, which) -> {
                                Map<String, Object> existing = logs.get(0);
                                Long id = com.example.project_pandora.core.util.NumberCompat.toLong(existing.get("id"));
                                if (id == null) return;
                                Intent intent = new Intent(this, LogCreateActivity.class);
                                intent.putExtra("editMode", true);
                                intent.putExtra("logId", id.longValue());
                                intent.putExtra("workItem", java.util.Objects.toString(existing.get("work_item"), ""));
                                intent.putExtra("workContent", java.util.Objects.toString(existing.get("work_content"), ""));
                                intent.putExtra("logDate", date);
                                startActivity(intent);
                            }).setNegativeButton("取消", (dialog, which) -> { selectedDate = previous; updateDateControls(); loadDailyItemsForDate(previous); })
                            .setOnCancelListener(dialog -> { selectedDate = previous; updateDateControls(); loadDailyItemsForDate(previous); }).show();
                }, error -> loadDailyItemsForDate(date)));
    }

    private void loadDailyItemsForDate(String date) {
        if (isEditMode || date == null) return;
        disposables.add(dailyItemRepository.listForDate(date).observeOn(AndroidSchedulers.mainThread()).subscribe(items -> {
            if (!date.equals(selectedDate) || isFinishing() || items.isEmpty()) return;
            String current = binding.editWorkContent.getText().toString();
            if (!current.trim().isEmpty() && !current.equals(dailyPrefill)) return;
            StringBuilder text = new StringBuilder("当日事项：");
            for (Map<String, Object> item : items) {
                String content = DailyItems.content(item).trim();
                if (!content.isEmpty()) text.append("\n- ").append(content);
            }
            if (text.length() == "当日事项：".length()) return;
            dailyPrefill = text.toString();
            isRestoringDraft = true;
            binding.editWorkContent.setText(dailyPrefill);
            binding.editWorkContent.setSelection(binding.editWorkContent.length());
            isRestoringDraft = false;
        }, error -> { /* 日志仍可离线填写，事项预填失败不阻塞编辑。 */ }));
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
        min.add(Calendar.DAY_OF_YEAR, -2);
        Calendar max = (Calendar) today.clone();
        max.set(Calendar.HOUR_OF_DAY, 23); max.set(Calendar.MINUTE, 59); max.set(Calendar.SECOND, 59); max.set(Calendar.MILLISECOND, 999);
        DatePickerDialog picker = new DatePickerDialog(this, (view, year, month, day) ->
                selectDate(String.format(Locale.ROOT, "%04d-%02d-%02d", year, month + 1, day)),
                selected.get(Calendar.YEAR), selected.get(Calendar.MONTH), selected.get(Calendar.DAY_OF_MONTH));
        picker.getDatePicker().setMinDate(min.getTimeInMillis());
        picker.getDatePicker().setMaxDate(max.getTimeInMillis());
        picker.show();
    }

    private void handleSave() {
        if (!LogDateWindow.isWritable(selectedDate)) {
            Toast.makeText(this, "仅可填写或编辑前天、昨天、今天的日志", Toast.LENGTH_LONG).show(); return;
        }
        String workItem = com.example.project_pandora.core.util.LogEntryRules.title(selectedDate);
        String workContent = binding.editWorkContent.getText().toString();
        if (!com.example.project_pandora.core.util.LogEntryRules.hasBody(workContent)) {
            binding.editWorkContent.setError("请填写日志正文");
            binding.editWorkContent.requestFocus();
            return;
        }

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
            String saveDate = selectedDate;
            disposables.add(logRepository.listLogs(saveDate, saveDate, null)
                    .onErrorReturnItem(java.util.Collections.emptyList())
                    .flatMap(logs -> logs.isEmpty()
                            ? syncRoutingRepository.createLog(workItem, workContent, "PENDING", null, saveDate)
                            : io.reactivex.rxjava3.core.Observable.error(new com.example.project_pandora.core.exception.BusinessException(4030, "该日期已有日志，请打开编辑")))
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
                                if (saveDate.equals(selectedDate) && error instanceof com.example.project_pandora.core.exception.BusinessException
                                        && ((com.example.project_pandora.core.exception.BusinessException) error).getCode() == 4030) selectDate(saveDate);
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

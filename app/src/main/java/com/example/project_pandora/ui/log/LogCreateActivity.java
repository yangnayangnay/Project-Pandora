package com.example.project_pandora.ui.log;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project_pandora.core.autosave.AutoSaveManager;
import com.example.project_pandora.core.autosave.DraftRestoreDialog;
import com.example.project_pandora.core.template.TemplateMigrationHelper;
import com.example.project_pandora.core.util.BackToolbarHelper;
import com.example.project_pandora.core.sync.RoutePath;
import com.example.project_pandora.core.sync.SyncRouteResult;
import com.example.project_pandora.databinding.ActivityLogCreateBinding;
import com.example.project_pandora.repository.DraftRepository;
import com.example.project_pandora.repository.LogRepository;
import com.example.project_pandora.repository.SyncRoutingRepository;
import com.example.project_pandora.repository.TemplateRepository;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class LogCreateActivity extends AppCompatActivity {

    private ActivityLogCreateBinding binding;
    private LogRepository logRepository;
    private SyncRoutingRepository syncRoutingRepository;
    private DraftRepository draftRepository;
    private TemplateRepository templateRepository;
    private TemplateMigrationHelper templateMigrationHelper;
    private AutoSaveManager autoSaveManager;
    private String selectedDate;
    private boolean isEditMode = false;
    private long editLogId = -1;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private boolean isRestoringDraft = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLogCreateBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        logRepository = new LogRepository(this);
        syncRoutingRepository = new SyncRoutingRepository(this);
        draftRepository = new DraftRepository(this);
        templateRepository = new TemplateRepository(this);
        templateMigrationHelper = new TemplateMigrationHelper(this);

        BackToolbarHelper.setupBack(binding.toolbar, this::finish);

        selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        binding.textLogDate.setText(selectedDate);
        binding.textLogDate.setOnClickListener(v -> {
            showDatePicker();
            onContentChanged();
        });

        checkEditMode();
        setupTemplateButtons();
        setupAutoSave();
        checkDraftRestore();
        templateMigrationHelper.migrateIfNeeded();

        binding.btnSave.setOnClickListener(v -> handleSave());
    }

    private void setupAutoSave() {
        autoSaveManager = new AutoSaveManager(this, "LOG",
                () -> {
                    Map<String, Object> content = new HashMap<>();
                    content.put("workItem", binding.editWorkItem.getText().toString());
                    content.put("timeCost", binding.editTimeCost.getText().toString());
                    content.put("logDate", selectedDate);
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
        binding.editTimeCost.addTextChangedListener(textWatcher);
    }

    public void onContentChanged() {
        if (autoSaveManager != null) {
            autoSaveManager.onContentChanged();
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
        Object timeCost = content.get("timeCost");
        if (timeCost != null) binding.editTimeCost.setText(timeCost.toString());
        Object logDate = content.get("logDate");
        if (logDate != null) {
            selectedDate = logDate.toString();
            binding.textLogDate.setText(selectedDate);
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

            String timeCost = intent.getStringExtra("timeCost");
            if (timeCost != null) binding.editTimeCost.setText(timeCost);

            String logDate = intent.getStringExtra("logDate");
            if (logDate != null) {
                selectedDate = logDate;
                binding.textLogDate.setText(selectedDate);
            }
        }
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day);
            binding.textLogDate.setText(selectedDate);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void setupTemplateButtons() {
        binding.btnSaveTemplate.setOnClickListener(v -> saveAsTemplate());
        binding.btnUseTemplate.setOnClickListener(v -> showTemplateDialog());
    }

    private void saveAsTemplate() {
        String workItem = binding.editWorkItem.getText().toString().trim();
        if (workItem.isEmpty()) {
            Toast.makeText(this, "请先输入工作事项", Toast.LENGTH_SHORT).show();
            return;
        }
        String timeCost = binding.editTimeCost.getText().toString().trim();

        Map<String, Object> content = new HashMap<>();
        content.put("workItem", workItem);
        content.put("timeCost", timeCost);

        disposables.add(templateRepository.saveTemplate("LOG", workItem, content)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> Toast.makeText(this, "模板已保存", Toast.LENGTH_SHORT).show(),
                        error -> Toast.makeText(this, "保存模板失败: " + error.getMessage(), Toast.LENGTH_SHORT).show()
                ));
    }

    private void showTemplateDialog() {
        disposables.add(templateRepository.listTemplates("LOG")
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        templates -> {
                            if (templates == null || templates.isEmpty()) {
                                Toast.makeText(this, "暂无模板，请先保存模板", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            String[] items = new String[templates.size()];
                            for (int i = 0; i < templates.size(); i++) {
                                items[i] = (String) templates.get(i).get("name");
                            }
                            new AlertDialog.Builder(this)
                                    .setTitle("选择模板")
                                    .setItems(items, (dialog, which) -> {
                                        Map<String, Object> tpl = templates.get(which);
                                        Object contentObj = tpl.get("content");
                                        if (contentObj instanceof Map) {
                                            Map<String, Object> content = (Map<String, Object>) contentObj;
                                            isRestoringDraft = true;
                                            Object wi = content.get("workItem");
                                            if (wi != null) binding.editWorkItem.setText(wi.toString());
                                            Object tc = content.get("timeCost");
                                            if (tc != null) binding.editTimeCost.setText(tc.toString());
                                            isRestoringDraft = false;
                                        }
                                    })
                                    .setNeutralButton("删除模板", (dialog, which) -> showDeleteTemplateDialog(templates))
                                    .show();
                        },
                        error -> Toast.makeText(this, "加载模板失败: " + error.getMessage(), Toast.LENGTH_SHORT).show()
                ));
    }

    private void showDeleteTemplateDialog(List<Map<String, Object>> templates) {
        String[] names = new String[templates.size()];
        for (int i = 0; i < templates.size(); i++) {
            names[i] = (String) templates.get(i).get("name");
        }
        new AlertDialog.Builder(this)
                .setTitle("删除模板")
                .setItems(names, (dialog, which) -> {
                    Object idObj = templates.get(which).get("templateId");
                    if (idObj == null) return;
                    Long templateId = Long.parseLong(idObj.toString());
                    disposables.add(templateRepository.deleteTemplate(templateId)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    v -> Toast.makeText(this, "模板已删除", Toast.LENGTH_SHORT).show(),
                                    error -> Toast.makeText(this, "删除失败: " + error.getMessage(), Toast.LENGTH_SHORT).show()
                            ));
                })
                .show();
    }

    private void handleSave() {
        String workItem = binding.editWorkItem.getText().toString().trim();
        if (workItem.isEmpty()) {
            Toast.makeText(this, "请输入工作事项", Toast.LENGTH_SHORT).show();
            return;
        }

        Double timeCost = null;
        String timeCostStr = binding.editTimeCost.getText().toString().trim();
        if (!timeCostStr.isEmpty()) {
            try {
                timeCost = Double.parseDouble(timeCostStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "时间花费格式错误", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        binding.btnSave.setEnabled(false);
        binding.btnSave.setText("保存中...");

        if (isEditMode && editLogId > 0) {
            disposables.add(syncRoutingRepository.editLog(editLogId, workItem, "PENDING", timeCost, selectedDate)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            result -> {
                                Toast.makeText(this, result.getMessage(), Toast.LENGTH_SHORT).show();
                                finish();
                            },
                            error -> {
                                Toast.makeText(this, "更新失败: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                binding.btnSave.setEnabled(true);
                                binding.btnSave.setText("更新");
                            }
                    ));
        } else {
            disposables.add(syncRoutingRepository.createLog(workItem, "PENDING", timeCost, selectedDate)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            result -> {
                                Toast.makeText(this, result.getMessage(), Toast.LENGTH_SHORT).show();
                                finish();
                            },
                            error -> {
                                Toast.makeText(this, "保存失败: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                binding.btnSave.setEnabled(true);
                                binding.btnSave.setText("保存");
                            }
                    ));
        }
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
        if (templateMigrationHelper != null) {
            templateMigrationHelper.dispose();
        }
        disposables.clear();
    }
}

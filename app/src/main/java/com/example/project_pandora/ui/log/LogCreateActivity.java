package com.example.project_pandora.ui.log;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project_pandora.databinding.ActivityLogCreateBinding;
import com.example.project_pandora.repository.LogRepository;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class LogCreateActivity extends AppCompatActivity {

    private static final String PREF_NAME = "log_templates";
    private static final String KEY_TEMPLATES = "templates";

    private ActivityLogCreateBinding binding;
    private LogRepository logRepository;
    private String selectedDate;
    private boolean isEditMode = false;
    private long editLogId = -1;
    private final CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLogCreateBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        logRepository = new LogRepository(this);

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        binding.textLogDate.setText(selectedDate);
        binding.textLogDate.setOnClickListener(v -> showDatePicker());

        setupCompletionSpinner();
        checkEditMode();
        setupTemplateButtons();
        binding.btnSave.setOnClickListener(v -> handleSave());
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

            String completionStatus = intent.getStringExtra("completionStatus");
            if (completionStatus != null) {
                switch (completionStatus) {
                    case "COMPLETED": binding.spinnerCompletionStatus.setSelection(1); break;
                    case "NOT_STARTED": binding.spinnerCompletionStatus.setSelection(2); break;
                    default: binding.spinnerCompletionStatus.setSelection(0); break;
                }
            }

            String timeCost = intent.getStringExtra("timeCost");
            if (timeCost != null) binding.editTimeCost.setText(timeCost);

            String logDate = intent.getStringExtra("logDate");
            if (logDate != null) {
                selectedDate = logDate;
                binding.textLogDate.setText(selectedDate);
            }
        }
    }

    private void setupCompletionSpinner() {
        String[] statuses = {"进行中 - IN_PROGRESS", "已完成 - COMPLETED", "未开始 - NOT_STARTED"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, statuses);
        binding.spinnerCompletionStatus.setAdapter(adapter);
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

        String completionStatus = getCompletionStatus();
        String timeCost = binding.editTimeCost.getText().toString().trim();

        try {
            SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
            String json = prefs.getString(KEY_TEMPLATES, "[]");
            JSONArray array = new JSONArray(json);

            JSONObject template = new JSONObject();
            template.put("workItem", workItem);
            template.put("completionStatus", completionStatus);
            template.put("timeCost", timeCost);
            template.put("savedAt", System.currentTimeMillis());
            array.put(template);

            prefs.edit().putString(KEY_TEMPLATES, array.toString()).apply();
            Toast.makeText(this, "模板已保存", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "保存模板失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void showTemplateDialog() {
        try {
            SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
            String json = prefs.getString(KEY_TEMPLATES, "[]");
            JSONArray array = new JSONArray(json);

            if (array.length() == 0) {
                Toast.makeText(this, "暂无模板，请先保存模板", Toast.LENGTH_SHORT).show();
                return;
            }

            List<String> templateNames = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject tpl = array.getJSONObject(i);
                templateNames.add(tpl.getString("workItem"));
            }

            String[] items = templateNames.toArray(new String[0]);
            new AlertDialog.Builder(this)
                    .setTitle("选择模板")
                    .setItems(items, (dialog, which) -> {
                        try {
                            JSONObject tpl = array.getJSONObject(which);
                            binding.editWorkItem.setText(tpl.getString("workItem"));
                            String status = tpl.optString("completionStatus", "IN_PROGRESS");
                            switch (status) {
                                case "COMPLETED": binding.spinnerCompletionStatus.setSelection(1); break;
                                case "NOT_STARTED": binding.spinnerCompletionStatus.setSelection(2); break;
                                default: binding.spinnerCompletionStatus.setSelection(0); break;
                            }
                            String timeCost = tpl.optString("timeCost", "");
                            if (!timeCost.isEmpty()) binding.editTimeCost.setText(timeCost);
                        } catch (Exception e) {
                            Toast.makeText(this, "加载模板失败", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNeutralButton("删除模板", (dialog, which) -> showDeleteTemplateDialog(array))
                    .show();
        } catch (Exception e) {
            Toast.makeText(this, "加载模板失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteTemplateDialog(JSONArray array) {
        try {
            List<String> names = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                names.add(array.getJSONObject(i).getString("workItem"));
            }
            new AlertDialog.Builder(this)
                    .setTitle("删除模板")
                    .setItems(names.toArray(new String[0]), (dialog, which) -> {
                        try {
                            JSONArray newArray = new JSONArray();
                            for (int i = 0; i < array.length(); i++) {
                                if (i != which) newArray.put(array.get(i));
                            }
                            getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                                    .edit().putString(KEY_TEMPLATES, newArray.toString()).apply();
                            Toast.makeText(this, "模板已删除", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .show();
        } catch (Exception e) {
            Toast.makeText(this, "操作失败", Toast.LENGTH_SHORT).show();
        }
    }

    private String getCompletionStatus() {
        switch (binding.spinnerCompletionStatus.getSelectedItemPosition()) {
            case 1: return "COMPLETED";
            case 2: return "NOT_STARTED";
            default: return "IN_PROGRESS";
        }
    }

    private void handleSave() {
        String workItem = binding.editWorkItem.getText().toString().trim();
        if (workItem.isEmpty()) {
            Toast.makeText(this, "请输入工作事项", Toast.LENGTH_SHORT).show();
            return;
        }

        String completionStatus = getCompletionStatus();

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
            disposables.add(logRepository.editLog(editLogId, workItem, completionStatus, timeCost, selectedDate)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            result -> {
                                Toast.makeText(this, "日志更新成功", Toast.LENGTH_SHORT).show();
                                finish();
                            },
                            error -> {
                                Toast.makeText(this, "更新失败: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                binding.btnSave.setEnabled(true);
                                binding.btnSave.setText("更新");
                            }
                    ));
        } else {
            disposables.add(logRepository.createLog(workItem, completionStatus, timeCost, selectedDate)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            result -> {
                                Toast.makeText(this, "日志保存成功", Toast.LENGTH_SHORT).show();
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
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }
}

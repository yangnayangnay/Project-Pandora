package com.example.project_pandora.ui.log;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project_pandora.databinding.ActivityLogCreateBinding;
import com.example.project_pandora.repository.LogRepository;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

public class LogCreateActivity extends AppCompatActivity {

    private ActivityLogCreateBinding binding;
    private LogRepository logRepository;
    private String selectedDate;

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
        binding.btnSave.setOnClickListener(v -> handleSave());
    }

    private void setupCompletionSpinner() {
        String[] statuses = {"进行中 - IN_PROGRESS", "已完成 - COMPLETED", "未开始 - NOT_STARTED"};
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
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

    private void handleSave() {
        String workItem = binding.editWorkItem.getText().toString().trim();
        if (workItem.isEmpty()) {
            Toast.makeText(this, "请输入工作事项", Toast.LENGTH_SHORT).show();
            return;
        }

        String completionStatus = "IN_PROGRESS";
        switch (binding.spinnerCompletionStatus.getSelectedItemPosition()) {
            case 0: completionStatus = "IN_PROGRESS"; break;
            case 1: completionStatus = "COMPLETED"; break;
            case 2: completionStatus = "NOT_STARTED"; break;
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

        logRepository.createLog(workItem, completionStatus, timeCost, selectedDate)
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
                );
    }
}
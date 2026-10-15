package com.example.project_pandora.ui.log;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project_pandora.core.network.TokenManager;
import com.example.project_pandora.databinding.ActivityLogDetailBinding;
import com.example.project_pandora.repository.LogRepository;

import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class LogDetailActivity extends AppCompatActivity {

    public static final String EXTRA_LOG_ID = "logId";

    private ActivityLogDetailBinding binding;
    private LogRepository logRepository;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private long logId;
    private String currentRole;
    private Long currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLogDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        logId = getIntent().getLongExtra(EXTRA_LOG_ID, -1);
        if (logId <= 0) {
            finish();
            return;
        }

        logRepository = new LogRepository(this);
        TokenManager tm = TokenManager.getInstance(this);
        currentRole = tm.getRole();
        currentUserId = tm.getUserId();

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("日志详情");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        binding.btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(this, LogCreateActivity.class);
            intent.putExtra("editMode", true);
            intent.putExtra("logId", logId);
            startActivity(intent);
        });

        binding.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("删除日志")
                    .setMessage("确定删除这条日志吗？")
                    .setPositiveButton("删除", (d, w) -> deleteLog())
                    .setNegativeButton("取消", null)
                    .show();
        });

        loadDetail();
    }

    private void loadDetail() {
        binding.progressBar.setVisibility(View.VISIBLE);
        disposables.add(logRepository.getLogDetail(logId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        this::bindDetail,
                        error -> {
                            binding.progressBar.setVisibility(View.GONE);
                            Toast.makeText(this, "加载失败: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                ));
    }

    @SuppressWarnings("unchecked")
    private void bindDetail(Map<String, Object> log) {
        binding.progressBar.setVisibility(View.GONE);

        Object workItem = log.get("work_item");
        binding.textWorkItem.setText(workItem != null ? workItem.toString() : "");

        Object date = log.get("log_date");
        binding.textDate.setText(date != null ? date.toString() : "");

        Object status = log.get("completion_status");
        String statusText = "进行中";
        if (status != null) {
            String s = status.toString();
            switch (s) {
                case "COMPLETED": statusText = "已完成"; break;
                case "PENDING": statusText = "待处理"; break;
                case "IN_PROGRESS": statusText = "进行中"; break;
                default: statusText = s;
            }
        }
        binding.textStatus.setText(statusText);

        Object timeCost = log.get("time_cost");
        binding.textTimeCost.setText(timeCost != null ? timeCost + " 小时" : "未记录");

        Object username = log.get("username");
        Object realName = log.get("real_name");
        String nameStr = realName != null ? realName.toString() : (username != null ? username.toString() : "");
        binding.textUsername.setText(nameStr);

        Object createdAt = log.get("created_at");
        binding.textCreatedAt.setText(createdAt != null ? createdAt.toString() : "");

        Object analysesObj = log.get("ai_analyses");
        if (analysesObj instanceof List) {
            List<Map<String, Object>> analyses = (List<Map<String, Object>>) analysesObj;
            if (!analyses.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (Map<String, Object> a : analyses) {
                    Object type = a.get("analysis_type");
                    Object result = a.get("result");
                    Object aStatus = a.get("status");
                    sb.append("【").append(type != null ? type : "分析").append("】");
                    sb.append(" (").append(aStatus != null ? aStatus : "").append(")\n");
                    if (result != null) {
                        String r = result.toString();
                        if (r.length() > 500) r = r.substring(0, 500) + "...";
                        sb.append(r);
                    }
                    sb.append("\n\n");
                }
                binding.textAiAnalysis.setText(sb.toString().trim());
            }
        }

        Object logUserIdObj = log.get("user_id");
        Long logUserId = logUserIdObj instanceof Number ? ((Number) logUserIdObj).longValue() : null;
        boolean isOwner = logUserId != null && logUserId.equals(currentUserId);
        boolean canManage = isOwner;
        if (!canManage) {
            binding.btnEdit.setVisibility(View.GONE);
            binding.btnDelete.setVisibility(View.GONE);
        }
    }

    private void deleteLog() {
        disposables.add(logRepository.deleteLog(logId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        v -> {
                            Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                            finish();
                        },
                        error -> Toast.makeText(this, "删除失败: " + error.getMessage(), Toast.LENGTH_SHORT).show()
                ));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (logId > 0) {
            loadDetail();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }
}
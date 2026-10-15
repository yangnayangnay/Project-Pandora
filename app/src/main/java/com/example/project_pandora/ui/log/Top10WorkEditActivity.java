package com.example.project_pandora.ui.log;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.project_pandora.databinding.ActivityTop10WorkEditBinding;
import com.example.project_pandora.repository.LogRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import com.example.project_pandora.core.util.BackToolbarHelper;

public class Top10WorkEditActivity extends AppCompatActivity {

    private ActivityTop10WorkEditBinding binding;
    private LogRepository logRepository;
    private final List<Map<String, Object>> top10List = new ArrayList<>();
    private String workType = "PERSONAL_IMPORTANT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTop10WorkEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        logRepository = new LogRepository(this);

        String extra = getIntent().getStringExtra("workType");
        if (extra != null && !extra.isEmpty()) {
            workType = extra;
        }

        BackToolbarHelper.setupBack(binding.toolbar, this::finish);
        binding.toolbar.setTitle("COMPANY_IMPORTANT".equals(workType) ? "公司十大重要工作" : "个人十大重要工作");
        binding.recyclerTop10.setLayoutManager(new LinearLayoutManager(this));

        loadTop10();
        binding.btnSave.setOnClickListener(v -> handleSave());
    }

    private void loadTop10() {
        logRepository.getTop10(workType)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        list -> {
                            top10List.clear();
                            for (Map<String, Object> raw : list) {
                                Map<String, Object> item = new HashMap<>();
                                item.put("id", raw.get("id") != null ? raw.get("id") : raw.get("ID"));
                                Object rankOrderRaw = raw.get("rankOrder") != null ? raw.get("rankOrder") : raw.get("RANK_ORDER");
                                item.put("rankOrder", rankOrderRaw instanceof Number ? ((Number) rankOrderRaw).intValue() : rankOrderRaw);
                                item.put("content", raw.get("content") != null ? raw.get("content") : raw.get("CONTENT"));
                                item.put("relatedTaskId", raw.get("relatedTaskId") != null ? raw.get("relatedTaskId") : raw.get("RELATED_TASK_ID"));
                                top10List.add(item);
                            }
                            for (int i = top10List.size(); i < 10; i++) {
                                Map<String, Object> item = new HashMap<>();
                                item.put("rankOrder", i + 1);
                                item.put("content", "");
                                top10List.add(item);
                            }
                            Top10Adapter adapter = new Top10Adapter(top10List);
                            binding.recyclerTop10.setAdapter(adapter);
                        },
                        error -> {
                            for (int i = 0; i < 10; i++) {
                                Map<String, Object> item = new HashMap<>();
                                item.put("rankOrder", i + 1);
                                item.put("content", "");
                                top10List.add(item);
                            }
                            Top10Adapter adapter = new Top10Adapter(top10List);
                            binding.recyclerTop10.setAdapter(adapter);
                        }
                );
    }

    private void handleSave() {
        List<Map<String, Object>> saveList = new ArrayList<>();
        for (Map<String, Object> item : top10List) {
            String content = (String) item.get("content");
            if (content != null && !content.trim().isEmpty()) {
                Map<String, Object> saveItem = new HashMap<>();
                saveItem.put("workType", workType);
                saveItem.put("rankOrder", item.get("rankOrder"));
                saveItem.put("content", content);
                saveList.add(saveItem);
            }
        }

        binding.btnSave.setEnabled(false);
        logRepository.updateTop10(saveList)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        ignored -> {
                            Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
                            finish();
                        },
                        error -> {
                            String errorMsg;
                            if (error instanceof com.example.project_pandora.core.exception.BusinessException) {
                                com.example.project_pandora.core.exception.BusinessException be =
                                        (com.example.project_pandora.core.exception.BusinessException) error;
                                errorMsg = com.example.project_pandora.core.util.ErrorMessageMapper.map(be.getCode(), be.getMessage());
                            } else {
                                errorMsg = com.example.project_pandora.core.util.NetworkErrorClassifier.classify(error);
                            }
                            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                            binding.btnSave.setEnabled(true);
                        }
                );
    }
}
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

public class Top10WorkEditActivity extends AppCompatActivity {

    private ActivityTop10WorkEditBinding binding;
    private LogRepository logRepository;
    private final List<Map<String, Object>> top10List = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTop10WorkEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        logRepository = new LogRepository(this);

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.recyclerTop10.setLayoutManager(new LinearLayoutManager(this));

        loadTop10();
        binding.btnSave.setOnClickListener(v -> handleSave());
    }

    private void loadTop10() {
        logRepository.getTop10("PERSONAL_IMPORTANT")
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        list -> {
                            top10List.clear();
                            top10List.addAll(list);
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
                saveItem.put("workType", "PERSONAL_IMPORTANT");
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
                            Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
                            binding.btnSave.setEnabled(true);
                        }
                );
    }
}
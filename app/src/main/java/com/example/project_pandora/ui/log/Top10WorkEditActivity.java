package com.example.project_pandora.ui.log;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.project_pandora.core.util.BackToolbarHelper;
import com.example.project_pandora.databinding.ActivityTop10WorkEditBinding;
import com.example.project_pandora.repository.LogRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class Top10WorkEditActivity extends AppCompatActivity {

    private ActivityTop10WorkEditBinding binding;
    private LogRepository logRepository;
    private final List<Map<String, Object>> top10List = new ArrayList<>();
    // Round26 B1（4.3 定稿方案）：加载完成后的原始快照（逐项独立拷贝，TextWatcher 回写只改 top10List），
    // 保存时逐槽位 diff 快照计算增/删/改操作
    private final List<Map<String, Object>> originalSnapshot = new ArrayList<>();
    private final CompositeDisposable disposable = new CompositeDisposable();
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

        // Round26 B1：保存链走 /view/personal-top10/*（后端固定 work_type=PERSONAL_IMPORTANT），
        // 非个人类型仅允许查看加载，禁存防误写
        if (!"PERSONAL_IMPORTANT".equals(workType)) {
            binding.btnSave.setEnabled(false);
        }

        loadTop10();
        binding.btnSave.setOnClickListener(v -> handleSave());
    }

    @Override
    protected void onDestroy() {
        disposable.clear();
        super.onDestroy();
    }

    private void loadTop10() {
        disposable.add(logRepository.getTop10(workType)
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
                            takeSnapshot();
                            binding.recyclerTop10.setAdapter(new Top10Adapter(top10List));
                        },
                        error -> {
                            top10List.clear();
                            for (int i = 0; i < 10; i++) {
                                Map<String, Object> item = new HashMap<>();
                                item.put("rankOrder", i + 1);
                                item.put("content", "");
                                top10List.add(item);
                            }
                            takeSnapshot();
                            binding.recyclerTop10.setAdapter(new Top10Adapter(top10List));
                        }
                ));
    }

    private void takeSnapshot() {
        originalSnapshot.clear();
        for (Map<String, Object> item : top10List) {
            originalSnapshot.add(new HashMap<>(item));
        }
    }

    private static String contentOf(Map<String, Object> item) {
        Object content = item.get("content");
        return content instanceof String ? ((String) content).trim() : "";
    }

    private static Long idOf(Map<String, Object> item) {
        Object id = item.get("id");
        return id instanceof Number ? ((Number) id).longValue() : null;
    }

    // Round26 B1（4.3 定稿方案）：老 PUT log/top10 批量保存因 top10_works 无复合唯一键已破（ON DUPLICATE
    // 永不触发，纯 INSERT 累积重复行），弃用。改为逐槽位 diff 原始快照：
    // 原空现非空 → POST inject（content + rankOrder=槽位号）；原非空现空 → DELETE inject/{id}（软删）；
    // 内容变更 → PUT inject/{id}（仅 content）。排序（内容跨槽位移动）由槽位级 diff 消解为对位操作；
    // Observable.concat 串行执行避免并发写。
    private void handleSave() {
        List<Observable<?>> operations = new ArrayList<>();
        for (int i = 0; i < top10List.size() && i < 10; i++) {
            Map<String, Object> current = top10List.get(i);
            Map<String, Object> original = i < originalSnapshot.size() ? originalSnapshot.get(i) : null;
            String currentContent = contentOf(current);
            String originalContent = original != null ? contentOf(original) : "";
            Long originalId = original != null ? idOf(original) : null;

            if (originalId == null || originalContent.isEmpty()) {
                if (!currentContent.isEmpty()) {
                    operations.add(logRepository.injectTop10(currentContent, i + 1));
                }
            } else if (currentContent.isEmpty()) {
                operations.add(logRepository.deleteTop10(originalId));
            } else if (!currentContent.equals(originalContent)) {
                operations.add(logRepository.updateTop10Content(originalId, currentContent));
            }
        }

        if (operations.isEmpty()) {
            Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding.btnSave.setEnabled(false);
        disposable.add(Observable.concat(operations)
                .ignoreElements()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> {
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
                ));
    }
}
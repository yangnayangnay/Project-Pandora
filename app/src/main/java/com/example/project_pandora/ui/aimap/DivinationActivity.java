package com.example.project_pandora.ui.aimap;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project_pandora.core.divination.DivinationEngine;
import com.example.project_pandora.core.network.ApiResponse;
import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.core.util.BackToolbarHelper;
import com.example.project_pandora.data.remote.AIApi;
import com.example.project_pandora.databinding.ActivityDivinationBinding;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Round19 REQ-19-12: 梅花易数占卜页面
 * Round23 REQ-23-08: 迁移至 ui/aimap 包（与 AI 数据分析页同包），改为随机起卦（和数起卦法），
 * 卦象先行展示不等 AI，随后异步请求解读；失败/超时显示本地降级文案
 * Round24 REQ-24-05/06: 每日一次状态机——进入先查 GET /ai/divination/today（已算回显+按钮置灰），
 * 起卦改走 POST /ai/divination/record（服务端生成解读并落库）；AI 解读区展示来源标识
 */
public class DivinationActivity extends AppCompatActivity {

    private static final String AI_FALLBACK_TEXT = "卦象已就绪。保持积极心态、顺势而为，好事正在靠近。";
    private static final String SOURCE_EXTERNAL = "EXTERNAL";

    private static final String SOURCE_LABEL_AI = "— AI 生成 —";
    private static final String SOURCE_LABEL_LOCAL = "— 本地兜底生成 —";

    private ActivityDivinationBinding binding;
    private final DivinationEngine engine = new DivinationEngine();
    private final CompositeDisposable disposables = new CompositeDisposable();
    private AIApi aiApi;
    private boolean hasDivinedToday = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDivinationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BackToolbarHelper.setupBack(binding.toolbar, this::finish);

        aiApi = NetworkClient.getInstance(this).createService(AIApi.class);

        binding.btnDivine.setOnClickListener(v -> performDivination());

        checkTodayDivination();
    }

    private void checkTodayDivination() {
        disposables.add(aiApi.getDivinationToday()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        response -> {
                            if (isDestroyed() || isFinishing()) return;
                            Map<String, Object> record = extractTodayRecord(response);
                            if (record != null) {
                                showExistingRecord(record);
                            }
                        },
                        error -> {
                            // 查询异常宁可放行（spec 5.6.5-1）：按钮保持"随机起卦"可点
                            if (isDestroyed() || isFinishing()) return;
                            markDivineable();
                        }));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractTodayRecord(ApiResponse<Map<String, Object>> response) {
        if (response == null || !response.isSuccess() || response.getData() == null) {
            return null;
        }
        Object exists = response.getData().get("exists");
        if (!Boolean.TRUE.equals(exists)) {
            return null;
        }
        Object record = response.getData().get("record");
        if (!(record instanceof Map)) {
            return null;
        }
        return (Map<String, Object>) record;
    }

    private void showExistingRecord(Map<String, Object> record) {
        try {
            // Round25 #R25-03: 互卦/变卦以落库名优先重建，与 AI 解读文本同源自洽
            DivinationEngine.DivinationResult result =
                    engine.fromNames(str(record, "originalName"), str(record, "mutualName"),
                            str(record, "changedName"), intVal(record, "movingLine"));
            displayResult(result);
        } catch (Exception e) {
            // 非法卦名容错：不崩溃，仍按当日已算收口（记录在服务端不可绕过）
            markDivined();
            return;
        }
        binding.cardAiAnalysis.setVisibility(View.VISIBLE);
        binding.textAiAnalysis.setText(str(record, "analysis"));
        bindAiSource(str(record, "source"));
        markDivined();
    }

    private void markDivined() {
        hasDivinedToday = true;
        binding.btnDivine.setEnabled(false);
        binding.btnDivine.setText("今日已算");
    }

    private void markDivineable() {
        hasDivinedToday = false;
        binding.btnDivine.setEnabled(true);
        binding.btnDivine.setText("随机起卦");
    }

    private void performDivination() {
        // 已算态防御入口：状态竞态兜底，不产生新卦象
        if (hasDivinedToday) {
            Toast.makeText(this, "今日已算过", Toast.LENGTH_SHORT).show();
            return;
        }

        DivinationEngine.DivinationResult result;
        try {
            result = engine.divineRandom();
        } catch (Exception e) {
            Toast.makeText(this, "起卦失败，请重试", Toast.LENGTH_SHORT).show();
            return;
        }

        displayResult(result);
        recordDivination(result);
    }

    private void recordDivination(DivinationEngine.DivinationResult result) {
        binding.cardAiAnalysis.setVisibility(View.VISIBLE);
        binding.textAiAnalysis.setText("正在生成 AI 解读...");
        binding.textAiSource.setText("");

        // 取消上一次未完成的请求，避免连续操作时旧结果覆盖新卦象
        disposables.clear();

        Map<String, Object> request = new HashMap<>();
        request.put("originalName", result.getOriginalName());
        request.put("mutualName", result.getMutualName());
        request.put("changedName", result.getChangedName());
        request.put("movingLine", result.getMovingLine());

        disposables.add(aiApi.recordDivination(request)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        response -> {
                            if (isDestroyed() || isFinishing()) return;
                            handleRecordResponse(response);
                        },
                        error -> {
                            if (isDestroyed() || isFinishing()) return;
                            showLocalFallback();
                        }));
    }

    @SuppressWarnings("unchecked")
    private void handleRecordResponse(ApiResponse<Map<String, Object>> response) {
        boolean alreadyToday = false;
        Map<String, Object> record = null;
        if (response != null && response.isSuccess() && response.getData() != null) {
            alreadyToday = Boolean.TRUE.equals(response.getData().get("alreadyToday"));
            Object rec = response.getData().get("record");
            if (rec instanceof Map) {
                record = (Map<String, Object>) rec;
            }
        }
        if (record == null) {
            showLocalFallback();
            return;
        }
        if (alreadyToday) {
            // 并发窗口兜底：覆盖渲染已存记录（以服务端落库结果为准）
            try {
                // Round25 #R25-03: 互卦/变卦以落库名优先重建，与 AI 解读文本同源自洽
                displayResult(engine.fromNames(str(record, "originalName"), str(record, "mutualName"),
                        str(record, "changedName"), intVal(record, "movingLine")));
            } catch (Exception ignored) {
            }
            Toast.makeText(this, "今日已算过，展示当日结果", Toast.LENGTH_SHORT).show();
        }
        binding.cardAiAnalysis.setVisibility(View.VISIBLE);
        binding.textAiAnalysis.setText(str(record, "analysis"));
        bindAiSource(str(record, "source"));
        markDivined();
    }

    private void showLocalFallback() {
        binding.cardAiAnalysis.setVisibility(View.VISIBLE);
        binding.textAiAnalysis.setText(AI_FALLBACK_TEXT);
        binding.textAiSource.setText(SOURCE_LABEL_LOCAL);
        // 网络失败当日未落库：保持可起卦态，重进可再起卦（放行语义）
    }

    private void bindAiSource(String source) {
        if (SOURCE_EXTERNAL.equals(source)) {
            binding.textAiSource.setText(SOURCE_LABEL_AI);
        } else {
            binding.textAiSource.setText(SOURCE_LABEL_LOCAL);
        }
    }

    private void displayResult(DivinationEngine.DivinationResult result) {
        // 结果标题
        binding.textResultTitle.setText("随机起卦结果");
        binding.textResultTitle.setVisibility(View.VISIBLE);

        // Round27 REQ-27-13: 三卡卦象注入（起卦/回显共用单点；pattern 异常降级 GONE 仅显卦名）
        boolean[] originalPattern = result.getOriginalLinePattern();
        if (originalPattern != null && originalPattern.length == 6) {
            binding.hexagramOriginal.setVisibility(View.VISIBLE);
            binding.hexagramOriginal.setLinePattern(originalPattern);
            binding.hexagramOriginal.setMovingLine(result.getMovingLine());
        } else {
            binding.hexagramOriginal.setVisibility(View.GONE);
            Log.w("DivinationActivity", "invalid originalLinePattern, hide hexagram");
        }
        boolean[] mutualPattern = result.getMutualLinePattern();
        if (mutualPattern != null && mutualPattern.length == 6) {
            binding.hexagramMutual.setVisibility(View.VISIBLE);
            binding.hexagramMutual.setLinePattern(mutualPattern);
        } else {
            binding.hexagramMutual.setVisibility(View.GONE);
            Log.w("DivinationActivity", "invalid mutualLinePattern, hide hexagram");
        }
        boolean[] changedPattern = result.getChangedLinePattern();
        if (changedPattern != null && changedPattern.length == 6) {
            binding.hexagramChanged.setVisibility(View.VISIBLE);
            binding.hexagramChanged.setLinePattern(changedPattern);
        } else {
            binding.hexagramChanged.setVisibility(View.GONE);
            Log.w("DivinationActivity", "invalid changedLinePattern, hide hexagram");
        }

        // 本卦
        binding.cardOriginal.setVisibility(View.VISIBLE);
        binding.textOriginalName.setText(result.getOriginalName());
        binding.textOriginalJudgment.setText(result.getOriginalJudgment());

        // 互卦
        binding.cardMutual.setVisibility(View.VISIBLE);
        binding.textMutualName.setText(result.getMutualName());
        binding.textMutualJudgment.setText(result.getMutualJudgment());

        // 变卦
        binding.cardChanged.setVisibility(View.VISIBLE);
        binding.textChangedName.setText(result.getChangedName());
        binding.textChangedJudgment.setText(result.getChangedJudgment());

        // 动爻
        binding.textMovingLine.setText("动爻：第 " + result.getMovingLine() + " 爻");
        binding.textMovingLine.setVisibility(View.VISIBLE);
    }

    private String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : "";
    }

    private int intVal(Map<String, Object> map, String key) {
        // Round26: AI 结果数字解析统一收敛至 NumberCompat（原 parseInt 失败→默认 1 语义保持）
        return com.example.project_pandora.core.util.NumberCompat.toIntOrDefault(map.get(key), 1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }
}
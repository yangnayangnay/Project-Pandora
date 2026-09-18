package com.example.project_pandora.ui.ai;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project_pandora.databinding.ActivityKeywordAnalysisBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class KeywordAnalysisActivity extends AppCompatActivity {

    private ActivityKeywordAnalysisBinding binding;
    private KeywordAdapter adapter;

    private static final Set<String> STOP_WORDS = new HashSet<>();
    static {
        String[] stops = {
            "的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一", "一个",
            "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好",
            "自己", "这", "那", "它", "他", "她", "们", "把", "被", "让", "使", "为", "以",
            "及", "等", "但", "而", "与", "或", "如果", "因为", "所以", "虽然", "但是",
            "可以", "这个", "那个", "什么", "怎么", "为什么", "哪里", "哪个", "怎样",
            "进行", "通过", "对于", "关于", "根据", "按照", "由于", "基于",
            "需要", "应该", "可能", "已经", "正在", "将要", "曾经",
            "他们", "她们", "它们", "我们", "你们", "以及", "并且", "或者",
            "不过", "然后", "还是", "只有", "只是", "就是", "还有", "不是",
            "一些", "一种", "一样", "一直", "一定", "一般", "一起", "一下",
            "现在", "今天", "明天", "昨天", "本", "该", "此", "其",
            "个", "中", "后", "前", "时", "里", "下", "又", "再", "更",
            "最", "太", "只", "还", "即", "则", "才", "便", "可"
        };
        for (String s : stops) STOP_WORDS.add(s);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityKeywordAnalysisBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        adapter = new KeywordAdapter();

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.recyclerKeywords.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerKeywords.setAdapter(adapter);

        binding.btnAnalyze.setOnClickListener(v -> handleAnalyze());
    }

    private void handleAnalyze() {
        String text = binding.editText.getText().toString().trim();
        if (text.isEmpty()) {
            binding.textProcess.setText("请先输入待分析文本");
            binding.textProcess.setVisibility(View.VISIBLE);
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.textProcess.setVisibility(View.VISIBLE);
        binding.recyclerKeywords.setVisibility(View.GONE);

        StringBuilder processLog = new StringBuilder();
        processLog.append("【分析开始】\n");
        processLog.append("时间：").append(
                new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new java.util.Date())).append("\n\n");

        processLog.append("【步骤1：文本预处理】\n");
        String cleaned = text.replaceAll("[\\p{Punct}\\s\uFF0C\u3002\u3001\uFF1B\uFF1A\uFF01\uFF1F\u201C\u201D\u2018\u2019\uFF08\uFF09\u3010\u3011\u300A\u300B\u2014\u2026]", "");
        processLog.append("原文长度：").append(text.length()).append(" 字符\n");
        processLog.append("清洗后长度：").append(cleaned.length()).append(" 字符\n\n");

        processLog.append("【步骤2：候选词提取】\n");
        Map<String, Integer> candidates = new HashMap<>();

        for (int n = 2; n <= 4; n++) {
            for (int i = 0; i <= cleaned.length() - n; i++) {
                String word = cleaned.substring(i, i + n);
                if (isMeaningfulWord(word)) {
                    candidates.merge(word, 1, Integer::sum);
                }
            }
        }
        processLog.append("提取候选词：").append(candidates.size()).append(" 个\n\n");

        processLog.append("【步骤3：停用词过滤】\n");
        int beforeFilter = candidates.size();
        candidates.entrySet().removeIf(e ->
                STOP_WORDS.contains(e.getKey()) || e.getValue() < 2);
        processLog.append("过滤前：").append(beforeFilter).append(" 个\n");
        processLog.append("过滤后：").append(candidates.size()).append(" 个\n");
        processLog.append("（移除停用词和仅出现1次的词）\n\n");

        processLog.append("【步骤4：词频排序】\n");
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(candidates.entrySet());
        sorted.sort(Comparator.comparing(Map.Entry<String, Integer>::getValue).reversed());

        int topN = Math.min(20, sorted.size());
        processLog.append("按词频降序排列，取前 ").append(topN).append(" 个关键词\n\n");

        processLog.append("【步骤5：去重合并】\n");
        List<Map.Entry<String, Integer>> merged = mergeSubWords(sorted, topN);
        processLog.append("合并子词后最终关键词：").append(merged.size()).append(" 个\n\n");

        processLog.append("【分析完成】\n");

        binding.textProcess.setText(processLog.toString());

        List<Map<String, Object>> result = new ArrayList<>();
        int maxCount = merged.isEmpty() ? 1 : merged.get(0).getValue();
        for (Map.Entry<String, Integer> entry : merged) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("word", entry.getKey());
            item.put("count", entry.getValue());
            item.put("ratio", (int) ((double) entry.getValue() / maxCount * 100));
            result.add(item);
        }

        adapter.setKeywords(result);

        binding.progressBar.setVisibility(View.GONE);
        binding.recyclerKeywords.setVisibility(View.VISIBLE);
    }

    private boolean isMeaningfulWord(String word) {
        for (char c : word.toCharArray()) {
            if (c < 0x4E00 || c > 0x9FFF) return false;
        }
        return true;
    }

    private List<Map.Entry<String, Integer>> mergeSubWords(
            List<Map.Entry<String, Integer>> sorted, int topN) {
        List<Map.Entry<String, Integer>> result = new ArrayList<>();
        Set<String> contained = new HashSet<>();

        int count = 0;
        for (Map.Entry<String, Integer> entry : sorted) {
            if (count >= topN) break;
            String word = entry.getKey();

            boolean isSub = false;
            for (String existing : contained) {
                if (existing.contains(word) && existing.length() > word.length()) {
                    isSub = true;
                    break;
                }
            }
            if (!isSub) {
                result.add(entry);
                contained.add(word);
                count++;
            }
        }
        return result;
    }

    private static class KeywordAdapter extends RecyclerView.Adapter<KeywordAdapter.KeywordViewHolder> {
        private final List<Map<String, Object>> keywords = new ArrayList<>();

        void setKeywords(List<Map<String, Object>> newKeywords) {
            keywords.clear();
            if (newKeywords != null) keywords.addAll(newKeywords);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public KeywordViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            TextView textView = new TextView(parent.getContext());
            textView.setPadding(32, 20, 32, 20);
            textView.setTextSize(15);
            return new KeywordViewHolder(textView);
        }

        @Override
        public void onBindViewHolder(@NonNull KeywordViewHolder holder, int position) {
            Map<String, Object> keyword = keywords.get(position);
            String word = (String) keyword.get("word");
            Object countObj = keyword.get("count");
            Object ratioObj = keyword.get("ratio");
            int count = countObj != null ? ((Number) countObj).intValue() : 0;
            int ratio = ratioObj != null ? ((Number) ratioObj).intValue() : 0;

            int barLength = ratio / 5;
            StringBuilder bar = new StringBuilder();
            for (int i = 0; i < barLength; i++) bar.append("█");

            String text = String.format(Locale.getDefault(),
                    "%d. %s  (%d次)\n   %s",
                    position + 1, word, count, bar.toString());
            ((TextView) holder.itemView).setText(text);
        }

        @Override
        public int getItemCount() { return keywords.size(); }

        static class KeywordViewHolder extends RecyclerView.ViewHolder {
            KeywordViewHolder(View itemView) { super(itemView); }
        }
    }
}

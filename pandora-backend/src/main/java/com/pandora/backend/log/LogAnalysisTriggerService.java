package com.pandora.backend.log;

import com.pandora.backend.ai.AIGatewayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class LogAnalysisTriggerService {

    private static final Logger log = LoggerFactory.getLogger(LogAnalysisTriggerService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AIGatewayService aiGatewayService;

    private static final String[] COMMON_WORDS = {
            "工作", "任务", "项目", "会议", "报告", "计划", "总结", "分析", "开发", "设计",
            "测试", "部署", "沟通", "协调", "跟进", "优化", "修复", "评审", "文档", "培训"
    };

    public void triggerAutoAnalysis(Long logId, Long userId) {
        new Thread(() -> {
            try {
                doAnalysis(logId, userId);
            } catch (Exception e) {
                log.error("日志自动分析失败: logId={}, userId={}, error={}", logId, userId, e.getMessage());
            }
        }, "log-auto-analysis-" + logId).start();
    }

    private void doAnalysis(Long logId, Long userId) {
        Map<String, Object> logRecord;
        try {
            logRecord = jdbcTemplate.queryForMap(
                    "SELECT work_item FROM work_logs WHERE id = ? AND deleted = 0", logId);
        } catch (Exception e) {
            log.warn("查询日志记录失败: logId={}", logId);
            return;
        }

        String workItem = logRecord.get("work_item") != null ? logRecord.get("work_item").toString() : "";
        if (workItem.length() < 5) {
            persistAnalysisResult(userId, logId, Collections.emptyList(),
                    "日志内容较少，建议补充更多工作描述以获得更精准分析。", null, "LOCAL");
            return;
        }

        List<Map<String, Object>> keywords = extractKeywords(workItem);
        String mbtiType = getLatestMbtiType(userId);
        String prompt = buildAnalysisPrompt(keywords, mbtiType);

        String analysisContent;
        String source;
        try {
            Map<String, Object> aiResult = aiGatewayService.invoke(prompt, "LOG_AUTO_ANALYSIS");
            analysisContent = aiResult.get("result") != null ? aiResult.get("result").toString() : "分析结果为空";
            String rawSource = aiResult.get("source") != null ? aiResult.get("source").toString() : "LOCAL";
            source = "EXTERNAL".equals(rawSource) ? "EXTERNAL" : "LOCAL";
        } catch (Exception e) {
            log.warn("AI网关调用失败，使用本地兜底: {}", e.getMessage());
            analysisContent = buildLocalAnalysis(keywords, mbtiType);
            source = "LOCAL";
        }

        persistAnalysisResult(userId, logId, keywords, analysisContent, mbtiType, source);
    }

    private List<Map<String, Object>> extractKeywords(String text) {
        Map<String, Integer> frequency = new HashMap<>();
        for (String word : COMMON_WORDS) {
            int idx = 0;
            while ((idx = text.indexOf(word, idx)) != -1) {
                frequency.merge(word, 1, Integer::sum);
                idx += word.length();
            }
        }
        String[] tokens = text.split("[\\s,，。.、！!？?；;]+");
        for (String token : tokens) {
            if (token.length() >= 2 && !frequency.containsKey(token)) {
                frequency.put(token, 1);
            }
        }
        List<Map<String, Object>> keywords = new ArrayList<>();
        frequency.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(10)
                .forEach(entry -> {
                    Map<String, Object> kw = new HashMap<>();
                    kw.put("word", entry.getKey());
                    kw.put("count", entry.getValue());
                    keywords.add(kw);
                });
        return keywords;
    }

    private String getLatestMbtiType(Long userId) {
        try {
            Map<String, Object> report = jdbcTemplate.queryForMap(
                    "SELECT personality_type FROM mbti_reports WHERE user_id = ? AND deleted = 0 ORDER BY generated_at DESC LIMIT 1", userId);
            return report.get("personality_type") != null ? report.get("personality_type").toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String buildAnalysisPrompt(List<Map<String, Object>> keywords, String mbtiType) {
        StringBuilder sb = new StringBuilder();
        sb.append("请基于以下工作日志关键词进行AI分析，给出总结和建设性意见。\n");
        sb.append("关键词：");
        for (int i = 0; i < keywords.size(); i++) {
            if (i > 0) sb.append("、");
            sb.append(keywords.get(i).get("word"));
        }
        if (mbtiType != null) {
            sb.append("\n用户MBTI性格类型：").append(mbtiType);
            sb.append("\n请结合该性格特点给出个性化建议。");
        }
        sb.append("\n请返回包含关键词总结、性格特点结合、建设性意见三部分的分析报告。");
        return sb.toString();
    }

    private String buildLocalAnalysis(List<Map<String, Object>> keywords, String mbtiType) {
        StringBuilder sb = new StringBuilder();
        sb.append("【关键词总结】根据日志内容，主要工作关键词为：");
        for (int i = 0; i < keywords.size(); i++) {
            if (i > 0) sb.append("、");
            sb.append(keywords.get(i).get("word"));
        }
        sb.append("。\n");
        if (mbtiType != null) {
            sb.append("【性格结合】结合您的MBTI类型").append(mbtiType).append("，");
            sb.append("建议发挥自身性格优势，合理规划工作节奏。\n");
        }
        sb.append("【建设性意见】建议保持每日日志记录习惯，定期复盘总结，");
        sb.append("关注重点工作事项的推进，提升工作效率。");
        return sb.toString();
    }

    private void persistAnalysisResult(Long userId, Long triggerLogId, List<Map<String, Object>> keywords,
                                       String content, String mbtiType, String source) {
        try {
            String keywordsJson = keywordsToJson(keywords);
            jdbcTemplate.update(
                    "INSERT INTO ai_analyses (user_id, analysis_type, input_summary, result, status, trigger_log_id, keywords, mbti_type, source) " +
                    "VALUES (?, 'LOG_AUTO_ANALYSIS', ?, ?, 'COMPLETED', ?, ?, ?, ?)",
                    userId, keywordsJson, content, triggerLogId, keywordsJson, mbtiType, source
            );
            log.info("日志自动分析结果已持久化: userId={}, logId={}", userId, triggerLogId);
        } catch (Exception e) {
            log.error("持久化分析结果失败: {}", e.getMessage());
        }
    }

    private String keywordsToJson(List<Map<String, Object>> keywords) {
        if (keywords.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < keywords.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("{\"word\":\"").append(keywords.get(i).get("word")).append("\"");
            sb.append(",\"count\":").append(keywords.get(i).get("count")).append("}");
        }
        sb.append("]");
        return sb.toString();
    }
}
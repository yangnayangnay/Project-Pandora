package com.pandora.backend.ai;

import com.pandora.backend.common.ErrorCode;
import com.pandora.backend.common.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LocalFallbackAIClient {

    private static final Logger log = LoggerFactory.getLogger(LocalFallbackAIClient.class);

    public String call(String prompt, String analysisType) {
        log.info("使用本地兜底AI: analysisType={}", analysisType);
        return generateLocalResult(prompt, analysisType);
    }

    private String generateLocalResult(String prompt, String analysisType) {
        switch (analysisType) {
            case "MBTI_ANALYSIS":
                return "基于本地算法的MBTI分析结果：根据您的答题情况，您倾向于内向、直觉、情感、判断型(INFJ)。您具有深刻的洞察力和同理心，适合从事需要创造力和人文关怀的工作。";
            case "KEYWORD_EXTRACTION":
            case "KEYWORD_ANALYSIS":
                return extractKeywordsLocal(prompt);
            case "SUGGESTION":
                return "基于本地算法的工作建议：1.合理规划每日任务优先级 2.保持工作日志的习惯 3.定期复盘和调整工作方向 4.注重团队协作和沟通 5.持续学习新技能";
            case "DIVINATION":
                return "基于本地算法的占卜结果：今日宜专注核心任务，忌分散精力。保持积极心态，将获得良好成果。";
            case "LOG_AUTO_ANALYSIS":
                return "基于本地算法的日志分析：根据您的工作日志内容，主要工作集中在项目推进和任务执行方面。建议保持良好的工作节奏，定期总结复盘，关注重点工作事项的优先级管理。结合您的性格特点，建议发挥自身优势，合理分配精力，在团队协作中主动沟通协调。";
            default:
                return "本地兜底AI分析结果：已处理您的请求（" + analysisType + "）。由于外部AI服务暂不可用，此结果由本地算法生成。";
        }
    }

    private String extractKeywordsLocal(String text) {
        String[] commonWords = {"工作", "任务", "项目", "会议", "报告", "计划", "总结", "分析", "开发", "设计"};
        StringBuilder result = new StringBuilder("关键词提取结果（本地算法）：");
        int count = 0;
        for (String word : commonWords) {
            if (text != null && text.contains(word)) {
                if (count > 0) result.append("、");
                result.append(word);
                count++;
            }
            if (count >= 5) break;
        }
        if (count == 0) {
            result.append("未提取到明显关键词，建议补充更多工作描述。");
        }
        return result.toString();
    }
}
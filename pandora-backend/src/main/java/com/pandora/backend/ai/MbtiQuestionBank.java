package com.pandora.backend.ai;

/**
 * Round27 REQ-27-11: 后端 MBTI 题库维度表（与 App 端 MbtiQuestions.java 逐题对齐）。
 * DIMENSIONS 共 60 项，按 EI/SN/TF/JP 循环穿插排列（16Personalities 风格）。
 * 计分语义（与 App 端一致）：1=选A → 该维度第一字母（E/S/T/J）+1；0=选B → 第二字母（I/N/F/P）+1。
 */
public final class MbtiQuestionBank {

    public static final int QUESTION_COUNT = 60;

    public static final String[] DIMENSIONS = {
            // 第 1 轮（1~4）
            "EI", "SN", "TF", "JP",
            // 第 2 轮（5~8）
            "EI", "SN", "TF", "JP",
            // 第 3 轮（9~12）
            "EI", "SN", "TF", "JP",
            // 第 4 轮（13~16）
            "EI", "SN", "TF", "JP",
            // 第 5 轮（17~20）
            "EI", "SN", "TF", "JP",
            // 第 6 轮（21~24）
            "EI", "SN", "TF", "JP",
            // 第 7 轮（25~28）
            "EI", "SN", "TF", "JP",
            // 第 8 轮（29~32）
            "EI", "SN", "TF", "JP",
            // 第 9 轮（33~36）
            "EI", "SN", "TF", "JP",
            // 第 10 轮（37~40）
            "EI", "SN", "TF", "JP",
            // 第 11 轮（41~44）
            "EI", "SN", "TF", "JP",
            // 第 12 轮（45~48）
            "EI", "SN", "TF", "JP",
            // 第 13 轮（49~52）
            "EI", "SN", "TF", "JP",
            // 第 14 轮（53~56）
            "EI", "SN", "TF", "JP",
            // 第 15 轮（57~60）
            "EI", "SN", "TF", "JP",
    };

    private MbtiQuestionBank() {
    }

    /**
     * 获取第 idx 题的维度；越界或数组异常时返回 null（调用方走回退分支）。
     */
    public static String dimensionOf(int idx) {
        if (idx < 0 || idx >= DIMENSIONS.length) {
            return null;
        }
        return DIMENSIONS[idx];
    }
}
package com.example.project_pandora.ui.ai;

import java.util.List;

/**
 * Round27 REQ-27-11: 60 题计分器。
 * 计分按题目 dimension 字段分桶（EI/SN/TF/JP 各 15 题），
 * 顺序不承担计分语义；1=选A（第一字母 E/S/T/J）、0=选B（第二字母 I/N/F/P）。
 */
public class MbtiCalculator {

    public static final int REQUIRED_ANSWER_COUNT = 60;

    public static String calculate(List<Integer> answers) {
        if (answers == null) {
            throw new IllegalArgumentException("还有" + REQUIRED_ANSWER_COUNT + "题未作答");
        }
        if (answers.size() < REQUIRED_ANSWER_COUNT) {
            int remaining = REQUIRED_ANSWER_COUNT - answers.size();
            throw new IllegalArgumentException("还有" + remaining + "题未作答");
        }

        List<MbtiQuestions.Question> questions = MbtiQuestions.getQuestions();
        int e = 0, i = 0, s = 0, n = 0, t = 0, f = 0, j = 0, p = 0;

        for (int idx = 0; idx < REQUIRED_ANSWER_COUNT; idx++) {
            int ans = answers.get(idx);
            String dim = (idx < questions.size()) ? questions.get(idx).dimension : "";
            if (dim == null) {
                dim = "";
            }
            switch (dim) {
                case "EI":
                    if (ans == 1) e++; else i++;
                    break;
                case "SN":
                    if (ans == 1) s++; else n++;
                    break;
                case "TF":
                    if (ans == 1) t++; else f++;
                    break;
                case "JP":
                    if (ans == 1) j++; else p++;
                    break;
                default:
                    if (idx % 4 == 0) { if (ans == 1) e++; else i++; }
                    else if (idx % 4 == 1) { if (ans == 1) s++; else n++; }
                    else if (idx % 4 == 2) { if (ans == 1) t++; else f++; }
                    else { if (ans == 1) j++; else p++; }
                    break;
            }
        }

        return (e > i ? "E" : "I") + (s > n ? "S" : "N") + (t > f ? "T" : "F") + (j > p ? "J" : "P");
    }
}
package com.example.project_pandora.ui.ai;

import java.util.List;

public class MbtiCalculator {

    public static final int REQUIRED_ANSWER_COUNT = 28;

    public static String calculate(List<Integer> answers) {
        if (answers == null) {
            throw new IllegalArgumentException("还有28题未作答");
        }
        if (answers.size() < REQUIRED_ANSWER_COUNT) {
            int remaining = REQUIRED_ANSWER_COUNT - answers.size();
            throw new IllegalArgumentException("还有" + remaining + "题未作答");
        }

        int e = 0, i = 0, s = 0, n = 0, t = 0, f = 0, j = 0, p = 0;

        for (int idx = 0; idx < REQUIRED_ANSWER_COUNT; idx++) {
            int ans = answers.get(idx);
            if (idx % 4 == 0) { if (ans == 1) e++; else i++; }
            else if (idx % 4 == 1) { if (ans == 1) s++; else n++; }
            else if (idx % 4 == 2) { if (ans == 1) t++; else f++; }
            else { if (ans == 1) j++; else p++; }
        }

        return (e > i ? "E" : "I") + (s > n ? "S" : "N") + (t > f ? "T" : "F") + (j > p ? "J" : "P");
    }
}
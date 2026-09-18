package com.example.project_pandora.core.divination;

import java.util.Calendar;
import java.util.Date;

public class DivinationEngine {

    private static final String[] ZHI_NAMES = {
        "子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"
    };

    public static class DivinationResult {
        private final String originalName;
        private final String mutualName;
        private final String changedName;
        private final String originalJudgment;
        private final String mutualJudgment;
        private final String changedJudgment;
        private final String[] originalLines;
        private final String[] mutualLines;
        private final String[] changedLines;
        private final int movingLine;

        public DivinationResult(String originalName, String mutualName, String changedName,
                                String originalJudgment, String mutualJudgment, String changedJudgment,
                                String[] originalLines, String[] mutualLines, String[] changedLines,
                                int movingLine) {
            this.originalName = originalName;
            this.mutualName = mutualName;
            this.changedName = changedName;
            this.originalJudgment = originalJudgment;
            this.mutualJudgment = mutualJudgment;
            this.changedJudgment = changedJudgment;
            this.originalLines = originalLines;
            this.mutualLines = mutualLines;
            this.changedLines = changedLines;
            this.movingLine = movingLine;
        }

        public String getOriginalName() { return originalName; }
        public String getMutualName() { return mutualName; }
        public String getChangedName() { return changedName; }
        public String getOriginalJudgment() { return originalJudgment; }
        public String getMutualJudgment() { return mutualJudgment; }
        public String getChangedJudgment() { return changedJudgment; }
        public String[] getOriginalLines() { return originalLines; }
        public String[] getMutualLines() { return mutualLines; }
        public String[] getChangedLines() { return changedLines; }
        public int getMovingLine() { return movingLine; }
    }

    public DivinationResult divine(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int hour = cal.get(Calendar.HOUR_OF_DAY);

        int yearZhi = (year - 4) % 12;
        if (yearZhi < 0) yearZhi += 12;
        int hourZhi = hour / 2 % 12;

        int upperSum = yearZhi + month + day;
        int lowerSum = upperSum + hourZhi;

        int upperHexagram = upperSum % 8;
        if (upperHexagram == 0) upperHexagram = 8;
        int lowerHexagram = lowerSum % 8;
        if (lowerHexagram == 0) lowerHexagram = 8;

        int movingLine = (upperSum + lowerSum) % 6;
        if (movingLine == 0) movingLine = 6;

        int originalHexagram = (lowerHexagram - 1) * 8 + upperHexagram;
        int mutualHexagram = calculateMutualHexagram(originalHexagram);
        int changedHexagram = calculateChangedHexagram(originalHexagram, movingLine);

        String originalName = HexagramTexts.getHexagramName(originalHexagram);
        String mutualName = HexagramTexts.getHexagramName(mutualHexagram);
        String changedName = HexagramTexts.getHexagramName(changedHexagram);

        String originalJudgment = HexagramTexts.getJudgment(originalHexagram);
        String mutualJudgment = HexagramTexts.getJudgment(mutualHexagram);
        String changedJudgment = HexagramTexts.getJudgment(changedHexagram);

        String[] originalLines = new String[6];
        String[] mutualLines = new String[6];
        String[] changedLines = new String[6];
        for (int i = 1; i <= 6; i++) {
            originalLines[i - 1] = HexagramTexts.getLineText(originalHexagram, i);
            mutualLines[i - 1] = HexagramTexts.getLineText(mutualHexagram, i);
            changedLines[i - 1] = HexagramTexts.getLineText(changedHexagram, i);
        }

        return new DivinationResult(originalName, mutualName, changedName,
                originalJudgment, mutualJudgment, changedJudgment,
                originalLines, mutualLines, changedLines, movingLine);
    }

    public DivinationResult divineToday() {
        return divine(new Date());
    }

    private int calculateChangedHexagram(int hexagram, int movingLine) {
        int upper = (hexagram - 1) % 8 + 1;
        int lower = (hexagram - 1) / 8 + 1;

        if (movingLine <= 3) {
            lower = flipTrigram(lower);
        } else {
            upper = flipTrigram(upper);
        }

        return (lower - 1) * 8 + upper;
    }

    private int calculateMutualHexagram(int hexagram) {
        int upper = (hexagram - 1) % 8 + 1;
        int lower = (hexagram - 1) / 8 + 1;

        int lowerLines = trigramToLines(lower);
        int upperLines = trigramToLines(upper);

        int line1 = lowerLines & 1;
        int line2 = (lowerLines >> 1) & 1;
        int line3 = (lowerLines >> 2) & 1;
        int line4 = upperLines & 1;
        int line5 = (upperLines >> 1) & 1;

        int mutualLowerLines = line2 | (line3 << 1) | (line4 << 2);
        int mutualUpperLines = line3 | (line4 << 1) | (line5 << 2);

        int mutualLower = linesToTrigram(mutualLowerLines);
        int mutualUpper = linesToTrigram(mutualUpperLines);

        return (mutualLower - 1) * 8 + mutualUpper;
    }

    private int flipTrigram(int trigram) {
        return trigram ^ 0x07;
    }

    private int trigramToLines(int trigram) {
        switch (trigram) {
            case 1: return 7;
            case 2: return 3;
            case 3: return 5;
            case 4: return 1;
            case 5: return 6;
            case 6: return 2;
            case 7: return 4;
            case 8: return 0;
            default: return 0;
        }
    }

    private int linesToTrigram(int lines) {
        switch (lines) {
            case 7: return 1;
            case 3: return 2;
            case 5: return 3;
            case 1: return 4;
            case 6: return 5;
            case 2: return 6;
            case 4: return 7;
            case 0: return 8;
            default: return 8;
        }
    }
}

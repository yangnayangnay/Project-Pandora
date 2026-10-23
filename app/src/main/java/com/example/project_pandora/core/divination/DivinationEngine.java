package com.example.project_pandora.core.divination;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

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
        private final boolean[] originalLinePattern;
        private final boolean[] mutualLinePattern;
        private final boolean[] changedLinePattern;

        public DivinationResult(String originalName, String mutualName, String changedName,
                                 String originalJudgment, String mutualJudgment, String changedJudgment,
                                 String[] originalLines, String[] mutualLines, String[] changedLines,
                                 int movingLine,
                                 boolean[] originalLinePattern, boolean[] mutualLinePattern, boolean[] changedLinePattern) {
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
            this.originalLinePattern = originalLinePattern;
            this.mutualLinePattern = mutualLinePattern;
            this.changedLinePattern = changedLinePattern;
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
        public boolean[] getOriginalLinePattern() { return originalLinePattern; }
        public boolean[] getMutualLinePattern() { return mutualLinePattern; }
        public boolean[] getChangedLinePattern() { return changedLinePattern; }
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

        return divineBySums(upperSum, lowerSum);
    }

    /**
     * Round23 REQ-23-08: 随机起卦（和数起卦法）。
     * 生成两个 1~8191 的随机数：R1 模拟"可数之物"之数定上卦基数，
     * R2 模拟"时辰之数"，与 divine(Date) 年月日时起卦完全同构——
     * 上卦由 R1 决定、下卦由 R1+R2 决定、动爻由两和总数决定。
     * 随机源选用 ThreadLocalRandom：娱乐场景无密码学安全要求，
     * 免种子管理、免对象创建，每次独立采样保证连续起卦互不相关。
     */
    public DivinationResult divineRandom() {
        int r1 = ThreadLocalRandom.current().nextInt(8191) + 1;
        int r2 = ThreadLocalRandom.current().nextInt(8191) + 1;
        return divineBySums(r1, r1 + r2);
    }

    /**
     * Round23 REQ-23-08: 和数起卦公共核心（自 divine(Date) 提取，语义逐行不变）。
     *
     * 算法:
     *   上卦数 = upperSum % 8，若为 0 取 8（先天卦数 1~8）
     *   下卦数 = lowerSum % 8，若为 0 取 8
     *   动爻 = (upperSum + lowerSum) % 6，若为 0 取 6（总数取动爻，1~6）
     *   本卦编号 = (下卦数 - 1) × 8 + 上卦数（范围 1~64）
     *   互卦/变卦/卦名卦辞爻辞沿用 calculateMutualHexagram /
     *   calculateChangedHexagram / HexagramTexts
     */
    private DivinationResult divineBySums(int upperSum, int lowerSum) {
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

        boolean[] originalLinePattern = getLinePattern(originalHexagram);
        boolean[] mutualLinePattern = getLinePattern(mutualHexagram);
        boolean[] changedLinePattern = getLinePattern(changedHexagram);

        return new DivinationResult(originalName, mutualName, changedName,
                originalJudgment, mutualJudgment, changedJudgment,
                originalLines, mutualLines, changedLines, movingLine,
                originalLinePattern, mutualLinePattern, changedLinePattern);
    }

    public boolean[] getLinePattern(int hexagram) {
        int upper = (hexagram - 1) % 8 + 1;
        int lower = (hexagram - 1) / 8 + 1;
        int lowerLines = trigramToLines(lower);
        int upperLines = trigramToLines(upper);
        boolean[] pattern = new boolean[6];
        pattern[0] = (lowerLines & 1) == 1;
        pattern[1] = ((lowerLines >> 1) & 1) == 1;
        pattern[2] = ((lowerLines >> 2) & 1) == 1;
        pattern[3] = (upperLines & 1) == 1;
        pattern[4] = ((upperLines >> 1) & 1) == 1;
        pattern[5] = ((upperLines >> 2) & 1) == 1;
        return pattern;
    }

    public DivinationResult divineToday() {
        return divine(new Date());
    }

    /**
     * Round19 REQ-19-12: 基于输入数字 N 的梅花易数起卦方法。
     *
     * 算法:
     *   上卦数 = N % 8，若为 0 取 8
     *   下卦数 = ((N/8 的余数 + N%8) % 8)，若为 0 取 8
     *       注: N/8 的余数即 N % 8，故下卦数 = ((N%8 + N%8) % 8)
     *   动爻 = ((N/64 的余数 + N/8 的余数 + N%8) % 6)，若为 0 取 6
     *       注: N/64 的余数即 N % 64，N/8 的余数即 N % 8
     *   本卦编号 = (下卦数 - 1) × 8 + 上卦数（范围 1-64）
     *
     * @param number 用户输入的正整数
     * @return DivinationResult 含本卦/互卦/变卦名称、卦辞、爻辞、动爻
     */
    public DivinationResult divine(int number) {
        // 上卦数 = N % 8，若为 0 取 8
        int upperHexagram = number % 8;
        if (upperHexagram == 0) upperHexagram = 8;

        // 下卦数 = ((N%8 + N%8) % 8)，若为 0 取 8
        int nMod8 = number % 8;
        int lowerHexagram = (nMod8 + nMod8) % 8;
        if (lowerHexagram == 0) lowerHexagram = 8;

        // 动爻 = ((N%64 + N%8 + N%8) % 6)，若为 0 取 6
        int nMod64 = number % 64;
        int movingLine = (nMod64 + nMod8 + nMod8) % 6;
        if (movingLine == 0) movingLine = 6;

        // 本卦编号 = (下卦数 - 1) × 8 + 上卦数（范围 1-64）
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

        boolean[] originalLinePattern = getLinePattern(originalHexagram);
        boolean[] mutualLinePattern = getLinePattern(mutualHexagram);
        boolean[] changedLinePattern = getLinePattern(changedHexagram);

        return new DivinationResult(originalName, mutualName, changedName,
                originalJudgment, mutualJudgment, changedJudgment,
                originalLines, mutualLines, changedLines, movingLine,
                originalLinePattern, mutualLinePattern, changedLinePattern);
    }

    /**
     * Round24 REQ-24-06: 按卦名+动爻重建完整卦象（当日已算结果回显用）。
     * HEXAGRAM_NAMES 反查本卦编号后复用互卦/变卦计算与 HexagramTexts 全量文本，
     * 与 divineRandom 直接产出同构；非法卦名抛 IllegalArgumentException（调用方容错不崩溃）。
     */
    public DivinationResult fromNames(String originalName, int movingLine) {
        return fromNames(originalName, null, null, movingLine);
    }

    /**
     * Round25 缺陷 #R25-03: 回显优先采用落库记录的互卦/变卦名。
     * 旧回显仅按"本卦+动爻"重推互卦/变卦，当落库值非当前引擎产物
     * （历史数据/算法演进/造数）时，卡片卦象与 AI 解读文本（基于落库名
     * 生成）同屏矛盾（实测 admin0 记录互卦"夬" vs 引擎推导"乾"）。
     * 落库名可反查时以其为准；空/未知名回退引擎推导。
     */
    public DivinationResult fromNames(String originalName, String mutualName, String changedName, int movingLine) {
        if (originalName == null || originalName.trim().isEmpty()) {
            throw new IllegalArgumentException("\u5366\u540D\u4E3A\u7A7A\uFF0C\u65E0\u6CD5\u91CD\u5EFA\u5366\u8C61");
        }
        int originalHexagram = nameToHexagram(originalName);
        if (originalHexagram < 1) {
            throw new IllegalArgumentException("\u672A\u77E5\u5366\u540D: " + originalName);
        }
        int line = movingLine;
        if (line < 1) line = 1;
        if (line > 6) line = 6;

        int mutualHexagram = nameToHexagram(mutualName);
        if (mutualHexagram < 1) {
            mutualHexagram = calculateMutualHexagram(originalHexagram);
        }
        int changedHexagram = nameToHexagram(changedName);
        if (changedHexagram < 1) {
            changedHexagram = calculateChangedHexagram(originalHexagram, line);
        }

        String mutualNameResolved = HexagramTexts.getHexagramName(mutualHexagram);
        String changedNameResolved = HexagramTexts.getHexagramName(changedHexagram);

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

        boolean[] originalLinePattern = getLinePattern(originalHexagram);
        boolean[] mutualLinePattern = getLinePattern(mutualHexagram);
        boolean[] changedLinePattern = getLinePattern(changedHexagram);

        return new DivinationResult(originalName, mutualNameResolved, changedNameResolved,
                originalJudgment, mutualJudgment, changedJudgment,
                originalLines, mutualLines, changedLines, line,
                originalLinePattern, mutualLinePattern, changedLinePattern);
    }

    private int nameToHexagram(String name) {
        if (name == null || name.trim().isEmpty()) {
            return -1;
        }
        String[] names = HexagramTexts.HEXAGRAM_NAMES;
        for (int i = 0; i < names.length; i++) {
            if (names[i].equals(name)) {
                return i + 1;
            }
        }
        return -1;
    }

    /**
     * Round25 缺陷 #R25-02: 变卦改为标准"动爻单翻"。
     * 旧实现按三爻卦编号整体 ^0x07 翻转：① 语义错误（动一爻不应整卦翻转，
     * 如乾九五动旧算法得"需"，标准应为"大有"）；② 艮(7)/坤(8) 编号异或后
     * 产生 0/15 无效编号，导致变卦名空串、落库 NULL（实测 id=5 遁+2爻、
     * id=7 渐+2爻 变卦均为 null）。
     * 标准算法：取本卦六爻画，仅翻转动爻一爻，重组上下卦编号。
     */
    private int calculateChangedHexagram(int hexagram, int movingLine) {
        boolean[] pattern = getLinePattern(hexagram);
        int line = Math.max(1, Math.min(6, movingLine));
        pattern[line - 1] = !pattern[line - 1];

        int lowerLines = (pattern[0] ? 1 : 0) | (pattern[1] ? 2 : 0) | (pattern[2] ? 4 : 0);
        int upperLines = (pattern[3] ? 1 : 0) | (pattern[4] ? 2 : 0) | (pattern[5] ? 4 : 0);

        return (linesToTrigram(lowerLines) - 1) * 8 + linesToTrigram(upperLines);
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

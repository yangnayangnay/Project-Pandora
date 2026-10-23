package com.example.project_pandora.core.divination;

import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Round23 REQ-23-08 (T9.1): DivinationEngine 和数核心重构回归与随机性验证。
 * 覆盖验收标准：① divineRandom 连续 10 次不全相同；② divine(Date) 重构前后行为一致；
 * ③ 随机起卦 64 卦全覆盖、动爻 1~6 全覆盖（无 divine(int) 的分布缺陷）。
 */
public class DivinationEngineTest {

    private final DivinationEngine engine = new DivinationEngine();

    @Test
    public void divineRandom_tenCalls_notAllSame() {
        Set<String> results = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            DivinationEngine.DivinationResult r = engine.divineRandom();
            results.add(r.getOriginalName() + "/" + r.getMutualName() + "/"
                    + r.getChangedName() + "/" + r.getMovingLine());
        }
        assertTrue("连续 10 次随机起卦结果不应全相同, 实际: " + results,
                results.size() > 1);
    }

    @Test
    public void divine_fixedDate_matchesLegacyAlgorithm() {
        int[][] dateCases = {
                {2025, Calendar.JUNE, 15, 14},
                {2024, Calendar.JANUARY, 1, 0},
                {2026, Calendar.OCTOBER, 19, 23},
                {1999, Calendar.DECEMBER, 31, 11},
                {2000, Calendar.FEBRUARY, 29, 6},
                {2023, Calendar.MARCH, 5, 19}
        };
        for (int[] c : dateCases) {
            Date date = mkDate(c[0], c[1], c[2], c[3]);
            DivinationEngine.DivinationResult r = engine.divine(date);
            String[] legacy = legacyDivineDate(date);
            assertEquals("本卦名不一致 " + Arrays.toString(c),
                    legacy[0], r.getOriginalName());
            assertEquals("动爻不一致 " + Arrays.toString(c),
                    legacy[1], String.valueOf(r.getMovingLine()));
        }
    }

    @Test
    public void divine_handComputedCase() {
        // 2025-06-15 14时: yearZhi=5, hourZhi=7, upperSum=26, lowerSum=33
        // 上卦=2, 下卦=1, 动爻=59%6=5 → 本卦"夬"(编号2), 互卦"乾"
        // Round25 #R25-02 变卦标准化: 夬九五动(上兑中爻阴→阳? 兑中爻动→震) → 雷天大壮
        DivinationEngine.DivinationResult r = engine.divine(mkDate(2025, Calendar.JUNE, 15, 14));
        assertEquals("夬", r.getOriginalName());
        assertEquals("乾", r.getMutualName());
        assertEquals("大壮", r.getChangedName());
        assertEquals(5, r.getMovingLine());
    }

    /**
     * Round25 #R25-02: 变卦"动爻单翻"标准算法教科书案例。
     * ① 乾九五动 → 上乾中爻变 → 上离下乾 = 火天大有；
     * ② 乾初九动 → 下乾初爻变 → 上乾下巽 = 天风姤；
     * ③ 乾九二动 → 下乾中爻变 → 上乾下离 = 天火同人；
     * ④ 坤六五动 → 上坤中爻变 → 上坎下坤 = 水地比；
     * ⑤ 泰初九动 → 下乾初爻变 → 上坤下巽 = 地风升。
     */
    @Test
    public void changedHexagram_standardMovingLineFlip() {
        assertChanged("乾", 5, "大有");
        assertChanged("乾", 1, "姤");
        assertChanged("乾", 2, "同人");
        assertChanged("坤", 5, "比");
        assertChanged("泰", 1, "升");
    }

    /**
     * Round25 #R25-02 回归: 艮/坤编号参与动爻翻转不再产生空变卦名
     * （旧编号 ^0x07 算法对艮(7)/坤(8) 得 0/15 无效编号 → 变卦名空串）。
     * 遁(上乾下艮)六二动 → 下艮变巽 → 天风姤；渐(上巽下艮)六二动 → 巽为风；
     * 剥(上艮下坤)六三动 → 下坤变艮 → 艮为山。
     */
    @Test
    public void changedHexagram_genKunNeverEmpty() {
        assertChanged("遁", 2, "姤");
        assertChanged("渐", 2, "巽");
        assertChanged("剥", 3, "艮");
    }

    private void assertChanged(String originalName, int movingLine, String expectedChangedName) {
        DivinationEngine.DivinationResult r = engine.fromNames(originalName, movingLine);
        assertEquals(originalName + "+" + movingLine + "爻 变卦应为 " + expectedChangedName,
                expectedChangedName, r.getChangedName());
    }

    /**
     * Round25 #R25-03: 回显重建时落库的互卦/变卦名优先于引擎推导。
     * 实测 admin0 记录（乾/夬/大有/5爻）: 引擎推导互卦=乾、变卦=大有，
     * 旧回显只重推导致卡片"乾/需"与 AI 解读（基于夬/大有）同屏矛盾。
     */
    @Test
    public void fromNames_recordNamesTakePrecedence() {
        DivinationEngine.DivinationResult r = engine.fromNames("乾", "夬", "大有", 5);
        assertEquals("夬", r.getMutualName());
        assertEquals("大有", r.getChangedName());

        DivinationEngine.DivinationResult fallback = engine.fromNames("乾", "", null, 5);
        assertEquals("落库名为空时互卦回退引擎推导", "乾", fallback.getMutualName());
        assertEquals("落库名为空时变卦回退引擎推导", "大有", fallback.getChangedName());
    }

    @Test
    public void divineRandom_distribution_coversAll() {
        Set<String> originalNames = new HashSet<>();
        Set<Integer> movingLines = new HashSet<>();
        for (int i = 0; i < 20000; i++) {
            DivinationEngine.DivinationResult r = engine.divineRandom();
            originalNames.add(r.getOriginalName());
            movingLines.add(r.getMovingLine());
        }
        assertEquals("随机起卦应覆盖全部 64 卦", 64, originalNames.size());
        for (int line = 1; line <= 6; line++) {
            assertTrue("动爻 " + line + " 应出现", movingLines.contains(line));
        }
    }

    private Date mkDate(int year, int month, int day, int hour) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(year, month, day, hour, 30, 0);
        return cal.getTime();
    }

    /**
     * 旧 divine(Date) 算法独立复刻（重构前语义），作 golden model
     * 对比重构后 divineBySums 提取路径的等价性。
     */
    private String[] legacyDivineDate(Date date) {
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
        return new String[]{HexagramTexts.getHexagramName(originalHexagram), String.valueOf(movingLine)};
    }
}
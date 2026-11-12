package com.example.project_pandora.ui.aimap;

import org.junit.Test;
import java.util.Calendar;
import java.util.TimeZone;
import static org.junit.Assert.*;

public class BirthFortuneEngineTest {
    private Calendar now() {
        Calendar value = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"));
        value.clear(); value.set(2026, Calendar.NOVEMBER, 10, 12, 0, 0); return value;
    }
    private BirthFortuneEngine.Result birth(int y, int m, int d, int h, int min) {
        return BirthFortuneEngine.calculate(y, m, d, h, min, now());
    }

    // Reference: lunar-java v1.7.4 BaZiTest.testGanZhi, rather than self-derived expectations.
    @Test public void knownFourPillarsAndElements() {
        BirthFortuneEngine.Result result = birth(2005, 12, 23, 8, 37);
        assertArrayEquals(new String[]{"乙酉", "戊子", "辛巳", "壬辰"}, result.pillars);
        assertArrayEquals(new String[]{"木金", "土水", "金火", "水土"}, result.elements);
        assertArrayEquals(new int[]{1, 1, 2, 2, 2}, result.elementCounts);
        assertEquals("鸡", result.zodiac); assertEquals("摩羯座", result.constellation);
    }

    @Test public void lateZiHourRetainsCivilDayUntilMidnight() {
        assertArrayEquals(new String[]{"戊辰", "甲寅", "庚子", "丁亥"}, birth(1988, 2, 15, 22, 30).pillars);
        assertArrayEquals(new String[]{"戊辰", "甲寅", "庚子", "戊子"}, birth(1988, 2, 15, 23, 30).pillars);
        assertEquals("辛丑", birth(1988, 2, 16, 0, 0).pillars[2]);
    }

    @Test public void liChunChangesPillarsBeforeLunarZodiacYear() {
        BirthFortuneEngine.Result before = birth(2024, 2, 4, 16, 26);
        BirthFortuneEngine.Result after = birth(2024, 2, 4, 16, 28);
        assertEquals("癸卯", before.pillars[0]); assertEquals("乙丑", before.pillars[1]);
        assertEquals("甲辰", after.pillars[0]); assertEquals("丙寅", after.pillars[1]);
        assertEquals("兔", before.zodiac); assertEquals("兔", after.zodiac);
        assertEquals("兔", birth(2024, 2, 9, 12, 0).zodiac);
        assertEquals("龙", birth(2024, 2, 10, 12, 0).zodiac);
    }

    @Test public void leapLunarMonthIsRepresentedInBirthDate() {
        assertTrue(birth(2023, 3, 22, 10, 0).lunarDate.contains("闰二月初一"));
    }

    @Test public void leapSolarDateAndEarliestSupportedDateAreAccepted() {
        assertEquals(4, birth(2000, 2, 29, 12, 0).pillars.length);
        assertEquals(4, birth(1900, 1, 1, 0, 0).pillars.length);
    }

    @Test public void allZodiacsAndConstellationsHaveActionableContent() {
        java.util.Set<String> zodiacs = new java.util.HashSet<>(), signs = new java.util.HashSet<>();
        for (int i = 0; i < 12; i++) {
            BirthFortuneEngine.Result animal = birth(2000 + i, 7, 1, 12, 0);
            zodiacs.add(animal.zodiac); assertFalse(animal.zodiacTip.isEmpty());
            BirthFortuneEngine.Result sign = birth(2000, i + 1, 10, 12, 0);
            signs.add(sign.constellation); assertFalse(sign.constellationTip.isEmpty());
        }
        assertEquals(12, zodiacs.size()); assertEquals(12, signs.size());
    }

    @Test public void constellationBoundariesUseCivilBirthday() {
        assertEquals("双鱼座", birth(2000, 3, 20, 12, 0).constellation);
        assertEquals("白羊座", birth(2000, 3, 21, 12, 0).constellation);
        assertEquals("射手座", birth(2000, 12, 21, 12, 0).constellation);
        assertEquals("摩羯座", birth(2000, 12, 22, 12, 0).constellation);
    }

    @Test public void readingIsStableWithinSameBeijingDay() {
        Calendar later = now(); later.add(Calendar.HOUR_OF_DAY, 10);
        BirthFortuneEngine.Result first = birth(2005, 12, 23, 8, 37);
        BirthFortuneEngine.Result second = BirthFortuneEngine.calculate(2005, 12, 23, 8, 37, later);
        assertEquals(first.date, second.date); assertEquals(first.theme, second.theme);
        assertEquals(first.action, second.action); assertEquals(first.caution, second.caution);
    }

    @Test public void deviceTimezoneDoesNotChangeBeijingReading() {
        TimeZone previous = TimeZone.getDefault();
        try {
            Calendar instant = now(); instant.set(2026, Calendar.NOVEMBER, 10, 0, 10, 0);
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));
            BirthFortuneEngine.Result result = BirthFortuneEngine.calculate(2005, 12, 23, 8, 37, instant);
            assertEquals("2026.11.10", result.date);
            assertArrayEquals(new String[]{"乙酉", "戊子", "辛巳", "壬辰"}, result.pillars);
        } finally { TimeZone.setDefault(previous); }
    }

    @Test public void dailyPromptsCanChangeAcrossDays() {
        Calendar next = now(); next.add(Calendar.DAY_OF_MONTH, 1);
        BirthFortuneEngine.Result first = birth(2005, 12, 23, 8, 37);
        BirthFortuneEngine.Result second = BirthFortuneEngine.calculate(2005, 12, 23, 8, 37, next);
        assertNotEquals(first.date, second.date); assertNotEquals(first.theme, second.theme);
    }

    @Test public void invalidOrFutureInputHasReadableError() {
        int[][] invalid = {{2001, 2, 29, 12, 0}, {2000, 0, 1, 12, 0}, {2000, 13, 1, 12, 0},
                {2000, 1, 0, 12, 0}, {2000, 4, 31, 12, 0}, {2000, 1, 1, -1, 0}, {2000, 1, 1, 24, 0},
                {2000, 1, 1, 12, 60}, {1899, 12, 31, 12, 0}, {2637, 1, 1, 12, 0}, {2026, 11, 10, 12, 1}};
        for (int[] input : invalid) {
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> birth(input[0], input[1], input[2], input[3], input[4]));
            assertNotNull(error.getMessage()); assertFalse(error.getMessage().isEmpty());
        }
        assertEquals("2026.11.10", birth(2026, 11, 10, 12, 0).date);
    }
}

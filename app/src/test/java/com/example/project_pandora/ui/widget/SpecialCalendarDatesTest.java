package com.example.project_pandora.ui.widget;

import org.junit.Test;
import java.util.Calendar;
import java.util.TimeZone;
import static org.junit.Assert.*;

public class SpecialCalendarDatesTest {
    private static Calendar date(int year, int month, int day) {
        Calendar date = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"));
        date.clear(); date.set(year, month - 1, day, 12, 0);
        return date;
    }
    @Test public void allTwentyFourTermsUseTheCorrectChinaCivilDateIn2026() {
        int[][] dates = {{1,5},{1,20},{2,4},{2,18},{3,5},{3,20},{4,5},{4,20},{5,5},{5,21},{6,5},{6,21},
                {7,7},{7,23},{8,7},{8,23},{9,7},{9,23},{10,8},{10,23},{11,7},{11,22},{12,7},{12,22}};
        String[] names = {"小寒","大寒","立春","雨水","惊蛰","春分","清明","谷雨","立夏","小满","芒种","夏至",
                "小暑","大暑","立秋","处暑","白露","秋分","寒露","霜降","立冬","小雪","大雪","冬至"};
        for (int i = 0; i < names.length; i++) assertEquals(names[i], SpecialCalendarDates.solarTerm(date(2026, dates[i][0], dates[i][1])));
    }
    @Test public void termsMoveAcrossYearsAndLeapYearsRatherThanUsingFixedDays() {
        assertEquals("清明", SpecialCalendarDates.solarTerm(date(2024, 4, 4)));
        assertEquals("冬至", SpecialCalendarDates.solarTerm(date(2025, 12, 21)));
        assertEquals("冬至", SpecialCalendarDates.solarTerm(date(2026, 12, 22)));
        assertEquals("", SpecialCalendarDates.solarTerm(date(2026, 12, 21)));
    }
    @Test public void lunarFestivalsSkipLeapMonthsAndHandleShortNewYearsEve() {
        assertEquals("春节", SpecialCalendarDates.label(date(2026, 2, 17), 1, 1, false, false));
        assertEquals("", SpecialCalendarDates.label(date(2026, 2, 17), 1, 1, true, false));
        assertEquals("除夕", SpecialCalendarDates.label(date(2026, 2, 16), 12, 29, false, true));
        assertEquals("中秋节", SpecialCalendarDates.label(date(2026, 9, 25), 8, 15, false, false));
        assertEquals("国庆节 · 中秋节", SpecialCalendarDates.label(date(2020, 10, 1), 8, 15, false, false));
    }
    @Test public void ordinaryDatesStayEmptyAndUnsupportedTermYearsDoNotFail() {
        assertEquals("", SpecialCalendarDates.label(date(2026, 11, 9), 10, 1, false, false));
        assertEquals("", SpecialCalendarDates.solarTerm(date(2200, 12, 22)));
        assertEquals("", SpecialCalendarDates.fixedFestival(12, 13));
    }

    @Test public void captionsExcludeSolarTermsAndNonStatutoryFestivals() {
        assertEquals("", SpecialCalendarDates.label(date(2026, 12, 22), 11, 14, false, false));
        assertEquals("", SpecialCalendarDates.label(date(2026, 3, 8), 1, 20, false, false));
        assertEquals("", SpecialCalendarDates.label(date(2026, 3, 3), 1, 15, false, false));
        assertEquals("清明节", SpecialCalendarDates.label(date(2026, 4, 5), 2, 18, false, false));
    }
}

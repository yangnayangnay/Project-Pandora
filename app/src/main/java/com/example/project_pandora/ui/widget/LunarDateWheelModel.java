package com.example.project_pandora.ui.widget;

import android.icu.util.ChineseCalendar;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

/** Real lunar months, including leap months; stored dates remain civil dates. */
public final class LunarDateWheelModel {
    private static final TimeZone CHINA = TimeZone.getTimeZone("Asia/Shanghai");
    // Android ICU versions use different Chinese-calendar epochs (2024 or 4661
    // for the same lunar year). Calibrate on a known new year's day instead of
    // assuming that EXTENDED_YEAR is always Gregorian-style year + 2637.
    private static final int YEAR_OFFSET = yearOffset();
    public final int year, month, day;
    public final boolean leap;
    private LunarDateWheelModel(int year, int month, int day, boolean leap) {
        this.year = year; this.month = month; this.day = day; this.leap = leap;
    }
    public static LunarDateWheelModel from(Calendar date) {
        Calendar noon = Calendar.getInstance(CHINA); noon.clear();
        noon.set(date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH), 12, 0);
        ChineseCalendar lunar = lunar(); lunar.setTimeInMillis(noon.getTimeInMillis());
        return new LunarDateWheelModel(lunar.get(ChineseCalendar.EXTENDED_YEAR) - YEAR_OFFSET,
                lunar.get(ChineseCalendar.MONTH) + 1, lunar.get(ChineseCalendar.DAY_OF_MONTH), lunar.get(ChineseCalendar.IS_LEAP_MONTH) == 1);
    }
    public static List<Month> months(int year) {
        ChineseCalendar lunar = lunar(); lunar.clear();
        lunar.set(ChineseCalendar.EXTENDED_YEAR, year + YEAR_OFFSET); lunar.set(ChineseCalendar.MONTH, 0);
        lunar.set(ChineseCalendar.IS_LEAP_MONTH, 0); lunar.set(ChineseCalendar.DAY_OF_MONTH, 1); lunar.set(ChineseCalendar.HOUR_OF_DAY, 12);
        List<Month> result = new ArrayList<>();
        for (int i = 0; i < 13 && lunar.get(ChineseCalendar.EXTENDED_YEAR) == year + YEAR_OFFSET; i++) {
            result.add(new Month(lunar.get(ChineseCalendar.MONTH) + 1, lunar.get(ChineseCalendar.IS_LEAP_MONTH) == 1,
                    lunar.getActualMaximum(ChineseCalendar.DAY_OF_MONTH), lunar.getTimeInMillis()));
            lunar.add(ChineseCalendar.MONTH, 1);
        }
        return result;
    }
    public static String dayName(int day) {
        if (day == 10) return "初十"; if (day == 20) return "二十"; if (day == 30) return "三十";
        return (day < 10 ? "初" : day < 20 ? "十" : "廿") + new String[]{"", "一", "二", "三", "四", "五", "六", "七", "八", "九"}[day % 10];
    }
    private static ChineseCalendar lunar() { return new ChineseCalendar(android.icu.util.TimeZone.getTimeZone("Asia/Shanghai")); }
    private static int yearOffset() {
        Calendar reference = Calendar.getInstance(CHINA); reference.clear();
        reference.set(2024, Calendar.FEBRUARY, 10, 12, 0);
        ChineseCalendar lunar = lunar(); lunar.setTimeInMillis(reference.getTimeInMillis());
        return lunar.get(ChineseCalendar.EXTENDED_YEAR) - 2024;
    }
    public static final class Month {
        public final int number, days;
        public final boolean leap;
        private final long first;
        private Month(int number, boolean leap, int days, long first) { this.number = number; this.leap = leap; this.days = days; this.first = first; }
        public String label() { return (leap ? "闰" : "") + new String[]{"正月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "冬月", "腊月"}[number - 1]; }
        public Calendar date(int day, Calendar original) {
            Calendar china = Calendar.getInstance(CHINA); china.setTimeInMillis(first); china.add(Calendar.DAY_OF_MONTH, Math.min(day, days) - 1);
            Calendar result = (Calendar) original.clone(); result.set(Calendar.DAY_OF_MONTH, 1);
            result.set(Calendar.ERA, china.get(Calendar.ERA)); result.set(Calendar.YEAR, china.get(Calendar.YEAR));
            result.set(Calendar.MONTH, china.get(Calendar.MONTH)); result.set(Calendar.DAY_OF_MONTH, china.get(Calendar.DAY_OF_MONTH));
            return result;
        }
    }
}

package com.example.project_pandora.ui.widget;

import android.icu.util.ChineseCalendar;
import java.util.Calendar;

/** Shared lunar and special-date captions for both calendars and their week/day headers. */
public final class CalendarDayLabels {
    public final String lunar, special;
    private CalendarDayLabels(String lunar, String special) { this.lunar = lunar; this.special = special; }

    public static CalendarDayLabels forDate(Calendar date) {
        ChineseCalendar chinese = new ChineseCalendar(android.icu.util.TimeZone.getTimeZone("Asia/Shanghai"));
        Calendar noon = Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Shanghai"));
        noon.clear(); noon.set(date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH), 12, 0);
        chinese.setTimeInMillis(noon.getTimeInMillis());
        int month = chinese.get(android.icu.util.Calendar.MONTH) + 1;
        int day = chinese.get(android.icu.util.Calendar.DAY_OF_MONTH);
        boolean leap = chinese.get(ChineseCalendar.IS_LEAP_MONTH) == 1;
        String lunar;
        if (day == 1) lunar = (leap ? "闰" : "") + new String[]{"正月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "冬月", "腊月"}[month - 1];
        else if (day == 10) lunar = "初十";
        else if (day == 20) lunar = "二十";
        else if (day == 30) lunar = "三十";
        else lunar = (day < 10 ? "初" : day < 20 ? "十" : "廿") + new String[]{"", "一", "二", "三", "四", "五", "六", "七", "八", "九"}[day % 10];
        chinese.add(android.icu.util.Calendar.DAY_OF_MONTH, 1);
        boolean eve = month == 12 && !leap && chinese.get(android.icu.util.Calendar.MONTH) == 0
                && chinese.get(android.icu.util.Calendar.DAY_OF_MONTH) == 1;
        return new CalendarDayLabels(lunar, SpecialCalendarDates.label(date, month, day, leap, eve));
    }
}

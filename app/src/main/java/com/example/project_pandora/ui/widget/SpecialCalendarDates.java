package com.example.project_pandora.ui.widget;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/** Offline calendar annotations. Festival dates do not imply statutory leave or makeup workdays. */
public final class SpecialCalendarDates {
    private static final TimeZone CHINA = TimeZone.getTimeZone("Asia/Shanghai");
    private static final String[] TERMS = {"小寒", "大寒", "立春", "雨水", "惊蛰", "春分",
            "清明", "谷雨", "立夏", "小满", "芒种", "夏至", "小暑", "大暑", "立秋", "处暑",
            "白露", "秋分", "寒露", "霜降", "立冬", "小雪", "大雪", "冬至"};
    private static final Map<Integer, Map<Integer, String>> TERM_CACHE = new LinkedHashMap<Integer, Map<Integer, String>>() {
        @Override protected boolean removeEldestEntry(Map.Entry<Integer, Map<Integer, String>> entry) { return size() > 8; }
    };
    private SpecialCalendarDates() {}

    public static String label(Calendar date, int lunarMonth, int lunarDay, boolean leapMonth, boolean newYearsEve) {
        List<String> names = new ArrayList<>();
        String fixed = fixedFestival(date.get(Calendar.MONTH) + 1, date.get(Calendar.DAY_OF_MONTH));
        if (!fixed.isEmpty()) names.add(fixed);
        if (newYearsEve) names.add("除夕");
        if (!leapMonth) {
            String festival = lunarFestival(lunarMonth, lunarDay);
            if (!festival.isEmpty()) names.add(festival);
        }
        String term = solarTerm(date);
        if (!term.isEmpty()) names.add(term);
        return String.join(" · ", names);
    }

    public static String fixedFestival(int month, int day) {
        switch (month * 100 + day) {
            case 101: return "元旦";
            case 214: return "情人节";
            case 308: return "妇女节";
            case 312: return "植树节";
            case 501: return "劳动节";
            case 504: return "青年节";
            case 601: return "儿童节";
            case 701: return "建党节";
            case 801: return "建军节";
            case 910: return "教师节";
            case 1001: return "国庆节";
            case 1213: return "国家公祭日";
            case 1220: return "澳门回归纪念日";
            case 1224: return "平安夜";
            case 1225: return "圣诞节";
            default: return "";
        }
    }

    private static String lunarFestival(int month, int day) {
        switch (month * 100 + day) {
            case 101: return "春节";
            case 115: return "元宵节";
            case 202: return "龙抬头";
            case 505: return "端午节";
            case 707: return "七夕";
            case 815: return "中秋节";
            case 909: return "重阳节";
            case 1208: return "腊八节";
            default: return "";
        }
    }

    public static String solarTerm(Calendar date) {
        int year = date.get(Calendar.YEAR);
        // The apparent solar longitude approximation is intended for modern civil calendars.
        if (year < 1900 || year > 2100) return "";
        Map<Integer, String> dates;
        synchronized (TERM_CACHE) {
            dates = TERM_CACHE.get(year);
            if (dates == null) { dates = computeTerms(year); TERM_CACHE.put(year, dates); }
        }
        String value = dates.get((date.get(Calendar.MONTH) + 1) * 100 + date.get(Calendar.DAY_OF_MONTH));
        return value == null ? "" : value;
    }

    private static Map<Integer, String> computeTerms(int year) {
        Map<Integer, String> result = new LinkedHashMap<>();
        for (int i = 0; i < TERMS.length; i++) {
            Calendar guess = new GregorianCalendar(CHINA);
            guess.clear(); guess.set(year, i / 2, i % 2 == 0 ? 5 : 20, 12, 0);
            double instant = guess.getTimeInMillis();
            double target = (285 + i * 15) % 360;
            // Solve apparent ecliptic longitude; using the instant keeps boundary dates in China time.
            for (int step = 0; step < 8; step++) {
                double difference = (solarLongitude(instant) - target + 540) % 360 - 180;
                instant -= difference / .98564736 * 86_400_000;
            }
            Calendar term = new GregorianCalendar(CHINA); term.setTimeInMillis(Math.round(instant));
            result.put((term.get(Calendar.MONTH) + 1) * 100 + term.get(Calendar.DAY_OF_MONTH), TERMS[i]);
        }
        return Collections.unmodifiableMap(result);
    }

    private static double solarLongitude(double utcMillis) {
        double julian = utcMillis / 86_400_000 + 2440587.5;
        double t = (julian - 2451545.0) / 36525;
        double meanLongitude = 280.46646 + t * (36000.76983 + .0003032 * t);
        double anomaly = Math.toRadians(357.52911 + t * (35999.05029 - .0001537 * t));
        double center = (1.914602 - t * (.004817 + .000014 * t)) * Math.sin(anomaly)
                + (.019993 - .000101 * t) * Math.sin(2 * anomaly) + .000289 * Math.sin(3 * anomaly);
        double longitude = meanLongitude + center - .00569 - .00478 * Math.sin(Math.toRadians(125.04 - 1934.136 * t));
        return (longitude % 360 + 360) % 360;
    }
}

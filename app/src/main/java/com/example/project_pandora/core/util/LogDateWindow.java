package com.example.project_pandora.core.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Uses the server's business timezone, including at midnight and across month boundaries. */
public final class LogDateWindow {
    private LogDateWindow() {}
    public static String daysAgo(int days) {
        return daysAgo(days, new Date());
    }
    static String daysAgo(int days, Date now) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"));
        calendar.setTime(now);
        calendar.add(Calendar.DAY_OF_YEAR, -days);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        format.setTimeZone(calendar.getTimeZone());
        return format.format(calendar.getTime());
    }
    public static boolean isWritable(String date) {
        return isWritable(date, new Date());
    }
    static boolean isWritable(String date, Date now) {
        if (date == null || !date.matches("\\d{4}-\\d{2}-\\d{2}")) return false;
        return date.equals(daysAgo(1, now)) || date.equals(daysAgo(0, now)) || date.equals(daysAgo(-1, now));
    }
}

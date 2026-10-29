package com.example.project_pandora.core.util;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Minute precision for task pickers, requests and restored legacy date-only drafts. */
public final class TaskDateTimes {
    private TaskDateTimes() {}

    public static Date parse(String value) {
        if (value == null || !value.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}")) return null;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT);
        format.setLenient(false);
        ParsePosition position = new ParsePosition(0);
        Date result = format.parse(value, position);
        return position.getIndex() == value.length() ? result : null;
    }

    public static String format(Date value) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT).format(value);
    }

    public static String restore(Object value, boolean end) {
        String local = DateCompat.toLocalDateTime(value);
        if (local != null && local.length() == 10) local += end ? " 23:59" : " 00:00";
        return parse(local) == null ? null : local;
    }

    public static boolean validRange(String start, String end) {
        Date first = parse(start), last = parse(end);
        return first != null && last != null && !last.before(first);
    }
}

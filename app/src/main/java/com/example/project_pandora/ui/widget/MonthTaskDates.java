package com.example.project_pandora.ui.widget;

/** Date-only end dates are inclusive; timed midnight ends belong to the previous day. */
public final class MonthTaskDates {
    private MonthTaskDates() {}
    public static boolean covers(String start, String end, String day) {
        if (day == null || day.length() != 10) return false;
        if (start == null || start.length() < 10) start = end;
        if (start == null || start.length() < 10) return false;
        if (end == null || end.length() < 10 || end.compareTo(start) < 0) end = start;
        String first = start.substring(0, 10), last = end.substring(0, 10);
        boolean allDay = (start.length() == 10 || start.endsWith("00:00"))
                && (end.length() == 10 || end.endsWith("00:00"));
        boolean exclusiveMidnight = !allDay && end.endsWith("00:00") && !first.equals(last);
        return first.compareTo(day) <= 0 && (exclusiveMidnight ? last.compareTo(day) > 0 : last.compareTo(day) >= 0);
    }
}

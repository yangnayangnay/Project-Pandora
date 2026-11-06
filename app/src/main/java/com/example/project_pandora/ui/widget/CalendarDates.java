package com.example.project_pandora.ui.widget;

import java.util.Calendar;

/** Shared date arithmetic for company month/week/day navigation and the quick picker. */
public final class CalendarDates {
    public static final int MONTH = 0, WEEK = 1, DAY = 2;
    public static final int MIN_YEAR = 1, MAX_YEAR = 9999;

    private CalendarDates() {}

    public static Calendar shift(Calendar selected, int mode, int direction) {
        if (direction != -1 && direction != 1) throw new IllegalArgumentException("Invalid direction");
        Calendar next = (Calendar) selected.clone();
        if (mode == MONTH) next.add(Calendar.MONTH, direction);
        else if (mode == WEEK) next.add(Calendar.DATE, direction * 7);
        else if (mode == DAY) next.add(Calendar.DATE, direction);
        else throw new IllegalArgumentException("Invalid calendar mode");
        if (next.get(Calendar.ERA) != java.util.GregorianCalendar.AD
                || next.get(Calendar.YEAR) > MAX_YEAR) return (Calendar) selected.clone();
        return next;
    }

    /** Month is one-based; a previous month-end selection clamps to the new month's last day. */
    public static Calendar select(Calendar original, int year, int month, int day) {
        if (year < MIN_YEAR || year > MAX_YEAR || month < 1 || month > 12 || day < 1)
            throw new IllegalArgumentException("Invalid date");
        Calendar result = (Calendar) original.clone();
        result.set(Calendar.DAY_OF_MONTH, 1);
        result.set(Calendar.ERA, java.util.GregorianCalendar.AD);
        result.set(Calendar.YEAR, year);
        result.set(Calendar.MONTH, month - 1);
        result.set(Calendar.DAY_OF_MONTH, Math.min(day, result.getActualMaximum(Calendar.DAY_OF_MONTH)));
        return result;
    }
}

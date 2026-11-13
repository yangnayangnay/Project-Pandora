package com.example.project_pandora.ui.widget;

import org.junit.Test;
import java.util.Calendar;
import static org.junit.Assert.*;

public class StatutoryHolidayScheduleTest {
    private String marker(int year, int month, int day) {
        Calendar date = Calendar.getInstance(); date.clear(); date.set(year, month - 1, day);
        return StatutoryHolidaySchedule.marker(date);
    }
    @Test public void nationalDayAndAllMakeupWorkdaysAreMarked() {
        for (int day = 1; day <= 7; day++) assertEquals("休", marker(2026, 10, day));
        int[][] workdays = {{1,4},{2,14},{2,28},{5,9},{9,20},{10,10}};
        for (int[] day : workdays) assertEquals("班", marker(2026, day[0], day[1]));
        assertEquals("", marker(2026, 10, 8));
    }
    @Test public void allPublishedLeaveRangesAreIncluded() {
        int[][] ranges = {{1,1,3},{2,15,23},{4,4,6},{5,1,5},{6,19,21},{9,25,27},{10,1,7}};
        for (int[] range : ranges) for (int day = range[1]; day <= range[2]; day++) assertEquals("休", marker(2026, range[0], day));
    }
    @Test public void official2027NoticeIncludesEveryLeaveAndMakeupDateWithoutExtraDays() {
        int[][] ranges = {{1,1,3},{2,5,13},{4,3,5},{5,1,5},{6,9,9},{9,15,15},{10,1,7}};
        java.util.Set<String> leave = new java.util.HashSet<>();
        for (int[] range : ranges) for (int day = range[1]; day <= range[2]; day++) leave.add(range[0] + "-" + day);
        java.util.Set<String> work = new java.util.HashSet<>(java.util.Arrays.asList("1-31","2-14","5-8","9-26","10-9"));
        Calendar date = Calendar.getInstance(); date.clear(); date.set(2027, Calendar.JANUARY, 1);
        for (int i = 0; i < 365; i++, date.add(Calendar.DATE, 1)) {
            String key = (date.get(Calendar.MONTH) + 1) + "-" + date.get(Calendar.DATE);
            assertEquals(key, work.contains(key) ? "班" : leave.contains(key) ? "休" : "", StatutoryHolidaySchedule.marker(date));
        }
    }
    @Test public void ordinaryWeekendsTermsAndUnknownFutureYearsAreNotInvented() {
        assertEquals("", marker(2026, 11, 14));
        assertEquals("", marker(2026, 12, 22));
        assertEquals("", marker(2028, 10, 1));
        assertEquals("休", marker(2025, 10, 1));
    }
}

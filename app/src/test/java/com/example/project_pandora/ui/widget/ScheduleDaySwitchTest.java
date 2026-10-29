package com.example.project_pandora.ui.widget;

import org.junit.Test;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

/** Regression tests for SchedulePeriod.dayMinutes when the selected day changes. */
public class ScheduleDaySwitchTest {

    private static final long MINUTE_MS = 60L * 1000L;
    private static final long HOUR_MS = 60L * MINUTE_MS;
    private static final long DAY_MS = 24L * HOUR_MS;

    /** 2024-06-10T00:00:00Z, the selected test day. */
    private static final long DAY = utcDay(2024, 6, 10);

    private static long utcDay(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US);
        calendar.clear();
        calendar.set(year, month - 1, day, 0, 0, 0);
        return calendar.getTimeInMillis();
    }

    private static void assertMinutes(float[] expected, long start, long end, long dayStart) {
        assertArrayEquals(expected,
                SchedulePeriod.dayMinutes(start, end, dayStart, dayStart + DAY_MS), 0.001f);
    }

    @Test
    public void nineToTenTaskAppearsOnlyOnItsOwnDay() {
        long start = DAY + 9L * HOUR_MS;
        long end = DAY + 10L * HOUR_MS;

        assertMinutes(new float[]{540f, 600f}, start, end, DAY);
        assertNull(SchedulePeriod.dayMinutes(start, end, DAY - DAY_MS, DAY));
        assertNull(SchedulePeriod.dayMinutes(start, end, DAY + DAY_MS, DAY + 2L * DAY_MS));
        assertMinutes(new float[]{540f, 600f}, start, end, DAY);
    }

    @Test
    public void twentyThreeToNextDayOneIsSplitAcrossBothDays() {
        long start = DAY + 23L * HOUR_MS;
        long end = DAY + DAY_MS + HOUR_MS;

        assertMinutes(new float[]{1380f, 1440f}, start, end, DAY);
        assertMinutes(new float[]{0f, 60f}, start, end, DAY + DAY_MS);
    }

    @Test
    public void taskEndingAtNextMidnightDoesNotAppearOnNextDay() {
        long start = DAY + 22L * HOUR_MS;
        long end = DAY + DAY_MS;

        assertMinutes(new float[]{1320f, 1440f}, start, end, DAY);
        assertNull(SchedulePeriod.dayMinutes(start, end, DAY + DAY_MS, DAY + 2L * DAY_MS));
    }

    @Test
    public void dayWithoutAnyTaskReturnsNull() {
        long start = DAY + 2L * DAY_MS + 9L * HOUR_MS;
        long end = DAY + 2L * DAY_MS + 10L * HOUR_MS;

        assertNull(SchedulePeriod.dayMinutes(start, end, DAY, DAY + DAY_MS));
    }

    @Test
    public void multiDayTaskStartingBeforeSelectedDayIsClippedToWholeDay() {
        long start = DAY - 3L * DAY_MS + 10L * HOUR_MS;
        long end = DAY + 2L * DAY_MS + 12L * HOUR_MS;

        assertMinutes(new float[]{0f, 1440f}, start, end, DAY);
    }
}

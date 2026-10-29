package com.example.project_pandora.ui.widget;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Pure-JVM boundary tests for month-grid date coverage; no Android classes are used. */
public class MonthTaskDatesTest {
    private static final String DAY = "2026-10-27";

    @Test
    public void sameDayTimedTaskIsIncludedOnItsDay() {
        assertTrue(MonthTaskDates.covers("2026-10-27 09:00", "2026-10-27 10:00", DAY));
    }

    @Test
    public void nextDayOutsideTimedTaskIsExcluded() {
        assertFalse(MonthTaskDates.covers("2026-10-27 09:00", "2026-10-27 10:00", "2026-10-28"));
    }

    @Test
    public void crossDayTimedTaskIsIncludedOnBothCalendarDays() {
        String start = "2026-10-27 23:00";
        String end = "2026-10-28 01:00";
        assertTrue(MonthTaskDates.covers(start, end, "2026-10-27"));
        assertTrue(MonthTaskDates.covers(start, end, "2026-10-28"));
    }

    @Test
    public void timedMidnightEndIsExcludedOnEndDay() {
        String start = "2026-10-27 22:00";
        String end = "2026-10-28 00:00";
        assertTrue(MonthTaskDates.covers(start, end, "2026-10-27"));
        assertFalse(MonthTaskDates.covers(start, end, "2026-10-28"));
    }

    @Test
    public void dateOnlyEndDayIsInclusive() {
        assertTrue(MonthTaskDates.covers("2026-10-27", "2026-10-29", "2026-10-29"));
        assertFalse(MonthTaskDates.covers("2026-10-27", "2026-10-29", "2026-10-30"));
    }

    @Test
    public void missingStartFallsBackToEndDate() {
        assertTrue(MonthTaskDates.covers(null, "2026-10-27 10:00", DAY));
        assertFalse(MonthTaskDates.covers(null, "2026-10-27 10:00", "2026-10-28"));
    }

    @Test
    public void undatedTaskIsExcludedFromSelectedDayList() {
        assertFalse(MonthTaskDates.covers(null, null, DAY));
        assertFalse(MonthTaskDates.covers("", "", DAY));
    }

    @Test
    public void invertedEndIsTreatedAsStartDateOnly() {
        String start = "2026-10-27 09:00";
        String end = "2026-10-25 10:00";
        assertTrue(MonthTaskDates.covers(start, end, DAY));
        assertFalse(MonthTaskDates.covers(start, end, "2026-10-25"));
    }
}

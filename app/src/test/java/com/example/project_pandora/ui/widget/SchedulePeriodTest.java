package com.example.project_pandora.ui.widget;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Boundary tests for week visibility; timed ends are exclusive, date-only ends are inclusive. */
public class SchedulePeriodTest {
    private static final long MINUTE_MS = 60L * 1000L;
    private static final long HOUR_MS = 60L * MINUTE_MS;
    private static final long DAY_MS = 24L * HOUR_MS;

    /** 2024-01-01T00:00:00Z, the Monday that starts the test week. */
    private static final long MONDAY_00 = 1_704_067_200_000L;
    /** 2024-01-08T00:00:00Z, the exclusive end of the test week. */
    private static final long NEXT_MONDAY_00 = MONDAY_00 + 7L * DAY_MS;

    @Test
    public void timedTaskEndingAtWeekStartMondayMidnightIsExcluded() {
        assertFalse(SchedulePeriod.intersects(
                MONDAY_00 - HOUR_MS, MONDAY_00, false, MONDAY_00, NEXT_MONDAY_00));
    }

    @Test
    public void dateOnlyTaskWithInclusiveMondayMidnightEndIsIncluded() {
        assertTrue(SchedulePeriod.intersects(
                MONDAY_00 - DAY_MS, MONDAY_00, true, MONDAY_00, NEXT_MONDAY_00));
    }

    @Test
    public void sundayLateTimedTaskIsIncluded() {
        assertTrue(SchedulePeriod.intersects(
                NEXT_MONDAY_00 - HOUR_MS, NEXT_MONDAY_00 - MINUTE_MS,
                false, MONDAY_00, NEXT_MONDAY_00));
    }

    @Test
    public void taskStartingAtNextMondayMidnightIsExcluded() {
        assertFalse(SchedulePeriod.intersects(
                NEXT_MONDAY_00, NEXT_MONDAY_00 + 2L * HOUR_MS, false, MONDAY_00, NEXT_MONDAY_00));
    }

    @Test
    public void taskSpanningWholeWeekIsIncluded() {
        assertTrue(SchedulePeriod.intersects(
                MONDAY_00 - DAY_MS, NEXT_MONDAY_00 + DAY_MS, false, MONDAY_00, NEXT_MONDAY_00));
    }

    @Test
    public void taskCompletelyOutsideWeekIsExcluded() {
        long afterWeek = NEXT_MONDAY_00 + DAY_MS;
        assertFalse(SchedulePeriod.intersects(
                afterWeek, afterWeek + HOUR_MS, false, MONDAY_00, NEXT_MONDAY_00));
    }
}

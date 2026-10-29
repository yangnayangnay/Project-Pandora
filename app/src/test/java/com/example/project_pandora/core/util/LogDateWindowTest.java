package com.example.project_pandora.core.util;

import org.junit.Test;

import java.time.Instant;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Fixed-clock contract tests for the shared yesterday/today/tomorrow date window. */
public class LogDateWindowTest {

    private static Date at(String instant) {
        return Date.from(Instant.parse(instant));
    }

    @Test
    public void acceptsYesterdayTodayTomorrow() {
        Date now = at("2026-10-27T16:01:00Z");
        assertEquals("2026-10-27", LogDateWindow.daysAgo(1, now));
        assertEquals("2026-10-28", LogDateWindow.daysAgo(0, now));
        assertEquals("2026-10-29", LogDateWindow.daysAgo(-1, now));
        assertTrue(LogDateWindow.isWritable("2026-10-27", now));
        assertTrue(LogDateWindow.isWritable("2026-10-28", now));
        assertTrue(LogDateWindow.isWritable("2026-10-29", now));
    }

    @Test
    public void rejectsDayBeforeAndDayAfter() {
        Date now = at("2026-10-27T16:01:00Z");
        assertFalse(LogDateWindow.isWritable("2026-10-26", now));
        assertFalse(LogDateWindow.isWritable("2026-10-30", now));
    }

    @Test
    public void rejectsInvalidEmptyAndNullDates() {
        Date now = at("2026-10-27T16:01:00Z");
        assertFalse(LogDateWindow.isWritable(null, now));
        assertFalse(LogDateWindow.isWritable("", now));
        assertFalse(LogDateWindow.isWritable("2026/10/28", now));
        assertFalse(LogDateWindow.isWritable("2026-1-1", now));
        assertFalse(LogDateWindow.isWritable("20261028", now));
        assertFalse(LogDateWindow.isWritable("2026-02-30", now));
    }

    @Test
    public void switchesAtShanghaiMidnight() {
        Date beforeMidnight = at("2026-10-27T15:59:59Z");
        assertEquals("2026-10-27", LogDateWindow.daysAgo(0, beforeMidnight));
        assertTrue(LogDateWindow.isWritable("2026-10-26", beforeMidnight));
        assertTrue(LogDateWindow.isWritable("2026-10-27", beforeMidnight));
        assertTrue(LogDateWindow.isWritable("2026-10-28", beforeMidnight));
        assertFalse(LogDateWindow.isWritable("2026-10-25", beforeMidnight));
        assertFalse(LogDateWindow.isWritable("2026-10-29", beforeMidnight));

        Date afterMidnight = at("2026-10-27T16:00:00Z");
        assertEquals("2026-10-28", LogDateWindow.daysAgo(0, afterMidnight));
        assertTrue(LogDateWindow.isWritable("2026-10-27", afterMidnight));
        assertTrue(LogDateWindow.isWritable("2026-10-28", afterMidnight));
        assertTrue(LogDateWindow.isWritable("2026-10-29", afterMidnight));
        assertFalse(LogDateWindow.isWritable("2026-10-26", afterMidnight));
        assertFalse(LogDateWindow.isWritable("2026-10-30", afterMidnight));
    }

    @Test
    public void handlesMonthEndAcrossMonths() {
        Date now = at("2026-10-31T15:30:00Z");
        assertTrue(LogDateWindow.isWritable("2026-10-30", now));
        assertTrue(LogDateWindow.isWritable("2026-10-31", now));
        assertTrue(LogDateWindow.isWritable("2026-11-01", now));
        assertFalse(LogDateWindow.isWritable("2026-10-29", now));
        assertFalse(LogDateWindow.isWritable("2026-11-02", now));
    }

    @Test
    public void handlesYearEndAcrossYears() {
        Date now = at("2026-12-31T15:30:00Z");
        assertTrue(LogDateWindow.isWritable("2026-12-30", now));
        assertTrue(LogDateWindow.isWritable("2026-12-31", now));
        assertTrue(LogDateWindow.isWritable("2027-01-01", now));
        assertFalse(LogDateWindow.isWritable("2026-12-29", now));
        assertFalse(LogDateWindow.isWritable("2027-01-02", now));
    }
}

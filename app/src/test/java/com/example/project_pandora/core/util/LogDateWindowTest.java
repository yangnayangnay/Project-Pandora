package com.example.project_pandora.core.util;

import org.junit.Test;
import java.time.Instant;
import java.util.Date;
import static org.junit.Assert.*;

public class LogDateWindowTest {
    private void window(String instant, String first, String yesterday, String today, String rejectedBefore, String tomorrow) {
        Date now = Date.from(Instant.parse(instant));
        assertEquals(first, LogDateWindow.daysAgo(2, now));
        assertEquals(yesterday, LogDateWindow.daysAgo(1, now));
        assertEquals(today, LogDateWindow.daysAgo(0, now));
        for (String date : new String[]{first, yesterday, today}) assertTrue(date, LogDateWindow.isWritable(date, now));
        assertFalse(LogDateWindow.isWritable(rejectedBefore, now));
        assertFalse(LogDateWindow.isWritable(tomorrow, now));
    }
    @Test public void acceptsPreviousTwoDaysThroughToday() {
        window("2026-10-27T16:01:00Z", "2026-10-26", "2026-10-27", "2026-10-28", "2026-10-25", "2026-10-29");
    }
    @Test public void beforeShanghaiMidnightUsesPreviousBusinessDay() {
        window("2026-10-27T15:59:59Z", "2026-10-25", "2026-10-26", "2026-10-27", "2026-10-24", "2026-10-28");
    }
    @Test public void switchesAtShanghaiMidnight() {
        window("2026-10-27T16:00:00Z", "2026-10-26", "2026-10-27", "2026-10-28", "2026-10-25", "2026-10-29");
    }
    @Test public void handlesMonthBoundary() {
        window("2026-10-31T16:01:00Z", "2026-10-30", "2026-10-31", "2026-11-01", "2026-10-29", "2026-11-02");
    }
    @Test public void handlesYearBoundary() {
        window("2026-12-31T16:01:00Z", "2026-12-30", "2026-12-31", "2027-01-01", "2026-12-29", "2027-01-02");
    }
    @Test public void rejectsMalformedDates() {
        Date now = Date.from(Instant.parse("2026-10-27T16:01:00Z"));
        for (String date : new String[]{null, "", "2026/10/28", "2026-1-1", "20261028", "2026-02-30"})
            assertFalse(LogDateWindow.isWritable(date, now));
    }
}

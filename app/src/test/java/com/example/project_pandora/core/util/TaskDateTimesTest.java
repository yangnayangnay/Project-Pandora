package com.example.project_pandora.core.util;

import org.junit.Test;

import java.util.Date;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Contract tests for task picker datetime text: minute precision, strict parsing and draft restore. */
public class TaskDateTimesTest {

    @Test
    public void formatKeepsHourAndMinute() {
        Date morning = TaskDateTimes.parse("2024-05-06 07:05");
        Date lateNight = TaskDateTimes.parse("2024-05-06 23:59");

        assertNotNull(morning);
        assertNotNull(lateNight);
        assertEquals("2024-05-06 07:05", TaskDateTimes.format(morning));
        assertEquals("2024-05-06 23:59", TaskDateTimes.format(lateNight));
    }

    @Test
    public void parseRejectsIllegalCalendarDates() {
        assertNull(TaskDateTimes.parse("2024-02-30 10:00"));
        assertNull(TaskDateTimes.parse("2024-13-01 10:00"));
        assertNull(TaskDateTimes.parse("2024-04-31 10:00"));
    }

    @Test
    public void parseRejects24Hour() {
        assertNull(TaskDateTimes.parse("2024-01-01 24:00"));
    }

    @Test
    public void parseRejectsShortStrings() {
        assertNull(TaskDateTimes.parse("2024-01-01"));
        assertNull(TaskDateTimes.parse("2024-01-01 1:00"));
        assertNull(TaskDateTimes.parse("2024-1-01 10:00"));
    }

    @Test
    public void parseRejectsTrailingCharacters() {
        assertNull(TaskDateTimes.parse("2024-01-01 10:00x"));
        assertNull(TaskDateTimes.parse("2024-01-01 10:00:00"));
        assertNull(TaskDateTimes.parse("2024-01-01 10:00 "));
    }

    @Test
    public void validRangeRejectsReversedStartAndEnd() {
        assertFalse(TaskDateTimes.validRange("2024-01-02 10:00", "2024-01-02 09:00"));
    }

    @Test
    public void validRangeAcceptsCrossDayRange() {
        assertTrue(TaskDateTimes.validRange("2024-12-31 22:00", "2025-01-01 02:00"));
    }

    @Test
    public void validRangeAcceptsEqualStartAndEnd() {
        assertTrue(TaskDateTimes.validRange("2024-03-01 08:30", "2024-03-01 08:30"));
    }

    @Test
    public void restoreDateOnlyDraftUsesStartAndEndOfDay() {
        assertEquals("2024-05-06 00:00", TaskDateTimes.restore("2024-05-06", false));
        assertEquals("2024-05-06 23:59", TaskDateTimes.restore("2024-05-06", true));
    }

    @Test
    public void restoreNullReturnsNull() {
        assertNull(TaskDateTimes.restore(null, false));
        assertNull(TaskDateTimes.restore(null, true));
        assertNull(TaskDateTimes.restore("", false));
    }

    @Test
    public void restoreIsoWithOffsetUsesFixedShanghaiTimeZone() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));

            assertEquals("2024-05-06 08:30",
                    TaskDateTimes.restore("2024-05-06T00:30:00.000+00:00", false));
            assertEquals("2024-05-06 04:30",
                    TaskDateTimes.restore("2024-05-05T20:30:00+00:00", false));
        } finally {
            TimeZone.setDefault(original);
        }
    }
}

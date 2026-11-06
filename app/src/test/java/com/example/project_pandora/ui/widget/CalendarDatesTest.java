package com.example.project_pandora.ui.widget;

import org.junit.Test;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import static org.junit.Assert.*;

public class CalendarDatesTest {
    @Test public void monthEndClampsToLeapDay() {
        assertDate(CalendarDates.shift(date(2024, 1, 31), CalendarDates.MONTH, 1), 2024, 2, 29);
        assertDate(CalendarDates.shift(date(2025, 1, 31), CalendarDates.MONTH, 1), 2025, 2, 28);
    }

    @Test public void monthNavigationPreservesTheSelectedDayAndCrossesYears() {
        Calendar selected = date(2026, 12, 18);
        assertDate(CalendarDates.shift(selected, CalendarDates.MONTH, 1), 2027, 1, 18);
        assertDate(CalendarDates.shift(date(2026, 1, 18), CalendarDates.MONTH, -1), 2025, 12, 18);
        assertDate(selected, 2026, 12, 18);
    }

    @Test public void weekNavigationMovesExactlySevenDatesAcrossYears() {
        assertDate(CalendarDates.shift(date(2026, 12, 28), CalendarDates.WEEK, 1), 2027, 1, 4);
        assertDate(CalendarDates.shift(date(2027, 1, 4), CalendarDates.WEEK, -1), 2026, 12, 28);
    }

    @Test public void dayNavigationHandlesLeapDays() {
        assertDate(CalendarDates.shift(date(2024, 2, 28), CalendarDates.DAY, 1), 2024, 2, 29);
        assertDate(CalendarDates.shift(date(2024, 3, 1), CalendarDates.DAY, -1), 2024, 2, 29);
    }

    @Test public void dayNavigationUsesLocalDatesAcrossDaylightSaving() {
        Calendar selected = new GregorianCalendar(TimeZone.getTimeZone("America/New_York"));
        selected.clear(); selected.set(2026, Calendar.MARCH, 7, 12, 0);
        Calendar next = CalendarDates.shift(selected, CalendarDates.DAY, 1);
        assertDate(next, 2026, 3, 8);
        assertEquals(12, next.get(Calendar.HOUR_OF_DAY));
        assertEquals(23L * 60 * 60 * 1000, next.getTimeInMillis() - selected.getTimeInMillis());
    }

    @Test public void selectingYearAndMonthClampsTheDayWithoutMutatingTheOriginal() {
        Calendar selected = date(2024, 2, 29);
        assertDate(CalendarDates.select(selected, 2025, 2, 29), 2025, 2, 28);
        assertDate(CalendarDates.select(selected, 2030, 4, 31), 2030, 4, 30);
        assertDate(selected, 2024, 2, 29);
    }

    @Test public void pickerAndPagingRespectSupportedYearBoundaries() {
        assertDate(CalendarDates.shift(date(1, 1, 1), CalendarDates.DAY, -1), 1, 1, 1);
        assertDate(CalendarDates.shift(date(9999, 12, 31), CalendarDates.DAY, 1), 9999, 12, 31);
        assertDate(CalendarDates.select(date(2026, 1, 1), 9999, 12, 31), 9999, 12, 31);
    }

    @Test(expected = IllegalArgumentException.class) public void invalidMonthIsRejected() {
        CalendarDates.select(date(2026, 1, 1), 2026, 13, 1);
    }

    @Test(expected = IllegalArgumentException.class) public void invalidYearIsRejected() {
        CalendarDates.select(date(2026, 1, 1), 0, 1, 1);
    }

    static Calendar date(int year, int month, int day) {
        Calendar date = new GregorianCalendar(); date.clear(); date.set(year, month - 1, day, 12, 0);
        return date;
    }

    static void assertDate(Calendar date, int year, int month, int day) {
        assertEquals(year, date.get(Calendar.YEAR));
        assertEquals(month - 1, date.get(Calendar.MONTH));
        assertEquals(day, date.get(Calendar.DAY_OF_MONTH));
    }
}

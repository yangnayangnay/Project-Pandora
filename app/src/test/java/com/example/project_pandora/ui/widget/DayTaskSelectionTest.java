package com.example.project_pandora.ui.widget;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** Pure-JVM selection, ordering and normalization rules shared by the month calendar and its popup. */
public class DayTaskSelectionTest {
    private static Map<String, Object> task(String name, String start, String end) {
        Map<String, Object> task = new LinkedHashMap<>();
        task.put("name", name);
        if (start != null) task.put("start_time", start);
        if (end != null) task.put("end_time", end);
        return task;
    }

    private static List<Map<String, Object>> only(Map<String, Object> task, String day) {
        return DayTaskSelection.onDate(Collections.singletonList(task), day);
    }

    @Test
    public void returnsOnlyTasksCoveringSelectedDay() {
        Map<String, Object> inside = task("inside", "2026-10-27", "2026-10-29");
        Map<String, Object> before = task("before", "2026-10-25", "2026-10-27");
        Map<String, Object> after = task("after", "2026-10-29", "2026-10-30");

        List<Map<String, Object>> result =
                DayTaskSelection.onDate(Arrays.asList(inside, before, after), "2026-10-28");

        assertEquals(1, result.size());
        assertSame(inside, result.get(0));
    }

    @Test
    public void timedMidnightEndIsExcludedOnEndDay() {
        Map<String, Object> task = task("timed", "2026-10-28 22:00", "2026-10-29 00:00");

        assertEquals(1, only(task, "2026-10-28").size());
        assertTrue(only(task, "2026-10-29").isEmpty());
    }

    @Test
    public void allDayEndIsInclusiveAndOtherDatesAreExcluded() {
        Map<String, Object> task = task("all-day", "2026-10-27", "2026-10-29");

        assertSame(task, only(task, "2026-10-29").get(0));
        assertTrue(only(task, "2026-10-26").isEmpty());
        assertTrue(only(task, "2026-10-30").isEmpty());
    }

    @Test
    public void missingStartFallsBackToEndForCoverageAndSorting() {
        Map<String, Object> missingStart = task("missing-start", null, "2026-10-28 11:00");
        Map<String, Object> timed = task("timed", "2026-10-28 09:00", "2026-10-28 10:00");

        List<Map<String, Object>> result =
                DayTaskSelection.onDate(Arrays.asList(missingStart, timed), "2026-10-28");

        assertEquals(2, result.size());
        assertSame(timed, result.get(0));
        assertSame(missingStart, result.get(1));
        assertNull(DayTaskSelection.start(missingStart));
        assertEquals("2026-10-28 11:00", DayTaskSelection.end(missingStart));
    }

    @Test
    public void invertedEndIsTreatedAsStartDateOnly() {
        Map<String, Object> task = task("inverted", "2026-10-29", "2026-10-27");

        assertEquals(1, only(task, "2026-10-29").size());
        assertTrue(only(task, "2026-10-27").isEmpty());
        assertTrue(only(task, "2026-10-28").isEmpty());
    }

    @Test
    public void sortsByNormalizedStartTimeWithEndFallback() {
        Map<String, Object> late = task("late", "2026-10-28 14:00", "2026-10-28 15:00");
        Map<String, Object> allDay = task("all-day", "2026-10-28", "2026-10-28");
        Map<String, Object> early = task("early", "2026-10-28 09:00", "2026-10-28 10:00");
        Map<String, Object> missingStart = task("missing-start", null, "2026-10-28 11:00");

        List<Map<String, Object>> result =
                DayTaskSelection.onDate(Arrays.asList(late, missingStart, allDay, early), "2026-10-28");

        assertEquals(4, result.size());
        assertSame(allDay, result.get(0));
        assertSame(early, result.get(1));
        assertSame(missingStart, result.get(2));
        assertSame(late, result.get(3));
    }

    @Test
    public void acceptsUppercaseKeys() {
        Map<String, Object> task = new LinkedHashMap<>();
        task.put("NAME", "uppercase");
        task.put("START_TIME", "2026-10-28");
        task.put("END_TIME", "2026-10-29");

        assertEquals("2026-10-28", DayTaskSelection.start(task));
        assertEquals("2026-10-29", DayTaskSelection.end(task));
        assertSame(task, only(task, "2026-10-28").get(0));
        assertSame(task, only(task, "2026-10-29").get(0));
        assertFalse(only(task, "2026-10-30").size() > 0);
    }

    @Test
    public void doesNotMutateOriginalMapsOrInputOrder() {
        Map<String, Object> late = task("late", "2026-10-28 14:00", "2026-10-28 15:00");
        Map<String, Object> allDay = task("all-day", "2026-10-28", "2026-10-28");
        Map<String, Object> early = task("early", "2026-10-28 09:00", "2026-10-28 10:00");
        List<Map<String, Object>> input = new ArrayList<>(Arrays.asList(late, allDay, early));
        List<Map<String, Object>> originalOrder = new ArrayList<>(input);
        Map<String, Object> lateSnapshot = new HashMap<>(late);

        List<Map<String, Object>> result = DayTaskSelection.onDate(input, "2026-10-28");

        assertEquals(originalOrder, input);
        assertSame(late, input.get(0));
        assertSame(allDay, input.get(1));
        assertSame(early, input.get(2));
        assertEquals(lateSnapshot, late);

        assertSame(allDay, result.get(0));
        assertSame(early, result.get(1));
        assertSame(late, result.get(2));
    }

    @Test
    public void convertsUtcOffsetsToTheLocalDate() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
            Map<String, Object> task = task("converted",
                    "2026-10-27T16:30:00.000+00:00",
                    "2026-10-28T02:00:00.000+00:00");

            assertEquals("2026-10-28 00:30", DayTaskSelection.start(task));
            assertEquals("2026-10-28 10:00", DayTaskSelection.end(task));
            assertSame(task, only(task, "2026-10-28").get(0));
            assertTrue(only(task, "2026-10-27").isEmpty());
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    public void undatedTaskIsExcludedWhileDatedTaskIsRetained() {
        Map<String, Object> undated = task("undated", null, null);
        Map<String, Object> dated = task("dated", "2026-10-28", "2026-10-28");

        List<Map<String, Object>> result =
                DayTaskSelection.onDate(Arrays.asList(undated, dated), "2026-10-28");

        assertEquals(1, result.size());
        assertSame(dated, result.get(0));
        assertTrue(DayTaskSelection.onDate(null, "2026-10-28").isEmpty());
    }
}

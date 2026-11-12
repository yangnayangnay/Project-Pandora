package com.example.project_pandora.ui.widget;

import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/** Observable week-packing properties for the month grid; no Android classes are used. */
public class MonthTaskLayoutTest {
    private static final String MONDAY = "2026-10-26";

    private static List<String> days() {
        List<String> days = new ArrayList<>();
        LocalDate start = LocalDate.parse(MONDAY);
        for (int i = 0; i < 14; i++) days.add(start.plusDays(i).toString());
        return days;
    }

    private static MonthTaskLayout.Task task(int index, String start, String end) {
        return new MonthTaskLayout.Task(index, start, end);
    }

    private static List<MonthTaskLayout.Segment> arrange(MonthTaskLayout.Task... tasks) {
        return MonthTaskLayout.arrange(Arrays.asList(tasks), days());
    }

    private static List<MonthTaskLayout.Segment> spans(List<MonthTaskLayout.Segment> segments, int taskIndex) {
        List<MonthTaskLayout.Segment> found = new ArrayList<>();
        for (MonthTaskLayout.Segment segment : segments) {
            if (segment.taskIndex == taskIndex) found.add(segment);
        }
        return found;
    }

    private static MonthTaskLayout.Segment span(List<MonthTaskLayout.Segment> segments, int taskIndex, int week) {
        for (MonthTaskLayout.Segment segment : segments) {
            if (segment.taskIndex == taskIndex && segment.week == week) return segment;
        }
        fail("missing segment for task " + taskIndex + " in week " + week);
        return null;
    }

    private static void assertNoOverlapInDateColumnsAndLane(List<MonthTaskLayout.Segment> segments) {
        for (int i = 0; i < segments.size(); i++) {
            for (int j = i + 1; j < segments.size(); j++) {
                MonthTaskLayout.Segment a = segments.get(i);
                MonthTaskLayout.Segment b = segments.get(j);
                if (a.week != b.week) continue;
                boolean sameDateColumns = a.startColumn <= b.endColumn && b.startColumn <= a.endColumn;
                if (sameDateColumns) {
                    assertNotEquals("segments sharing date columns must use different lanes", a.lane, b.lane);
                }
                if (a.lane == b.lane) {
                    assertFalse("segments in one lane must not share date columns", sameDateColumns);
                }
            }
        }
    }

    @Test
    public void singleWeekTaskHasExactlyOneSpanningSegment() {
        List<MonthTaskLayout.Segment> segments = arrange(task(0, "2026-10-27", "2026-10-29"));

        assertEquals(1, segments.size());
        MonthTaskLayout.Segment segment = segments.get(0);
        assertEquals(0, segment.taskIndex);
        assertEquals(0, segment.week);
        assertEquals(1, segment.startColumn);
        assertEquals(3, segment.endColumn);
    }

    @Test
    public void overlappingTasksUseDistinctLanesAndDisjointTasksReuseALane() {
        List<MonthTaskLayout.Segment> segments = arrange(
                task(0, "2026-10-27", "2026-10-29"),
                task(1, "2026-10-28", "2026-10-30"),
                task(2, "2026-10-30", "2026-10-31"));

        assertNoOverlapInDateColumnsAndLane(segments);
        assertEquals(3, segments.size());

        MonthTaskLayout.Segment first = span(segments, 0, 0);
        MonthTaskLayout.Segment overlapping = span(segments, 1, 0);
        MonthTaskLayout.Segment disjoint = span(segments, 2, 0);

        assertTrue(first.startColumn <= overlapping.endColumn && overlapping.startColumn <= first.endColumn);
        assertNotEquals(first.lane, overlapping.lane);
        assertTrue(disjoint.startColumn > first.endColumn);
        assertEquals(first.lane, disjoint.lane);
    }

    @Test
    public void taskAcrossWeekBoundarySplitsIntoTwoWeeklySegments() {
        List<MonthTaskLayout.Segment> segments = arrange(task(0, "2026-11-01", "2026-11-03"));

        assertEquals(2, segments.size());
        assertEquals(2, spans(segments, 0).size());

        MonthTaskLayout.Segment sunday = span(segments, 0, 0);
        MonthTaskLayout.Segment monday = span(segments, 0, 1);
        assertEquals(6, sunday.startColumn);
        assertEquals(6, sunday.endColumn);
        assertEquals(0, monday.startColumn);
        assertEquals(1, monday.endColumn);
        assertEquals(sunday.taskIndex, monday.taskIndex);
    }

    @Test
    public void taskOutsideVisibleRangeIsAbsent() {
        List<MonthTaskLayout.Segment> segments = arrange(task(0, "2026-11-09", "2026-11-11"));

        assertTrue(segments.isEmpty());
        assertTrue(spans(segments, 0).isEmpty());
    }

    @Test
    public void timedMidnightEndExcludesTheEndDate() {
        List<MonthTaskLayout.Segment> segments =
                arrange(task(0, "2026-11-01T22:00", "2026-11-02T00:00"));

        assertEquals(1, segments.size());
        MonthTaskLayout.Segment segment = span(segments, 0, 0);
        assertEquals(6, segment.startColumn);
        assertEquals(6, segment.endColumn);
        assertTrue(spans(segments, 0).size() == 1);
    }

    @Test
    public void taskCoveringWholeVisibleRangeHasOneFullWeekSpanPerWeek() {
        List<MonthTaskLayout.Segment> segments = arrange(task(0, "2026-10-01", "2026-11-30"));

        assertEquals(2, segments.size());
        assertEquals(2, spans(segments, 0).size());
        for (int week = 0; week < 2; week++) {
            MonthTaskLayout.Segment segment = span(segments, 0, week);
            assertEquals(0, segment.startColumn);
            assertEquals(6, segment.endColumn);
        }
    }

    @Test
    public void unrelatedLaterTaskDoesNotMoveLongTaskLanesWithinItsWeeklySpans() {
        MonthTaskLayout.Task longTask = task(0, "2026-10-28", "2026-11-05");
        List<MonthTaskLayout.Segment> baseline = arrange(longTask);
        List<MonthTaskLayout.Segment> packed = arrange(longTask, task(1, "2026-11-03", "2026-11-03"));

        assertEquals(2, spans(baseline, 0).size());
        assertEquals(2, spans(packed, 0).size());
        for (int week = 0; week < 2; week++) {
            MonthTaskLayout.Segment before = span(baseline, 0, week);
            MonthTaskLayout.Segment after = span(packed, 0, week);
            assertEquals(before.startColumn, after.startColumn);
            assertEquals(before.endColumn, after.endColumn);
            assertEquals(before.lane, after.lane);
        }

        MonthTaskLayout.Segment later = span(packed, 1, 1);
        assertEquals(1, later.startColumn);
        assertEquals(1, later.endColumn);
        assertNotEquals(span(packed, 0, 1).lane, later.lane);
        assertNoOverlapInDateColumnsAndLane(packed);
    }

    private static List<MonthTaskLayout.Segment> onDay(
            List<MonthTaskLayout.Segment> segments, int week, int column) {
        List<MonthTaskLayout.Segment> found = new ArrayList<>();
        for (MonthTaskLayout.Segment segment : segments) {
            if (segment.week == week && segment.startColumn <= column && segment.endColumn >= column) {
                found.add(segment);
            }
        }
        return found;
    }

    private static void assertLanes(List<MonthTaskLayout.Segment> segments, int expected) {
        assertEquals(expected, segments.size());
        boolean[] lanes = new boolean[expected];
        for (MonthTaskLayout.Segment segment : segments) {
            assertTrue("lane " + segment.lane + " is outside the visible limit",
                    segment.lane >= 0 && segment.lane < expected);
            assertFalse("two visible segments share lane " + segment.lane, lanes[segment.lane]);
            lanes[segment.lane] = true;
        }
    }

    @Test
    public void fourVisibleLanesKeepDailyAndCrossWeekEntriesSeparate() {
        List<MonthTaskLayout.Task> tasks = new ArrayList<>();
        for (int i = 0; i < 7; i++) tasks.add(task(i, "2026-10-26", i < 5 ? "2026-11-03" : "2026-10-27"));
        List<MonthTaskLayout.Segment> visible = MonthTaskLayout.visibleSegments(tasks, days(), CalendarCellMetrics.VISIBLE_ENTRIES);
        for (int week = 0; week < 2; week++) {
            for (int column = 0; column < (week == 0 ? 7 : 2); column++) assertLanes(onDay(visible, week, column), 4);
        }
        assertNoOverlapInDateColumnsAndLane(visible);
    }

    @Test
    public void fourOverlappingTasksAreAllVisibleOnTheirDay() {
        List<MonthTaskLayout.Task> tasks = Arrays.asList(
                task(0, "2026-10-28", "2026-10-28"),
                task(1, "2026-10-28", "2026-10-28"),
                task(2, "2026-10-28", "2026-10-28"),
                task(3, "2026-10-28", "2026-10-28"));
        List<MonthTaskLayout.Segment> visible = MonthTaskLayout.visibleSegments(tasks, days(), 4);

        List<MonthTaskLayout.Segment> wednesday = onDay(visible, 0, 2);
        assertLanes(wednesday, 4);
        assertNoOverlapInDateColumnsAndLane(visible);
    }

    @Test
    public void fiveAndSevenOverlappingTasksCapAtFourVisibleLanes() {
        for (int count : new int[]{5, 7}) {
            List<MonthTaskLayout.Task> tasks = new ArrayList<>();
            for (int index = 0; index < count; index++) {
                tasks.add(task(index, "2026-10-28", "2026-10-28"));
            }
            List<MonthTaskLayout.Segment> visible =
                    MonthTaskLayout.visibleSegments(tasks, days(), 4);

            assertLanes(onDay(visible, 0, 2), 4);
            assertNoOverlapInDateColumnsAndLane(visible);
        }
    }

    @Test
    public void busyMondayDoesNotHideContinuingTasksOnTuesday() {
        List<MonthTaskLayout.Task> tasks = Arrays.asList(
                task(0, "2026-10-26", "2026-10-27"),
                task(1, "2026-10-26", "2026-10-27"),
                task(2, "2026-10-26", "2026-10-27"),
                task(3, "2026-10-26", "2026-10-27"),
                task(4, "2026-10-26", "2026-10-26"),
                task(5, "2026-10-26", "2026-10-26"));
        List<MonthTaskLayout.Segment> visible = MonthTaskLayout.visibleSegments(tasks, days(), 4);

        assertLanes(onDay(visible, 0, 0), 4);
        assertLanes(onDay(visible, 0, 1), 4);
        for (int index = 0; index < 4; index++) {
            List<MonthTaskLayout.Segment> continuing = spans(visible, index);
            assertEquals(1, continuing.size());
            assertEquals(0, continuing.get(0).startColumn);
            assertEquals(1, continuing.get(0).endColumn);
        }
        assertTrue(spans(visible, 4).isEmpty());
        assertTrue(spans(visible, 5).isEmpty());
        assertNoOverlapInDateColumnsAndLane(visible);
    }

    @Test
    public void hiddenLongTaskAppearsWhenEarlierShortTasksFinish() {
        List<MonthTaskLayout.Task> tasks = Arrays.asList(
                task(0, "2026-10-26", "2026-10-27"),
                task(1, "2026-10-26", "2026-10-27"),
                task(2, "2026-10-26", "2026-10-27"),
                task(3, "2026-10-26", "2026-10-27"),
                task(4, "2026-10-27", "2026-10-28"));
        List<MonthTaskLayout.Segment> visible = MonthTaskLayout.visibleSegments(tasks, days(), 4);

        assertLanes(onDay(visible, 0, 1), 4);
        assertTrue(onDay(visible, 0, 1).stream().noneMatch(segment -> segment.taskIndex == 4));

        List<MonthTaskLayout.Segment> wednesday = onDay(visible, 0, 2);
        assertEquals(1, wednesday.size());
        assertEquals(4, wednesday.get(0).taskIndex);
        assertEquals(2, wednesday.get(0).startColumn);
        assertEquals(2, wednesday.get(0).endColumn);
        assertNoOverlapInDateColumnsAndLane(visible);
    }

    @Test
    public void loneMultiDayBarStaysContinuousAndSplitsOnlyAtTheWeekBoundary() {
        List<MonthTaskLayout.Segment> withinWeek = MonthTaskLayout.visibleSegments(
                Arrays.asList(task(0, "2026-10-28", "2026-10-30")), days(), 4);
        assertEquals(1, withinWeek.size());
        assertEquals(2, withinWeek.get(0).startColumn);
        assertEquals(4, withinWeek.get(0).endColumn);

        List<MonthTaskLayout.Segment> acrossWeek = MonthTaskLayout.visibleSegments(
                Arrays.asList(task(0, "2026-10-28", "2026-11-03")), days(), 4);
        assertEquals(2, acrossWeek.size());
        MonthTaskLayout.Segment firstWeek = span(acrossWeek, 0, 0);
        MonthTaskLayout.Segment secondWeek = span(acrossWeek, 0, 1);
        assertEquals(2, firstWeek.startColumn);
        assertEquals(6, firstWeek.endColumn);
        assertEquals(0, secondWeek.startColumn);
        assertEquals(1, secondWeek.endColumn);
        assertEquals(firstWeek.lane, secondWeek.lane);
    }

    @Test
    public void timedMidnightEndDoesNotCreateAPhantomSegmentOnTheNextDay() {
        List<MonthTaskLayout.Segment> visible = MonthTaskLayout.visibleSegments(
                Arrays.asList(task(0, "2026-11-01T22:00", "2026-11-02T00:00")), days(), 4);

        assertEquals(1, visible.size());
        MonthTaskLayout.Segment sunday = span(visible, 0, 0);
        assertEquals(6, sunday.startColumn);
        assertEquals(6, sunday.endColumn);
        assertTrue(onDay(visible, 1, 0).isEmpty());
    }
}

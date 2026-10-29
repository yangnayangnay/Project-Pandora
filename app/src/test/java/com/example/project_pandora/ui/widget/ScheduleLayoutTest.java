package com.example.project_pandora.ui.widget;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class ScheduleLayoutTest {
    @Test public void overlappingTasksGetSeparateColumns() {
        ScheduleLayout.Slot a = new ScheduleLayout.Slot(0, 540, 660);
        ScheduleLayout.Slot b = new ScheduleLayout.Slot(1, 600, 720);
        ScheduleLayout.arrange(new ArrayList<>(Arrays.asList(a, b)));
        assertEquals(2, a.columns); assertEquals(2, b.columns);
        assertNotEquals(a.column, b.column);
    }
    @Test public void adjacentTasksUseFullWidthAndSeparateGroupsReset() {
        ScheduleLayout.Slot a = new ScheduleLayout.Slot(0, 540, 600);
        ScheduleLayout.Slot b = new ScheduleLayout.Slot(1, 600, 660);
        ScheduleLayout.Slot c = new ScheduleLayout.Slot(2, 900, 960);
        List<ScheduleLayout.Slot> rows = new ArrayList<>(Arrays.asList(c, b, a));
        ScheduleLayout.arrange(rows);
        for (ScheduleLayout.Slot row : rows) { assertEquals(1, row.columns); assertEquals(0, row.column); }
    }
    @Test public void chainedOverlapsShareOneConsistentColumnCount() {
        List<ScheduleLayout.Slot> rows = new ArrayList<>(Arrays.asList(
            new ScheduleLayout.Slot(0, 540, 600), new ScheduleLayout.Slot(1, 570, 630), new ScheduleLayout.Slot(2, 600, 660)));
        ScheduleLayout.arrange(rows);
        for (ScheduleLayout.Slot row : rows) assertEquals(2, row.columns);
        assertEquals(rows.get(0).column, rows.get(2).column);
    }
    @Test public void minimumTouchableDurationAndDayBoundsAreClamped() {
        ScheduleLayout.Slot shortTask = new ScheduleLayout.Slot(0, 600, 601);
        assertEquals(630, shortTask.end, 0);
        ScheduleLayout.Slot overnight = new ScheduleLayout.Slot(1, -60, 1600);
        assertEquals(0, overnight.start, 0); assertEquals(1440, overnight.end, 0);
    }
}

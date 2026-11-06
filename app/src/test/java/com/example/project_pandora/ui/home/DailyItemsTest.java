package com.example.project_pandora.ui.home;

import com.example.project_pandora.ui.widget.DayTaskSelection;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class DailyItemsTest {
    private static Map<String, Object> item(long id, String day, Object completed) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id); item.put("content", "个人事项" + id);
        item.put("item_date", day); item.put("completed", completed); return item;
    }

    @Test public void dateValidationRejectsImpossibleDatesAndNonDateValues() {
        for (String invalid : Arrays.asList(null, "", "2026-2-03", "2026-02-29", "1900-02-29", "2026-04-31", "2026-00-01", "2026-11-00", "1899-12-31", "2026-11-03T00:00:00", "2026-11-03 "))
            assertFalse("accepted invalid day " + invalid, DailyItems.validDate(invalid));
        for (String valid : Arrays.asList("1900-01-01", "2000-02-29", "2028-02-29", "2026-12-31", "9999-12-31"))
            assertTrue(valid, DailyItems.validDate(valid));
    }

    @Test public void checklistSelectsOnlyThatDayAndKeepsStableUnfinishedFirstOrdering() {
        Map<String, Object> done = item(1, "2026-11-03", true);
        Map<String, Object> first = item(2, "2026-11-03", false);
        Map<String, Object> second = item(3, "2026-11-03", false);
        List<Map<String, Object>> original = Arrays.asList(done, item(4, "2026-11-04", false), first, second);
        assertEquals(Arrays.asList(first, second, done), DailyItems.onDate(original, "2026-11-03"));
        assertSame(done, original.get(0));
        assertTrue(DailyItems.onDate(original, "2026-11-05").isEmpty());
    }

    @Test public void calendarEntriesStayOnOneDateIncludingMonthAndYearBoundaries() {
        List<Map<String, Object>> entries = DailyItems.calendarEntries(Arrays.asList(
                item(1, "2026-11-30", false), item(2, "2026-12-31", false), item(3, "2028-02-29", false)));
        for (Map<String, Object> entry : entries) {
            String day = DailyItems.date(entry);
            assertEquals(day, entry.get("start_time")); assertEquals(day, entry.get("end_time"));
            assertEquals(Collections.singletonList(entry), DayTaskSelection.onDate(Collections.singletonList(entry), day));
        }
        assertTrue(DayTaskSelection.onDate(entries, "2026-12-01").isEmpty());
        assertTrue(DayTaskSelection.onDate(entries, "2027-01-01").isEmpty());
    }

    @Test public void calendarDisplayDoesNotMutateStoredTodoOrAddTaskFieldsToIt() {
        Map<String, Object> todo = item(1, "2026-11-03", false);
        Map<String, Object> original = new LinkedHashMap<>(todo);
        Map<String, Object> display = DailyItems.calendarEntries(Collections.singletonList(todo)).get(0);
        assertEquals(todo.get("content"), display.get("name"));
        assertNotSame(todo, display); assertEquals(original, todo);
        display.put("completed", true); assertFalse(DailyItems.completed(todo));
        assertFalse(todo.containsKey("start_time")); assertFalse(todo.containsKey("assignee_id"));
    }

    @Test public void malformedDateDoesNotProducePhantomCalendarItems() {
        List<Map<String, Object>> entries = DailyItems.calendarEntries(Arrays.asList(
                item(1, "2026-11-03", true), item(2, "2026-02-29", false), item(3, "", false), item(4, "2026-11-03", false)));
        assertEquals(2, entries.size()); assertEquals(4L, entries.get(0).get("id")); assertEquals(1L, entries.get(1).get("id"));
    }

    @Test public void completionAcceptsBooleanAndDatabaseNumericRepresentations() {
        assertTrue(DailyItems.completed(item(1, "2026-11-03", true)));
        assertTrue(DailyItems.completed(item(1, "2026-11-03", 1.0)));
        assertFalse(DailyItems.completed(item(1, "2026-11-03", 0)));
        assertFalse(DailyItems.completed(item(1, "2026-11-03", false)));
        Map<String, Object> upper = new LinkedHashMap<>();
        upper.put("CONTENT", "大写字段"); upper.put("ITEM_DATE", "2026-11-03"); upper.put("COMPLETED", 1);
        assertEquals("大写字段", DailyItems.content(upper));
        assertEquals("2026-11-03", DailyItems.date(upper)); assertTrue(DailyItems.completed(upper));
    }
}

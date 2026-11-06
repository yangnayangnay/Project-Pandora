package com.example.project_pandora.ui.widget;

import com.example.project_pandora.core.util.DateCompat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Shared local-date selection for the month calendar, its popup and the task panel. */
public final class DayTaskSelection {
    private DayTaskSelection() {}

    public static Object value(Map<String, Object> task, String key) {
        Object value = task.get(key);
        return value != null ? value : task.get(key.toUpperCase(Locale.ROOT));
    }

    public static List<Map<String, Object>> onDate(List<Map<String, Object>> tasks, String day) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (tasks == null) return result;
        for (Map<String, Object> task : tasks) {
            if (MonthTaskDates.covers(start(task), end(task), day)) result.add(task);
        }
        result.sort(Comparator.comparing(DayTaskSelection::sortTime));
        return result;
    }

    private static String sortTime(Map<String, Object> task) {
        String start = start(task);
        return Objects.toString(start != null ? start : end(task), "");
    }

    public static String start(Map<String, Object> task) {
        return DateCompat.toLocalDateTime(value(task, "start_time"));
    }

    public static String end(Map<String, Object> task) {
        return DateCompat.toLocalDateTime(value(task, "end_time"));
    }
}

package com.example.project_pandora.ui.home;

import com.example.project_pandora.core.util.NumberCompat;
import java.text.*;
import java.util.*;

/** Presentation adapter only: personal todos remain separate from server tasks. */
public final class DailyItems {
    private DailyItems() {}
    public static Object value(Map<String, Object> item, String key) {
        Object value = item.get(key); return value != null ? value : item.get(key.toUpperCase(Locale.ROOT));
    }
    public static boolean completed(Map<String, Object> item) { return NumberCompat.toBool(value(item, "completed")); }
    public static String date(Map<String, Object> item) { return Objects.toString(value(item, "item_date"), ""); }
    public static String content(Map<String, Object> item) { return Objects.toString(value(item, "content"), ""); }
    public static List<Map<String, Object>> onDate(List<Map<String, Object>> items, String day) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> item : items) if (day.equals(date(item))) result.add(item);
        result.sort(Comparator.comparing(DailyItems::completed));
        return result;
    }
    public static List<Map<String, Object>> calendarEntries(List<Map<String, Object>> items) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> item : items) {
            String date = date(item); if (!validDate(date)) continue;
            // Date-only, same-day display coordinates; these are never sent to a task API.
            Map<String, Object> entry = new LinkedHashMap<>(item);
            entry.put("name", content(item)); entry.put("start_time", date); entry.put("end_time", date);
            result.add(entry);
        }
        result.sort(Comparator.comparing(DailyItems::completed));
        return result;
    }
    public static boolean validDate(String day) {
        if (day == null || !day.matches("\\d{4}-\\d{2}-\\d{2}") || day.compareTo("1900-01-01") < 0) return false;
        SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT); parser.setLenient(false);
        ParsePosition position = new ParsePosition(0);
        return parser.parse(day, position) != null && position.getIndex() == day.length();
    }
}

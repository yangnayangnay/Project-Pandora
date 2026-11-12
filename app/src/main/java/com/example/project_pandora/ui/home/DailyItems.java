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
    public static String startDate(Map<String, Object> item) {
        Object value = value(item, "start_date");
        return Objects.toString(value != null ? value : value(item, "item_date"), "");
    }
    public static String endDate(Map<String, Object> item) {
        Object value = value(item, "end_date");
        String end = Objects.toString(value, "");
        return end.isEmpty() ? startDate(item) : end;
    }
    /** Compatibility alias for callers that need the first covered date. */
    public static String date(Map<String, Object> item) { return startDate(item); }
    public static String content(Map<String, Object> item) { return Objects.toString(value(item, "content"), ""); }
    public static List<Map<String, Object>> onDate(List<Map<String, Object>> items, String day) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> item : items) {
            String start = startDate(item), end = endDate(item);
            if (validDate(start) && validDate(end) && day.compareTo(start) >= 0 && day.compareTo(end) <= 0) result.add(item);
        }
        result.sort(Comparator.comparing(DailyItems::completed));
        return result;
    }
    public static List<Map<String, Object>> calendarEntries(List<Map<String, Object>> items) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> item : items) {
            String start = startDate(item), end = endDate(item);
            if (!validDate(start) || !validDate(end)) continue;
            Map<String, Object> entry = new LinkedHashMap<>(item);
            entry.put("name", content(item)); entry.put("start_time", start); entry.put("end_time", end);
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

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
            entry.put("name", content(item));
            String startClock = Objects.toString(value(item, "start_time"), "");
            String endClock = Objects.toString(value(item, "end_time"), "");
            entry.put("item_start_clock", startClock); entry.put("item_end_clock", endClock);
            entry.put("start_time", startClock.matches("[0-2][0-9]:[0-5][0-9]") ? start + "T" + startClock + ":00" : start);
            entry.put("end_time", endClock.matches("[0-2][0-9]:[0-5][0-9]") ? end + "T" + endClock + ":00" : end);
            result.add(entry);
        }
        result.sort(Comparator.comparing(DailyItems::completed));
        return result;
    }
    public static String clock(Map<String, Object> item, boolean start) {
        Object explicit = value(item, start ? "item_start_clock" : "item_end_clock");
        String raw = Objects.toString(explicit != null ? explicit : value(item, start ? "start_time" : "end_time"), "");
        return raw.matches("(?:[01][0-9]|2[0-3]):[0-5][0-9]") ? raw : null;
    }
    public static String timeLabel(Map<String, Object> item) {
        String start = Objects.toString(clock(item, true), "");
        String end = Objects.toString(clock(item, false), "");
        return start.isEmpty() ? "全天" : start + "—" + end;
    }
    public static boolean validDate(String day) {
        if (day == null || !day.matches("\\d{4}-\\d{2}-\\d{2}") || day.compareTo("1900-01-01") < 0) return false;
        SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT); parser.setLenient(false);
        ParsePosition position = new ParsePosition(0);
        return parser.parse(day, position) != null && position.getIndex() == day.length();
    }
}

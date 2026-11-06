package com.example.project_pandora.core.util;

import java.util.*;

/** Shared wire-format parsing and first-tag colour rules, independent of Android views. */
public final class TaskTags {
    private TaskTags() {}
    public static Object value(Map<String, Object> row, String key) {
        if (row == null) return null;
        Object value = row.get(key); return value != null ? value : row.get(key.toUpperCase(Locale.ROOT));
    }
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> of(Map<String, Object> task) {
        List<Map<String, Object>> result = new ArrayList<>();
        Object raw = value(task, "tags");
        if (raw instanceof List<?>) for (Object item : (List<?>) raw) if (item instanceof Map<?, ?>) result.add((Map<String, Object>) item);
        return result;
    }
    public static ArrayList<Long> ids(Map<String, Object> task) {
        ArrayList<Long> result = new ArrayList<>();
        for (Map<String, Object> tag : of(task)) {
            Long id = NumberCompat.toLong(value(tag, "id"));
            if (id != null && id > 0 && !result.contains(id)) result.add(id);
        }
        return result;
    }
    public static String names(Map<String, Object> task) {
        List<String> names = new ArrayList<>();
        for (Map<String, Object> tag : of(task)) names.add("#" + Objects.toString(value(tag, "name"), ""));
        StringBuilder text = new StringBuilder();
        for (String name : names) { if (text.length() > 0) text.append("  "); text.append(name); }
        return text.toString();
    }
    public static int colorIndex(Map<String, Object> task, int count) {
        List<Map<String, Object>> tags = of(task);
        if (!tags.isEmpty()) {
            Object raw = value(tags.get(0), "color_index");
            if (raw instanceof Number) {
                double index = ((Number) raw).doubleValue();
                if (index >= 0 && index < count && index == Math.floor(index)) return (int) index;
            }
        }
        String name = Objects.toString(value(task, "name"), "未命名任务");
        return count > 0 ? (name.hashCode() & Integer.MAX_VALUE) % count : 0;
    }
    public static boolean canManage(String role) { return "FOUNDER".equals(role); }
    public static String searchText(String query) { return query.trim().replaceFirst("^[#＃]+", "").toLowerCase(Locale.ROOT); }
}

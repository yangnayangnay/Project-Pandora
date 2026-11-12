package com.example.project_pandora.core.util;

import java.util.*;

/** Handles the case and naming differences of existing SQL/JSON task responses. */
public final class TaskAssignees {
    private TaskAssignees() {}
    public static ArrayList<Long> ids(Map<String, Object> task) {
        LinkedHashSet<Long> result = new LinkedHashSet<>();
        Object people = value(task, "assignees");
        if (people instanceof Collection<?>) for (Object person : (Collection<?>) people) {
            Object id = person instanceof Map<?, ?> ? value((Map<?, ?>) person, "id", "user_id", "userId") : person;
            add(result, id);
        }
        Object ids = value(task, "assigneeIds", "assignee_ids");
        if (ids instanceof Collection<?>) for (Object id : (Collection<?>) ids) add(result, id);
        if (result.isEmpty()) add(result, value(task, "assignee_id", "assigneeId"));
        return new ArrayList<>(result);
    }
    public static List<Map<String, Object>> people(Map<String, Object> task) {
        List<Map<String, Object>> result = new ArrayList<>();
        Object raw = value(task, "assignees");
        if (raw instanceof Collection<?>) for (Object person : (Collection<?>) raw) if (person instanceof Map<?, ?>) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (Map.Entry<?, ?> field : ((Map<?, ?>) person).entrySet()) row.put(field.getKey().toString(), field.getValue());
            Object id = value(row, "id", "user_id", "userId"); if (id != null) row.put("id", id);
            result.add(row);
        }
        return result;
    }
    private static void add(Set<Long> ids, Object value) {
        Long id = NumberCompat.toLong(value); if (id != null && id > 0) ids.add(id);
    }
    private static Object value(Map<?, ?> row, String... keys) {
        for (String key : keys) for (Map.Entry<?, ?> entry : row.entrySet())
            if (key.equalsIgnoreCase(Objects.toString(entry.getKey())) && entry.getValue() != null) return entry.getValue();
        return null;
    }
}

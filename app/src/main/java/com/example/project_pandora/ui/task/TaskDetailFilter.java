package com.example.project_pandora.ui.task;

import com.example.project_pandora.core.util.*;
import com.example.project_pandora.ui.widget.DayTaskSelection;
import java.util.*;

public final class TaskDetailFilter {
    private TaskDetailFilter() {}
    public static final class Query {
        public int scope;
        public String keyword = "", status = "", from = "", to = "";
        public Long assignee, tag;
    }
    public static List<Map<String, Object>> merge(List<Map<String, Object>> mine, List<Map<String, Object>> dispatched) {
        Map<Long, Map<String, Object>> unique = new LinkedHashMap<>();
        for (List<Map<String, Object>> list : Arrays.asList(mine, dispatched)) for (Map<String, Object> row : list) {
            Long id = NumberCompat.toLong(TaskTags.value(row, "id")); if (id != null) unique.put(id, row);
        }
        List<Map<String, Object>> result = new ArrayList<>(unique.values());
        result.sort(Comparator.comparing((Map<String, Object> row) -> Objects.toString(TaskTags.value(row, "created_at"), "")).reversed());
        return result;
    }
    public static List<Map<String, Object>> apply(List<Map<String, Object>> tasks, Long user, Query query) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (user == null) return result;
        String keyword = query.keyword.trim().toLowerCase(Locale.ROOT);
        for (Map<String, Object> task : tasks) {
            Long dispatcher = NumberCompat.toLong(TaskTags.value(task, "dispatcher_id"));
            Set<Long> assignees = assigneeIds(task);
            boolean mine = assignees.contains(user), created = user.equals(dispatcher);
            if (!mine && !created || query.scope == 1 && !mine || query.scope == 2 && !created) continue;
            if (!query.status.isEmpty() && !query.status.equals(TaskTags.value(task, "status"))) continue;
            if (query.assignee != null && !assignees.contains(query.assignee)) continue;
            if (query.tag != null && !TaskTags.ids(task).contains(query.tag)) continue;
            if (!keyword.isEmpty()) {
                StringBuilder haystack = new StringBuilder(TaskTags.names(task));
                for (String key : Arrays.asList("name", "content", "progress_note", "assignee_real_name", "assignee_name", "dispatcher_real_name", "dispatcher_name")) haystack.append(' ').append(Objects.toString(TaskTags.value(task, key), ""));
                if (!haystack.toString().toLowerCase(Locale.ROOT).contains(keyword.replace('＃', '#'))) continue;
            }
            if ((!query.from.isEmpty() || !query.to.isEmpty()) && !overlaps(task, query.from, query.to)) continue;
            result.add(task);
        }
        return result;
    }
    static boolean overlaps(Map<String, Object> task, String from, String to) {
        String start = DayTaskSelection.start(task), end = DayTaskSelection.end(task);
        if (start == null || start.length() < 10) start = end;
        if (start == null || start.length() < 10) return false;
        if (end == null || end.length() < 10 || end.compareTo(start) < 0) end = start;
        String first = start.substring(0, 10), last = end.substring(0, 10);
        boolean allDay = (start.length() == 10 || start.endsWith("00:00")) && (end.length() == 10 || end.endsWith("00:00"));
        boolean exclusive = !allDay && end.endsWith("00:00") && !first.equals(last);
        return (to.isEmpty() || first.compareTo(to) <= 0) && (from.isEmpty() || (exclusive ? last.compareTo(from) > 0 : last.compareTo(from) >= 0));
    }
    public static String person(Map<String, Object> task, String prefix) {
        for (String suffix : Arrays.asList("_real_name", "_name", "_id")) {
            Object value = TaskTags.value(task, prefix + suffix);
            if (value != null && !value.toString().isEmpty()) return value.toString();
        }
        return "—";
    }
    @SuppressWarnings("unchecked")
    public static Set<Long> assigneeIds(Map<String, Object> task) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        Object raw = TaskTags.value(task, "assignees");
        if (raw instanceof List<?>) for (Object item : (List<?>) raw) if (item instanceof Map<?, ?>) {
            Long id = NumberCompat.toLong(TaskTags.value((Map<String, Object>) item, "id")); if (id != null) ids.add(id);
        }
        Long legacy = NumberCompat.toLong(TaskTags.value(task, "assignee_id")); if (legacy != null) ids.add(legacy);
        return ids;
    }
    @SuppressWarnings("unchecked")
    public static String assigneeNames(Map<String, Object> task) {
        List<String> names = new ArrayList<>();
        Object raw = TaskTags.value(task, "assignees");
        if (raw instanceof List<?>) for (Object item : (List<?>) raw) if (item instanceof Map<?, ?>) {
            Map<String, Object> person = (Map<String, Object>) item;
            String name = Objects.toString(TaskTags.value(person, "real_name"), "").trim();
            if (name.isEmpty()) name = Objects.toString(TaskTags.value(person, "username"), "").trim();
            if (!name.isEmpty()) names.add(name);
        }
        return names.isEmpty() ? person(task, "assignee") : String.join("、", names);
    }
}

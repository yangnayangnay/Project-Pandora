package com.example.project_pandora.ui.task;

import com.example.project_pandora.core.util.NumberCompat;
import com.example.project_pandora.core.util.TaskTags;
import java.util.Map;
import java.util.Set;
import java.util.List;

final class TaskPinOrder {
    private TaskPinOrder() {}
    static boolean isPinned(Map<String, Object> task, Set<String> pins) {
        Long id = NumberCompat.toLong(TaskTags.value(task, "id"));
        return id != null && pins.contains(Long.toString(id));
    }
    static void sort(List<Map<String, Object>> tasks, Set<String> pins) {
        tasks.sort(java.util.Comparator.comparing(task -> !isPinned(task, pins)));
    }
}

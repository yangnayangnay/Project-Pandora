package com.example.project_pandora.ui.task;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/** Original edit values survive recreation; assignee order is not a task change. */
final class TaskFormState implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String name, content, start, end;
    private final boolean urgent;
    private final List<Long> tags;
    private final Set<Long> assignees;

    TaskFormState(String name, String content, String start, String end, boolean urgent,
                  Collection<Long> tags, Collection<Long> assignees) {
        this.name = name; this.content = content; this.start = start; this.end = end; this.urgent = urgent;
        this.tags = new ArrayList<>(tags); this.assignees = new TreeSet<>(assignees);
    }

    static boolean isCreationDraft(Object content) {
        return content instanceof Map && "CREATE".equals(((Map<?, ?>) content).get("editorMode"));
    }

    @Override public boolean equals(Object other) {
        if (!(other instanceof TaskFormState)) return false;
        TaskFormState value = (TaskFormState) other;
        return urgent == value.urgent && Objects.equals(name, value.name) && Objects.equals(content, value.content)
                && Objects.equals(start, value.start) && Objects.equals(end, value.end)
                && tags.equals(value.tags) && assignees.equals(value.assignees);
    }
    @Override public int hashCode() { return Objects.hash(name, content, start, end, urgent, tags, assignees); }
}

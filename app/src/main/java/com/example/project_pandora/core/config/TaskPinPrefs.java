package com.example.project_pandora.core.config;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

/** Personal task-table ordering, isolated by signed-in account on this device. */
public final class TaskPinPrefs {
    private final SharedPreferences prefs;
    private final String key;
    public TaskPinPrefs(Context context, Long userId) {
        prefs = context.getSharedPreferences("task_table_pins", Context.MODE_PRIVATE);
        key = "user_" + userId;
    }
    public Set<String> ids() { return new HashSet<>(prefs.getStringSet(key, java.util.Collections.emptySet())); }
    public void toggle(long taskId) {
        Set<String> ids = ids(); String id = Long.toString(taskId);
        if (!ids.add(id)) ids.remove(id);
        prefs.edit().putStringSet(key, ids).apply();
    }
}

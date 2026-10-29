package com.example.project_pandora.ui.widget;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Assigns overlapping appointments to columns without changing their times. */
public final class ScheduleLayout {
    private ScheduleLayout() {}
    public static final class Slot {
        public final int taskIndex;
        public final float start, end;
        public int column, columns;
        public Slot(int taskIndex, float start, float end) {
            this.taskIndex = taskIndex;
            this.start = Math.max(0, Math.min(1439, start));
            this.end = Math.min(1440, Math.max(this.start + 30, end));
        }
    }
    public static void arrange(List<Slot> slots) {
        slots.sort(Comparator.comparingDouble(s -> s.start));
        List<Slot> group = new ArrayList<>();
        List<Float> ends = new ArrayList<>();
        float groupEnd = -1;
        for (Slot slot : slots) {
            if (slot.start >= groupEnd && !group.isEmpty()) {
                finish(group, ends.size()); group.clear(); ends.clear(); groupEnd = -1;
            }
            int column = 0;
            while (column < ends.size() && ends.get(column) > slot.start) column++;
            if (column == ends.size()) ends.add(slot.end); else ends.set(column, slot.end);
            slot.column = column; group.add(slot); groupEnd = Math.max(groupEnd, slot.end);
        }
        finish(group, ends.size());
    }
    private static void finish(List<Slot> group, int columns) {
        for (Slot slot : group) slot.columns = Math.max(1, columns);
    }
}

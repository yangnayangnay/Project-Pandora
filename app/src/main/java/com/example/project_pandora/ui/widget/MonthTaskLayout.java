package com.example.project_pandora.ui.widget;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Packs each week's task spans into lanes, keeping a task on one lane across its dates. */
public final class MonthTaskLayout {
    private MonthTaskLayout() {}

    public static final class Task {
        public final int index;
        public final String start, end;

        public Task(int index, String start, String end) {
            this.index = index;
            this.start = start;
            this.end = end;
        }
    }

    public static final class Segment {
        public final int taskIndex, week, startColumn, endColumn, lane;

        private Segment(int taskIndex, int week, int startColumn, int endColumn, int lane) {
            this.taskIndex = taskIndex;
            this.week = week;
            this.startColumn = startColumn;
            this.endColumn = endColumn;
            this.lane = lane;
        }
    }

    public static List<Segment> arrange(List<Task> tasks, List<String> days) {
        List<Segment> result = new ArrayList<>();
        for (int first = 0; first < days.size(); first += 7) {
            List<Segment> week = new ArrayList<>();
            int last = Math.min(first + 7, days.size());
            for (Task task : tasks) {
                int start = -1, end = -1;
                for (int cell = first; cell < last; cell++) {
                    if (MonthTaskDates.covers(task.start, task.end, days.get(cell))) {
                        if (start < 0) start = cell - first;
                        end = cell - first;
                    }
                }
                if (start >= 0) week.add(new Segment(task.index, first / 7, start, end, -1));
            }
            week.sort(Comparator.comparingInt((Segment span) -> span.startColumn)
                    .thenComparing(Comparator.comparingInt((Segment span) -> span.endColumn).reversed())
                    .thenComparingInt(span -> span.taskIndex));
            List<Integer> laneEnds = new ArrayList<>();
            for (Segment span : week) {
                int lane = 0;
                while (lane < laneEnds.size() && laneEnds.get(lane) >= span.startColumn) lane++;
                if (lane == laneEnds.size()) laneEnds.add(span.endColumn);
                else laneEnds.set(lane, span.endColumn);
                result.add(new Segment(span.taskIndex, span.week, span.startColumn, span.endColumn, lane));
            }
        }
        return result;
    }

    /** Keeps continuing bars on their lanes, filling free lanes on each date before hiding tasks. */
    public static List<Segment> visibleSegments(List<Task> tasks, List<String> days, int limit) {
        List<Segment> result = new ArrayList<>();
        if (limit <= 0) return result;
        List<Segment> spans = arrange(tasks, days);
        for (int first = 0; first < days.size(); first += 7) {
            int week = first / 7;
            Segment[] previous = new Segment[limit];
            int[] last = new int[limit];
            java.util.Arrays.fill(last, -1);
            for (int column = 0; column < Math.min(7, days.size() - first); column++) {
                List<Segment> active = new ArrayList<>();
                for (Segment span : spans) {
                    if (span.week == week && span.startColumn <= column && span.endColumn >= column) active.add(span);
                }
                active.sort(Comparator.comparingInt(span -> span.lane));
                Segment[] next = new Segment[limit];
                for (int lane = 0; lane < limit; lane++) {
                    if (previous[lane] == null) continue;
                    for (Segment span : active) {
                        if (span.taskIndex == previous[lane].taskIndex) { next[lane] = span; break; }
                    }
                }
                for (Segment span : active) {
                    boolean present = false;
                    for (Segment assigned : next) {
                        if (assigned != null && assigned.taskIndex == span.taskIndex) { present = true; break; }
                    }
                    if (present) continue;
                    for (int lane = 0; lane < limit; lane++) {
                        if (next[lane] == null) { next[lane] = span; break; }
                    }
                }
                for (int lane = 0; lane < limit; lane++) {
                    if (next[lane] == null) { last[lane] = -1; continue; }
                    int start = column;
                    if (last[lane] >= 0 && previous[lane] != null
                            && previous[lane].taskIndex == next[lane].taskIndex) {
                        start = result.get(last[lane]).startColumn;
                        result.set(last[lane], new Segment(next[lane].taskIndex, week, start, column, lane));
                    } else {
                        last[lane] = result.size();
                        result.add(new Segment(next[lane].taskIndex, week, start, column, lane));
                    }
                }
                previous = next;
            }
        }
        return result;
    }
}

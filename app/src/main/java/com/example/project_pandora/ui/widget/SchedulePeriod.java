package com.example.project_pandora.ui.widget;

/** Date-only tasks include their end date; timed tasks use an exclusive end. */
public final class SchedulePeriod {
    private SchedulePeriod() {}
    public static boolean intersects(long start, long end, boolean allDay, long periodStart, long periodEnd) {
        return start < periodEnd && (allDay ? end >= periodStart : end > periodStart);
    }

    /** Recomputed for each selected day; midnight is an exclusive timed end. */
    public static float[] dayMinutes(long start, long end, long dayStart, long nextDay) {
        if (!intersects(start, end, false, dayStart, nextDay)) return null;
        return new float[]{Math.max(0, (start - dayStart) / 60_000f),
                Math.min(1440, (end - dayStart) / 60_000f)};
    }
}

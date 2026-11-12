package com.example.project_pandora.ui.widget;

/** Shared logical sizes keep date labels, four entries and overflow in separate rows. */
public final class CalendarCellMetrics {
    public static final int VISIBLE_ENTRIES = 4;
    public static final float DATE_TEXT_SP = 18f;
    private final float scale;
    public CalendarCellMetrics(float fontScale) { scale = Math.max(.9f, fontScale); }
    public float weekdayHeight() { return Math.max(28f, 12f * scale * 1.3f + 10f); }
    public float numberHeight() { return DATE_TEXT_SP * scale * 1.25f; }
    public float lunarHeight() { return 12f * scale * 1.25f; }
    public float lunarTop() { return 4f + numberHeight() + 1f; }
    public float selectionHeight() { return numberHeight() + lunarHeight() + 5f; }
    public float dateHeight() { return Math.max(52f, lunarTop() + lunarHeight() + 10f); }
    public float entryHeight() { return Math.max(19f, 11f * scale * 1.3f + 4f); }
    public float entryStep() { return entryHeight() + 2f; }
    public float specialRowHeight() { return Math.max(entryStep(), 12f * scale * 1.3f + 4f); }
    public float taskTop() { return dateHeight(); }
    public float specialTop(int occupiedLanes, boolean overflow) {
        int rows = overflow ? VISIBLE_ENTRIES + 1 : Math.max(0, Math.min(VISIBLE_ENTRIES, occupiedLanes));
        return taskTop() + rows * entryStep();
    }
    public float rowHeight() { return specialTop(VISIBLE_ENTRIES, true) + specialRowHeight() + 4f; }
}

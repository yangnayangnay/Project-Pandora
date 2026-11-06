package com.example.project_pandora.ui.widget;

/** Direction lock keeps calendar paging separate from vertical scrolling and task taps. */
public final class PeriodSwipeGesture {
    private final float slop, distance;
    private float startX, startY;
    private int axis; // 0 undecided, 1 horizontal, 2 vertical
    private boolean active;

    public PeriodSwipeGesture(float slop, float distance) {
        this.slop = slop;
        this.distance = Math.max(slop, distance);
    }

    public void start(float x, float y) {
        startX = x; startY = y; axis = 0; active = true;
    }

    public boolean move(float x, float y) {
        if (!active) return false;
        float dx = Math.abs(x - startX), dy = Math.abs(y - startY);
        if (axis == 0 && Math.max(dx, dy) > slop) {
            if (dx > dy * 1.2f) axis = 1;
            else if (dy >= dx) axis = 2;
        }
        return axis == 1;
    }

    /** A left swipe advances one period; a right swipe goes back. Only UP commits. */
    public int finish(float x, float y) {
        boolean horizontal = move(x, y);
        active = false;
        float dx = x - startX, dy = y - startY;
        return horizontal && Math.abs(dx) >= distance && Math.abs(dx) > Math.abs(dy) * 1.2f
                ? (dx < 0 ? 1 : -1) : 0;
    }

    public void cancel() { active = false; }
}

package com.example.project_pandora.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;

import java.util.function.IntConsumer;

/** Intercepts deliberate horizontal paging while child taps and vertical scroll stay native. */
public class PeriodSwipeLayout extends LinearLayout {
    private final PeriodSwipeGesture swipe;
    private IntConsumer periodListener;

    public PeriodSwipeLayout(Context context) { this(context, null); }
    public PeriodSwipeLayout(Context context, AttributeSet attrs) { this(context, attrs, 0); }
    public PeriodSwipeLayout(Context context, AttributeSet attrs, int style) {
        super(context, attrs, style);
        swipe = new PeriodSwipeGesture(ViewConfiguration.get(context).getScaledTouchSlop(),
                48 * getResources().getDisplayMetrics().density);
    }

    public void setOnPeriodSwipe(IntConsumer listener) { periodListener = listener; }

    @Override public boolean onInterceptTouchEvent(MotionEvent event) {
        if (periodListener == null) return super.onInterceptTouchEvent(event);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                swipe.start(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                if (event.getPointerCount() == 1 && swipe.move(event.getX(), event.getY())) {
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                swipe.cancel();
                break;
        }
        return super.onInterceptTouchEvent(event);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (periodListener == null) return super.onTouchEvent(event);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                swipe.start(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                swipe.move(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_UP:
                int direction = swipe.finish(event.getX(), event.getY());
                releaseParent();
                if (direction != 0) periodListener.accept(direction);
                else performClick();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_CANCEL:
                swipe.cancel(); releaseParent();
                break;
        }
        return true;
    }

    private void releaseParent() {
        if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
    }

    @Override public boolean performClick() { super.performClick(); return true; }
}

package com.example.project_pandora.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.core.view.NestedScrollingParent3;
import androidx.core.view.NestedScrollingParentHelper;
import androidx.core.view.ViewCompat;

/** Three stable month heights: agenda, month dates with all tasks, and daily detail. */
public class MonthCalendarLayout extends LinearLayout implements NestedScrollingParent3 {
    public static final int LIST = 0, NORMAL = 1, DETAIL = 2;
    private final NestedScrollingParentHelper helper = new NestedScrollingParentHelper(this);
    private int state = NORMAL, headerHeight, minHeight, normalHeight, maxHeight;
    private ValueAnimator animation;
    private java.util.function.IntConsumer listener;
    private int direction;
    private float headerDownX, headerDownY;
    private boolean headerGesture;
    public MonthCalendarLayout(Context context) { this(context, null); }
    public MonthCalendarLayout(Context context, AttributeSet attrs) { super(context, attrs); setOrientation(VERTICAL); }
    public void setOnStateChanged(java.util.function.IntConsumer callback) { listener = callback; if (listener != null) listener.accept(state); }
    public int getState() { return state; }
    public void swipeUp() { setState(Math.max(LIST, state - 1), true); }
    public void swipeDown() { setState(Math.min(DETAIL, state + 1), true); }
    public void setState(int next, boolean animate) {
        direction = 0;
        state = Math.max(LIST, Math.min(DETAIL, next));
        if (listener != null) listener.accept(state);
        if (maxHeight == 0) return;
        if (animation != null) animation.cancel();
        int target = heightForState();
        if (!animate || !isLaidOut()) { setHeaderHeight(target); return; }
        animation = ValueAnimator.ofInt(headerHeight, target); animation.setDuration(220);
        animation.addUpdateListener(value -> setHeaderHeight((int) value.getAnimatedValue())); animation.start();
    }
    @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        minHeight = Math.min(dp(144), Math.max(dp(88), height / 3));
        maxHeight = Math.max(minHeight, height - dp(24));
        normalHeight = Math.max(minHeight, Math.min(maxHeight, Math.round(height * .64f)));
        setHeaderHeight(heightForState());
    }
    private int heightForState() { return state == LIST ? minHeight : state == DETAIL ? maxHeight : normalHeight; }
    @Override public boolean onInterceptTouchEvent(android.view.MotionEvent event) {
        if (event.getActionMasked() == android.view.MotionEvent.ACTION_DOWN) {
            headerDownX = event.getX(); headerDownY = event.getY(); headerGesture = false;
        } else if (event.getActionMasked() == android.view.MotionEvent.ACTION_MOVE && state == LIST && headerDownY < headerHeight
                && Math.abs(event.getY() - headerDownY) > android.view.ViewConfiguration.get(getContext()).getScaledTouchSlop() * 2
                && Math.abs(event.getY() - headerDownY) > Math.abs(event.getX() - headerDownX)) {
            headerGesture = true; return true;
        }
        return super.onInterceptTouchEvent(event);
    }
    @Override public boolean onTouchEvent(android.view.MotionEvent event) {
        if (headerGesture) {
            if (event.getActionMasked() == android.view.MotionEvent.ACTION_UP) {
                if (event.getY() > headerDownY) swipeDown(); else swipeUp();
                headerGesture = false;
            }
            return true;
        }
        return super.onTouchEvent(event);
    }
    private void setHeaderHeight(int height) {
        headerHeight = Math.max(minHeight, Math.min(maxHeight, height));
        if (getChildCount() == 0) return;
        View header = getChildAt(0);
        if (header.getLayoutParams().height != headerHeight) { header.getLayoutParams().height = headerHeight; header.requestLayout(); }
    }
    private void consume(int dy, int[] consumed) {
        int previous = headerHeight;
        setHeaderHeight(headerHeight - dy);
        consumed[1] += previous - headerHeight;
        if (previous != headerHeight) direction = Integer.signum(dy);
    }
    private void settle() {
        if (direction == 0) return;
        int closest = LIST;
        int distance = Math.abs(headerHeight - minHeight);
        if (Math.abs(headerHeight - normalHeight) < distance) { closest = NORMAL; distance = Math.abs(headerHeight - normalHeight); }
        if (Math.abs(headerHeight - maxHeight) < distance) closest = DETAIL;
        // A short deliberate drag still advances one stop; a long drag can cross both.
        if (closest == state && Math.abs(headerHeight - heightForState()) > dp(24)) closest = Math.max(LIST, Math.min(DETAIL, state - direction));
        direction = 0; setState(closest, true);
    }
    @Override public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int axes, int type) { return (axes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0; }
    @Override public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int axes, int type) { if (animation != null) animation.cancel(); helper.onNestedScrollAccepted(child, target, axes, type); direction = 0; }
    @Override public void onStopNestedScroll(@NonNull View target, int type) { helper.onStopNestedScroll(target, type); settle(); }
    @Override public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed, int type) {
        if (dy > 0 || !target.canScrollVertically(-1)) consume(dy, consumed);
    }
    @Override public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type, @NonNull int[] consumed) { if (dyUnconsumed < 0) consume(dyUnconsumed, consumed); }
    @Override public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) { onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type, new int[2]); }
    @Override public boolean onStartNestedScroll(View child, View target, int axes) { return onStartNestedScroll(child, target, axes, ViewCompat.TYPE_TOUCH); }
    @Override public void onNestedScrollAccepted(View child, View target, int axes) { onNestedScrollAccepted(child, target, axes, ViewCompat.TYPE_TOUCH); }
    @Override public void onStopNestedScroll(View target) { onStopNestedScroll(target, ViewCompat.TYPE_TOUCH); }
    @Override public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) { onNestedPreScroll(target, dx, dy, consumed, ViewCompat.TYPE_TOUCH); }
    @Override public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) { onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, ViewCompat.TYPE_TOUCH); }
    @Override public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) { return false; }
    @Override public boolean onNestedPreFling(View target, float velocityX, float velocityY) { if (headerHeight != minHeight && velocityY > 0) { swipeUp(); return true; } return false; }
    @Override public int getNestedScrollAxes() { return helper.getNestedScrollAxes(); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    @Override protected void onDetachedFromWindow() { if (animation != null) animation.cancel(); super.onDetachedFromWindow(); }
}

package com.example.project_pandora.ui.widget;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ScrollView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.project_pandora.R;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Regression tests for the real day-mode {@link TaskScheduleView}.
 *
 * <p>The view is inflated without an Activity and is never attached to a window, so the
 * {@code post(rebuildTask)} scheduled by the widget never runs. Each test therefore invokes the
 * private {@code Timeline.rebuild()} method explicitly before measuring and laying out again.</p>
 */
@RunWith(AndroidJUnit4.class)
public class TaskScheduleTimelineTest {

    private static final int YEAR = 2026;
    private static final int MONTH = Calendar.OCTOBER;
    private static final int DAY = 28;
    private static final String DATE = "2026-10-28";

    @Test
    public void emptyDay_keepsFullDayHeightAndScrollsToMidnight() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            TaskScheduleView view = setup(context);
            view.setDate(day(YEAR, MONTH, DAY));
            view.setTasks(Collections.emptyList());

            layout(view, context);
            ScrollView scroll = findScrollView(view);
            assertNotNull("未找到 TaskScheduleView 内部的 ScrollView", scroll);
            View timeline = onlyTimelineChild(scroll);
            rebuild(timeline);
            layout(view, context);

            assertEquals("无任务时时间轴不应包含任何日内任务子 View",
                    0, ((ViewGroup) timeline).getChildCount());
            assertFullDayHeight(timeline, context);
            assertScrollRange(scroll, timeline, context);
        });
    }

    @Test
    public void onlyMorningTask_keepsFullDayHeightAndScrollsToMidnight() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            TaskScheduleView view = setup(context);
            view.setDate(day(YEAR, MONTH, DAY));
            view.setTasks(Collections.singletonList(
                    task("上午会议", DATE + " 09:00", DATE + " 10:00")));

            layout(view, context);
            ScrollView scroll = findScrollView(view);
            assertNotNull("未找到 TaskScheduleView 内部的 ScrollView", scroll);
            View timeline = onlyTimelineChild(scroll);
            rebuild(timeline);
            layout(view, context);

            assertTrue("有日内任务时重建后时间轴应包含任务子 View",
                    ((ViewGroup) timeline).getChildCount() > 0);
            assertFullDayHeight(timeline, context);
            assertScrollRange(scroll, timeline, context);
        });
    }

    @Test
    public void onlyLateNightTask_keepsFullDayHeightAndScrollsToMidnight() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            TaskScheduleView view = setup(context);
            view.setDate(day(YEAR, MONTH, DAY));
            view.setTasks(Collections.singletonList(
                    task("深夜收尾", DATE + " 23:30", DATE + " 23:59")));

            layout(view, context);
            ScrollView scroll = findScrollView(view);
            assertNotNull("未找到 TaskScheduleView 内部的 ScrollView", scroll);
            View timeline = onlyTimelineChild(scroll);
            rebuild(timeline);
            layout(view, context);

            assertTrue("有日内任务时重建后时间轴应包含任务子 View",
                    ((ViewGroup) timeline).getChildCount() > 0);
            assertFullDayHeight(timeline, context);
            assertScrollRange(scroll, timeline, context);
        });
    }

    @Test
    public void onlyAllDayTask_keepsFullDayHeightAndScrollsToMidnight() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            TaskScheduleView view = setup(context);
            view.setDate(day(YEAR, MONTH, DAY));
            view.setTasks(Collections.singletonList(
                    task("全天值班", DATE, DATE)));

            layout(view, context);
            ScrollView scroll = findScrollView(view);
            assertNotNull("未找到 TaskScheduleView 内部的 ScrollView", scroll);
            View timeline = onlyTimelineChild(scroll);
            rebuild(timeline);
            layout(view, context);

            assertEquals("全天任务不应放入日内时间轴",
                    0, ((ViewGroup) timeline).getChildCount());
            assertTrue("全天任务应渲染在“全天”区域",
                    containsContentDescription(view, "全天值班"));
            assertFullDayHeight(timeline, context);
            assertScrollRange(scroll, timeline, context);
        });
    }

    @Test
    public void switchingFromTaskDayToEmptyDay_keepsFullDayHeight() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            TaskScheduleView view = setup(context);
            view.setDate(day(YEAR, MONTH, DAY));
            view.setTasks(Collections.singletonList(
                    task("上午会议", DATE + " 09:00", DATE + " 10:00")));

            layout(view, context);
            ScrollView scroll = findScrollView(view);
            assertNotNull("未找到 TaskScheduleView 内部的 ScrollView", scroll);
            View timeline = onlyTimelineChild(scroll);
            rebuild(timeline);
            layout(view, context);

            assertTrue("切换日期前时间轴应已渲染 10 月 28 日的任务",
                    ((ViewGroup) timeline).getChildCount() > 0);

            view.setDate(day(YEAR, MONTH, 29));
            rebuild(timeline);
            layout(view, context);

            assertEquals("切换到无任务日期后时间轴应清空任务子 View",
                    0, ((ViewGroup) timeline).getChildCount());
            assertFullDayHeight(timeline, context);
            assertScrollRange(scroll, timeline, context);
        });
    }

    /** Inflates the real day-mode preview with the application theme. */
    private static TaskScheduleView setup(Context targetContext) {
        Context themed = new ContextThemeWrapper(targetContext, R.style.Theme_Project_Pandora);
        View root = LayoutInflater.from(themed)
                .inflate(R.layout.preview_schedule_day, null, false);
        assertTrue("preview_schedule_day 的根 View 应为 TaskScheduleView",
                root instanceof TaskScheduleView);
        return (TaskScheduleView) root;
    }

    /** Measures the root at exactly 390dp x 800dp and lays it out. */
    private static void layout(View root, Context context) {
        int width = dp(context, 390);
        int height = dp(context, 800);
        root.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, width, height);
    }

    private static ScrollView findScrollView(View view) {
        if (view instanceof ScrollView) {
            return (ScrollView) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                ScrollView found = findScrollView(group.getChildAt(i));
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /** Returns the only child of the internal ScrollView, which must be the FrameLayout timeline. */
    private static View onlyTimelineChild(ScrollView scroll) {
        assertEquals("ScrollView 应只有唯一的时间轴子 View", 1, scroll.getChildCount());
        View child = scroll.getChildAt(0);
        assertTrue("ScrollView 的唯一子 View 应为 FrameLayout 时间轴",
                child instanceof FrameLayout);
        return child;
    }

    /**
     * Runs the real Timeline.rebuild(). The view is not attached to a window, so the widget's
     * posted rebuild task never executes; reflection is the only way to exercise the real layout.
     */
    private static void rebuild(View timeline) {
        try {
            Method method = timeline.getClass().getDeclaredMethod("rebuild");
            method.setAccessible(true);
            method.invoke(timeline);
        } catch (Exception e) {
            throw new AssertionError("无法通过反射执行 Timeline.rebuild()", e);
        }
    }

    private static void assertFullDayHeight(View timeline, Context context) {
        int hourHeight = context.getResources()
                .getDimensionPixelSize(R.dimen.schedule_hour_height);
        int expected = hourHeight * 24 + dp(context, 64);
        assertEquals("时间轴测量高度应覆盖完整 24 小时并保留底部 64dp",
                expected, timeline.getMeasuredHeight());
    }

    /** Scrolls to 24:00, checks its baseline is visible, then scrolls back to 00:00. */
    private static void assertScrollRange(ScrollView scroll, View timeline, Context context) {
        int hourHeight = context.getResources()
                .getDimensionPixelSize(R.dimen.schedule_hour_height);
        int baseline24 = dp(context, 16) + 24 * hourHeight;

        scroll.scrollTo(0, timeline.getHeight());
        int scrollY = scroll.getScrollY();
        assertTrue("应能向下滚动到 24:00，实际 scrollY=" + scrollY, scrollY > 0);

        int visibleTop = scrollY + scroll.getPaddingTop();
        int visibleBottom = scrollY + scroll.getHeight() - scroll.getPaddingBottom();
        assertTrue("24:00 文字基线应位于可视区域内: baseline=" + baseline24
                        + ", visible=[" + visibleTop + ", " + visibleBottom + "]",
                baseline24 >= visibleTop && baseline24 <= visibleBottom);

        scroll.scrollTo(0, 0);
        assertEquals("滚回顶部后 scrollY 应为 0", 0, scroll.getScrollY());
    }

    private static boolean containsContentDescription(View view, String text) {
        CharSequence description = view.getContentDescription();
        if (description != null && description.toString().contains(text)) {
            return true;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (containsContentDescription(group.getChildAt(i), text)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static Calendar day(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(year, month, day, 0, 0, 0);
        return calendar;
    }

    private static Map<String, Object> task(String name, String start, String end) {
        Map<String, Object> task = new LinkedHashMap<>();
        task.put("name", name);
        task.put("start_time", start);
        task.put("end_time", end);
        return task;
    }
}

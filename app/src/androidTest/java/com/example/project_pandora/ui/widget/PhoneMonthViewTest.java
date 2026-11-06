package com.example.project_pandora.ui.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.project_pandora.R;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/** Verifies actual Canvas output and hit targets for the expanded month calendar. */
@RunWith(AndroidJUnit4.class)
public class PhoneMonthViewTest {
    @Test public void multiDayBarFillsDateBoundariesAndEveryDayOpensSameTask() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Fixture fixture = new Fixture(2026, Calendar.OCTOBER, 28);
            Map<String, Object> task = task("跨日任务", "2026-10-27", "2026-10-29");
            fixture.view.setTasks(Collections.singletonList(task));
            Bitmap bitmap = fixture.draw();
            int expected = fixture.taskColor("跨日任务");
            float y = fixture.taskY(fixture.rowOf(28), 0);
            for (int boundary = 2; boundary <= 3; boundary++) {
                for (int offset = -1; offset <= 1; offset++) {
                    assertEquals("A task bar must cover the boundary between dates", expected,
                            bitmap.getPixel(Math.round(boundary * fixture.cellWidth + fixture.dp(offset)), Math.round(y)));
                }
            }
            AtomicReference<Map<String, Object>> opened = new AtomicReference<>();
            fixture.view.setOnTaskClick(opened::set);
            for (int column = 1; column <= 3; column++) {
                opened.set(null);
                fixture.tap((column + .5f) * fixture.cellWidth, y);
                assertSame("Each date within the bar must open the original task", task, opened.get());
            }
            fixture.save(bitmap, "month-continuous-bars.png");
        });
    }

    @Test public void overlappingTaskDoesNotBreakTheExistingBar() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Fixture fixture = new Fixture(2026, Calendar.OCTOBER, 28);
            Map<String, Object> longTask = task("连续事项", "2026-10-26", "2026-10-30");
            Map<String, Object> overlap = task("重叠事项", "2026-10-28", "2026-10-29");
            fixture.view.setTasks(Arrays.asList(overlap, longTask));
            Bitmap bitmap = fixture.draw();
            for (int boundary = 1; boundary <= 4; boundary++) {
                assertEquals(fixture.taskColor("连续事项"), bitmap.getPixel(
                        Math.round(boundary * fixture.cellWidth), Math.round(fixture.taskY(fixture.rowOf(28), 0))));
            }
            AtomicReference<Map<String, Object>> opened = new AtomicReference<>();
            fixture.view.setOnTaskClick(opened::set);
            fixture.tap(2.5f * fixture.cellWidth, fixture.taskY(fixture.rowOf(28), 1));
            assertSame(overlap, opened.get());
            fixture.save(bitmap, "month-overlapping-bars.png");
        });
    }

    @Test public void exactlyFourTasksAreVisibleAndTaskListOpensDayPopup() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Fixture fixture = new Fixture(2026, Calendar.OCTOBER, 30);
            List<Map<String, Object>> tasks = Arrays.asList(
                    task("事项一", "2026-10-27", "2026-10-29"),
                    task("事项二", "2026-10-27", "2026-10-29"),
                    task("事项三", "2026-10-27", "2026-10-29"),
                    task("事项四", "2026-10-27", "2026-10-29"));
            fixture.view.setTasks(tasks);
            Bitmap bitmap = fixture.draw();
            int day = 28;
            assertEquals(fixture.taskColor("事项四"), fixture.barPixel(bitmap, day, 3));

            Clicks clicks = new Clicks(fixture.view);
            fixture.tap((fixture.columnOf(day) + .5f) * fixture.cellWidth,
                    fixture.taskY(fixture.rowOf(day), 3));

            assertEquals("the fourth lane selects its day", day, clicks.day.get());
            assertNull("task strips open the day agenda before a task detail", clicks.opened.get());
            assertNotNull("task lists open even with four or fewer tasks", clicks.dayTasks.get());
            assertEquals("a detailed-view tap is not a vertical swipe", 0, clicks.swipes.get());
        });
    }

    @Test public void fiveOverlappingTasksOnlyOpenPopupFromTaskBarsAndOverflow() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Fixture fixture = new Fixture(2026, Calendar.OCTOBER, 30);
            List<Map<String, Object>> tasks = Arrays.asList(
                    task("重叠一", "2026-10-27", "2026-10-29"),
                    task("重叠二", "2026-10-27", "2026-10-29"),
                    task("重叠三", "2026-10-27", "2026-10-29"),
                    task("重叠四", "2026-10-27", "2026-10-29"),
                    task("重叠五", "2026-10-27", "2026-10-29"));
            fixture.view.setTasks(tasks);
            Bitmap bitmap = fixture.draw();
            int day = 28;
            int secondary = ContextCompat.getColor(fixture.context, R.color.color_text_secondary);
            for (int lane = 0; lane < 4; lane++) {
                assertNotEquals("lane " + lane + " must show a task bar, not the overflow bar",
                        secondary, fixture.barPixel(bitmap, day, lane));
            }
            assertTrue(fixture.barContainsColor(bitmap, day, 4, secondary));

            Clicks clicks = new Clicks(fixture.view);
            int centerX = Math.round((fixture.columnOf(day) + .5f) * fixture.cellWidth);
            assertRoutesToDayPopup(fixture, clicks, day, centerX,
                    fixture.taskY(fixture.rowOf(day), 0), "task bar");
            assertSelectsWithoutPopup(fixture, clicks, day, centerX,
                    fixture.cellTop(fixture.rowOf(day)) + fixture.dp(10), "date");
            assertSelectsWithoutPopup(fixture, clicks, day,
                    fixture.columnOf(day) * fixture.cellWidth + fixture.dp(.5f),
                    fixture.taskY(fixture.rowOf(day), 0), "blank");
            assertRoutesToDayPopup(fixture, clicks, day, centerX,
                    fixture.taskY(fixture.rowOf(day), 4), "+N");
        });
    }

    @Test public void normalMonthDateWithManyTasksOnlySelectsDate() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Fixture fixture = new Fixture(2026, Calendar.OCTOBER, 30);
            fixture.view.setTasks(Arrays.asList(
                    task("一", "2026-10-28", "2026-10-28"),
                    task("二", "2026-10-28", "2026-10-28"),
                    task("三", "2026-10-28", "2026-10-28"),
                    task("四", "2026-10-28", "2026-10-28"),
                    task("五", "2026-10-28", "2026-10-28")));
            fixture.view.setDetailed(false);
            Clicks clicks = new Clicks(fixture.view);
            assertSelectsWithoutPopup(fixture, clicks, 28,
                    (fixture.columnOf(28) + .5f) * fixture.cellWidth,
                    fixture.cellTop(fixture.rowOf(28)) + fixture.dp(10), "normal date");
        });
    }

    @Test public void oneTaskStripOpensDayAgendaAndProvidesItsAnchor() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Fixture fixture = new Fixture(2026, Calendar.OCTOBER, 28);
            fixture.view.setTasks(Collections.singletonList(task("一", "2026-10-28", "2026-10-28")));
            Clicks clicks = new Clicks(fixture.view);
            assertRoutesToDayPopup(fixture, clicks, 28,
                    (fixture.columnOf(28) + .5f) * fixture.cellWidth,
                    fixture.taskY(fixture.rowOf(28), 0), "one task");
            assertNotNull(fixture.view.getDayTasksAnchor());
            assertTrue(fixture.view.getDayTasksAnchor().height() > 0);
        });
    }

    @Test public void wrapContentMeasurementCoversFiveAndSixRowMonths() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            assertWrapHeight(context, 2026, Calendar.OCTOBER, 5);
            assertWrapHeight(context, 2026, Calendar.MARCH, 6);
        });
    }

    @Test public void nestedScrollViewCanRevealTheLastRow() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            NestedScrollView scroll = new NestedScrollView(context);
            PhoneMonthView month = new PhoneMonthView(context);
            month.setDate(calendar(2026, Calendar.MARCH, 15));
            month.setDetailed(true);
            scroll.addView(month, new NestedScrollView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            int width = dp(context, 320);
            int viewport = dp(context, 360);
            scroll.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(viewport, View.MeasureSpec.EXACTLY));
            scroll.layout(0, 0, width, viewport);

            int contentHeight = month.getMeasuredHeight();
            assertTrue("a six-row month must be taller than the small viewport",
                    contentHeight > viewport);
            scroll.scrollTo(0, contentHeight);
            assertTrue("the detailed month content must actually scroll", scroll.getScrollY() > 0);

            int visibleBottom = scroll.getScrollY() + scroll.getHeight() - scroll.getPaddingBottom();
            assertTrue("the last calendar row must be reachable by scrolling",
                    month.getTop() + contentHeight <= visibleBottom + 1);
        });
    }

    @Test public void differentTaskBackgroundsUseTheSamePrimaryTextColor() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Fixture fixture = new Fixture(2026, Calendar.OCTOBER, 28);
            fixture.view.setTasks(Arrays.asList(
                    task("AlphaAlpha", "2026-10-28", "2026-10-28"),
                    task("AlphaBeta", "2026-10-28", "2026-10-28")));
            assertNotEquals("the fixture must exercise two background colors",
                    fixture.taskColor("AlphaAlpha"), fixture.taskColor("AlphaBeta"));

            Bitmap bitmap = fixture.draw();
            int primary = ContextCompat.getColor(fixture.context, R.color.color_text_primary);
            assertTrue("first task bar must paint text in color_text_primary",
                    fixture.barContainsColor(bitmap, 28, 0, primary));
            assertTrue("second task bar must paint text in color_text_primary",
                    fixture.barContainsColor(bitmap, 28, 1, primary));
        });
    }

    private static void assertRoutesToDayPopup(Fixture fixture, Clicks clicks, int expectedDay,
            float x, float y, String target) {
        clicks.reset();
        fixture.draw();
        fixture.tap(x, y);

        assertEquals(target + " must select the clicked day", expectedDay, clicks.day.get());
        assertEquals(target + " must report the clicked month", Calendar.OCTOBER + 1, clicks.month.get());
        assertEquals(target + " must report the clicked year", 2026, clicks.year.get());
        assertNotNull(target + " must open the day task list", clicks.dayTasks.get());
        assertEquals(target + " must open the clicked day, not the initially selected day",
                expectedDay, clicks.dayTasks.get().get(Calendar.DAY_OF_MONTH));
        assertNull(target + " must not open a task directly", clicks.opened.get());
        assertEquals(target + " must not trigger the vertical swipe listener", 0, clicks.swipes.get());
    }

    private static void assertSelectsWithoutPopup(Fixture fixture, Clicks clicks, int day,
            float x, float y, String target) {
        clicks.reset();
        fixture.draw(); fixture.tap(x, y);
        assertEquals(target + " must select the date", day, clicks.day.get());
        assertNull(target + " must not open the day agenda", clicks.dayTasks.get());
        assertNull(target + " must not open a task detail", clicks.opened.get());
    }

    private static void assertWrapHeight(Context context, int year, int month, int expectedRows) {
        PhoneMonthView view = new PhoneMonthView(context);
        view.setDate(calendar(year, month, 15));
        view.setDetailed(true);
        view.measure(View.MeasureSpec.makeMeasureSpec(dp(context, 320), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

        int expected = Math.round(dpf(context, 34) + expectedRows * dpf(context, 152));
        assertEquals("wrap_content must reserve the detailed minimum height",
                expected, view.getMeasuredHeight());
    }

    private static Map<String, Object> task(String name, String start, String end) {
        Map<String, Object> task = new HashMap<>();
        task.put("name", name);
        task.put("start_time", start);
        task.put("end_time", end);
        return task;
    }

    private static Calendar calendar(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(year, month, day, 0, 0, 0);
        return calendar;
    }

    private static int rowCount(Calendar selected) {
        Calendar first = (Calendar) selected.clone();
        first.set(Calendar.DAY_OF_MONTH, 1);
        return (((first.get(Calendar.DAY_OF_WEEK) + 5) % 7)
                + first.getActualMaximum(Calendar.DAY_OF_MONTH) + 6) / 7;
    }

    private static int dp(Context context, int value) {
        return Math.round(dpf(context, value));
    }

    private static float dpf(Context context, float value) {
        return value * context.getResources().getDisplayMetrics().density;
    }

    private static final class Clicks {
        final AtomicInteger year = new AtomicInteger();
        final AtomicInteger month = new AtomicInteger();
        final AtomicInteger day = new AtomicInteger();
        final AtomicReference<Calendar> dayTasks = new AtomicReference<>();
        final AtomicReference<Map<String, Object>> opened = new AtomicReference<>();
        final AtomicInteger swipes = new AtomicInteger();

        Clicks(PhoneMonthView view) {
            view.setOnDateSelectedListener((selectedYear, selectedMonth, selectedDay) -> {
                year.set(selectedYear);
                month.set(selectedMonth);
                day.set(selectedDay);
            });
            view.setOnDayTasksClick(dayTasks::set);
            view.setOnTaskClick(opened::set);
            view.setOnVerticalSwipeListener(new CalendarView.OnVerticalSwipeListener() {
                @Override public void onSwipeUp() { swipes.incrementAndGet(); }
                @Override public void onSwipeDown() { swipes.incrementAndGet(); }
            });
        }

        void reset() {
            year.set(0);
            month.set(0);
            day.set(0);
            dayTasks.set(null);
            opened.set(null);
            swipes.set(0);
        }
    }

    private static final class Fixture {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        final PhoneMonthView view = new PhoneMonthView(context);
        final FrameLayout parent = new FrameLayout(context);
        final Calendar date;
        final int rows, width, height;
        final float cellWidth, rowHeight;

        Fixture(int year, int month, int day) {
            date = calendar(year, month, day);
            rows = rowCount(date);
            width = Math.round(dp(390));
            height = Math.round(dp(34) + rows * dp(152));
            view.setDate(date);
            view.setDetailed(true);
            parent.addView(view, new FrameLayout.LayoutParams(width, height));
            parent.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
            parent.layout(0, 0, width, height);
            cellWidth = view.getWidth() / 7f;
            rowHeight = Math.max(dp(152f), (view.getHeight() - dp(34)) / rows);
        }

        float dp(float value) { return dpf(context, value); }
        float cellTop(int row) { return dp(34) + row * rowHeight; }
        float taskY(int row, int lane) { return cellTop(row) + dp(53 + lane * 19 + 8.5f); }
        int taskColor(String name) {
            String[] colors = context.getResources().getStringArray(R.array.calendar_month_detail_colors);
            return Color.parseColor(colors[(name.hashCode() & Integer.MAX_VALUE) % colors.length]);
        }
        int rowOf(int day) { return offsetOf(day) / 7; }
        int columnOf(int day) { return offsetOf(day) % 7; }
        private int offsetOf(int day) {
            Calendar first = (Calendar) date.clone();
            first.set(Calendar.DAY_OF_MONTH, 1);
            first.add(Calendar.DATE, -((first.get(Calendar.DAY_OF_WEEK) + 5) % 7));
            int offset = 0;
            while (offset < 42 && !(first.get(Calendar.YEAR) == date.get(Calendar.YEAR)
                    && first.get(Calendar.MONTH) == date.get(Calendar.MONTH)
                    && first.get(Calendar.DAY_OF_MONTH) == day)) {
                first.add(Calendar.DATE, 1);
                offset++;
            }
            assertTrue("day is outside the measured month: " + day, offset < 42);
            return offset;
        }
        int barPixel(Bitmap bitmap, int day, int lane) {
            int row = rowOf(day), column = columnOf(day);
            float right = (column + 1) * cellWidth - dp(2);
            return bitmap.getPixel(Math.round(right - dp(1)), Math.round(taskY(row, lane)));
        }
        boolean barContainsColor(Bitmap bitmap, int day, int lane, int color) {
            int row = rowOf(day), column = columnOf(day);
            int left = Math.round(column * cellWidth + dp(2));
            int right = Math.round((column + 1) * cellWidth - dp(2));
            int top = Math.round(cellTop(row) + dp(53 + lane * 19));
            int bottom = Math.round(cellTop(row) + dp(53 + lane * 19 + 17));
            for (int y = Math.max(0, top + 1); y < Math.min(bitmap.getHeight(), bottom - 1); y++) {
                for (int x = Math.max(0, left + 1); x < Math.min(bitmap.getWidth(), right - 1); x++) {
                    if (bitmap.getPixel(x, y) == color) return true;
                }
            }
            return false;
        }
        Bitmap draw() {
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.eraseColor(Color.WHITE);
            view.draw(new Canvas(bitmap));
            return bitmap;
        }
        void tap(float x, float y) {
            MotionEvent down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, x, y, 0);
            MotionEvent up = MotionEvent.obtain(0, 10, MotionEvent.ACTION_UP, x, y, 0);
            view.onTouchEvent(down); view.onTouchEvent(up); down.recycle(); up.recycle();
        }
        void save(Bitmap bitmap, String name) {
            File directory = context.getExternalFilesDir("month-render-tests");
            assertNotNull(directory);
            try (FileOutputStream stream = new FileOutputStream(new File(directory, name))) {
                assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream));
            } catch (java.io.IOException error) {
                throw new AssertionError(error);
            }
        }
    }
}

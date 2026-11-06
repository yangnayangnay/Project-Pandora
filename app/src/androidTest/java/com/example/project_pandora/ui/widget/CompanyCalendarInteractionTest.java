package com.example.project_pandora.ui.widget;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.NumberPicker;

import androidx.core.widget.NestedScrollView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.project_pandora.R;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/** Real View dispatch checks. These tests are compiled without creating or installing an APK. */
@RunWith(AndroidJUnit4.class)
public class CompanyCalendarInteractionTest {
    @Test public void normalAndExpandedMonthSwipesDoNotSelectADateOrOpenATask() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            for (boolean detailed : new boolean[]{false, true}) {
                Context context = context();
                NestedScrollView parent = new NestedScrollView(context);
                parent.setFillViewport(true);
                PhoneMonthView month = new PhoneMonthView(context); month.setDetailed(detailed);
                month.setDate(date(2026, 11, 4));
                parent.addView(month, new ViewGroup.LayoutParams(-1, -2));
                layout(parent, 350, 600);
                AtomicInteger changed = new AtomicInteger(), selected = new AtomicInteger(), opened = new AtomicInteger();
                month.setOnPeriodSwipe(changed::addAndGet);
                month.setOnDateSelectedListener((year, value, day) -> selected.incrementAndGet());
                month.setOnDayTasksClick(day -> opened.incrementAndGet());
                month.setOnTaskClick(task -> opened.incrementAndGet());
                swipe(parent, 280, 130, 70, 135, MotionEvent.ACTION_UP);
                assertEquals(1, changed.get()); assertEquals(0, selected.get()); assertEquals(0, opened.get());
                swipe(parent, 70, 130, 280, 135, MotionEvent.ACTION_UP);
                assertEquals(0, changed.get());
                swipe(parent, 280, 130, 70, 135, MotionEvent.ACTION_CANCEL);
                assertEquals(0, changed.get());
            }
        });
    }

    @Test public void weekAndDaySwipeAdvanceTheRightPeriodAndVerticalScrollingDoesNotPage() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            for (int resource : new int[]{R.layout.preview_schedule_week, R.layout.preview_schedule_day}) {
                TaskScheduleView schedule = (TaskScheduleView) LayoutInflater.from(context()).inflate(resource, null, false);
                Calendar original = date(2026, 12, 28); schedule.setDate(original);
                AtomicReference<Calendar> changed = new AtomicReference<>();
                AtomicInteger opened = new AtomicInteger();
                schedule.setOnDateChanged(changed::set); schedule.setOnTaskClick(task -> opened.incrementAndGet());
                layout(schedule, 350, 640);
                swipe(schedule, 280, 260, 70, 265, MotionEvent.ACTION_UP);
                Calendar expected = (Calendar) original.clone();
                expected.add(Calendar.DATE, resource == R.layout.preview_schedule_week ? 7 : 1);
                assertNotNull(changed.get()); assertDate(expected, changed.get()); assertEquals(0, opened.get());
                changed.set(null);
                swipe(schedule, 160, 360, 165, 240, MotionEvent.ACTION_UP);
                assertNull("vertical scrolling must not change the period", changed.get());
            }
        });
    }

    @Test public void collapsedMonthStripPagesWithoutSelectingADate() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            WeekDateStrip strip = new WeekDateStrip(context()); strip.setDate(date(2026, 11, 4));
            AtomicInteger pages = new AtomicInteger(), selections = new AtomicInteger();
            strip.setOnPeriodSwipe(pages::addAndGet); strip.setOnDateSelected(date -> selections.incrementAndGet());
            layout(strip, 350, 88);
            swipe(strip, 280, 40, 60, 42, MotionEvent.ACTION_UP);
            assertEquals(1, pages.get()); assertEquals(0, selections.get());
        });
    }

    @Test public void yearAndMonthEditsClampLeapDaysAndTypedYearsAreCommitted() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Calendar original = date(2024, 2, 29);
            YearMonthDayPicker picker = new YearMonthDayPicker(context(), original);
            NumberPicker year = findWheel(picker, "年份");
            NumberPicker month = findWheel(picker, "月份");
            NumberPicker day = findWheel(picker, "日期");
            // A real editor focus change exercises NumberPicker's native input commit path.
            EditText yearInput = findEditor(year);
            yearInput.requestFocus(); yearInput.setText("2030");
            Calendar chosen = picker.getSelectedDate();
            assertEquals(2030, chosen.get(Calendar.YEAR));
            assertEquals(Calendar.FEBRUARY, chosen.get(Calendar.MONTH));
            assertEquals(28, chosen.get(Calendar.DAY_OF_MONTH));
            picker.setDate(date(2026, 1, 31));
            EditText monthInput = findEditor(month);
            monthInput.requestFocus(); monthInput.setText("4");
            assertEquals(30, picker.getSelectedDate().get(Calendar.DAY_OF_MONTH));
            assertEquals(30, day.getMaxValue());
            assertDate(date(2024, 2, 29), original);
        });
    }

    private static Context context() {
        return new ContextThemeWrapper(InstrumentationRegistry.getInstrumentation().getTargetContext(),
                R.style.Theme_Project_Pandora);
    }

    private static Calendar date(int year, int month, int day) {
        Calendar result = Calendar.getInstance(); result.clear(); result.set(year, month - 1, day, 12, 0); return result;
    }

    private static void assertDate(Calendar expected, Calendar actual) {
        assertEquals(expected.get(Calendar.YEAR), actual.get(Calendar.YEAR));
        assertEquals(expected.get(Calendar.DAY_OF_YEAR), actual.get(Calendar.DAY_OF_YEAR));
    }

    private static void layout(View view, int width, int height) {
        float density = view.getResources().getDisplayMetrics().density;
        int w = Math.round(width * density), h = Math.round(height * density);
        view.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
        view.layout(0, 0, w, h);
    }

    private static void swipe(View target, float x, float y, float endX, float endY, int lastAction) {
        float density = target.getResources().getDisplayMetrics().density;
        long start = android.os.SystemClock.uptimeMillis();
        dispatch(target, start, start, MotionEvent.ACTION_DOWN, x * density, y * density);
        dispatch(target, start, start + 30, MotionEvent.ACTION_MOVE, (x + (endX - x) / 3) * density, (y + (endY - y) / 3) * density);
        dispatch(target, start, start + 80, MotionEvent.ACTION_MOVE, endX * density, endY * density);
        dispatch(target, start, start + 100, lastAction, endX * density, endY * density);
    }

    private static void dispatch(View target, long downTime, long time, int action, float x, float y) {
        MotionEvent event = MotionEvent.obtain(downTime, time, action, x, y, 0);
        try { target.dispatchTouchEvent(event); } finally { event.recycle(); }
    }

    private static NumberPicker findWheel(View view, String label) {
        if (view instanceof NumberPicker && label.contentEquals(view.getContentDescription())) return (NumberPicker) view;
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
            NumberPicker found = findWheel(((ViewGroup) view).getChildAt(i), label); if (found != null) return found;
        }
        return null;
    }

    private static EditText findEditor(ViewGroup picker) {
        for (int i = 0; i < picker.getChildCount(); i++) if (picker.getChildAt(i) instanceof EditText)
            return (EditText) picker.getChildAt(i);
        throw new AssertionError("Native NumberPicker editor missing");
    }
}

package com.example.project_pandora.ui.widget;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.project_pandora.R;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Verifies the shared week/day month heading requests the date picker without an Activity. */
@RunWith(AndroidJUnit4.class)
public class CalendarDatePickerTest {
    @Test
    public void monthHeadingRequestsTheDatePickerInWeekAndDayModes() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            assertPickerRequested(R.layout.preview_schedule_day);
            assertPickerRequested(R.layout.preview_schedule_week);
        });
    }

    private static void assertPickerRequested(int layout) {
        Context context = new ContextThemeWrapper(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                R.style.Theme_Project_Pandora);
        View root = LayoutInflater.from(context).inflate(layout, null, false);
        assertTrue("preview layout must inflate a TaskScheduleView", root instanceof TaskScheduleView);
        TaskScheduleView schedule = (TaskScheduleView) root;

        TextView month = findMonthLabel(schedule);
        assertNotNull("the navigation month label must be present", month);
        assertTrue("the navigation month label must contain the current month",
                month.getText().toString().contains("年"));

        AtomicInteger requested = new AtomicInteger();
        schedule.setOnDatePickerRequested(requested::incrementAndGet);
        assertTrue("the month label must be clickable", month.performClick());
        assertEquals("clicking the month label must request the date picker once", 1, requested.get());
    }

    private static TextView findMonthLabel(View view) {
        if (view instanceof TextView) {
            CharSequence description = view.getContentDescription();
            if (description != null && description.toString().contains("选择日期")) {
                return (TextView) view;
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                TextView found = findMonthLabel(group.getChildAt(index));
                if (found != null) return found;
            }
        }
        return null;
    }
}

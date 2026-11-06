package com.example.project_pandora.ui.widget;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.project_pandora.R;
import com.google.android.material.button.MaterialButton;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/** Can be compiled without packaging; execution requires an explicitly authorized device build. */
@RunWith(AndroidJUnit4.class)
public class WheelTimePickerDialogTest {
    @Test public void wheelsWrapAtMidnightAndOnlyConfirmCommits() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = context();
            AtomicReference<String> chosen = new AtomicReference<>();
            WheelTimePickerDialog dialog = new WheelTimePickerDialog(context, "结束时间", 23, 59,
                    (hour, minute) -> chosen.set(hour + ":" + minute));
            dialog.create();
            View root = dialog.getWindow().getDecorView();
            measure(root, context);
            List<RecyclerView> wheels = find(root, RecyclerView.class);
            assertEquals(2, wheels.size());
            assertEquals("23", centered(wheels.get(0)).getText().toString());
            assertEquals("59", centered(wheels.get(1)).getText().toString());
            assertNull(chosen.get());
            for (RecyclerView wheel : wheels) wheel.scrollBy(0, centered(wheel).getHeight());
            assertEquals("00", centered(wheels.get(0)).getText().toString());
            assertEquals("00", centered(wheels.get(1)).getText().toString());
            assertTrue(find(root, TextView.class).stream().anyMatch(v -> "00:00".contentEquals(v.getText())));
            assertNull("scrolling must not change the form", chosen.get());
            button(root, "确定").performClick();
            assertEquals("0:0", chosen.get());
        });
    }

    @Test public void cancelDoesNotCommitAndReopeningUsesProvidedValue() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = context();
            AtomicReference<String> chosen = new AtomicReference<>();
            WheelTimePickerDialog dialog = new WheelTimePickerDialog(context, "开始时间", 8, 5,
                    (hour, minute) -> chosen.set(hour + ":" + minute));
            dialog.create();
            View root = dialog.getWindow().getDecorView();
            measure(root, context);
            List<RecyclerView> wheels = find(root, RecyclerView.class);
            assertEquals("08", centered(wheels.get(0)).getText().toString());
            assertEquals("05", centered(wheels.get(1)).getText().toString());
            wheels.get(0).scrollBy(0, centered(wheels.get(0)).getHeight());
            button(root, "取消").performClick();
            assertNull(chosen.get());
            WheelTimePickerDialog reopened = new WheelTimePickerDialog(context, "开始时间", 8, 5, (h, m) -> {});
            reopened.create();
            View reopenedRoot = reopened.getWindow().getDecorView();
            measure(reopenedRoot, context);
            assertEquals("08", centered(find(reopenedRoot, RecyclerView.class).get(0)).getText().toString());
            reopened.dismiss();
        });
    }

    private static Context context() {
        return new ContextThemeWrapper(InstrumentationRegistry.getInstrumentation().getTargetContext(), R.style.Theme_Project_Pandora);
    }
    private static void measure(View root, Context context) {
        int width = Math.round(320 * context.getResources().getDisplayMetrics().density);
        root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(width * 2, View.MeasureSpec.AT_MOST));
        root.layout(0, 0, root.getMeasuredWidth(), root.getMeasuredHeight());
    }
    private static TextView centered(RecyclerView wheel) {
        TextView result = (TextView) wheel.findChildViewUnder(wheel.getWidth() / 2f, wheel.getHeight() / 2f);
        assertNotNull(result); return result;
    }
    private static MaterialButton button(View root, String title) {
        return find(root, MaterialButton.class).stream().filter(v -> title.contentEquals(v.getText())).findFirst().orElseThrow(AssertionError::new);
    }
    private static <T> List<T> find(View root, Class<T> type) {
        List<T> matches = new ArrayList<>();
        if (type.isInstance(root)) matches.add(type.cast(root));
        if (root instanceof ViewGroup) for (int index = 0; index < ((ViewGroup) root).getChildCount(); index++)
            matches.addAll(find(((ViewGroup) root).getChildAt(index), type));
        return matches;
    }
}

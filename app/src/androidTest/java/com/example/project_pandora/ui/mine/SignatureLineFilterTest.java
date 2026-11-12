package com.example.project_pandora.ui.mine;

import android.widget.EditText;
import android.text.InputFilter;
import android.view.View;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SignatureLineFilterTest {
    private EditText editor(int widthDp) {
        android.content.Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        EditText view = new EditText(context); view.setTextSize(18); view.setPadding(0, 0, 0, 0); view.setMaxLines(4);
        int width = Math.round(widthDp * context.getResources().getDisplayMetrics().density);
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        view.layout(0, 0, width, view.getMeasuredHeight());
        view.setFilters(new InputFilter[]{new SignatureLineFilter(view, null)}); return view;
    }
    @Test public void fifthNewlineIsRejectedAndReplacingSelectionDoesNotDeleteOldText() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            EditText view = editor(240); view.setText("一\n二\n三\n四");
            view.getText().append("\n"); assertEquals("一\n二\n三\n四", view.getText().toString());
            view.getText().replace(0, 1, "甲\n乙"); assertEquals("一\n二\n三\n四", view.getText().toString());
            view.getText().delete(0, 2); assertEquals("二\n三\n四", view.getText().toString());
            view.getText().append("\n五"); assertEquals("二\n三\n四\n五", view.getText().toString());
        });
    }
    @Test public void softWrappedFifthLineIsRejectedAndPastedLineEndingsNormalize() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            EditText narrow = editor(22); narrow.setText("甲乙丙丁");
            assertTrue(SignatureLineFilter.fits(narrow, narrow.getText()));
            narrow.getText().append("戊己庚辛壬癸"); assertEquals("甲乙丙丁", narrow.getText().toString());
            EditText wide = editor(240); wide.setText("一\r\n二\r三\u2028四");
            assertEquals("一\n二\n三\n四", wide.getText().toString());
        });
    }
}

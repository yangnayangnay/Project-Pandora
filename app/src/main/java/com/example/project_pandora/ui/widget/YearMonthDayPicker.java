package com.example.project_pandora.ui.widget;

import android.content.Context;
import android.os.Build;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.project_pandora.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/** Native editable wheels make any year/month/day reachable without paging month by month. */
public final class YearMonthDayPicker extends LinearLayout {
    private final Calendar original;
    private final NumberPicker years, months, days;
    private final TextView preview;

    public YearMonthDayPicker(Context context, Calendar selected) {
        this(context, selected, CalendarDates.MIN_YEAR);
    }

    public YearMonthDayPicker(Context context, Calendar selected, int minYear) {
        super(context);
        original = (Calendar) selected.clone();
        setOrientation(VERTICAL);
        setPadding(dp(12), dp(8), dp(12), 0);
        preview = label("", 16);
        preview.setTextColor(ContextCompat.getColor(context, R.color.color_accent));
        addView(preview, new LayoutParams(-1, dp(40)));
        TextView hint = label("滚动选择，或点击数字直接输入年月日", 12);
        hint.setTextColor(ContextCompat.getColor(context, R.color.color_text_secondary));
        addView(hint, new LayoutParams(-1, -2));

        LinearLayout headings = new LinearLayout(context);
        for (String text : new String[]{"年", "月", "日"})
            headings.addView(label(text, 14), new LayoutParams(0, dp(32), 1));
        addView(headings);
        LinearLayout columns = new LinearLayout(context);
        years = wheel(minYear, CalendarDates.MAX_YEAR, "年份");
        months = wheel(1, 12, "月份");
        days = wheel(1, 31, "日期");
        int height = dp(getResources().getConfiguration().screenHeightDp < 420 ? 112 : 160);
        columns.addView(years, new LayoutParams(0, height, 1));
        columns.addView(months, new LayoutParams(0, height, 1));
        columns.addView(days, new LayoutParams(0, height, 1));
        addView(columns);
        setDate(selected);
        years.setOnValueChangedListener((picker, old, value) -> updateDays());
        months.setOnValueChangedListener((picker, old, value) -> updateDays());
        days.setOnValueChangedListener((picker, old, value) -> updatePreview());
    }

    public void setDate(Calendar selected) {
        years.setValue(selected.get(Calendar.YEAR));
        months.setValue(selected.get(Calendar.MONTH) + 1);
        updateDays();
        days.setValue(selected.get(Calendar.DAY_OF_MONTH));
        updatePreview();
    }

    /** Clearing native editors commits a typed value before the dialog's positive action. */
    public Calendar getSelectedDate() {
        years.clearFocus(); months.clearFocus(); days.clearFocus();
        return CalendarDates.select(original, years.getValue(), months.getValue(), days.getValue());
    }

    private void updateDays() {
        Calendar date = CalendarDates.select(original, years.getValue(), months.getValue(), days.getValue());
        int maximum = date.getActualMaximum(Calendar.DAY_OF_MONTH);
        // NumberPicker may wrap an out-of-range current value when its maximum changes.
        // Save the clamped day first so Jan 31 -> Feb means Feb 28/29, never Feb 1.
        days.setValue(date.get(Calendar.DAY_OF_MONTH));
        days.setMaxValue(maximum);
        updatePreview();
    }

    private void updatePreview() {
        Calendar date = CalendarDates.select(original, years.getValue(), months.getValue(), days.getValue());
        preview.setText(new SimpleDateFormat("yyyy年M月d日 EEEE", Locale.CHINA).format(date.getTime()));
    }

    private NumberPicker wheel(int minimum, int maximum, String description) {
        NumberPicker picker = new NumberPicker(getContext());
        picker.setMinValue(minimum); picker.setMaxValue(maximum);
        picker.setWrapSelectorWheel(false);
        picker.setContentDescription(description);
        picker.setFormatter(value -> String.format(Locale.ROOT, "%02d", value));
        int textColor = ContextCompat.getColor(getContext(), R.color.color_text_primary);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) picker.setTextColor(textColor);
        for (int i = 0; i < picker.getChildCount(); i++) {
            View child = picker.getChildAt(i);
            if (child instanceof EditText) {
                EditText editor = (EditText) child;
                editor.setTextColor(textColor);
                editor.setInputType(InputType.TYPE_CLASS_NUMBER);
                editor.setContentDescription(description + "，可直接输入");
            }
        }
        return picker;
    }

    private TextView label(String text, int size) {
        TextView label = new TextView(getContext()); label.setText(text); label.setTextSize(size);
        label.setTextColor(ContextCompat.getColor(getContext(), R.color.color_text_primary));
        label.setGravity(Gravity.CENTER); return label;
    }

    private int dp(int size) { return Math.round(size * getResources().getDisplayMetrics().density); }
}

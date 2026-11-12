package com.example.project_pandora.ui.widget;

import android.content.Context;
import android.os.Build;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;
import androidx.core.content.ContextCompat;
import com.example.project_pandora.R;
import java.text.SimpleDateFormat;
import java.util.*;

/** Civil/lunar wheels; switching mode preserves the selected real date. */
public final class YearMonthDayPicker extends LinearLayout {
    private final Calendar original;
    private final int minYear;
    private final NumberPicker years, months, days;
    private final TextView preview;
    private boolean showLunar, updating;
    private List<LunarDateWheelModel.Month> lunarMonths = Collections.emptyList();
    public YearMonthDayPicker(Context context, Calendar selected) { this(context, selected, CalendarDates.MIN_YEAR); }
    public YearMonthDayPicker(Context context, Calendar selected, int minYear) {
        super(context); this.minYear = minYear; original = (Calendar) selected.clone();
        setOrientation(VERTICAL); setPadding(dp(12), dp(8), dp(12), 0);
        preview = label("", 16); preview.setPadding(0, dp(6), 0, dp(6)); preview.setMinHeight(dp(40));
        addView(preview, new LayoutParams(-1, -2));
        TextView hint = label("滚动选择年月日，也可点击数字输入", 12);
        hint.setTextColor(color(R.color.color_text_secondary)); addView(hint);
        LinearLayout headings = new LinearLayout(context);
        for (String text : new String[]{"年", "月", "日"}) headings.addView(label(text, 14), new LayoutParams(0, dp(32), 1));
        addView(headings);
        FrameLayout wheels = new FrameLayout(context); View band = new View(context);
        android.graphics.drawable.GradientDrawable surface = new android.graphics.drawable.GradientDrawable();
        surface.setColor(color(R.color.color_accent_soft)); surface.setCornerRadius(dp(16)); band.setBackground(surface);
        int bandHeight = dp(Math.max(44, Math.round(44 * getResources().getConfiguration().fontScale)));
        wheels.addView(band, new FrameLayout.LayoutParams(-1, bandHeight, Gravity.CENTER));
        LinearLayout columns = new LinearLayout(context);
        years = wheel(minYear, CalendarDates.MAX_YEAR, "年份"); months = wheel(1, 12, "月份"); days = wheel(1, 31, "日期");
        for (NumberPicker wheel : new NumberPicker[]{years, months, days}) columns.addView(wheel, new LayoutParams(0, -1, 1));
        wheels.addView(columns, new FrameLayout.LayoutParams(-1, -1));
        int height = dp(Math.round((getResources().getConfiguration().screenHeightDp < 420 ? 120 : 168) * Math.max(1f, getResources().getConfiguration().fontScale)));
        addView(wheels, new LayoutParams(-1, height));
        setDate(selected);
        years.setOnValueChangedListener((picker, old, value) -> changedYear());
        months.setOnValueChangedListener((picker, old, value) -> updateDays());
        days.setOnValueChangedListener((picker, old, value) -> { if (!updating) updatePreview(); });
    }
    public void setDate(Calendar selected) {
        updating = true;
        if (showLunar) {
            LunarDateWheelModel value = LunarDateWheelModel.from(selected);
            years.setMinValue(Math.max(0, minYear - 1)); years.setValue(value.year);
            populateLunarMonths(value.month, value.leap); populateDays(value.day);
        } else {
            years.setMinValue(minYear); years.setValue(selected.get(Calendar.YEAR));
            months.setDisplayedValues(null); months.setMaxValue(12); months.setValue(selected.get(Calendar.MONTH) + 1);
            populateDays(selected.get(Calendar.DAY_OF_MONTH));
        }
        updating = false; updatePreview();
    }
    public Calendar getSelectedDate() { years.clearFocus(); months.clearFocus(); days.clearFocus(); return selectedDate(); }
    private Calendar selectedDate() {
        if (showLunar) return lunarMonths.get(months.getValue() - 1).date(days.getValue(), original);
        return CalendarDates.select(original, years.getValue(), months.getValue(), days.getValue());
    }
    private void changedYear() {
        if (updating) return;
        int day = days.getValue(); updating = true;
        if (showLunar) {
            LunarDateWheelModel.Month old = lunarMonths.get(months.getValue() - 1); populateLunarMonths(old.number, old.leap);
        }
        populateDays(day); updating = false; updatePreview();
    }
    private void populateLunarMonths(int number, boolean leap) {
        lunarMonths = LunarDateWheelModel.months(years.getValue());
        String[] labels = new String[lunarMonths.size()]; int selected = 1;
        for (int i = 0; i < labels.length; i++) {
            LunarDateWheelModel.Month month = lunarMonths.get(i); labels[i] = month.label();
            if (month.number == number && !month.leap) selected = i + 1;
        }
        for (int i = 0; i < labels.length; i++) if (lunarMonths.get(i).number == number && lunarMonths.get(i).leap == leap) selected = i + 1;
        months.setDisplayedValues(null); months.setMaxValue(labels.length); months.setDisplayedValues(labels); months.setValue(selected);
    }
    private void updateDays() {
        if (updating) return;
        updating = true; populateDays(days.getValue()); updating = false; updatePreview();
    }
    private void populateDays(int preferred) {
        int max = showLunar ? lunarMonths.get(months.getValue() - 1).days
                : CalendarDates.select(original, years.getValue(), months.getValue(), 1).getActualMaximum(Calendar.DAY_OF_MONTH);
        days.setDisplayedValues(null); days.setValue(Math.min(preferred, max)); days.setMaxValue(max);
        if (showLunar) {
            String[] labels = new String[max]; for (int i = 0; i < max; i++) labels[i] = LunarDateWheelModel.dayName(i + 1);
            days.setDisplayedValues(labels);
        }
        days.setValue(Math.min(preferred, max));
    }
    private void updatePreview() { preview.setText(new SimpleDateFormat("yyyy年M月d日 EEEE", Locale.CHINA).format(selectedDate().getTime())); }
    public void setShowLunar(boolean enabled) {
        if (showLunar == enabled) return;
        Calendar selected = getSelectedDate(); showLunar = enabled; setDate(selected);
    }
    private NumberPicker wheel(int minimum, int maximum, String description) {
        NumberPicker picker = new NumberPicker(getContext()); picker.setMinValue(minimum); picker.setMaxValue(maximum);
        picker.setWrapSelectorWheel(false); picker.setContentDescription(description);
        picker.setFormatter(value -> String.format(Locale.ROOT, "%02d", value));
        if (Build.VERSION.SDK_INT >= 29) { picker.setTextColor(color(R.color.color_text_primary)); picker.setSelectionDividerHeight(0); }
        else {
            try {
                java.lang.reflect.Field divider = NumberPicker.class.getDeclaredField("mSelectionDivider"); divider.setAccessible(true);
                divider.set(picker, new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            } catch (ReflectiveOperationException ignored) { /* Older OEM widgets may not expose the divider. */ }
        }
        picker.setOnTouchListener((view, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN && view.getParent() != null) view.getParent().requestDisallowInterceptTouchEvent(true);
            if ((event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL) && view.getParent() != null) view.getParent().requestDisallowInterceptTouchEvent(false);
            return false;
        });
        for (int i = 0; i < picker.getChildCount(); i++) if (picker.getChildAt(i) instanceof EditText) {
            EditText editor = (EditText) picker.getChildAt(i); editor.setTextColor(color(R.color.color_text_primary));
            editor.setInputType(InputType.TYPE_CLASS_NUMBER); editor.setContentDescription(description + "，可输入或滚动选择");
        }
        return picker;
    }
    private TextView label(String text, int size) {
        TextView view = new TextView(getContext()); view.setText(text); view.setTextSize(size);
        view.setTextColor(color(R.color.color_text_primary)); view.setGravity(Gravity.CENTER); return view;
    }
    private int color(int resource) { return ContextCompat.getColor(getContext(), resource); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}

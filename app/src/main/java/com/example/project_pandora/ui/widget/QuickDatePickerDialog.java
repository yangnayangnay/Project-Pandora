package com.example.project_pandora.ui.widget;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.appcompat.app.AlertDialog;

import com.example.project_pandora.R;
import com.google.android.material.button.MaterialButton;

import java.util.Calendar;
import java.util.function.Consumer;

/** Shared date picker for company navigation and personal daily items. */
public final class QuickDatePickerDialog {
    private QuickDatePickerDialog() {}

    public static AlertDialog create(Context context, Calendar selected, Consumer<Calendar> onSelected) {
        return create(context, selected, CalendarDates.MIN_YEAR, onSelected);
    }

    public static AlertDialog create(Context context, Calendar selected, int minYear, Consumer<Calendar> onSelected) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context).setTitle("选择日期");
        Context themed = builder.getContext();
        LinearLayout content = new LinearLayout(themed); content.setOrientation(LinearLayout.VERTICAL);
        YearMonthDayPicker picker = new YearMonthDayPicker(themed, selected, minYear);
        content.addView(picker);
        MaterialButton today = new MaterialButton(themed, null, com.google.android.material.R.attr.borderlessButtonStyle);
        today.setText("回到今天"); today.setMinHeight(dp(context, 48));
        today.setOnClickListener(v -> {
            picker.clearFocus();
            picker.setDate(Calendar.getInstance());
        });
        LinearLayout.LayoutParams todayParams = new LinearLayout.LayoutParams(-1, dp(context, 48));
        todayParams.setMargins(dp(context, 16), 0, dp(context, 16), 0);
        content.addView(today, todayParams);
        ScrollView scroll = new ScrollView(themed); scroll.addView(content);
        AlertDialog dialog = builder.setView(scroll).setNegativeButton(R.string.cancel, null)
                .setPositiveButton("确定", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            Calendar chosen = picker.getSelectedDate();
            View focused = dialog.getCurrentFocus();
            InputMethodManager keyboard = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (keyboard != null && focused != null) keyboard.hideSoftInputFromWindow(focused.getWindowToken(), 0);
            dialog.dismiss();
            onSelected.accept(chosen);
        }));
        return dialog;
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}

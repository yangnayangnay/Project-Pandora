package com.example.project_pandora.ui.widget;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.example.project_pandora.R;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.function.Consumer;

/** Shared Monday-to-Sunday date header for month collapse, week and day. */
public class WeekDateStrip extends PeriodSwipeLayout {
    private Calendar selected = Calendar.getInstance();
    private Consumer<Calendar> listener;
    private Integer selectionColor;

    public WeekDateStrip(Context context) { this(context, null); }
    public WeekDateStrip(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);
        setDate(selected);
    }

    public void setOnDateSelected(Consumer<Calendar> listener) { this.listener = listener; }
    public void setSelectionColor(int color) { selectionColor = color; setDate(selected); }

    public void setDate(Calendar date) {
        selected = (Calendar) date.clone();
        removeAllViews();
        Calendar monday = (Calendar) selected.clone();
        monday.add(Calendar.DATE, -((monday.get(Calendar.DAY_OF_WEEK) + 5) % 7));
        String[] names = {"一", "二", "三", "四", "五", "六", "日"};
        Calendar today = Calendar.getInstance();
        for (int i = 0; i < 7; i++) {
            Calendar value = (Calendar) monday.clone(); value.add(Calendar.DATE, i);
            LinearLayout cell = new LinearLayout(getContext());
            cell.setOrientation(VERTICAL); cell.setGravity(Gravity.CENTER);
            boolean active = sameDay(value, selected);
            TextView weekday = label(names[i], 12);
            TextView number = label(Integer.toString(value.get(Calendar.DAY_OF_MONTH)), CalendarCellMetrics.DATE_TEXT_SP);
            number.setTypeface(null, Typeface.BOLD);
            float scale = getResources().getConfiguration().fontScale;
            int numberHeight = dp(24 * scale), captionHeight = dp(17 * scale);
            LinearLayout badge = new LinearLayout(getContext()); badge.setOrientation(VERTICAL); badge.setGravity(Gravity.CENTER);
            GradientDrawable circle = new GradientDrawable(); circle.setShape(GradientDrawable.OVAL);
            circle.setColor(active ? (selectionColor == null ? color(R.color.color_calendar_today) : selectionColor) : android.graphics.Color.TRANSPARENT);
            badge.setBackground(circle);
            if (active) number.setTextColor(color(R.color.color_on_accent));
            CalendarDayLabels labels = CalendarDayLabels.forDate(value);
            TextView caption = label(labels.special.isEmpty() ? labels.lunar : labels.special, 12);
            caption.setSingleLine(true); caption.setEllipsize(android.text.TextUtils.TruncateAt.END);
            caption.setTextColor(color(active ? R.color.color_on_accent : labels.special.isEmpty() ? R.color.color_text_secondary : R.color.color_accent));
            cell.addView(weekday, new LayoutParams(LayoutParams.MATCH_PARENT, dp(20 * scale)));
            badge.addView(number, new LayoutParams(LayoutParams.MATCH_PARENT, numberHeight));
            badge.addView(caption, new LayoutParams(LayoutParams.MATCH_PARENT, captionHeight));
            cell.addView(badge, new LayoutParams(LayoutParams.MATCH_PARENT, Math.max(dp(44), numberHeight + captionHeight + dp(4))));
            cell.setSelected(active); cell.setFocusable(true);
            cell.setContentDescription(new SimpleDateFormat("yyyy年M月d日 EEEE", Locale.CHINA).format(value.getTime())
                    + "，" + labels.lunar + (labels.special.isEmpty() ? "" : "，" + labels.special)
                    + (sameDay(value, today) ? "，今天" : "") + (active ? "，已选择" : ""));
            cell.setOnClickListener(v -> {
                setDate(value);
                if (listener != null) listener.accept((Calendar) value.clone());
            });
            addView(cell, new LayoutParams(0, Math.max(dp(72), dp(20 * scale) + numberHeight + captionHeight + dp(8)), 1));
        }
    }

    private boolean sameDay(Calendar a, Calendar b) { return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR); }
    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private int color(int id) {
        return ContextCompat.getColor(getContext(), id == R.color.color_text_secondary
                ? R.color.color_company_photo_secondary : id);
    }
    private TextView label(String text, float size) {
        TextView view = new TextView(getContext()); view.setText(text); view.setTextSize(size);
        view.setGravity(Gravity.CENTER); view.setIncludeFontPadding(false); view.setTextColor(color(R.color.color_text_primary));
        return view;
    }
}

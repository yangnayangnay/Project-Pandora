package com.example.project_pandora.ui.home;

import android.content.Context;
import android.util.AttributeSet;
import com.example.project_pandora.R;
import com.example.project_pandora.ui.widget.PhoneMonthView;
import androidx.core.content.ContextCompat;
import java.util.*;

/** Reuses the company calendar appearance with private-todo display and gestures. */
public final class DailyItemsMonthView extends PhoneMonthView {
    public DailyItemsMonthView(Context context) { this(context, null); }
    public DailyItemsMonthView(Context context, AttributeSet attrs) {
        super(context, attrs); setMinimumHeight(Math.round(320 * getResources().getDisplayMetrics().density));
        setParentScrollingEnabled(true);
        setContentDescription("每日事项月历，选择日期查看个人待办，上滑收起顶部板块");
    }
    public void setItems(List<Map<String, Object>> items) { setTasks(DailyItems.calendarEntries(items)); }
    @Override protected int color(int id) {
        return super.color(id == R.color.color_text_secondary ? R.color.home_photo_secondary : id);
    }
    @Override protected String entryName(Map<String, Object> item) { return (DailyItems.completed(item) ? "✓ " : "") + DailyItems.content(item); }
    @Override protected int entryColor(Map<String, Object> item) {
        return ContextCompat.getColor(getContext(), R.color.color_calendar_neutral_entry);
    }
    @Override protected boolean entryCompleted(Map<String, Object> item) { return DailyItems.completed(item); }
}

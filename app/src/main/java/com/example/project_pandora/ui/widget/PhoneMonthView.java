package com.example.project_pandora.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.icu.util.ChineseCalendar;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import androidx.core.content.ContextCompat;
import com.example.project_pandora.R;
import com.example.project_pandora.core.util.DateCompat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Phone-style month grid: dates and dots normally, per-day task chips when expanded. */
public class PhoneMonthView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<DatedTask> tasks = new ArrayList<>();
    private final Map<String, String> lunarLabels = new java.util.HashMap<>();
    private final List<Hit> hits = new ArrayList<>();
    private Calendar selected = Calendar.getInstance();
    private boolean details, swiped;
    private float downX, downY;
    private final int slop;
    private CalendarView.OnDateSelectedListener dateListener;
    private CalendarView.OnVerticalSwipeListener swipeListener;
    private java.util.function.Consumer<Map<String, Object>> taskListener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
    private final int[] colors, textColors;

    public PhoneMonthView(Context context) { this(context, null); }
    public PhoneMonthView(Context context, AttributeSet attrs) {
        super(context, attrs); setFocusable(true); setClickable(true);
        slop = ViewConfiguration.get(context).getScaledTouchSlop();
        colors = palette(R.array.calendar_month_detail_colors); textColors = palette(R.array.calendar_month_detail_text_colors);
        if (isInEditMode()) {
            java.util.Map<String, Object> sample = new java.util.HashMap<>();
            sample.put("name", "项目会议");
            sample.put("start_time", dateFormat.format(selected.getTime()) + " 18:30");
            sample.put("end_time", dateFormat.format(selected.getTime()) + " 19:30"); setTasks(java.util.Collections.singletonList(sample));
        }
    }
    public void setDate(Calendar date) {
        if (selected.get(Calendar.YEAR) != date.get(Calendar.YEAR) || selected.get(Calendar.MONTH) != date.get(Calendar.MONTH)) lunarLabels.clear();
        selected = (Calendar) date.clone(); invalidate();
    }
    public void setTasks(List<Map<String, Object>> values) {
        tasks.clear();
        if (values != null) for (Map<String, Object> task : values) tasks.add(new DatedTask(task));
        invalidate();
    }
    public void setDetailed(boolean value) { details = value; invalidate(); }
    public void setOnDateSelectedListener(CalendarView.OnDateSelectedListener value) { dateListener = value; }
    public void setOnVerticalSwipeListener(CalendarView.OnVerticalSwipeListener value) { swipeListener = value; }
    public void setOnTaskClick(java.util.function.Consumer<Map<String, Object>> value) { taskListener = value; }

    private Calendar firstCell() {
        Calendar first = (Calendar) selected.clone(); first.set(Calendar.DAY_OF_MONTH, 1);
        first.add(Calendar.DATE, -((first.get(Calendar.DAY_OF_WEEK) + 5) % 7)); return first;
    }
    private int rowCount() {
        Calendar first = (Calendar) selected.clone(); first.set(Calendar.DAY_OF_MONTH, 1);
        return (((first.get(Calendar.DAY_OF_WEEK) + 5) % 7) + first.getActualMaximum(Calendar.DAY_OF_MONTH) + 6) / 7;
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas); hits.clear();
        float width = getWidth() / 7f, top = dp(34), height = Math.max(1, (getHeight() - top) / rowCount());
        String[] weekdays = {"一", "二", "三", "四", "五", "六", "日"};
        paint.setTextAlign(Paint.Align.CENTER); paint.setTextSize(sp(13)); paint.setTypeface(android.graphics.Typeface.DEFAULT);
        for (int i = 0; i < 7; i++) {
            paint.setColor(color(i >= 5 ? R.color.color_text_secondary : R.color.color_text_primary));
            canvas.drawText(weekdays[i], (i + .5f) * width, dp(22), paint);
        }
        Calendar day = firstCell();
        for (int index = 0; index < rowCount() * 7; index++, day.add(Calendar.DATE, 1)) {
            int row = index / 7, column = index % 7;
            float left = column * width, cellTop = top + row * height, center = left + width / 2;
            boolean active = sameDay(day, selected), currentMonth = day.get(Calendar.MONTH) == selected.get(Calendar.MONTH);
            if (details) {
                paint.setColor(color(R.color.color_calendar_grid)); paint.setStrokeWidth(dp(.5f));
                canvas.drawLine(left, cellTop, left + width, cellTop, paint);
            }
            float scale = Math.min(1f, height / dp(58));
            float dateY = cellTop + dp(26) * scale, lunarY = cellTop + dp(42) * scale;
            if (active) {
                paint.setColor(color(R.color.color_month_selected));
                canvas.drawCircle(center, cellTop + dp(26) * scale, dp(22) * scale, paint);
            }
            paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD); paint.setTextSize(sp(details ? 20 : 22) * scale);
            paint.setColor(active ? color(R.color.color_on_accent) : color(currentMonth && column < 5 ? R.color.color_text_primary : R.color.color_text_secondary));
            paint.setAlpha(currentMonth || active ? 255 : 100);
            canvas.drawText(Integer.toString(day.get(Calendar.DAY_OF_MONTH)), center, dateY, paint);
            paint.setTypeface(android.graphics.Typeface.DEFAULT); paint.setTextSize(sp(10) * scale);
            paint.setColor(active ? color(R.color.color_on_accent) : color(R.color.color_text_secondary));
            paint.setAlpha(currentMonth || active ? 255 : 100);
            canvas.drawText(lunar(day), center, lunarY, paint); paint.setAlpha(255);
            List<Map<String, Object>> daily = tasksOn(day);
            if (!details) {
                if (!daily.isEmpty()) { paint.setColor(color(R.color.color_calendar_today)); canvas.drawCircle(center, cellTop + dp(52) * scale, dp(2), paint); }
                continue;
            }
            float taskTop = cellTop + dp(53), bottom = cellTop + height - dp(4);
            int capacity = Math.max(0, (int) ((bottom - taskTop) / dp(19)));
            for (int n = 0; n < Math.min(capacity, daily.size()); n++) {
                Map<String, Object> task = daily.get(n);
                String name = java.util.Objects.toString(task.get("name"), "未命名任务");
                int colorIndex = (name.hashCode() & Integer.MAX_VALUE) % colors.length;
                RectF rect = new RectF(left + dp(2), taskTop + n * dp(19), left + width - dp(2), taskTop + n * dp(19) + dp(17));
                paint.setColor(colors[colorIndex]); canvas.drawRoundRect(rect, dp(4), dp(4), paint);
                paint.setColor(textColors[colorIndex % textColors.length]); paint.setTextSize(sp(10));
                String text = n == capacity - 1 && daily.size() > capacity ? "+" + (daily.size() - n) + "项" : name;
                canvas.save(); canvas.clipRect(rect);
                canvas.drawText(ellipsize(text, rect.width() - dp(4)), center, rect.top + dp(12), paint); canvas.restore();
                // The overflow chip selects the date so the full day list remains reachable.
                hits.add(new Hit(rect, daily.size() > capacity && n == capacity - 1 ? null : task));
            }
        }
    }
    private List<Map<String, Object>> tasksOn(Calendar day) {
        List<Map<String, Object>> result = new ArrayList<>(); String date = dateFormat.format(day.getTime());
        for (DatedTask task : tasks) if (MonthTaskDates.covers(task.start, task.end, date)) result.add(task.raw);
        return result;
    }
    private String lunar(Calendar day) {
        String key = dateFormat.format(day.getTime());
        if (lunarLabels.containsKey(key)) return lunarLabels.get(key);
        String label = computeLunar(day); lunarLabels.put(key, label); return label;
    }
    private String computeLunar(Calendar day) {
        ChineseCalendar chinese = new ChineseCalendar(); chinese.setTimeInMillis(day.getTimeInMillis());
        int value = chinese.get(android.icu.util.Calendar.DAY_OF_MONTH);
        if (value == 1) return new String[]{"正月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "冬月", "腊月"}[chinese.get(android.icu.util.Calendar.MONTH) % 12];
        if (value == 10) return "初十"; if (value == 20) return "二十"; if (value == 30) return "三十";
        return (value < 10 ? "初" : value < 20 ? "十" : "廿") + new String[]{"", "一", "二", "三", "四", "五", "六", "七", "八", "九"}[value % 10];
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX(); downY = event.getY(); swiped = false;
                getParent().requestDisallowInterceptTouchEvent(true); return true;
            case MotionEvent.ACTION_MOVE:
                if (!swiped && event.getPointerCount() == 1 && Math.abs(event.getY() - downY) > slop * 3 && Math.abs(event.getY() - downY) > Math.abs(event.getX() - downX)) {
                    swiped = true;
                    if (swipeListener != null) { if (event.getY() < downY) swipeListener.onSwipeUp(); else swipeListener.onSwipeDown(); }
                }
                return true;
            case MotionEvent.ACTION_UP:
                getParent().requestDisallowInterceptTouchEvent(false);
                if (!swiped && Math.abs(event.getX() - downX) < slop && Math.abs(event.getY() - downY) < slop) {
                    performClick();
                    boolean overflow = false;
                    for (Hit hit : hits) if (hit.rect.contains(event.getX(), event.getY()) && hit.task != null && taskListener != null) { taskListener.accept(hit.task); return true; }
                    for (Hit hit : hits) if (hit.task == null && hit.rect.contains(event.getX(), event.getY())) overflow = true;
                    float top = dp(34); if (event.getY() < top) return true;
                    int row = (int) ((event.getY() - top) / Math.max(1, (getHeight() - top) / rowCount()));
                    int column = Math.min(6, (int) (event.getX() / (getWidth() / 7f)));
                    if (row >= 0 && row < rowCount()) {
                        Calendar day = firstCell(); day.add(Calendar.DATE, row * 7 + column); setDate(day);
                        if (dateListener != null) dateListener.onDateSelected(day.get(Calendar.YEAR), day.get(Calendar.MONTH) + 1, day.get(Calendar.DAY_OF_MONTH));
                        if (overflow && swipeListener != null) swipeListener.onSwipeUp();
                    }
                }
                return true;
            case MotionEvent.ACTION_CANCEL: swiped = true; getParent().requestDisallowInterceptTouchEvent(false); return true;
            default: return true;
        }
    }
    @Override public boolean performClick() { super.performClick(); return true; }
    private String ellipsize(String text, float width) { if (paint.measureText(text) <= width) return text; int count = paint.breakText(text, true, Math.max(0, width - paint.measureText("…")), null); return text.substring(0, count) + "…"; }
    private boolean sameDay(Calendar a, Calendar b) { return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR); }
    private int color(int id) { return ContextCompat.getColor(getContext(), id); }
    private int[] palette(int id) { String[] values = getResources().getStringArray(id); int[] result = new int[values.length]; for (int i = 0; i < values.length; i++) result[i] = android.graphics.Color.parseColor(values[i]); return result; }
    private float dp(float value) { return value * getResources().getDisplayMetrics().density; }
    private float sp(float value) { return value * getResources().getDisplayMetrics().scaledDensity; }
    private static class Hit { final RectF rect; final Map<String, Object> task; Hit(RectF rect, Map<String, Object> task) { this.rect = rect; this.task = task; } }
    private static class DatedTask {
        final Map<String, Object> raw; final String start, end;
        DatedTask(Map<String, Object> task) {
            raw = task; start = DateCompat.toLocalDateTime(task.get("start_time")); end = DateCompat.toLocalDateTime(task.get("end_time"));
        }
    }
}

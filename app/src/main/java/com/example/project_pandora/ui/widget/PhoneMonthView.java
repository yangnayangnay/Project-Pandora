package com.example.project_pandora.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.icu.util.ChineseCalendar;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import androidx.core.content.ContextCompat;
import com.example.project_pandora.R;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Phone-style month grid: dates and dots normally, continuous weekly task bars when expanded. */
public class PhoneMonthView extends View {
    public static final int VISIBLE_TASK_LIMIT = 4;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<DatedTask> tasks = new ArrayList<>();
    private final Map<String, String> lunarLabels = new java.util.HashMap<>();
    private final List<Hit> hits = new ArrayList<>();
    private Calendar selected = Calendar.getInstance();
    private boolean details, swiped;
    private boolean parentScrolling;
    private float downX, downY;
    private final int slop;
    private final PeriodSwipeGesture periodSwipe;
    private java.util.function.IntConsumer periodListener;
    private CalendarView.OnDateSelectedListener dateListener;
    private CalendarView.OnVerticalSwipeListener swipeListener;
    private java.util.function.Consumer<Map<String, Object>> taskListener;
    private java.util.function.Consumer<Calendar> dayTasksListener;
    private Rect dayTasksAnchor;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
    private final int[] colors;

    public PhoneMonthView(Context context) { this(context, null); }
    public PhoneMonthView(Context context, AttributeSet attrs) {
        super(context, attrs); setFocusable(true); setClickable(true);
        slop = ViewConfiguration.get(context).getScaledTouchSlop();
        periodSwipe = new PeriodSwipeGesture(slop, dp(48));
        colors = palette(R.array.calendar_month_detail_colors);
        if (isInEditMode()) {
            java.util.Map<String, Object> sample = new java.util.HashMap<>();
            sample.put("name", "项目会议");
            sample.put("start_time", dateFormat.format(selected.getTime()) + " 18:30");
            sample.put("end_time", dateFormat.format(selected.getTime()) + " 19:30"); setTasks(java.util.Collections.singletonList(sample));
        }
    }
    public void setDate(Calendar date) {
        if (selected.get(Calendar.YEAR) != date.get(Calendar.YEAR) || selected.get(Calendar.MONTH) != date.get(Calendar.MONTH)) {
            lunarLabels.clear(); hits.clear(); requestLayout();
        }
        selected = (Calendar) date.clone(); invalidate();
    }
    public void setTasks(List<Map<String, Object>> values) {
        tasks.clear(); hits.clear();
        if (values != null) for (Map<String, Object> task : values) tasks.add(new DatedTask(task));
        invalidate();
    }
    public void setDetailed(boolean value) {
        if (details == value) return;
        details = value; hits.clear(); requestLayout(); invalidate();
    }
    public void setOnDateSelectedListener(CalendarView.OnDateSelectedListener value) { dateListener = value; }
    public void setOnVerticalSwipeListener(CalendarView.OnVerticalSwipeListener value) { swipeListener = value; }
    public void setOnPeriodSwipe(java.util.function.IntConsumer value) { periodListener = value; }
    public void setOnTaskClick(java.util.function.Consumer<Map<String, Object>> value) { taskListener = value; }
    public void setParentScrollingEnabled(boolean value) { parentScrolling = value; }
    public void setOnDayTasksClick(java.util.function.Consumer<Calendar> value) { dayTasksListener = value; }
    /** Screen bounds of the task list that opened the day popup. */
    public Rect getDayTasksAnchor() { return dayTasksAnchor == null ? null : new Rect(dayTasksAnchor); }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredHeight = details ? Math.round(dp(34) + rowCount() * dp(152)) : getSuggestedMinimumHeight();
        setMeasuredDimension(resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec),
                resolveSize(desiredHeight, heightMeasureSpec));
    }

    private float rowHeight() {
        return Math.max(details ? dp(152) : 1, (getHeight() - dp(34)) / rowCount());
    }

    public int selectedDateTop() {
        Calendar first = firstCell();
        int offset = 0;
        while (!sameDay(first, selected) && offset < 42) { first.add(Calendar.DATE, 1); offset++; }
        return Math.round(dp(34) + (offset / 7) * rowHeight());
    }

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
        float width = getWidth() / 7f, top = dp(34), height = rowHeight();
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
            if (!details) {
                if (!tasksOn(day).isEmpty()) { paint.setColor(color(R.color.color_calendar_today)); canvas.drawCircle(center, cellTop + dp(52) * scale, dp(2), paint); }
                continue;
            }
        }
        if (details) drawTaskBars(canvas, width, top, height);
    }

    private void drawTaskBars(Canvas canvas, float cellWidth, float gridTop, float rowHeight) {
        List<String> days = new ArrayList<>();
        Calendar day = firstCell();
        for (int cell = 0; cell < rowCount() * 7; cell++, day.add(Calendar.DATE, 1)) {
            days.add(dateFormat.format(day.getTime()));
        }
        List<MonthTaskLayout.Task> dated = new ArrayList<>();
        for (int i = 0; i < tasks.size(); i++) {
            DatedTask task = tasks.get(i);
            dated.add(new MonthTaskLayout.Task(i, task.start, task.end));
        }
        List<MonthTaskLayout.Segment> spans = MonthTaskLayout.visibleSegments(dated, days, VISIBLE_TASK_LIMIT);
        paint.setTypeface(android.graphics.Typeface.DEFAULT);
        paint.setTextSize(sp(10));
        paint.setTextAlign(Paint.Align.LEFT);
        for (int week = 0; week < rowCount(); week++) {
            int[] hidden = new int[7];
            for (int column = 0; column < 7; column++) {
                String date = days.get(week * 7 + column);
                for (DatedTask task : tasks) {
                    if (MonthTaskDates.covers(task.start, task.end, date)) hidden[column]++;
                }
            }
            float taskTop = gridTop + week * rowHeight + dp(53);
            for (MonthTaskLayout.Segment span : spans) {
                if (span.week != week) continue;
                for (int column = span.startColumn; column <= span.endColumn; column++) hidden[column]--;
                Map<String, Object> task = tasks.get(span.taskIndex).raw;
                String name = entryName(task);
                RectF rect = new RectF(span.startColumn * cellWidth + dp(2), taskTop + span.lane * dp(19),
                        (span.endColumn + 1) * cellWidth - dp(2), taskTop + span.lane * dp(19) + dp(17));
                paint.setColor(entryColor(task));
                canvas.drawRoundRect(rect, dp(4), dp(4), paint);
                paint.setColor(color(R.color.color_text_primary));
                paint.setStrikeThruText(entryCompleted(task));
                paint.setAlpha(entryCompleted(task) ? 150 : 255);
                drawBarText(canvas, rect, name);
                paint.setStrikeThruText(false); paint.setAlpha(255);
                hits.add(new Hit(rect, task));
            }
            for (int column = 0; column < 7; column++) {
                if (hidden[column] == 0) continue;
                RectF rect = new RectF(column * cellWidth + dp(2), taskTop + VISIBLE_TASK_LIMIT * dp(19),
                        (column + 1) * cellWidth - dp(2), taskTop + VISIBLE_TASK_LIMIT * dp(19) + dp(17));
                paint.setColor(color(R.color.color_text_secondary));
                drawBarText(canvas, rect, "+" + hidden[column] + "项");
                hits.add(new Hit(rect, null));
            }
        }
    }

    private void drawBarText(Canvas canvas, RectF rect, String text) {
        canvas.save();
        canvas.clipRect(rect);
        float baseline = rect.centerY() - (paint.ascent() + paint.descent()) / 2;
        canvas.drawText(ellipsize(text, rect.width() - dp(8)), rect.left + dp(4), baseline, paint);
        canvas.restore();
    }

    protected String entryName(Map<String, Object> item) { return java.util.Objects.toString(DayTaskSelection.value(item, "name"), "未命名任务"); }
    protected int entryColor(Map<String, Object> item) { return colors[com.example.project_pandora.core.util.TaskTags.colorIndex(item, colors.length)]; }
    protected boolean entryCompleted(Map<String, Object> item) { return false; }
    private List<Map<String, Object>> tasksOn(Calendar day) {
        List<Map<String, Object>> result = new ArrayList<>();
        String date = dateFormat.format(day.getTime());
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
        if (event.getPointerCount() > 1 || event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
            periodSwipe.cancel(); swiped = true;
            return true;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX(); downY = event.getY(); swiped = false;
                periodSwipe.start(downX, downY);
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(!details && !parentScrolling);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (periodListener != null && periodSwipe.move(event.getX(), event.getY())) {
                    swiped = true;
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                }
                if (details) {
                    if (Math.abs(event.getY() - downY) > slop || Math.abs(event.getX() - downX) > slop) swiped = true;
                    return true;
                }
                if (!swiped && event.getPointerCount() == 1 && Math.abs(event.getY() - downY) > slop * 3 && Math.abs(event.getY() - downY) > Math.abs(event.getX() - downX)) {
                    swiped = true;
                    if (swipeListener != null) { if (event.getY() < downY) swipeListener.onSwipeUp(); else swipeListener.onSwipeDown(); }
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                int direction = periodSwipe.finish(event.getX(), event.getY());
                if (direction != 0 && periodListener != null) {
                    swiped = true;
                    periodListener.accept(direction);
                    return true;
                }
                if (!swiped && Math.abs(event.getX() - downX) < slop && Math.abs(event.getY() - downY) < slop) {
                    performClick();
                    float top = dp(34);
                    if (event.getY() < top || event.getX() < 0 || event.getX() >= getWidth()) return true;
                    int row = (int) ((event.getY() - top) / rowHeight());
                    int column = (int) (event.getX() / (getWidth() / 7f));
                    if (row >= 0 && row < rowCount()) {
                        Calendar day = firstCell(); day.add(Calendar.DATE, row * 7 + column);
                        boolean clickedTaskList = false;
                        Map<String, Object> clickedTask = null;
                        for (Hit hit : hits) {
                            if (hit.rect.contains(event.getX(), event.getY())) {
                                clickedTaskList = true; clickedTask = hit.task; break;
                            }
                        }
                        if (clickedTaskList) {
                            int[] location = new int[2];
                            getLocationOnScreen(location);
                            dayTasksAnchor = new Rect(location[0] + Math.round(column * getWidth() / 7f),
                                    location[1] + Math.round(top + row * rowHeight() + dp(53)),
                                    location[0] + Math.round((column + 1) * getWidth() / 7f),
                                    location[1] + Math.round(top + (row + 1) * rowHeight()));
                        }
                        setDate(day);
                        if (dateListener != null) dateListener.onDateSelected(day.get(Calendar.YEAR), day.get(Calendar.MONTH) + 1, day.get(Calendar.DAY_OF_MONTH));
                        if (clickedTaskList && dayTasksListener != null) dayTasksListener.accept((Calendar) day.clone());
                        else if (clickedTask != null && taskListener != null) taskListener.accept(clickedTask);
                    }
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                swiped = true; periodSwipe.cancel();
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                return true;
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
            raw = task; start = DayTaskSelection.start(task); end = DayTaskSelection.end(task);
        }
    }
}

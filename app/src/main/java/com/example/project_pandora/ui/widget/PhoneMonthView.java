package com.example.project_pandora.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
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
    public static final int VISIBLE_TASK_LIMIT = CalendarCellMetrics.VISIBLE_ENTRIES;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<DatedTask> tasks = new ArrayList<>();
    private final Map<String, CalendarDayLabels> dayLabels = new java.util.HashMap<>();
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
    private OnTaskMoveListener taskMoveListener;
    private java.util.function.Consumer<Calendar> dayTasksListener;
    private Rect dayTasksAnchor;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
    private final CalendarCellMetrics cellMetrics;
    private CalendarCellMetrics metrics() { return cellMetrics; }
    private float weekdayHeight() { return dp(metrics().weekdayHeight()); }
    private Map<String, Object> dragCandidate, draggingTask;
    private Calendar dragTarget;
    private long downTime;
    private float dragX, dragY;

    public interface OnTaskMoveListener {
        void onTaskMove(Map<String, Object> task, Calendar targetStartDay);
    }

    public PhoneMonthView(Context context) { this(context, null); }
    public PhoneMonthView(Context context, AttributeSet attrs) {
        super(context, attrs); setFocusable(true); setClickable(true);
        cellMetrics = new CalendarCellMetrics(getResources().getConfiguration().fontScale);
        slop = ViewConfiguration.get(context).getScaledTouchSlop();
        periodSwipe = new PeriodSwipeGesture(slop, dp(48));
        if (isInEditMode()) {
            java.util.Map<String, Object> sample = new java.util.HashMap<>();
            sample.put("name", "项目会议");
            sample.put("start_time", dateFormat.format(selected.getTime()) + " 18:30");
            sample.put("end_time", dateFormat.format(selected.getTime()) + " 19:30"); setTasks(java.util.Collections.singletonList(sample));
        }
    }
    public void setDate(Calendar date) {
        if (selected.get(Calendar.YEAR) != date.get(Calendar.YEAR) || selected.get(Calendar.MONTH) != date.get(Calendar.MONTH)) {
            dayLabels.clear(); hits.clear(); requestLayout();
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
    public void setOnTaskMoveListener(OnTaskMoveListener value) { taskMoveListener = value; }
    public void setParentScrollingEnabled(boolean value) { parentScrolling = value; }
    public void setOnDayTasksClick(java.util.function.Consumer<Calendar> value) { dayTasksListener = value; }
    /** Screen bounds of the task list that opened the day popup. */
    public Rect getDayTasksAnchor() { return dayTasksAnchor == null ? null : new Rect(dayTasksAnchor); }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredHeight = details ? Math.round(weekdayHeight() + rowCount() * dp(metrics().rowHeight()))
                : Math.max(getSuggestedMinimumHeight(), Math.round(weekdayHeight() + rowCount() * dp(metrics().dateHeight())));
        setMeasuredDimension(resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec),
                resolveSize(desiredHeight, heightMeasureSpec));
    }

    private float rowHeight() {
        return Math.max(details ? dp(metrics().rowHeight()) : dp(metrics().dateHeight()), (getHeight() - weekdayHeight()) / rowCount());
    }

    public int selectedDateTop() {
        Calendar first = firstCell();
        int offset = 0;
        while (!sameDay(first, selected) && offset < 42) { first.add(Calendar.DATE, 1); offset++; }
        return Math.round(weekdayHeight() + (offset / 7) * rowHeight());
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
        float width = getWidth() / 7f, top = weekdayHeight(), height = rowHeight();
        String[] weekdays = {"一", "二", "三", "四", "五", "六", "日"};
        paint.setTextAlign(Paint.Align.CENTER); paint.setTextSize(sp(12)); paint.setTypeface(android.graphics.Typeface.DEFAULT);
        for (int i = 0; i < 7; i++) {
            paint.setColor(color(i >= 5 ? R.color.color_text_secondary : R.color.color_text_primary));
            canvas.drawText(weekdays[i], (i + .5f) * width, weekdayHeight() / 2 - (paint.ascent() + paint.descent()) / 2, paint);
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
            int cellSave = canvas.save();
            canvas.clipRect(left, cellTop, left + width, cellTop + height);
            float numberCenter = cellTop + dp(4 + metrics().numberHeight() / 2);
            if (active) {
                paint.setColor(color(R.color.color_month_selected));
                float radius = Math.min(width / 2 - dp(1), dp(metrics().selectionHeight() / 2));
                canvas.drawRoundRect(center - radius, cellTop + dp(2), center + radius,
                        cellTop + dp(2 + metrics().selectionHeight()), radius, radius, paint);
            }
            paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            paint.setTextSize(getResources().getDimension(R.dimen.calendar_date_text_size));
            String dayNumber = Integer.toString(day.get(Calendar.DAY_OF_MONTH));
            float dateWidth = paint.measureText(dayNumber);
            if (dateWidth > width - dp(4)) paint.setTextSize(paint.getTextSize() * (width - dp(4)) / dateWidth);
            float dateY = numberCenter - (paint.ascent() + paint.descent()) / 2;
            paint.setColor(active ? color(R.color.color_on_accent) : color(currentMonth && column < 5 ? R.color.color_text_primary : R.color.color_text_secondary));
            paint.setAlpha(currentMonth || active ? 255 : 180);
            canvas.drawText(dayNumber, center, dateY, paint);
            paint.setTypeface(android.graphics.Typeface.DEFAULT); paint.setTextSize(sp(12));
            CalendarDayLabels labels = labels(day);
            paint.setColor(active ? color(R.color.color_on_accent) : color(labels.special.isEmpty() ? R.color.color_text_secondary : R.color.color_accent));
            float lunarY = cellTop + dp(metrics().lunarTop() + metrics().lunarHeight() / 2) - (paint.ascent() + paint.descent()) / 2;
            float captionWidth = active ? Math.min(width - dp(4), dp(metrics().selectionHeight() - 4)) : width - dp(2);
            canvas.drawText(ellipsize(details || labels.special.isEmpty() ? labels.lunar : labels.special, captionWidth), center, lunarY, paint);
            paint.setAlpha(255);
            if (!details) {
                List<Map<String, Object>> onDay = tasksOn(day);
                if (!onDay.isEmpty()) {
                    float dotY = cellTop + dp(metrics().dateHeight()) - dp(4);
                    paint.setColor(color(R.color.color_calendar_today)); canvas.drawCircle(center, dotY, dp(2), paint);
                    hits.add(new Hit(new RectF(center - dp(12), dotY - dp(4), center + dp(12),
                            Math.min(cellTop + height, dotY + dp(8))), onDay.get(0)));
                }
                canvas.restoreToCount(cellSave);
                continue;
            }
            canvas.restoreToCount(cellSave);
        }
        if (details) drawTaskBars(canvas, width, top, height);
        if (draggingTask != null) {
            paint.setColor(color(R.color.color_accent));
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(2));
            canvas.drawCircle(dragX, dragY, dp(18), paint); paint.setStyle(Paint.Style.FILL);
        }
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
        paint.setTextSize(sp(11));
        paint.setTextAlign(Paint.Align.LEFT);
        for (int week = 0; week < rowCount(); week++) {
            int[] hidden = new int[7];
            int[] occupiedLanes = new int[7];
            for (int column = 0; column < 7; column++) {
                String date = days.get(week * 7 + column);
                for (DatedTask task : tasks) {
                    if (MonthTaskDates.covers(task.start, task.end, date)) hidden[column]++;
                }
            }
            float taskTop = gridTop + week * rowHeight + dp(metrics().taskTop());
            for (MonthTaskLayout.Segment span : spans) {
                if (span.week != week) continue;
                for (int column = span.startColumn; column <= span.endColumn; column++) {
                    hidden[column]--;
                    occupiedLanes[column] = Math.max(occupiedLanes[column], span.lane + 1);
                }
                Map<String, Object> task = tasks.get(span.taskIndex).raw;
                String name = entryName(task);
                RectF rect = new RectF(span.startColumn * cellWidth + dp(2), taskTop + span.lane * dp(metrics().entryStep()),
                        (span.endColumn + 1) * cellWidth - dp(2), taskTop + span.lane * dp(metrics().entryStep()) + dp(metrics().entryHeight()));
                paint.setColor(entryColor(task));
                canvas.drawRoundRect(rect, dp(4), dp(4), paint);
                paint.setColor(entryTextColor(task));
                paint.setStrikeThruText(entryCompleted(task));
                paint.setAlpha(255);
                drawBarText(canvas, rect, name);
                paint.setStrikeThruText(false); paint.setAlpha(255);
                hits.add(new Hit(rect, task));
            }
            for (int column = 0; column < 7; column++) {
                if (hidden[column] == 0) continue;
                RectF rect = new RectF(column * cellWidth + dp(2), taskTop + VISIBLE_TASK_LIMIT * dp(metrics().entryStep()),
                        (column + 1) * cellWidth - dp(2), taskTop + VISIBLE_TASK_LIMIT * dp(metrics().entryStep()) + dp(metrics().entryHeight()));
                paint.setColor(color(R.color.color_text_secondary));
                drawBarText(canvas, rect, "+" + hidden[column] + "项");
                hits.add(new Hit(rect, null));
            }
            // Holidays follow the occupied lanes for this date; they never push
            // tasks down or split a continuous task bar across adjacent dates.
            Calendar specialDay = firstCell(); specialDay.add(Calendar.DATE, week * 7);
            for (int column = 0; column < 7; column++, specialDay.add(Calendar.DATE, 1)) {
                String special = labels(specialDay).special;
                if (special.isEmpty()) continue;
                float holidayTop = gridTop + week * rowHeight
                        + dp(metrics().specialTop(occupiedLanes[column], hidden[column] > 0));
                RectF rect = new RectF(column * cellWidth + dp(2), holidayTop,
                        (column + 1) * cellWidth - dp(2), holidayTop + dp(metrics().specialRowHeight() - 2));
                paint.setColor(color(R.color.color_surface_soft));
                canvas.drawRoundRect(rect, dp(4), dp(4), paint);
                paint.setTextSize(sp(12)); paint.setColor(color(R.color.color_text_primary));
                drawBarText(canvas, rect, special);
                paint.setTextSize(sp(11));
            }
        }
    }

    private void drawBarText(Canvas canvas, RectF rect, String text) {
        canvas.save();
        canvas.clipRect(rect);
        float available = rect.width() - dp(4);
        String value = text.replace('\n', ' ');
        float baseline = rect.centerY() - (paint.ascent() + paint.descent()) / 2;
        canvas.drawText(ellipsize(value, available), rect.left + dp(2), baseline, paint);
        canvas.restore();
    }

    protected String entryName(Map<String, Object> item) { return java.util.Objects.toString(DayTaskSelection.value(item, "name"), "未命名任务"); }
    protected int entryColor(Map<String, Object> item) { return com.example.project_pandora.ui.task.TaskTagColors.task(getContext(), item); }
    protected int entryTextColor(Map<String, Object> item) {
        return com.example.project_pandora.ui.task.TaskTagColors.text(getContext(), entryColor(item), android.graphics.Color.BLACK);
    }
    protected boolean entryCompleted(Map<String, Object> item) { return false; }
    private List<Map<String, Object>> tasksOn(Calendar day) {
        List<Map<String, Object>> result = new ArrayList<>();
        String date = dateFormat.format(day.getTime());
        for (DatedTask task : tasks) if (MonthTaskDates.covers(task.start, task.end, date)) result.add(task.raw);
        return result;
    }
    private CalendarDayLabels labels(Calendar day) {
        String key = dateFormat.format(day.getTime());
        CalendarDayLabels cached = dayLabels.get(key);
        if (cached == null) { cached = CalendarDayLabels.forDate(day); dayLabels.put(key, cached); }
        return cached;
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() > 1 || event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
            periodSwipe.cancel(); swiped = true;
            return true;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX(); downY = event.getY(); swiped = false;
                downTime = event.getEventTime();
                dragCandidate = taskMoveListener == null ? null : hitTask(downX, downY);
                draggingTask = null; dragTarget = null;
                dragX = downX; dragY = downY;
                periodSwipe.start(downX, downY);
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(dragCandidate != null || (!details && !parentScrolling));
                return true;
            case MotionEvent.ACTION_MOVE:
                if (draggingTask == null && dragCandidate != null
                        && event.getEventTime() - downTime < ViewConfiguration.getLongPressTimeout()
                        && Math.hypot(event.getX() - downX, event.getY() - downY) > slop) {
                    dragCandidate = null;
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(!details && !parentScrolling);
                }
                if (dragCandidate != null && draggingTask == null
                        && event.getEventTime() - downTime >= ViewConfiguration.getLongPressTimeout()) {
                    draggingTask = dragCandidate; swiped = true; periodSwipe.cancel();
                    performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                }
                if (draggingTask != null) {
                    dragX = event.getX(); dragY = event.getY();
                    dragTarget = dayAt(event.getX(), event.getY());
                    invalidate();
                    return true;
                }
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
                if (draggingTask == null && dragCandidate != null
                        && event.getEventTime() - downTime >= ViewConfiguration.getLongPressTimeout()) {
                    draggingTask = dragCandidate; dragTarget = dayAt(event.getX(), event.getY());
                }
                if (draggingTask != null) {
                    Calendar target = dragTarget != null ? dragTarget : dayAt(event.getX(), event.getY());
                    Map<String, Object> moved = draggingTask;
                    draggingTask = null; dragCandidate = null; dragTarget = null; periodSwipe.cancel();
                    invalidate();
                    if (target != null && taskMoveListener != null) taskMoveListener.onTaskMove(moved, target);
                    return true;
                }
                int direction = periodSwipe.finish(event.getX(), event.getY());
                if (direction != 0 && periodListener != null) {
                    swiped = true;
                    periodListener.accept(direction);
                    CalendarPageMotion.enter(this, direction);
                    return true;
                }
                if (!swiped && Math.abs(event.getX() - downX) < slop && Math.abs(event.getY() - downY) < slop) {
                    performClick();
                    float top = weekdayHeight();
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
                        boolean openDay = clickedTaskList;
                        dayTasksAnchor = null;
                        if (openDay) {
                            int[] location = new int[2];
                            getLocationOnScreen(location);
                            dayTasksAnchor = new Rect(location[0] + Math.round(column * getWidth() / 7f),
                                    location[1] + Math.round(top + row * rowHeight() + (details ? dp(metrics().dateHeight()) : 0)),
                                    location[0] + Math.round((column + 1) * getWidth() / 7f),
                                    location[1] + Math.round(top + (row + 1) * rowHeight()));
                        }
                        setDate(day);
                        if (dateListener != null) dateListener.onDateSelected(day.get(Calendar.YEAR), day.get(Calendar.MONTH) + 1, day.get(Calendar.DAY_OF_MONTH));
                        if (openDay && dayTasksListener != null) dayTasksListener.accept((Calendar) day.clone());
                        else if (clickedTask != null && taskListener != null) taskListener.accept(clickedTask);
                    }
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                swiped = true; periodSwipe.cancel(); dragCandidate = null; draggingTask = null; dragTarget = null;
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                return true;
            default: return true;
        }
    }
    @Override public boolean performClick() { super.performClick(); return true; }
    private Map<String, Object> hitTask(float x, float y) {
        for (Hit hit : hits) if (hit.task != null && hit.rect.contains(x, y)) return hit.task;
        return null;
    }
    private Calendar dayAt(float x, float y) {
        float top = weekdayHeight();
        if (x < 0 || x >= getWidth() || y < top) return null;
        int row = (int) ((y - top) / rowHeight());
        int column = (int) (x / (getWidth() / 7f));
        if (row < 0 || row >= rowCount() || column < 0 || column > 6) return null;
        Calendar day = firstCell(); day.add(Calendar.DATE, row * 7 + column); return day;
    }
    private String ellipsize(String text, float width) { if (paint.measureText(text) <= width) return text; int count = paint.breakText(text, true, Math.max(0, width - paint.measureText("…")), null); return text.substring(0, count) + "…"; }
    private boolean sameDay(Calendar a, Calendar b) { return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR); }
    protected int color(int id) {
        return ContextCompat.getColor(getContext(), id == R.color.color_text_secondary
                ? R.color.color_company_photo_secondary : id);
    }
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

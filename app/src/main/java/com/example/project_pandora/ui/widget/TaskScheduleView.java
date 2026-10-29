package com.example.project_pandora.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import com.example.project_pandora.R;
import com.example.project_pandora.core.util.DateCompat;
import java.text.SimpleDateFormat;
import java.util.*;

/** Native week agenda and full-day timeline with a shared date header. */
public class TaskScheduleView extends LinearLayout {
    private static final long MINUTE = 60_000L;
    private boolean dayMode;
    private Calendar selected = midnight(Calendar.getInstance());
    private final List<Entry> entries = new ArrayList<>();
    private final List<Map<String, Object>> rawTasks = new ArrayList<>();
    private WeekDateStrip dates;
    private LinearLayout allDayColumns, weekTasks;
    private LinearLayout allDayRow;
    private TextView month, message;
    private ScrollView scroll;
    private Timeline timeline;
    private java.util.function.Consumer<Calendar> dateListener;
    private java.util.function.Consumer<Map<String, Object>> taskListener;
    private final SimpleDateFormat dateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT);
    private int[] backgrounds, foregrounds;
    private boolean initialScroll = true;
    private String loadError;
    private final Runnable clockTick = new Runnable() {
        @Override public void run() { timeline.invalidate(); if (isAttachedToWindow()) postDelayed(this, 60_000); }
    };

    public TaskScheduleView(Context c) { this(c, null); }
    public TaskScheduleView(Context c, AttributeSet attrs) { this(c, attrs, 0); }
    public TaskScheduleView(Context c, AttributeSet attrs, int style) {
        super(c, attrs, style);
        TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.TaskScheduleView);
        dayMode = a.getInt(R.styleable.TaskScheduleView_scheduleMode, 0) == 1; a.recycle();
        backgrounds = colors(R.array.calendar_month_detail_colors);
        foregrounds = colors(R.array.calendar_month_detail_text_colors);
        setOrientation(VERTICAL);
        setBackgroundColor(color(R.color.color_bg_page));
        build();
        if (isInEditMode()) {
            List<Map<String, Object>> samples = new ArrayList<>();
            samples.add(sample("界面优化与验收", 0, true));
            samples.add(sample("项目进度讨论", 9, false));
            samples.add(sample("整理今日工作纪要", 14, false));
            setTasks(samples);
        } else render();
    }

    public void setOnDateChanged(java.util.function.Consumer<Calendar> listener) { dateListener = listener; }
    public void setOnTaskClick(java.util.function.Consumer<Map<String, Object>> listener) { taskListener = listener; }
    public void setDate(Calendar date) {
        if (sameDay(selected, date)) return;
        selected = midnight(date);
        initialScroll = true;
        render();
    }
    public void setTasks(List<Map<String, Object>> tasks) {
        loadError = null;
        message.setOnClickListener(null);
        rawTasks.clear(); if (tasks != null) rawTasks.addAll(tasks);
        entries.clear();
        for (Map<String, Object> task : rawTasks) {
            Entry entry = parse(task); if (entry != null) entries.add(entry);
        }
        render();
    }
    public void setError(String error) { loadError = error; renderMessage(); }

    private void renderMessage() {
        if (loadError != null) {
            message.setText(loadError + " · 点击重试");
            message.setOnClickListener(v -> notifyDate());
            return;
        }
        message.setOnClickListener(null);
        Calendar first = dayMode ? midnight(selected) : weekStart();
        Calendar next = (Calendar) first.clone(); next.add(Calendar.DATE, dayMode ? 1 : 7);
        int count = 0;
        for (Entry e : entries) if (SchedulePeriod.intersects(e.start, e.end, e.allDay,
                first.getTimeInMillis(), next.getTimeInMillis())) count++;
        message.setText((dayMode ? "当天" : "本周") + "共 " + count + " 项任务 · "
                + (count == 0 ? "点击右下角 + 创建" : "点击查看详情"));
    }

    private void build() {
        LinearLayout nav = new LinearLayout(getContext()); nav.setGravity(Gravity.CENTER_VERTICAL);
        nav.setPadding(dp(12), dp(4), dp(12), dp(4));
        month = label("", 22); month.setTypeface(null, Typeface.BOLD);
        nav.addView(month, new LayoutParams(0, dp(56), 1));
        TextView previous = action("‹", "上一" + (dayMode ? "天" : "周"));
        TextView today = action("今天", "回到今天"); today.setTextSize(14);
        TextView next = action("›", "下一" + (dayMode ? "天" : "周"));
        previous.setOnClickListener(v -> shift(dayMode ? -1 : -7));
        next.setOnClickListener(v -> shift(dayMode ? 1 : 7));
        today.setOnClickListener(v -> { selected = midnight(Calendar.getInstance()); initialScroll = true; render(); notifyDate(); });
        nav.addView(previous, new LayoutParams(dp(48), dp(48)));
        nav.addView(today, new LayoutParams(dp(48), dp(48)));
        nav.addView(next, new LayoutParams(dp(48), dp(48))); addView(nav);
        dates = new WeekDateStrip(getContext());
        dates.setOnDateSelected(date -> { selected = midnight(date); initialScroll = true; render(); notifyDate(); });
        addView(dates, new LayoutParams(LayoutParams.MATCH_PARENT, dp(88)));
        LinearLayout allDay = new LinearLayout(getContext()); allDayRow = allDay; allDay.setPadding(0, dp(6), dp(6), dp(6));
        allDay.setBackgroundColor(color(R.color.color_calendar_bg));
        TextView allDayLabel = label("全天", 12); allDayLabel.setTextColor(color(R.color.color_text_secondary));
        allDay.addView(allDayLabel, new LayoutParams(dp(44), dp(48)));
        allDayColumns = new LinearLayout(getContext());
        allDay.addView(allDayColumns, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1)); addView(allDay);
        message = label("", 12); message.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        message.setTextColor(color(R.color.color_text_secondary)); message.setPadding(dp(16), dp(4), dp(16), dp(4));
        addView(message, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        scroll = new ScrollView(getContext()); scroll.setFillViewport(false); scroll.setClipToPadding(false);
        scroll.setPadding(0, 0, 0, dp(88));
        timeline = new Timeline(getContext());
        GradientDrawable surface = rounded(color(R.color.color_calendar_bg), 20);
        scroll.setBackground(surface); scroll.setClipToOutline(true);
        if (dayMode) {
            scroll.addView(timeline, new ScrollView.LayoutParams(LayoutParams.MATCH_PARENT, hourHeight() * 24 + dp(64)));
        } else {
            allDayRow.setVisibility(GONE);
            weekTasks = new LinearLayout(getContext()); weekTasks.setOrientation(VERTICAL);
            weekTasks.setPadding(dp(12), dp(8), dp(12), dp(12));
            scroll.addView(weekTasks, new ScrollView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        }
        addView(scroll, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1));
    }

    private void render() {
        if (dates == null) return;
        month.setText(new SimpleDateFormat("yyyy年M月", Locale.CHINA).format(selected.getTime()));
        dates.setDate(selected);
        renderMessage();
        allDayColumns.removeAllViews();
        Calendar start = weekStart();
        if (!dayMode) {
            renderWeekTasks(start);
            if (initialScroll) { initialScroll = false; scroll.post(() -> scroll.scrollTo(0, 0)); }
            return;
        }
        Calendar first = dayMode ? (Calendar) selected.clone() : start;
        int count = dayMode ? 1 : 7;
        for (int col = 0; col < count; col++) {
            Calendar day = (Calendar) first.clone(); day.add(Calendar.DATE, col);
            LinearLayout column = new LinearLayout(getContext()); column.setOrientation(VERTICAL);
            List<Entry> allDay = new ArrayList<>();
            for (Entry e : entries) if (e.allDay && covers(e, day)) allDay.add(e);
            for (int n = 0; n < Math.min(2, allDay.size()); n++) {
                Entry e = allDay.get(n); TextView event = eventLabel(e, dayMode ? 15 : 12);
                event.setMaxLines(1); event.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
                LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, dp(dayMode ? 44 : 48)); lp.setMargins(dp(2), dp(2), dp(2), dp(2));
                column.addView(event, lp);
            }
            if (allDay.size() > 2) {
                TextView more = label("+" + (allDay.size() - 2) + "项", 12);
                more.setTextColor(color(R.color.color_calendar_today));
                more.setOnClickListener(v -> showAllDay(allDay));
                column.addView(more, new LayoutParams(LayoutParams.MATCH_PARENT, dp(48)));
            }
            allDayColumns.addView(column, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1));
        }
        timeline.requestRebuild();
        if (initialScroll) {
            initialScroll = false;
            scroll.post(() -> scroll.scrollTo(0, 0));
        }
    }

    private void renderWeekTasks(Calendar first) {
        weekTasks.removeAllViews();
        Calendar next = (Calendar) first.clone(); next.add(Calendar.DATE, 7);
        List<Entry> visible = new ArrayList<>();
        for (Entry e : entries) {
            if (SchedulePeriod.intersects(e.start, e.end, e.allDay, first.getTimeInMillis(), next.getTimeInMillis())) visible.add(e);
        }
        visible.sort(Comparator.comparingLong(e -> e.start));
        Calendar last = (Calendar) next.clone(); last.add(Calendar.DATE, -1);
        SimpleDateFormat shortDate = new SimpleDateFormat("M月d日", Locale.CHINA);
        TextView heading = label("本周所有任务 · " + visible.size() + " 项", 18);
        heading.setGravity(Gravity.START | Gravity.CENTER_VERTICAL); heading.setTypeface(null, Typeface.BOLD);
        weekTasks.addView(heading, new LayoutParams(LayoutParams.MATCH_PARENT, dp(44)));
        TextView range = label(shortDate.format(first.getTime()) + " — " + shortDate.format(last.getTime()), 12);
        range.setGravity(Gravity.START); range.setTextColor(color(R.color.color_text_secondary));
        weekTasks.addView(range, new LayoutParams(LayoutParams.MATCH_PARENT, dp(28)));
        if (visible.isEmpty()) {
            TextView empty = label("本周暂无任务", 14);
            empty.setTextColor(color(R.color.color_text_secondary));
            weekTasks.addView(empty, new LayoutParams(LayoutParams.MATCH_PARENT, dp(96)));
        }
        for (Entry e : visible) {
            LinearLayout card = new LinearLayout(getContext()); card.setOrientation(VERTICAL);
            card.setPadding(dp(14), dp(12), dp(14), dp(12)); card.setBackground(rounded(backgrounds[e.color], 12));
            TextView title = label(e.name, 12); title.setGravity(Gravity.START); title.setTextColor(foregrounds[e.color % foregrounds.length]);
            title.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.schedule_week_task_text_size));
            title.setTypeface(null, Typeface.BOLD);
            card.addView(title, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            SimpleDateFormat format = new SimpleDateFormat(e.allDay ? "M/d" : "M/d HH:mm", Locale.ROOT);
            TextView interval = label((e.allDay ? "全天 · " : "") + format.format(new Date(e.start)) + " — " + format.format(new Date(e.end)), 12);
            interval.setTextColor(foregrounds[e.color % foregrounds.length]); interval.setGravity(Gravity.START);
            LayoutParams textParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT); textParams.topMargin = dp(8);
            card.addView(interval, textParams); card.setMinimumHeight(dp(76)); card.setFocusable(true);
            card.setContentDescription(e.name + "，" + interval.getText() + "，点击查看详情");
            card.setOnClickListener(v -> open(e));
            LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT); params.bottomMargin = dp(10);
            weekTasks.addView(card, params);
        }
    }

    private void showAllDay(List<Entry> allDay) {
        String[] titles = new String[allDay.size()]; for (int i = 0; i < titles.length; i++) titles[i] = allDay.get(i).name;
        new AlertDialog.Builder(getContext()).setTitle("全天任务").setItems(titles, (d, index) -> open(allDay.get(index))).setNegativeButton("关闭", null).show();
    }
    private TextView eventLabel(Entry e, float size) {
        TextView v = label(e.name, size);
        v.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(dayMode ? R.dimen.schedule_day_task_text_size : R.dimen.schedule_week_task_text_size));
        v.setGravity(Gravity.TOP | Gravity.START);
        v.setPadding(dp(5), dp(4), dp(4), dp(3)); v.setMaxLines(dayMode ? 3 : 2);
        v.setEllipsize(android.text.TextUtils.TruncateAt.END); v.setBackground(rounded(backgrounds[e.color], 6));
        v.setTextColor(foregrounds[e.color % foregrounds.length]); v.setContentDescription(e.name + "，" + (e.allDay ? "全天任务" : "定时任务") + "，点击查看详情");
        v.setOnClickListener(view -> open(e)); v.setFocusable(true); return v;
    }
    private void open(Entry e) { if (taskListener != null) taskListener.accept(e.raw); }
    private void shift(int days) { selected.add(Calendar.DATE, days); initialScroll = true; render(); notifyDate(); }
    private void notifyDate() { message.setOnClickListener(null); if (dateListener != null) dateListener.accept((Calendar) selected.clone()); }

    private class Timeline extends FrameLayout {
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Runnable rebuildTask = this::rebuild;
        Timeline(Context c) { super(c); setWillNotDraw(false); }
        @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            // ScrollView supplies an unspecified height; FrameLayout would otherwise
            // shrink to the last task (or zero when empty), clipping later hours.
            int fullDayHeight = hourHeight() * 24 + dp(64);
            super.onMeasure(widthMeasureSpec, View.MeasureSpec.makeMeasureSpec(fullDayHeight, View.MeasureSpec.EXACTLY));
        }
        @Override protected void onSizeChanged(int w, int h, int oldW, int oldH) { super.onSizeChanged(w,h,oldW,oldH); if (w != oldW) requestRebuild(); }
        void requestRebuild() {
            removeCallbacks(rebuildTask);
            // Add children after the current layout pass, using the latest date and task data.
            post(rebuildTask);
        }
        @Override protected void onDetachedFromWindow() { removeCallbacks(rebuildTask); super.onDetachedFromWindow(); }
        void rebuild() {
            removeAllViews();
            if (getWidth() == 0) { invalidate(); return; }
            Calendar first = dayMode ? (Calendar) selected.clone() : weekStart();
            int count = dayMode ? 1 : 7;
            float gutter = dp(dayMode ? 52 : 44), width = (getWidth() - gutter) / count;
            for (int col = 0; col < count; col++) {
                Calendar date = (Calendar) first.clone(); date.add(Calendar.DATE, col);
                long start = date.getTimeInMillis(); Calendar next = (Calendar) date.clone(); next.add(Calendar.DATE, 1);
                long end = next.getTimeInMillis();
                List<ScheduleLayout.Slot> slots = new ArrayList<>();
                for (int i = 0; i < entries.size(); i++) {
                    Entry e = entries.get(i);
                    float[] minutes = e.allDay ? null : SchedulePeriod.dayMinutes(e.start, e.end, start, end);
                    if (minutes != null) slots.add(new ScheduleLayout.Slot(i, minutes[0], minutes[1]));
                }
                ScheduleLayout.arrange(slots);
                for (ScheduleLayout.Slot slot : slots) {
                    Entry e = entries.get(slot.taskIndex);
                    TextView v = eventLabel(e, dayMode ? 15 : 12);
                    SimpleDateFormat clock = new SimpleDateFormat("HH:mm", Locale.ROOT);
                    String range = (e.start < start ? "前日 " : "") + clock.format(new Date(e.start))
                            + "—" + (e.end >= end ? "次日 " : "") + clock.format(new Date(e.end));
                    int cardWidth = Math.max(1, (int) (width / slot.columns) - dp(3));
                    int cardHeight = Math.max(dp(28), Math.round((slot.end - slot.start) * hourHeight() / 60f) - dp(2));
                    int lines = Math.max(1, (int) ((cardHeight - dp(7)) / (v.getTextSize() * 1.25f)));
                    v.setMaxLines(Math.min(3, lines));
                    v.setText(e.name + (lines >= 2 ? "\n" + range : ""));
                    v.setContentDescription(e.name + "，" + new SimpleDateFormat("M月d日 HH:mm", Locale.CHINA).format(new Date(e.start))
                            + " 至 " + new SimpleDateFormat("M月d日 HH:mm", Locale.CHINA).format(new Date(e.end)) + "，点击查看详情");
                    FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(cardWidth, cardHeight);
                    lp.leftMargin = Math.round(gutter + col * width + slot.column * width / slot.columns) + dp(2);
                    lp.topMargin = dp(12) + Math.round(slot.start * hourHeight() / 60f);
                    addView(v, lp);
                }
            }
            invalidate();
        }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float gutter = dp(dayMode ? 52 : 44); int count = dayMode ? 1 : 7;
            float width = (getWidth() - gutter) / count;
            paint.setStrokeWidth(dp(0.5f)); paint.setColor(color(R.color.color_calendar_grid));
            for (int i = 0; i <= 24; i++) { float y = dp(12) + i * hourHeight(); canvas.drawLine(gutter, y, getWidth(), y, paint); }
            if (!dayMode) for (int i = 0; i <= count; i++) canvas.drawLine(gutter + i * width, 0, gutter + i * width, getHeight(), paint);
            paint.setTextSize(sp(11)); paint.setTextAlign(Paint.Align.RIGHT); paint.setColor(color(R.color.color_text_secondary));
            for (int i = 0; i <= 24; i++) canvas.drawText(String.format(Locale.ROOT, "%02d:00", i), gutter - dp(7), dp(16) + i * hourHeight(), paint);
        }
        @Override public void onDrawForeground(Canvas canvas) {
            super.onDrawForeground(canvas);
            Calendar now = Calendar.getInstance(), first = dayMode ? selected : weekStart();
            int col = -1, count = dayMode ? 1 : 7;
            for (int i = 0; i < count; i++) { Calendar d = (Calendar) first.clone(); d.add(Calendar.DATE, i); if (sameDay(d, now)) col = i; }
            if (col < 0) return;
            float gutter = dp(dayMode ? 52 : 44), width = (getWidth() - gutter) / count;
            float y = dp(12) + (now.get(Calendar.HOUR_OF_DAY) + now.get(Calendar.MINUTE) / 60f) * hourHeight();
            paint.setColor(color(R.color.color_calendar_today)); paint.setStrokeWidth(dp(1.5f));
            float left = gutter + col * width;
            canvas.drawLine(left, y, left + width, y, paint); canvas.drawCircle(left, y, dp(2.5f), paint);
            if (dayMode) {
                canvas.drawRoundRect(0, y - dp(10), gutter - dp(3), y + dp(10), dp(10), dp(10), paint);
                paint.setColor(color(R.color.color_on_accent)); paint.setTextAlign(Paint.Align.CENTER); paint.setTextSize(sp(10));
                canvas.drawText(new SimpleDateFormat("HH:mm", Locale.ROOT).format(now.getTime()), gutter / 2 - dp(1), y + dp(3.5f), paint);
            }
        }
    }

    private Entry parse(Map<String, Object> task) {
        Object startValue = get(task, "start_time"), endValue = get(task, "end_time");
        if (startValue == null) startValue = endValue;
        if (startValue == null) return null;
        try {
            String startText = DateCompat.toLocalDateTime(startValue), endText = DateCompat.toLocalDateTime(endValue == null ? startValue : endValue);
            long start = parseTime(startText), end = parseTime(endText);
            boolean allDay = (startText.length() == 10 || startText.endsWith("00:00")) && (endText.length() == 10 || endText.endsWith("00:00"));
            if (end < start) end = start;
            if (!allDay && end == start) end += 60 * MINUTE;
            Entry entry = new Entry(); entry.raw = task; entry.start = start; entry.end = end; entry.allDay = allDay;
            entry.name = Objects.toString(get(task, "name"), "未命名任务");
            entry.color = (entry.name.hashCode() & Integer.MAX_VALUE) % backgrounds.length;
            return entry;
        } catch (Exception e) { return null; }
    }
    private long parseTime(String text) throws java.text.ParseException {
        if (text.length() == 10) text += " 00:00";
        dateTime.setLenient(false); return Objects.requireNonNull(dateTime.parse(text)).getTime();
    }
    private boolean covers(Entry e, Calendar day) { return e.start <= day.getTimeInMillis() && e.end >= day.getTimeInMillis(); }
    private Object get(Map<String, Object> task, String key) { Object value = task.get(key); return value == null ? task.get(key.toUpperCase(Locale.ROOT)) : value; }
    private Calendar weekStart() { Calendar c = midnight(selected); c.add(Calendar.DATE, -((c.get(Calendar.DAY_OF_WEEK) + 5) % 7)); return c; }
    private static Calendar midnight(Calendar value) { Calendar c = (Calendar) value.clone(); c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0); return c; }
    private boolean sameDay(Calendar a, Calendar b) { return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR); }
    private int[] colors(int id) { String[] strings = getResources().getStringArray(id); int[] result = new int[strings.length]; for (int i = 0; i < strings.length; i++) result[i] = android.graphics.Color.parseColor(strings[i]); return result; }
    private int color(int id) { return ContextCompat.getColor(getContext(), id); }
    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private float sp(float value) { return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, getResources().getDisplayMetrics()); }
    private int hourHeight() { return getResources().getDimensionPixelSize(R.dimen.schedule_hour_height); }
    private GradientDrawable rounded(int color, int radius) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private TextView label(String text, float size) { TextView v = new TextView(getContext()); v.setText(text); v.setTextSize(size); v.setTextColor(color(R.color.color_text_primary)); v.setGravity(Gravity.CENTER); v.setIncludeFontPadding(false); return v; }
    private TextView action(String text, String description) { TextView v = label(text, 26); v.setContentDescription(description); v.setFocusable(true); return v; }
    private static class Entry { Map<String, Object> raw; long start, end; boolean allDay; String name; int color; }
    private Map<String, Object> sample(String title, int hour, boolean allDay) {
        Map<String, Object> task = new HashMap<>(); String day = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(selected.getTime());
        task.put("name", title); task.put("start_time", day + String.format(Locale.ROOT, " %02d:00", hour));
        task.put("end_time", day + String.format(Locale.ROOT, " %02d:00", allDay ? 0 : hour + 2)); return task;
    }
    @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); if (dayMode && !isInEditMode()) post(clockTick); }
    @Override protected void onDetachedFromWindow() { removeCallbacks(clockTick); super.onDetachedFromWindow(); }
}

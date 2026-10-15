package com.example.project_pandora.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalendarView extends View {

    private Paint paint;
    private Paint textPaint;
    private Paint headerPaint;
    private Paint todayPaint;
    private Paint weekHeaderPaint;
    private Paint weekHeaderBgPaint;

    private Calendar currentDate;
    private int selectedDay = -1;
    private Map<Integer, List<TaskEvent>> dayEvents;
    private List<TaskSpan> taskSpans;

    private OnDateSelectedListener listener;

    private static final int[] PASTEL_COLORS = {
        Color.parseColor("#FFF9C4"), // 浅黄
        Color.parseColor("#FFCDD2"), // 浅粉
        Color.parseColor("#E1BEE7"), // 浅紫
        Color.parseColor("#BBDEFB"), // 浅蓝
        Color.parseColor("#C8E6C9"), // 浅绿
        Color.parseColor("#FFE0B2"), // 浅橙
        Color.parseColor("#B2DFDB"), // 浅青
        Color.parseColor("#F8BBD0"), // 粉红
    };

    private static final int[] PASTEL_TEXT_COLORS = {
        Color.parseColor("#795548"),
        Color.parseColor("#C62828"),
        Color.parseColor("#6A1B9A"),
        Color.parseColor("#1565C0"),
        Color.parseColor("#2E7D32"),
        Color.parseColor("#E65100"),
        Color.parseColor("#00695C"),
        Color.parseColor("#AD1457"),
    };

    public static class TaskEvent {
        public String name;
        public int colorIndex;

        public TaskEvent(String name, int colorIndex) {
            this.name = name;
            this.colorIndex = colorIndex;
        }
    }

    public interface OnDateSelectedListener {
        void onDateSelected(int year, int month, int day);
    }

    public CalendarView(Context context) {
        super(context);
        init();
    }

    public CalendarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CalendarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        currentDate = Calendar.getInstance();
        dayEvents = new HashMap<>();

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.parseColor("#E8E8E8"));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1f);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#333333"));
        textPaint.setTextSize(36f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        headerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        headerPaint.setColor(Color.parseColor("#333333"));
        headerPaint.setTextSize(40f);
        headerPaint.setTextAlign(Paint.Align.CENTER);
        headerPaint.setFakeBoldText(true);

        todayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        todayPaint.setColor(Color.parseColor("#FF5722"));
        todayPaint.setStyle(Paint.Style.FILL);

        weekHeaderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        weekHeaderPaint.setColor(Color.parseColor("#666666"));
        weekHeaderPaint.setTextSize(34f);
        weekHeaderPaint.setTextAlign(Paint.Align.CENTER);

        weekHeaderBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        weekHeaderBgPaint.setColor(Color.parseColor("#F5F5F5"));
        weekHeaderBgPaint.setStyle(Paint.Style.FILL);
    }

    public void setOnDateSelectedListener(OnDateSelectedListener listener) {
        this.listener = listener;
    }

    public void setDayEvents(Map<Integer, List<TaskEvent>> events) {
        this.dayEvents = events;
        invalidate();
    }

    public void setTaskSpans(List<TaskSpan> spans) {
        this.taskSpans = spans;
        invalidate();
    }

    public void nextMonth() {
        currentDate.add(Calendar.MONTH, 1);
        selectedDay = -1;
        invalidate();
    }

    public void previousMonth() {
        currentDate.add(Calendar.MONTH, -1);
        selectedDay = -1;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int cellWidth = width / 7;
        int headerHeight = 60;
        int weekHeaderHeight = 56;
        int cellHeight = (height - headerHeight - weekHeaderHeight) / 6;

        String[] weekDays = {"日", "一", "二", "三", "四", "五", "六"};
        int year = currentDate.get(Calendar.YEAR);
        int month = currentDate.get(Calendar.MONTH) + 1;

        canvas.drawColor(Color.parseColor("#FFFFFF"));

        canvas.drawRect(0, headerHeight, width, headerHeight + weekHeaderHeight, weekHeaderBgPaint);
        for (int i = 0; i < 7; i++) {
            float x = cellWidth * i + cellWidth / 2f;
            canvas.drawText(weekDays[i], x, headerHeight + 38f, weekHeaderPaint);
        }

        Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(Color.parseColor("#F0F0F0"));
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f);

        int gridTop = headerHeight + weekHeaderHeight;
        for (int i = 0; i <= 6; i++) {
            canvas.drawLine(0, gridTop + cellHeight * i, width, gridTop + cellHeight * i, gridPaint);
        }
        for (int i = 0; i <= 7; i++) {
            canvas.drawLine(cellWidth * i, gridTop, cellWidth * i, gridTop + cellHeight * 6, gridPaint);
        }

        Calendar cal = (Calendar) currentDate.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        Calendar today = Calendar.getInstance();
        int todayYear = today.get(Calendar.YEAR);
        int todayMonth = today.get(Calendar.MONTH) + 1;
        int todayDay = today.get(Calendar.DAY_OF_MONTH);

        Paint dayNumPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dayNumPaint.setTextSize(34f);
        dayNumPaint.setTextAlign(Paint.Align.LEFT);

        Paint eventTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        eventTextPaint.setTextSize(28f);
        eventTextPaint.setTextAlign(Paint.Align.LEFT);

        Paint eventBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        eventBgPaint.setStyle(Paint.Style.FILL);

        Paint selectedBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectedBgPaint.setColor(Color.parseColor("#E3F2FD"));
        selectedBgPaint.setStyle(Paint.Style.FILL);

        for (int day = 1; day <= daysInMonth; day++) {
            int pos = firstDayOfWeek + day - 1;
            int row = pos / 7;
            int col = pos % 7;

            float cellLeft = cellWidth * col;
            float cellTop = gridTop + cellHeight * row;
            float cellRight = cellLeft + cellWidth;
            float cellBottom = cellTop + cellHeight;

            boolean isToday = (year == todayYear && month == todayMonth && day == todayDay);
            boolean isSelected = (day == selectedDay);

            if (isSelected) {
                canvas.drawRect(cellLeft + 1, cellTop + 1, cellRight - 1, cellBottom - 1, selectedBgPaint);
            }

            if (isToday) {
                dayNumPaint.setColor(Color.WHITE);
                float circleX = cellLeft + 28f;
                float circleY = cellTop + 28f;
                canvas.drawCircle(circleX, circleY, 22f, todayPaint);
            } else {
                dayNumPaint.setColor(Color.parseColor("#333333"));
            }

            canvas.drawText(String.valueOf(day), cellLeft + 14f, cellTop + 40f, dayNumPaint);

            if (taskSpans == null && dayEvents.containsKey(day)) {
                List<TaskEvent> events = dayEvents.get(day);
                float eventTop = cellTop + 50f;
                float eventLeft = cellLeft + 6f;
                float eventRight = cellRight - 6f;
                float eventHeight = 36f;
                float eventMargin = 4f;
                float cornerRadius = 8f;

                int maxEvents = (int) ((cellBottom - eventTop - 4f) / (eventHeight + eventMargin));
                int displayCount = Math.min(events.size(), maxEvents);

                for (int i = 0; i < displayCount; i++) {
                    TaskEvent event = events.get(i);
                    int colorIdx = event.colorIndex % PASTEL_COLORS.length;
                    eventBgPaint.setColor(PASTEL_COLORS[colorIdx]);
                    eventTextPaint.setColor(PASTEL_TEXT_COLORS[colorIdx]);

                    RectF rect = new RectF(eventLeft, eventTop + i * (eventHeight + eventMargin),
                            eventRight, eventTop + i * (eventHeight + eventMargin) + eventHeight);
                    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, eventBgPaint);

                    float textMaxWidth = eventRight - eventLeft - 12f;
                    String displayName = event.name;
                    if (eventTextPaint.measureText(displayName) > textMaxWidth) {
                        int chars = eventTextPaint.breakText(displayName, 0, displayName.length(),
                                true, textMaxWidth - eventTextPaint.measureText("…"), null);
                        displayName = displayName.substring(0, chars) + "…";
                    }
                    canvas.drawText(displayName, eventLeft + 6f,
                            rect.top + 25f, eventTextPaint);
                }

                if (events.size() > maxEvents) {
                    eventTextPaint.setColor(Color.parseColor("#999999"));
                    eventTextPaint.setTextSize(24f);
                    canvas.drawText("+" + (events.size() - maxEvents),
                            eventLeft + 6f, cellBottom - 10f, eventTextPaint);
                    eventTextPaint.setTextSize(28f);
                }
            }
        }

        if (taskSpans != null && !taskSpans.isEmpty()) {
            Paint spanBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            spanBgPaint.setStyle(Paint.Style.FILL);

            Paint spanTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            spanTextPaint.setTextSize(22f);
            spanTextPaint.setTextAlign(Paint.Align.LEFT);

            float dayNumHeight = 48f;
            float slotPadding = 2f;
            float cornerRadius = 6f;

            Calendar monthStart = (Calendar) currentDate.clone();
            monthStart.set(Calendar.DAY_OF_MONTH, 1);
            Calendar monthEnd = (Calendar) currentDate.clone();
            monthEnd.set(Calendar.DAY_OF_MONTH, daysInMonth);

            for (TaskSpan span : taskSpans) {
                if (span.getStartDate() == null || span.getEndDate() == null) continue;

                Calendar spanStart = (Calendar) span.getStartDate().clone();
                Calendar spanEnd = (Calendar) span.getEndDate().clone();

                if (spanEnd.before(spanStart)) {
                    Log.w("CalendarView", "TaskSpan date inverted: " + span.getTaskName());
                    spanEnd = (Calendar) spanStart.clone();
                }

                if (spanEnd.before(monthStart) || spanStart.after(monthEnd)) continue;

                if (spanStart.before(monthStart)) spanStart = (Calendar) monthStart.clone();
                if (spanEnd.after(monthEnd)) spanEnd = (Calendar) monthEnd.clone();

                int sDay = spanStart.get(Calendar.DAY_OF_MONTH);
                int eDay = spanEnd.get(Calendar.DAY_OF_MONTH);
                if (eDay < sDay) continue;

                int startPos = firstDayOfWeek + sDay - 1;
                int endPos = firstDayOfWeek + eDay - 1;
                int startRow = startPos / 7;
                int startCol = startPos % 7;
                int endRow = endPos / 7;
                int endCol = endPos % 7;

                int totalLanes = Math.max(span.getTotalLanes(), 1);
                int laneIdx = span.getLaneIndex();

                int colorIdx = span.getColorIndex() % PASTEL_COLORS.length;
                spanBgPaint.setColor(PASTEL_COLORS[colorIdx]);
                spanTextPaint.setColor(PASTEL_TEXT_COLORS[colorIdx]);

                for (int row = startRow; row <= endRow; row++) {
                    float rowTop = gridTop + cellHeight * row;
                    float rowBottom = rowTop + cellHeight;

                    float slotTop = rowTop + dayNumHeight;
                    float slotBottom = rowBottom - slotPadding;
                    float slotHeight = slotBottom - slotTop;

                    float lineHeight = slotHeight / totalLanes;
                    float lineTop = slotTop + laneIdx * lineHeight + slotPadding;
                    float lineBottom = lineTop + lineHeight - slotPadding * 2;
                    if (lineBottom <= lineTop) lineBottom = lineTop + 1f;

                    float segLeft, segRight;
                    if (row == startRow) {
                        segLeft = cellWidth * startCol + slotPadding;
                    } else {
                        segLeft = slotPadding;
                    }
                    if (row == endRow) {
                        segRight = cellWidth * (endCol + 1) - slotPadding;
                    } else {
                        segRight = cellWidth * 7 - slotPadding;
                    }

                    RectF segRect = new RectF(segLeft, lineTop, segRight, lineBottom);
                    canvas.drawRoundRect(segRect, cornerRadius, cornerRadius, spanBgPaint);

                    if (row == startRow) {
                        float textMaxWidth = segRight - segLeft - 8f;
                        String displayName = span.getTaskName();
                        if (displayName != null && spanTextPaint.measureText(displayName) > textMaxWidth) {
                            int chars = spanTextPaint.breakText(displayName, 0, displayName.length(),
                                    true, textMaxWidth - spanTextPaint.measureText("…"), null);
                            if (chars > 0) {
                                displayName = displayName.substring(0, chars) + "…";
                            } else {
                                displayName = "";
                            }
                        }
                        if (displayName != null && !displayName.isEmpty()) {
                            canvas.drawText(displayName, segLeft + 4f,
                                    lineBottom - 4f, spanTextPaint);
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            float x = event.getX();
            float y = event.getY();

            int width = getWidth();
            int height = getHeight();
            int cellWidth = width / 7;
            int headerHeight = 60;
            int weekHeaderHeight = 56;
            int cellHeight = (height - headerHeight - weekHeaderHeight) / 6;

            int gridTop = headerHeight + weekHeaderHeight;
            if (y > gridTop) {
                int col = (int) (x / cellWidth);
                int row = (int) ((y - gridTop) / cellHeight);

                Calendar cal = (Calendar) currentDate.clone();
                cal.set(Calendar.DAY_OF_MONTH, 1);
                int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;
                int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

                int pos = row * 7 + col;
                int day = pos - firstDayOfWeek + 1;

                if (day >= 1 && day <= daysInMonth) {
                    selectedDay = day;
                    invalidate();
                    if (listener != null) {
                        listener.onDateSelected(
                                currentDate.get(Calendar.YEAR),
                                currentDate.get(Calendar.MONTH) + 1,
                                day
                        );
                    }
                }
            }
        }
        return true;
    }
}

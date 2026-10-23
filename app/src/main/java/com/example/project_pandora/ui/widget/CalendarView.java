package com.example.project_pandora.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalendarView extends View {

    /** R36: 原 60px 纯空白标题区归零回收（空间让给缩放网格） */
    public static final int HEADER_HEIGHT = 0;
    public static final int WEEK_HEADER_HEIGHT = 56;
    public static final int GRID_ROWS = 6;

    /** R36 REQ-36-C: 月视图上下缩放（zoomFactor=行高利用率；上限 1.0=内容不超屏） */
    public static final float MIN_ZOOM = 0.5f;
    /** R37 REQ-37-C1: 上限=满屏单行（cellHeight=avail ⇔ zoom=GRID_ROWS=6.0） */
    public static final float MAX_ZOOM = 6.0f;
    public static final float DEFAULT_ZOOM = 0.7f;
    private float zoomFactor = DEFAULT_ZOOM;
    private android.view.ScaleGestureDetector scaleGestureDetector;

    /** R36 REQ-36-C5: 切 tab 离开月视图后返回时重置（翻月保持由本类不重置实现） */
    public void resetZoom() {
        zoomFactor = DEFAULT_ZOOM;
        scrollOffsetY = 0;
        invalidate();
    }

    /** R37 REQ-37-C4: 网格内容超屏后的垂直滚动偏移（px；[0, maxScroll] clamp） */
    private int scrollOffsetY = 0;
    private float lastScrollY = -1f;

    private int maxScroll() {
        int avail = getHeight() - HEADER_HEIGHT - WEEK_HEADER_HEIGHT;
        int contentH = computeCellHeight() * GRID_ROWS;
        return Math.max(0, contentH - avail);
    }

    /**
     * R36 REQ-36-C: 唯一几何源——onDraw 与点击反算共用（结构性同步，根治 Round31 点击错位教训）。
     * cellHeight = 可用高度 × zoomFactor / 6 行；缩放只改行高不改行数。
     */
    private int computeCellHeight() {
        int avail = getHeight() - HEADER_HEIGHT - WEEK_HEADER_HEIGHT;
        return Math.max(1, (int) (avail * zoomFactor / GRID_ROWS));
    }

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

    private OnVerticalSwipeListener verticalSwipeListener;
    private boolean verticalSwipeTriggered;
    private float touchDownX;
    private float touchDownY;
    private int touchSlop;

    public interface OnVerticalSwipeListener {
        void onSwipeUp();
        void onSwipeDown();
    }

    public void setOnVerticalSwipeListener(OnVerticalSwipeListener listener) {
        this.verticalSwipeListener = listener;
    }

    // R37 REQ-37-E: PASTEL 色板迁移 values/values-night integer-array 双资源（View 重建自动切套）
    private int[] pastelColors;
    private int[] pastelTextColors;

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
        touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();

        // R37 REQ-37-E: 色板资源化读取（昼夜由资源系统自动选套；本 View 重建即切套）
        String[] spanHex = getResources().getStringArray(com.example.project_pandora.R.array.calendar_span_colors);
        String[] spanTextHex = getResources().getStringArray(com.example.project_pandora.R.array.calendar_span_text_colors);
        pastelColors = new int[spanHex.length];
        for (int i = 0; i < spanHex.length; i++) pastelColors[i] = Color.parseColor(spanHex[i]);
        pastelTextColors = new int[spanTextHex.length];
        for (int i = 0; i < spanTextHex.length; i++) pastelTextColors[i] = Color.parseColor(spanTextHex[i]);
        if (pastelColors == null || pastelColors.length == 0) {
            pastelColors = new int[]{0xFF9C4, 0xFFCDD2, 0xE1BEE7, 0xBBDEFB, 0xC8E6C9, 0xFFE0B2, 0xB2DFDB, 0xF8BBD0};
        }
        if (pastelTextColors == null || pastelTextColors.length == 0) {
            pastelTextColors = new int[]{0x795548, 0xC62828, 0x6A1B9A, 0x1565C0, 0x2E7D32, 0xE65100, 0x00695C, 0xAD1457};
        }

        // REQ-31-05④: 自绘色资源化（昼夜双套，View 重建时自动切换）
        android.content.res.Resources res = getResources();

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(res.getColor(com.example.project_pandora.R.color.color_calendar_grid));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1f);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(res.getColor(com.example.project_pandora.R.color.color_calendar_text));
        textPaint.setTextSize(36f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        headerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        headerPaint.setColor(res.getColor(com.example.project_pandora.R.color.color_calendar_text));
        headerPaint.setTextSize(40f);
        headerPaint.setTextAlign(Paint.Align.CENTER);
        headerPaint.setFakeBoldText(true);

        todayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        todayPaint.setColor(res.getColor(com.example.project_pandora.R.color.color_calendar_today));
        todayPaint.setStyle(Paint.Style.FILL);

        weekHeaderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        weekHeaderPaint.setColor(res.getColor(com.example.project_pandora.R.color.color_text_secondary));
        weekHeaderPaint.setTextSize(34f);
        weekHeaderPaint.setTextAlign(Paint.Align.CENTER);

        weekHeaderBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        weekHeaderBgPaint.setColor(res.getColor(com.example.project_pandora.R.color.color_calendar_header_bg));
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
        scrollOffsetY = 0; // R37 REQ-37-C6: 翻月保持倍率，滚动复位顶部
        invalidate();
    }

    public void previousMonth() {
        currentDate.add(Calendar.MONTH, -1);
        selectedDay = -1;
        scrollOffsetY = 0; // R37 REQ-37-C6: 翻月保持倍率，滚动复位顶部
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int cellWidth = width / 7;
        int headerHeight = HEADER_HEIGHT;
        int weekHeaderHeight = WEEK_HEADER_HEIGHT;
        int cellHeight = computeCellHeight(); // R36: 唯一几何源（缩放可变）


        String[] weekDays = {"日", "一", "二", "三", "四", "五", "六"};
        int year = currentDate.get(Calendar.YEAR);
        int month = currentDate.get(Calendar.MONTH) + 1;

        canvas.drawColor(getResources().getColor(com.example.project_pandora.R.color.color_calendar_bg));

        canvas.drawRect(0, headerHeight, width, headerHeight + weekHeaderHeight, weekHeaderBgPaint);
        for (int i = 0; i < 7; i++) {
            float x = cellWidth * i + cellWidth / 2f;
            canvas.drawText(weekDays[i], x, headerHeight + 38f, weekHeaderPaint);
        }

        Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(getResources().getColor(com.example.project_pandora.R.color.color_calendar_grid));
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f);

        int gridTop = headerHeight + weekHeaderHeight;
        // R37 REQ-37-C4: 网格区滚动——save+clipRect（防侵入星期头）+translate(-scrollOffsetY)
        // 网格线/日期/spans 绘制坐标系自动平移；星期头在 save 外不受影响
        canvas.save();
        canvas.clipRect(0, gridTop, width, gridTop + (getHeight() - gridTop));
        canvas.translate(0, -scrollOffsetY);
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
        selectedBgPaint.setColor(getResources().getColor(com.example.project_pandora.R.color.color_calendar_cell_bg));
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
                dayNumPaint.setColor(getResources().getColor(com.example.project_pandora.R.color.color_calendar_text));
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
                    int colorIdx = event.colorIndex % pastelColors.length;
                    eventBgPaint.setColor(pastelColors[colorIdx]);
                    eventTextPaint.setColor(pastelTextColors[colorIdx]);

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
                    // Round27 REQ-27-07: 日视图事件条文字顶部基线起排（替换经验值 rect.top + 25f），保持单行截断
                    Paint.FontMetrics fmEvent = eventTextPaint.getFontMetrics();
                    canvas.drawText(displayName, eventLeft + 6f,
                            rect.top + 3f + (-fmEvent.ascent), eventTextPaint);
                }

                if (events.size() > maxEvents) {
                    eventTextPaint.setColor(getResources().getColor(com.example.project_pandora.R.color.color_text_secondary));
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

                int colorIdx = span.getColorIndex() % pastelColors.length;
                // R45 REQ-B: 移除 DISPATCHED 蓝色描边分支（用户 2.7），双源统一 PASTEL 彩色圆角实底；
                // TaskSpan.source 字段仅存数据语义（上游双源聚合继续写入，渲染层不再区分）
                spanBgPaint.setStyle(Paint.Style.FILL);
                spanBgPaint.setStrokeWidth(0f);
                spanBgPaint.setColor(pastelColors[colorIdx]);
                spanTextPaint.setColor(pastelTextColors[colorIdx]);

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
                        if (displayName != null && !displayName.isEmpty()) {
                            // Round27 REQ-27-07: 月视图任务条文字左上对齐 + 自动换行
                            Paint.FontMetrics fm = spanTextPaint.getFontMetrics();
                            float textPadTop = 3f;
                            float textRowHeight = fm.descent - fm.ascent;
                            float firstBaselineY = lineTop + textPadTop + (-fm.ascent);
                            int maxRows = (int) Math.floor((lineBottom - lineTop - textPadTop - 2f) / textRowHeight);
                            if (maxRows < 1) maxRows = 1;
                            if (maxRows > 3) maxRows = 3;
                            String remaining = displayName;
                            int rows = 0;
                            while (rows < maxRows && remaining != null && !remaining.isEmpty()) {
                                boolean isLastRow = (rows == maxRows - 1);
                                int chars = spanTextPaint.breakText(remaining, true, textMaxWidth, null);
                                if (chars <= 0) {
                                    remaining = null;
                                    break;
                                }
                                String lineText = remaining.substring(0, chars);
                                String rest = remaining.substring(chars);
                                if (!rest.isEmpty() && isLastRow) {
                                    chars = spanTextPaint.breakText(remaining, true,
                                            textMaxWidth - spanTextPaint.measureText("…"), null);
                                    if (chars <= 0) {
                                        remaining = null;
                                        break;
                                    }
                                    lineText = remaining.substring(0, chars) + "…";
                                    rest = "";
                                }
                                canvas.drawText(lineText, segLeft + 4f,
                                        firstBaselineY + rows * textRowHeight, spanTextPaint);
                                remaining = rest;
                                rows++;
                            }
                            if (rows > 1) {
                                Log.d("CalendarView", "span multiline: " + span.getTaskId() + " rows=" + rows);
                            }
                        }
                    }
                }
            }
        }
        canvas.restore(); // R37: 网格滚动区结束
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // R36 REQ-36-C1: 双指捏合缩放（ScaleGestureDetector 优先；缩放期间抑制单指点击/滑动）
        if (scaleGestureDetector == null) {
            scaleGestureDetector = new android.view.ScaleGestureDetector(getContext(),
                    new android.view.ScaleGestureDetector.SimpleOnScaleGestureListener() {
                        @Override
                        public boolean onScale(android.view.ScaleGestureDetector detector) {
                            zoomFactor *= detector.getScaleFactor();
                            if (zoomFactor < MIN_ZOOM) zoomFactor = MIN_ZOOM;
                            if (zoomFactor > MAX_ZOOM) zoomFactor = MAX_ZOOM;
                            invalidate();
                            return true;
                        }
                    });
        }
        scaleGestureDetector.onTouchEvent(event);
        if (scaleGestureDetector.isInProgress() || event.getPointerCount() >= 2) {
            // 双指期间视为已消费：防止单指点击选日期与垂直滑动误触发
            verticalSwipeTriggered = true;
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_POINTER_UP
                || event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
            // 双指起落过渡帧不作为单指手势起点
            verticalSwipeTriggered = true;
            return true;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchDownX = event.getX();
                touchDownY = event.getY();
                lastScrollY = event.getY();
                verticalSwipeTriggered = false;
                break;
            case MotionEvent.ACTION_MOVE:
                // R37 REQ-37-C4: 网格超屏后单指垂直拖动=滚动（zoom≤1 时 maxScroll=0 天然无滚动）
                if (lastScrollY >= 0f) {
                    float dy = event.getY() - lastScrollY;
                    lastScrollY = event.getY();
                    if (maxScroll() > 0) {
                        scrollOffsetY = Math.max(0, Math.min(maxScroll(), scrollOffsetY - (int) dy));
                        invalidate();
                    }
                }
                // Round23 REQ-23-02: 垂直滑动检测（越过 touchSlop 且垂直分量优先时回调一次）
                if (!verticalSwipeTriggered) {
                    float dx = event.getX() - touchDownX;
                    float dy = event.getY() - touchDownY;
                    if (Math.abs(dy) > touchSlop && Math.abs(dy) > Math.abs(dx)) {
                        verticalSwipeTriggered = true;
                        if (verticalSwipeListener != null) {
                            if (dy < 0) {
                                verticalSwipeListener.onSwipeUp();
                            } else {
                                verticalSwipeListener.onSwipeDown();
                            }
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                // 未触发滑动时才执行原有点击选日期逻辑（滑动/点击互斥）
                if (!verticalSwipeTriggered) {
                    // R37: 点击坐标补偿滚动偏移（与 onDraw translate 同源）
                    handleTapToSelectDate(event.getX(), event.getY() + scrollOffsetY);
                }
                lastScrollY = -1f;
                break;
            case MotionEvent.ACTION_CANCEL:
                lastScrollY = -1f;
                break;
            default:
                break;
        }
        return true;
    }

    private void handleTapToSelectDate(float x, float y) {
        int width = getWidth();
        int height = getHeight();
        int cellWidth = width / 7;
        int headerHeight = HEADER_HEIGHT;
        int weekHeaderHeight = WEEK_HEADER_HEIGHT;
        int cellHeight = computeCellHeight(); // R36: 唯一几何源（与 onDraw 同步，防点击错位）

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
}

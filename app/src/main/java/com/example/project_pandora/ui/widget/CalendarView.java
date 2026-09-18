package com.example.project_pandora.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

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
    private Paint eventPaint;

    private Calendar currentDate;
    private int selectedDay = -1;
    private Map<Integer, List<String>> dayEvents;

    private OnDateSelectedListener listener;

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
        paint.setColor(Color.parseColor("#E0E0E0"));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1f);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#333333"));
        textPaint.setTextSize(32f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        headerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        headerPaint.setColor(Color.parseColor("#FF5722"));
        headerPaint.setTextSize(36f);
        headerPaint.setTextAlign(Paint.Align.CENTER);
        headerPaint.setFakeBoldText(true);

        todayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        todayPaint.setColor(Color.parseColor("#FF5722"));
        todayPaint.setStyle(Paint.Style.FILL);

        eventPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        eventPaint.setColor(Color.parseColor("#4CAF50"));
        eventPaint.setStyle(Paint.Style.FILL);
    }

    public void setOnDateSelectedListener(OnDateSelectedListener listener) {
        this.listener = listener;
    }

    public void setDayEvents(Map<Integer, List<String>> events) {
        this.dayEvents = events;
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
        int headerHeight = 80;
        int weekHeight = 60;
        int cellHeight = (height - headerHeight - weekHeight) / 6;

        String[] weekDays = {"日", "一", "二", "三", "四", "五", "六"};
        int year = currentDate.get(Calendar.YEAR);
        int month = currentDate.get(Calendar.MONTH) + 1;

        canvas.drawText(year + "年" + month + "月", width / 2f, 50f, headerPaint);

        for (int i = 0; i < 7; i++) {
            float x = cellWidth * i + cellWidth / 2f;
            canvas.drawText(weekDays[i], x, headerHeight + 40f, textPaint);
        }

        Calendar cal = (Calendar) currentDate.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        Calendar today = Calendar.getInstance();
        int todayYear = today.get(Calendar.YEAR);
        int todayMonth = today.get(Calendar.MONTH) + 1;
        int todayDay = today.get(Calendar.DAY_OF_MONTH);

        for (int day = 1; day <= daysInMonth; day++) {
            int pos = firstDayOfWeek + day - 1;
            int row = pos / 7;
            int col = pos % 7;

            float x = cellWidth * col + cellWidth / 2f;
            float y = headerHeight + weekHeight + cellHeight * row + cellHeight / 2f;

            if (year == todayYear && month == todayMonth && day == todayDay) {
                canvas.drawCircle(x, y, 24f, todayPaint);
                textPaint.setColor(Color.WHITE);
            } else if (day == selectedDay) {
                canvas.drawCircle(x, y, 24f, eventPaint);
                textPaint.setColor(Color.WHITE);
            } else {
                textPaint.setColor(Color.parseColor("#333333"));
            }

            canvas.drawText(String.valueOf(day), x, y + 12f, textPaint);

            if (dayEvents.containsKey(day)) {
                float dotY = y + 30f;
                for (int i = 0; i < Math.min(dayEvents.get(day).size(), 3); i++) {
                    canvas.drawCircle(x - 10f + i * 10f, dotY, 3f, eventPaint);
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
            int headerHeight = 80;
            int weekHeight = 60;
            int cellHeight = (height - headerHeight - weekHeight) / 6;

            if (y > headerHeight + weekHeight) {
                int col = (int) (x / cellWidth);
                int row = (int) ((y - headerHeight - weekHeight) / cellHeight);

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
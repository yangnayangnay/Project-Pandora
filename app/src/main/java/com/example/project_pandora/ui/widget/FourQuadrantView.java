package com.example.project_pandora.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FourQuadrantView extends View {

    private Paint paint;
    private Paint textPaint;
    private Paint bgPaint;

    private Map<String, List<String>> quadrantTasks;
    private OnQuadrantClickListener listener;

    public interface OnQuadrantClickListener {
        void onQuadrantClick(String quadrant);
    }

    public FourQuadrantView(Context context) {
        super(context);
        init();
    }

    public FourQuadrantView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FourQuadrantView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        quadrantTasks = new HashMap<>();
        quadrantTasks.put("importantUrgent", new ArrayList<>());
        quadrantTasks.put("importantNotUrgent", new ArrayList<>());
        quadrantTasks.put("notImportantUrgent", new ArrayList<>());
        quadrantTasks.put("notImportantNotUrgent", new ArrayList<>());

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(36f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setStyle(Paint.Style.STROKE);
        bgPaint.setStrokeWidth(2f);
        bgPaint.setColor(Color.parseColor("#CCCCCC"));
    }

    public void setQuadrantTasks(Map<String, List<String>> tasks) {
        this.quadrantTasks = tasks;
        invalidate();
    }

    public void setOnQuadrantClickListener(OnQuadrantClickListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int halfWidth = width / 2;
        int halfHeight = height / 2;

        paint.setColor(Color.parseColor("#F44336"));
        canvas.drawRect(0, 0, halfWidth, halfHeight, paint);
        canvas.drawText("重要且紧急", halfWidth / 2f, halfHeight / 2f, textPaint);
        canvas.drawText(String.valueOf(quadrantTasks.get("importantUrgent").size()) + "项", halfWidth / 2f, halfHeight / 2f + 40f, textPaint);

        paint.setColor(Color.parseColor("#FF9800"));
        canvas.drawRect(halfWidth, 0, width, halfHeight, paint);
        canvas.drawText("重要不紧急", halfWidth + halfWidth / 2f, halfHeight / 2f, textPaint);
        canvas.drawText(String.valueOf(quadrantTasks.get("importantNotUrgent").size()) + "项", halfWidth + halfWidth / 2f, halfHeight / 2f + 40f, textPaint);

        paint.setColor(Color.parseColor("#2196F3"));
        canvas.drawRect(0, halfHeight, halfWidth, height, paint);
        canvas.drawText("紧急不重要", halfWidth / 2f, halfHeight + halfHeight / 2f, textPaint);
        canvas.drawText(String.valueOf(quadrantTasks.get("notImportantUrgent").size()) + "项", halfWidth / 2f, halfHeight + halfHeight / 2f + 40f, textPaint);

        paint.setColor(Color.parseColor("#4CAF50"));
        canvas.drawRect(halfWidth, halfHeight, width, height, paint);
        canvas.drawText("不重要不紧急", halfWidth + halfWidth / 2f, halfHeight + halfHeight / 2f, textPaint);
        canvas.drawText(String.valueOf(quadrantTasks.get("notImportantNotUrgent").size()) + "项", halfWidth + halfWidth / 2f, halfHeight + halfHeight / 2f + 40f, textPaint);

        canvas.drawLine(halfWidth, 0, halfWidth, height, bgPaint);
        canvas.drawLine(0, halfHeight, width, halfHeight, bgPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP && listener != null) {
            float x = event.getX();
            float y = event.getY();
            int halfWidth = getWidth() / 2;
            int halfHeight = getHeight() / 2;

            String quadrant;
            if (x < halfWidth && y < halfHeight) quadrant = "importantUrgent";
            else if (x >= halfWidth && y < halfHeight) quadrant = "importantNotUrgent";
            else if (x < halfWidth && y >= halfHeight) quadrant = "notImportantUrgent";
            else quadrant = "notImportantNotUrgent";

            listener.onQuadrantClick(quadrant);
        }
        return true;
    }
}
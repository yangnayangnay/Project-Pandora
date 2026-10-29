package com.example.project_pandora.ui.ai;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.example.project_pandora.R;

public class HexagramView extends View {

    private boolean[] linePattern = new boolean[6];
    private int movingLine = -1;
    private final Paint yangPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint yinPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint movingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public HexagramView(Context context) {
        super(context);
        init();
    }

    public HexagramView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HexagramView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        yangPaint.setColor(ContextCompat.getColor(getContext(), R.color.color_text_primary));
        yangPaint.setStyle(Paint.Style.FILL);

        yinPaint.setColor(ContextCompat.getColor(getContext(), R.color.color_text_primary));
        yinPaint.setStyle(Paint.Style.FILL);

        movingPaint.setColor(ContextCompat.getColor(getContext(), R.color.color_accent));
        movingPaint.setStyle(Paint.Style.STROKE);
        movingPaint.setStrokeWidth(3f);

        labelPaint.setColor(ContextCompat.getColor(getContext(), R.color.color_text_secondary));
        labelPaint.setTextSize(28f);
        labelPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setLinePattern(boolean[] pattern) {
        if (pattern != null && pattern.length == 6) {
            this.linePattern = pattern;
            invalidate();
        }
    }

    public void setMovingLine(int line) {
        this.movingLine = line;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = width * 7 / 10;
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        int padding = w / 10;
        int lineAreaWidth = w - padding * 2 - 40;
        int lineSpacing = lineAreaWidth / 20;
        int lineThickness = h / 18;
        int gapBetweenLines = lineThickness * 2;
        int totalHeight = 6 * lineThickness + 5 * gapBetweenLines;
        int startY = (h - totalHeight) / 2;

        for (int i = 5; i >= 0; i--) {
            int y = startY + (5 - i) * (lineThickness + gapBetweenLines);
            int left = padding;
            int right = padding + lineAreaWidth;
            boolean isYang = linePattern[i];
            boolean isMoving = (i + 1) == movingLine;

            if (isYang) {
                canvas.drawRect(left, y, right, y + lineThickness, yangPaint);
            } else {
                int midGap = lineAreaWidth / 5;
                canvas.drawRect(left, y, right / 2 - midGap / 2, y + lineThickness, yinPaint);
                canvas.drawRect(right / 2 + midGap / 2, y, right, y + lineThickness, yinPaint);
            }

            if (isMoving) {
                canvas.drawRect(left - 4, y - 4, right + 4, y + lineThickness + 4, movingPaint);
            }

            String positionLabel = String.valueOf(i + 1);
            canvas.drawText(positionLabel, w - padding, y + lineThickness / 2 + 10, labelPaint);
        }
    }
}
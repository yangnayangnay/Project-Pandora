package com.example.project_pandora.ui.aimap;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import androidx.core.content.ContextCompat;
import com.example.project_pandora.R;

/** A single clock-like orbit provides continuity while each reading stage pauses. */
public final class FortuneOrbitView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float progress;
    private String center = "生辰";
    public FortuneOrbitView(Context context, AttributeSet attrs) { super(context, attrs); }
    public void setProgress(float value, String label) { progress = value; center = label; invalidate(); }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;
        float radius = Math.min(getWidth(), getHeight()) * .35f;
        int save = canvas.save(); canvas.translate(getWidth() / 2f, getHeight() / 2f);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.fortune_chart_ink));
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(density);
        paint.setAlpha(65); canvas.drawCircle(0, 0, radius, paint); canvas.drawCircle(0, 0, radius * .75f, paint);
        int orbit = canvas.save(); canvas.rotate(progress * 90);
        for (int i = 0; i < 12; i++) {
            canvas.drawLine(0, -radius - 5 * density, 0, -radius + 5 * density, paint); canvas.rotate(30);
        }
        paint.setAlpha(255); paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(0, -radius, 4 * density, paint);
        canvas.restoreToCount(orbit);
        paint.setTextAlign(Paint.Align.CENTER); paint.setTypeface(android.graphics.Typeface.create("sans-serif-medium", 0));
        paint.setTextSize(22 * getResources().getDisplayMetrics().scaledDensity);
        canvas.drawText(center, 0, -(paint.ascent() + paint.descent()) / 2, paint);
        canvas.restoreToCount(save);
    }
}

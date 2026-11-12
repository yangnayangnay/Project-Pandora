package com.example.project_pandora.ui.aimap;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import androidx.core.content.ContextCompat;
import com.example.project_pandora.R;

/** A brief shake, released bamboo slip and floating hexagram; no continuous idle motion. */
public final class DivinationRitualView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float progress;
    private ValueAnimator animation;
    private boolean[] pattern = {true, false, true, false, true, false};
    public DivinationRitualView(Context context, AttributeSet attrs) {
        super(context, attrs); setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    public void play(Runnable complete) {
        if (animation != null) return;
        if (android.os.Build.VERSION.SDK_INT >= 26 && !ValueAnimator.areAnimatorsEnabled()) {
            progress = 1; invalidate(); complete.run(); return;
        }
        progress = 0;
        animation = ValueAnimator.ofFloat(0, 1); animation.setDuration(5250);
        animation.setInterpolator(new android.view.animation.LinearInterpolator());
        animation.addUpdateListener(value -> { progress = (float) value.getAnimatedValue(); invalidate(); });
        animation.addListener(new android.animation.AnimatorListenerAdapter() {
            private boolean cancelled;
            @Override public void onAnimationCancel(android.animation.Animator value) { cancelled = true; }
            @Override public void onAnimationEnd(android.animation.Animator value) { animation = null; if (!cancelled) complete.run(); }
        }); animation.start();
    }
    public void setPattern(boolean[] value) { if (value != null && value.length == 6) pattern = value.clone(); }
    public void cancel() { if (animation != null) animation.cancel(); }
    @Override protected void onDetachedFromWindow() { cancel(); super.onDetachedFromWindow(); }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float unit = Math.min(getWidth() / 300f, getHeight() / 180f);
        int save = canvas.save(); canvas.translate(getWidth() / 2f, getHeight() / 2f); canvas.scale(unit, unit);
        int accent = ContextCompat.getColor(getContext(), R.color.color_accent);
        int soft = ContextCompat.getColor(getContext(), R.color.color_accent_soft);
        paint.setColor(soft); canvas.drawOval(-105, 60, 105, 75, paint);
        int cup = canvas.save();
        // Keep the shake lively, pause on the released slip, then hold the complete hexagram.
        float elapsed = progress * 5250;
        float shake = elapsed < 1900 ? (float) Math.sin(elapsed / 70f) * 7
                * Math.min(1, (1900 - elapsed) / 300f) : 0;
        canvas.rotate(shake, 0, 50);
        paint.setStrokeWidth(9); paint.setStrokeCap(Paint.Cap.ROUND); paint.setColor(accent);
        for (int i = -2; i <= 2; i++) canvas.drawLine(i * 13, 5, i * 16, -62 + Math.abs(i) * 7, paint);
        paint.setColor(soft); canvas.drawRoundRect(-47, -5, 47, 64, 15, 15, paint);
        paint.setColor(accent); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(2);
        canvas.drawRoundRect(-47, -5, 47, 64, 15, 15, paint); paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(-18, 12, 18, 43, 6, 6, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.color_on_accent)); paint.setTextAlign(Paint.Align.CENTER); paint.setTextSize(17);
        canvas.drawText("卦", 0, 34, paint); canvas.restoreToCount(cup);
        float release = Math.max(0, Math.min(1, (elapsed - 1600) / 700));
        if (release > 0) {
            int slip = canvas.save(); canvas.translate(80 * release, -60 * release); canvas.rotate(25 * release);
            paint.setColor(accent); canvas.drawRoundRect(-5, -44, 5, 12, 3, 3, paint); canvas.restoreToCount(slip);
        }
        float reveal = Math.max(0, Math.min(1, (elapsed - 3000) / 650));
        if (reveal > 0) {
            paint.setColor(accent); paint.setAlpha(Math.round(reveal * 255)); paint.setStrokeWidth(3);
            for (int line = 0; line < 6; line++) {
                float y = -70 + line * 8 - reveal * 8;
                if (pattern[5 - line]) canvas.drawLine(-23, y, 23, y, paint);
                else { canvas.drawLine(-23, y, -5, y, paint); canvas.drawLine(5, y, 23, y, paint); }
            }
            paint.setAlpha(255);
        }
        canvas.restoreToCount(save);
    }
}

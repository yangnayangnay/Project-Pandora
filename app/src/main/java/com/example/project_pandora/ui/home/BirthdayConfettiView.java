package com.example.project_pandora.ui.home;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Lightweight celebratory ribbons and confetti drawn without external assets. */
public final class BirthdayConfettiView extends View {
    private static final int[] COLORS = {0xFFE45756, 0xFF4C78A8, 0xFFF2CF5B, 0xFF54A24B, 0xFFB279A2, 0xFFFF9D3D};
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Piece> pieces = new ArrayList<>();
    private ValueAnimator animator;
    private float phase;

    public BirthdayConfettiView(Context context) { this(context, null); }
    public BirthdayConfettiView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setContentDescription("生日礼花和彩带");
        Random random = new Random(20261107L);
        for (int i = 0; i < 56; i++) pieces.add(new Piece(random.nextFloat(), random.nextFloat(),
                0.55f + random.nextFloat() * 0.9f, random.nextFloat() * 360f, COLORS[i % COLORS.length], i % 4 == 0));
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(2600L); animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addUpdateListener(value -> { phase = (float) value.getAnimatedValue(); invalidate(); });
        animator.start();
    }

    @Override protected void onDetachedFromWindow() {
        if (animator != null) animator.cancel();
        animator = null; super.onDetachedFromWindow();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight();
        for (Piece piece : pieces) {
            float progress = (piece.y + phase * piece.speed) % 1.12f;
            float x = piece.x * w + (float) Math.sin((phase * 6.28f) + piece.angle) * w * .035f;
            float y = progress * h;
            paint.setColor(piece.color); paint.setStrokeWidth(dp(piece.ribbon ? 3f : 2f));
            if (piece.ribbon) {
                android.graphics.Path path = new android.graphics.Path(); path.moveTo(x, y);
                path.cubicTo(x + dp(7), y + dp(4), x - dp(7), y + dp(11), x, y + dp(16));
                canvas.drawPath(path, paint);
            } else {
                canvas.save(); canvas.rotate(piece.angle + phase * 180f, x, y);
                canvas.drawRect(x - dp(3), y - dp(2), x + dp(3), y + dp(2), paint); canvas.restore();
            }
        }
    }

    private float dp(float value) { return value * getResources().getDisplayMetrics().density; }
    private static final class Piece {
        final float x, y, speed, angle; final int color; final boolean ribbon;
        Piece(float x, float y, float speed, float angle, int color, boolean ribbon) {
            this.x = x; this.y = y; this.speed = speed; this.angle = angle; this.color = color; this.ribbon = ribbon;
        }
    }
}

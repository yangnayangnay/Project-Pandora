package com.example.project_pandora.ui.login;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.Build;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;
import androidx.core.content.ContextCompat;
import com.example.project_pandora.R;

/** Soft circular color fields behind the form; shaders are reused rather than blurred every frame. */
public final class WarmLoginBackdropView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Shader base, peach, cream, light;
    private float radius, phase;
    private ValueAnimator motion;

    public WarmLoginBackdropView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setClickable(false); setFocusable(false);
    }

    @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        base = new LinearGradient(0, 0, width * .4f, Math.max(1, height),
                color(R.color.login_background_top), color(R.color.login_background_bottom), Shader.TileMode.CLAMP);
        radius = Math.max(width * .68f, Math.min(height * .42f, width));
        peach = orb(color(R.color.login_orb_peach));
        cream = orb(color(R.color.login_orb_cream));
        light = orb(color(R.color.login_orb_light));
    }

    private Shader orb(int color) {
        return new RadialGradient(0, 0, Math.max(1, radius),
                new int[]{color, (color & 0x00ffffff) | 0xd9000000, color & 0x00ffffff},
                new float[]{0, .55f, 1}, Shader.TileMode.CLAMP);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (base == null) return;
        paint.setShader(base); canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        float angle = phase * (float) (2 * Math.PI);
        drawOrb(canvas, peach, getWidth() * (.12f + .05f * (float) Math.sin(angle)),
                getHeight() * (.17f + .025f * (float) Math.cos(angle)), 1);
        drawOrb(canvas, cream, getWidth() * (.94f + .04f * (float) Math.cos(angle)),
                getHeight() * (.80f + .03f * (float) Math.sin(angle)), .92f);
        drawOrb(canvas, light, getWidth() * .68f, getHeight() * .44f, .80f);
        paint.setShader(null);
    }

    private void drawOrb(Canvas canvas, Shader shader, float x, float y, float scale) {
        int save = canvas.save(); canvas.translate(x, y); canvas.scale(scale, scale);
        paint.setShader(shader); canvas.drawCircle(0, 0, radius, paint);
        canvas.restoreToCount(save);
    }

    private boolean animationsEnabled() {
        if (Build.VERSION.SDK_INT >= 26) return ValueAnimator.areAnimatorsEnabled();
        return Settings.Global.getFloat(getContext().getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0;
    }

    private void updateMotion() {
        if (!isAttachedToWindow() || !isShown() || getWindowVisibility() != VISIBLE || !animationsEnabled()) {
            stopMotion(); return;
        }
        if (motion != null) return;
        motion = ValueAnimator.ofFloat(phase, phase + 1);
        motion.setDuration(28_000); motion.setRepeatCount(ValueAnimator.INFINITE);
        motion.setInterpolator(new LinearInterpolator());
        motion.addUpdateListener(value -> { phase = (float) value.getAnimatedValue() % 1; invalidate(); });
        motion.start();
    }

    private void stopMotion() {
        if (motion != null) { motion.cancel(); motion.removeAllUpdateListeners(); motion = null; }
    }

    @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); updateMotion(); }
    @Override protected void onDetachedFromWindow() { stopMotion(); super.onDetachedFromWindow(); }
    @Override protected void onWindowVisibilityChanged(int visibility) { super.onWindowVisibilityChanged(visibility); updateMotion(); }
    @Override public void onVisibilityAggregated(boolean visible) { super.onVisibilityAggregated(visible); updateMotion(); }
    private int color(int id) { return ContextCompat.getColor(getContext(), id); }
}

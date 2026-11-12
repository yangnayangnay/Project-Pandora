package com.example.project_pandora.ui.home;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.ViewParent;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import com.example.project_pandora.R;

/** Blurs only the shared background; foreground text, controls and scrolling stay sharp. */
public final class HomeGlassPanel extends LinearLayout {
    private final int tintTop, tintBottom, border, highlight;
    private final float radius;
    private final int[] panelLocation = new int[2], sceneLocation = new int[2];

    public HomeGlassPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.HomeGlassPanel);
        boolean events = attributes.getInt(R.styleable.HomeGlassPanel_homeGlassTone, 0) == 0;
        attributes.recycle();
        tintTop = color(events ? R.color.home_event_glass_top : R.color.home_task_glass_top);
        tintBottom = color(events ? R.color.home_event_glass_bottom : R.color.home_task_glass_bottom);
        border = color(events ? R.color.home_event_glass_border : R.color.home_task_glass_border);
        highlight = color(R.color.home_glass_highlight);
        radius = 18 * getResources().getDisplayMetrics().density;
        setBackground(new GlassBackground());
        setClipToOutline(true);
        setElevation(6 * getResources().getDisplayMetrics().density);
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            setOutlineAmbientShadowColor(color(R.color.home_glass_shadow));
            setOutlineSpotShadowColor(color(R.color.home_glass_shadow));
        }
    }

    private int color(int resource) { return ContextCompat.getColor(getContext(), resource); }

    private final class GlassBackground extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final RectF bounds = new RectF();
        private final Path clip = new Path();
        private LinearGradient tint, sheen;

        @Override protected void onBoundsChange(Rect rect) {
            bounds.set(rect); clip.reset(); clip.addRoundRect(bounds, radius, radius, Path.Direction.CW);
            tint = new LinearGradient(0, rect.top, rect.width() * .3f, rect.bottom,
                    tintTop, tintBottom, Shader.TileMode.CLAMP);
            sheen = new LinearGradient(0, rect.top, 0, rect.bottom,
                    new int[]{highlight, highlight & 0x00ffffff, highlight & 0x00ffffff},
                    new float[]{0f, .30f, 1f}, Shader.TileMode.CLAMP);
        }

        @Override public void draw(@NonNull Canvas canvas) {
            int save = canvas.save(); canvas.clipPath(clip);
            ViewParent ancestor = getParent();
            while (ancestor != null && !(ancestor instanceof HomeBackdropLayout)) ancestor = ancestor.getParent();
            if (ancestor instanceof HomeBackdropLayout) {
                HomeBackdropLayout scene = (HomeBackdropLayout) ancestor;
                getLocationInWindow(panelLocation); scene.getLocationInWindow(sceneLocation);
                int backgroundSave = canvas.save();
                canvas.translate(sceneLocation[0] - panelLocation[0], sceneLocation[1] - panelLocation[1]);
                scene.drawScene(canvas, true); canvas.restoreToCount(backgroundSave);
            }
            paint.setStyle(Paint.Style.FILL); paint.setShader(tint); canvas.drawRect(bounds, paint);
            paint.setShader(sheen); canvas.drawRect(bounds, paint);
            paint.setShader(null); paint.setColor(border); paint.setStyle(Paint.Style.STROKE);
            float stroke = getResources().getDisplayMetrics().density;
            paint.setStrokeWidth(stroke);
            // A single inset stroke follows the outer contour at the same width on every edge.
            float inset = stroke / 2;
            canvas.drawRoundRect(bounds.left + inset, bounds.top + inset, bounds.right - inset, bounds.bottom - inset,
                    radius - inset, radius - inset, paint);
            canvas.restoreToCount(save);
        }

        @Override public void getOutline(@NonNull Outline outline) { outline.setRoundRect(getBounds(), radius); }
        @Override public void setAlpha(int alpha) { }
        @Override public void setColorFilter(ColorFilter filter) { }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}

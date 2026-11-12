package com.example.project_pandora.ui.mine;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import android.content.Context;
import com.example.project_pandora.R;

/** Theme-aware, circular name avatar used while a photo is absent or unavailable. */
final class NameAvatarDrawable extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final String initials;
    private final int background, foreground;
    private final int intrinsicSize;
    private int alpha = 255;
    NameAvatarDrawable(Context context, String name) {
        initials = AvatarInitials.fromName(name);
        background = ContextCompat.getColor(context, R.color.color_accent);
        foreground = ContextCompat.getColor(context, R.color.color_on_accent);
        // CircleImageView converts Drawables into Bitmaps before drawing. Without
        // intrinsic dimensions that conversion fails and the name avatar vanishes.
        intrinsicSize = Math.max(1, Math.round(96 * context.getResources().getDisplayMetrics().density));
    }
    @Override public int getIntrinsicWidth() { return intrinsicSize; }
    @Override public int getIntrinsicHeight() { return intrinsicSize; }
    @Override public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        float diameter = Math.min(bounds.width(), bounds.height());
        paint.setColor(background); paint.setAlpha(alpha);
        canvas.drawCircle(bounds.exactCenterX(), bounds.exactCenterY(), diameter / 2, paint);
        paint.setColor(foreground); paint.setAlpha(alpha);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER); paint.setTextSize(diameter * .32f);
        float width = paint.measureText(initials);
        if (width > diameter * .78f) paint.setTextSize(paint.getTextSize() * diameter * .78f / width);
        canvas.drawText(initials, bounds.exactCenterX(), bounds.exactCenterY() - (paint.ascent() + paint.descent()) / 2, paint);
    }
    @Override public void setAlpha(int value) { alpha = value; invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}

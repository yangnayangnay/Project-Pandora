package com.example.project_pandora.ui.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import com.example.project_pandora.R;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Low-opacity, full-viewport photographs; one cached decode and blur per bundled image. */
public final class CompanyBackdropDrawable extends Drawable {
    private static final Map<Integer, CompletableFuture<Images>> CACHE = new HashMap<>();
    private final Context context;
    private final int resource, pageColor, photoAlpha;
    private final Runnable changed;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF photoBounds = new RectF();
    private LinearGradient fade;
    private Images images;
    private boolean enabled, requested;

    CompanyBackdropDrawable(Context context, int resource, Runnable changed) {
        this.context = context.getApplicationContext(); this.resource = resource; this.changed = changed;
        pageColor = ContextCompat.getColor(context, R.color.color_bg_page);
        photoAlpha = context.getResources().getInteger(R.integer.home_photo_alpha);
    }

    public void setCompanyEnabled(boolean enabled) {
        this.enabled = enabled;
        invalidateSelf(); changed.run();
        if (!enabled || requested) return;
        requested = true;
        WeakReference<CompanyBackdropDrawable> target = new WeakReference<>(this);
        load(context, resource).thenAccept(result -> new Handler(Looper.getMainLooper()).post(() -> {
            CompanyBackdropDrawable drawable = target.get();
            if (drawable == null) return;
            drawable.images = result; drawable.invalidateSelf(); drawable.changed.run();
        }));
    }

    @Override protected void onBoundsChange(Rect bounds) {
        fade = new LinearGradient(0, bounds.top, 0, Math.max(bounds.top + 1, bounds.bottom),
                new int[]{pageColor & 0x00ffffff, (pageColor & 0x00ffffff) | 0x18000000,
                        (pageColor & 0x00ffffff) | 0x30000000},
                new float[]{0, .55f, 1}, Shader.TileMode.CLAMP);
    }

    @Override public void draw(@NonNull Canvas canvas) { drawScene(canvas, false); }

    public void drawScene(Canvas canvas, boolean frosted) {
        Rect bounds = getBounds();
        paint.setShader(null); paint.setAlpha(255); paint.setColor(pageColor);
        canvas.drawRect(bounds, paint);
        if (enabled && images != null && bounds.width() > 0 && bounds.height() > 0) {
            Bitmap photo = frosted ? images.blurred : images.original;
            float scale = Math.max(bounds.width() / (float) photo.getWidth(), bounds.height() / (float) photo.getHeight());
            float width = photo.getWidth() * scale, height = photo.getHeight() * scale;
            photoBounds.set(bounds.exactCenterX() - width / 2, bounds.top,
                    bounds.exactCenterX() + width / 2, bounds.top + height);
            paint.setAlpha(photoAlpha); canvas.drawBitmap(photo, null, photoBounds, paint);
            paint.setAlpha(255); paint.setShader(fade); canvas.drawRect(bounds, paint); paint.setShader(null);
        }
    }

    private static synchronized CompletableFuture<Images> load(Context context, int resource) {
        CompletableFuture<Images> existing = CACHE.get(resource);
        if (existing != null) return existing;
        CompletableFuture<Images> future = CompletableFuture.supplyAsync(() -> {
            BitmapFactory.Options options = new BitmapFactory.Options(); options.inScaled = false;
            Bitmap original = BitmapFactory.decodeResource(context.getResources(), resource, options);
            if (original == null) return null;
            int width = 384, height = Math.max(1, Math.round(original.getHeight() * width / (float) original.getWidth()));
            Bitmap small = Bitmap.createScaledBitmap(original, width, height, true);
            int[] pixels = new int[width * height], scratch = new int[width * height];
            small.getPixels(pixels, 0, width, 0, 0, width, height);
            for (int pass = 0; pass < 3; pass++) {
                blurPass(pixels, scratch, width, height, 10, true);
                blurPass(scratch, pixels, width, height, 10, false);
            }
            Bitmap blurred = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
            if (small != original) small.recycle();
            return new Images(original, blurred);
        }).exceptionally(error -> null);
        CACHE.put(resource, future); return future;
    }

    private static void blurPass(int[] input, int[] output, int width, int height, int radius, boolean horizontal) {
        int lines = horizontal ? height : width, length = horizontal ? width : height, count = radius * 2 + 1;
        for (int line = 0; line < lines; line++) {
            int red = 0, green = 0, blue = 0;
            for (int offset = -radius; offset <= radius; offset++) {
                int position = Math.max(0, Math.min(length - 1, offset));
                int color = input[horizontal ? line * width + position : position * width + line];
                red += (color >> 16) & 255; green += (color >> 8) & 255; blue += color & 255;
            }
            for (int position = 0; position < length; position++) {
                output[horizontal ? line * width + position : position * width + line] =
                        0xff000000 | (red / count << 16) | (green / count << 8) | blue / count;
                int leaving = Math.max(0, position - radius), entering = Math.min(length - 1, position + radius + 1);
                int oldColor = input[horizontal ? line * width + leaving : leaving * width + line];
                int newColor = input[horizontal ? line * width + entering : entering * width + line];
                red += ((newColor >> 16) & 255) - ((oldColor >> 16) & 255);
                green += ((newColor >> 8) & 255) - ((oldColor >> 8) & 255);
                blue += (newColor & 255) - (oldColor & 255);
            }
        }
    }

    @Override public void setAlpha(int alpha) { }
    @Override public void setColorFilter(ColorFilter filter) { }
    @Override public int getOpacity() { return PixelFormat.OPAQUE; }
    private static final class Images {
        final Bitmap original, blurred;
        Images(Bitmap original, Bitmap blurred) { this.original = original; this.blurred = blurred; }
    }
}

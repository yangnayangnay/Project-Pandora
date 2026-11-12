package com.example.project_pandora.ui.aimap;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.view.*;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.example.project_pandora.R;

/** A single transient ritual window; the activity never retains a blur or invisible touch layer. */
final class DivinationRitualOverlay {
    private Dialog dialog;
    private DivinationRitualView ritual;
    private Bitmap snapshot;

    void play(Activity activity, View source, boolean[] pattern, Runnable complete, Runnable cancelled) {
        close();
        if (android.os.Build.VERSION.SDK_INT >= 26 && !android.animation.ValueAnimator.areAnimatorsEnabled()) { complete.run(); return; }
        View decor = activity.getWindow().getDecorView();
        int[] origin = new int[2]; source.getLocationOnScreen(origin);
        FrameLayout stage = new FrameLayout(activity);
        snapshot = blurredSnapshot(decor);
        BitmapDrawable backdrop = new BitmapDrawable(activity.getResources(), snapshot);
        backdrop.setFilterBitmap(true); stage.setBackground(backdrop);
        View veil = new View(activity); veil.setBackgroundColor((ContextCompat.getColor(activity, R.color.color_bg_page) & 0xffffff) | 0x99000000);
        stage.addView(veil, new FrameLayout.LayoutParams(-1, -1));
        ritual = new DivinationRitualView(activity, null); ritual.setPattern(pattern);
        int width = Math.max(1, Math.min(decor.getWidth() - dp(activity, 32), dp(activity, 432)));
        int height = Math.max(1, Math.min(dp(activity, 300), Math.round(decor.getHeight() * .58f)));
        stage.addView(ritual, new FrameLayout.LayoutParams(width, height, Gravity.CENTER));
        TextView caption = new TextView(activity); caption.setText("静心片刻，卦象将至"); caption.setTextSize(16);
        caption.setTextColor(ContextCompat.getColor(activity, R.color.color_text_primary)); caption.setGravity(Gravity.CENTER);
        caption.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        FrameLayout.LayoutParams captionParams = new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER);
        caption.setTranslationY(height / 2f + dp(activity, 32)); stage.addView(caption, captionParams);
        Dialog window = new Dialog(activity, android.R.style.Theme_Material_NoActionBar_Fullscreen);
        dialog = window; window.setContentView(stage); window.setCanceledOnTouchOutside(false);
        window.setOnCancelListener(ignored -> { close(); cancelled.run(); });
        window.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.show(); window.getWindow().setLayout(-1, -1);
        // View.post can run before the dialog's first layout. The measured
        // center is needed for both the move-to-center and enlarged animation.
        androidx.core.view.OneShotPreDrawListener.add(stage, () -> {
            if (dialog != window || !window.isShowing()) return;
            int[] location = new int[2]; ritual.getLocationOnScreen(location);
            ritual.setPivotX(width / 2f); ritual.setPivotY(height / 2f);
            float scale = Math.min(source.getWidth() / 300f, source.getHeight() / 180f) / Math.min(width / 300f, height / 180f);
            ritual.setScaleX(scale); ritual.setScaleY(scale);
            ritual.setTranslationX(origin[0] + source.getWidth() / 2f - location[0] - width / 2f);
            ritual.setTranslationY(origin[1] + source.getHeight() / 2f - location[1] - height / 2f);
            ritual.animate().scaleX(1).scaleY(1).translationX(0).translationY(0).setDuration(300)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator()).withEndAction(() -> {
                if (dialog != window) return;
                ritual.play(() -> { close(); complete.run(); });
            }).start();
        });
    }
    void close() {
        if (ritual != null) { ritual.animate().cancel(); ritual.cancel(); ritual = null; }
        if (dialog != null) {
            Dialog old = dialog; dialog = null; old.setOnCancelListener(null); old.dismiss();
        }
        // The dismissed window may be drawn for one final frame, so let its bitmap be collected.
        snapshot = null;
    }
    private static Bitmap blurredSnapshot(View decor) {
        int width = Math.max(1, decor.getWidth() / 12), height = Math.max(1, decor.getHeight() / 12);
        Bitmap tiny = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(tiny); canvas.scale(width / (float) Math.max(1, decor.getWidth()), height / (float) Math.max(1, decor.getHeight())); decor.draw(canvas);
        int[] pixels = new int[width * height], result = new int[pixels.length]; tiny.getPixels(pixels, 0, width, 0, 0, width, height);
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            int red = 0, green = 0, blue = 0, count = 0;
            for (int dy = -2; dy <= 2; dy++) for (int dx = -2; dx <= 2; dx++) {
                int c = pixels[Math.max(0, Math.min(height - 1, y + dy)) * width + Math.max(0, Math.min(width - 1, x + dx))];
                red += Color.red(c); green += Color.green(c); blue += Color.blue(c); count++;
            }
            result[y * width + x] = Color.rgb(red / count, green / count, blue / count);
        }
        tiny.setPixels(result, 0, width, 0, 0, width, height); return tiny;
    }
    private static int dp(Activity activity, int value) { return Math.round(value * activity.getResources().getDisplayMetrics().density); }
}

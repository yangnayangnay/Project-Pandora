package com.example.project_pandora.core.config;

import android.content.Context;

/** A device-local multiplier, applied on top of the system accessibility font size. */
public final class FontSizePrefs {
    private FontSizePrefs() {}
    public static float get(Context context) {
        return normalize(context.getSharedPreferences("appearance", Context.MODE_PRIVATE).getFloat("font_size", 1f));
    }
    public static void save(Context context, float value) {
        context.getSharedPreferences("appearance", Context.MODE_PRIVATE).edit().putFloat("font_size", normalize(value)).apply();
    }
    private static float normalize(float value) {
        return value == .9f || value == 1.1f || value == 1.2f ? value : 1f;
    }
}

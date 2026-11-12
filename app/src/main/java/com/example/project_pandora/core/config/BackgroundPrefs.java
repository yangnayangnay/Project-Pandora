package com.example.project_pandora.core.config;

import android.content.Context;
import android.content.SharedPreferences;

/** Device-local appearance choice, independent from server settings and login state. */
public final class BackgroundPrefs {
    public static final String KEY_STYLE = "background_style";
    public static final int SOLID = 0, COMPANY = 1;
    private BackgroundPrefs() {}
    public static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences("background_prefs", Context.MODE_PRIVATE);
    }
    public static int getStyle(Context context) {
        int value = preferences(context).getInt(KEY_STYLE, COMPANY);
        return value == SOLID ? SOLID : COMPANY;
    }
    public static void saveStyle(Context context, int value) {
        preferences(context).edit().putInt(KEY_STYLE, value == SOLID ? SOLID : COMPANY).apply();
    }
}

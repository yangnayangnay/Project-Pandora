package com.example.project_pandora.core.config;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * R37 REQ-37-B: 主题模式持久化（三态）。
 * 0=跟随系统（默认，无记录时兜底）｜1=始终白天｜2=始终黑夜。
 * 非法值一律按跟随系统处理（升级/损坏容错）。
 */
public final class ThemePrefs {

    public static final int MODE_FOLLOW_SYSTEM = 0;
    public static final int MODE_ALWAYS_DAY = 1;
    public static final int MODE_ALWAYS_NIGHT = 2;

    private static final String PREF_NAME = "theme_prefs";
    private static final String KEY_MODE = "theme_mode";

    private ThemePrefs() {}

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static int getMode(Context context) {
        int mode = prefs(context).getInt(KEY_MODE, MODE_FOLLOW_SYSTEM);
        if (mode != MODE_ALWAYS_DAY && mode != MODE_ALWAYS_NIGHT) {
            return MODE_FOLLOW_SYSTEM;
        }
        return mode;
    }

    public static void saveMode(Context context, int mode) {
        prefs(context).edit().putInt(KEY_MODE, mode).apply();
    }

    /** 三态 → AppCompatDelegate 常量映射 */
    public static int toDelegateFlag(int mode) {
        switch (mode) {
            case MODE_ALWAYS_DAY:
                return AppCompatDelegate.MODE_NIGHT_NO;
            case MODE_ALWAYS_NIGHT:
                return AppCompatDelegate.MODE_NIGHT_YES;
            default:
                return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
    }

    /**
     * R37 REQ-37-B4/B5: 应用主题（点选即生效）+ 冷启动首帧前应用（Application.onCreate 调用）。
     */
    public static void applyTheme(Context context) {
        AppCompatDelegate.setDefaultNightMode(toDelegateFlag(getMode(context)));
    }
}
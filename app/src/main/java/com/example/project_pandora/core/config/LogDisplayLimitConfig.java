package com.example.project_pandora.core.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class LogDisplayLimitConfig {

    private static final String PREF_NAME = "log_display_prefs";
    private static final String KEY_TIME_LIMIT = "log_display_time_limit";
    private static final String KEY_COUNT_LIMIT = "log_display_count_limit";

    private static final int DEFAULT_TIME_DAYS = 30;
    private static final int DEFAULT_COUNT = 50;

    private final SharedPreferences prefs;

    public LogDisplayLimitConfig(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public int getTimeLimitDays() {
        try {
            int value = prefs.getInt(KEY_TIME_LIMIT, DEFAULT_TIME_DAYS);
            LogTimeLimit validated = LogTimeLimit.fromDays(value);
            if (validated.getDays() != value) {
                prefs.edit().putInt(KEY_TIME_LIMIT, validated.getDays()).apply();
            }
            return validated.getDays();
        } catch (Exception e) {
            Log.e("LogDisplayLimitConfig", "getTimeLimitDays error", e);
            return DEFAULT_TIME_DAYS;
        }
    }

    public int getCountLimit() {
        try {
            int value = prefs.getInt(KEY_COUNT_LIMIT, DEFAULT_COUNT);
            LogCountLimit validated = LogCountLimit.fromCount(value);
            if (validated.getCount() != value) {
                prefs.edit().putInt(KEY_COUNT_LIMIT, validated.getCount()).apply();
            }
            return validated.getCount();
        } catch (Exception e) {
            Log.e("LogDisplayLimitConfig", "getCountLimit error", e);
            return DEFAULT_COUNT;
        }
    }

    public boolean setTimeLimitDays(int days) {
        try {
            LogTimeLimit validated = LogTimeLimit.fromDays(days);
            prefs.edit().putInt(KEY_TIME_LIMIT, validated.getDays()).apply();
            return true;
        } catch (Exception e) {
            Log.e("LogDisplayLimitConfig", "setTimeLimitDays error", e);
            return false;
        }
    }

    public boolean setCountLimit(int count) {
        try {
            LogCountLimit validated = LogCountLimit.fromCount(count);
            prefs.edit().putInt(KEY_COUNT_LIMIT, validated.getCount()).apply();
            return true;
        } catch (Exception e) {
            Log.e("LogDisplayLimitConfig", "setCountLimit error", e);
            return false;
        }
    }
}
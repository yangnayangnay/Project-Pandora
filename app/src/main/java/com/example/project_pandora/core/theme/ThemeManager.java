package com.example.project_pandora.core.theme;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.Log;

import com.example.project_pandora.core.moon.MoonPhaseCalculator;
import com.example.project_pandora.core.moon.MoonPhaseCalculator.MoonPhase;

import java.util.Date;

public class ThemeManager {

    private static final String TAG = "ThemeManager";
    private static final String PREF_NAME = "theme_prefs";
    private static final String KEY_MODE = "theme_mode";
    private static final String KEY_MANUAL_THEME = "manual_theme";
    private static final String KEY_LAST_PHASE = "last_phase";

    public enum ThemeMode { AUTO, MANUAL }

    private final Context context;
    private final SharedPreferences prefs;
    private final MoonPhaseCalculator moonPhaseCalculator;

    public ThemeManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.moonPhaseCalculator = new MoonPhaseCalculator();
    }

    public ThemeMode getThemeMode() {
        String mode = prefs.getString(KEY_MODE, "AUTO");
        return "MANUAL".equals(mode) ? ThemeMode.MANUAL : ThemeMode.AUTO;
    }

    public void setThemeMode(ThemeMode mode) {
        prefs.edit().putString(KEY_MODE, mode.name()).apply();
    }

    public void setManualTheme(MoonPhase phase) {
        prefs.edit()
             .putString(KEY_MODE, "MANUAL")
             .putString(KEY_MANUAL_THEME, phase.name())
             .apply();
    }

    public MoonPhase getCurrentTheme() {
        ThemeMode mode = getThemeMode();
        if (mode == ThemeMode.MANUAL) {
            String manualTheme = prefs.getString(KEY_MANUAL_THEME, MoonPhase.NEW_MOON.name());
            try {
                return MoonPhase.valueOf(manualTheme);
            } catch (IllegalArgumentException e) {
                return MoonPhase.NEW_MOON;
            }
        }
        return moonPhaseCalculator.calculateCurrentMoonPhase();
    }

    public boolean shouldApplyTheme() {
        MoonPhase currentPhase = getCurrentTheme();
        String lastPhase = prefs.getString(KEY_LAST_PHASE, "");
        if (!currentPhase.name().equals(lastPhase)) {
            prefs.edit().putString(KEY_LAST_PHASE, currentPhase.name()).apply();
            return true;
        }
        return false;
    }

    public int getThemeOverlayResId(MoonPhase phase) {
        Resources res = context.getResources();
        String themeName = phase.getThemeName().replace("values-theme-", "");
        int resId = res.getIdentifier("ThemeOverlay_" + themeName, "style", context.getPackageName());
        return resId != 0 ? resId : 0;
    }

    public void applyTheme() {
        MoonPhase phase = getCurrentTheme();
        Log.d(TAG, "Applying theme: " + phase.getDisplayName());
    }
}
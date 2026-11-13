package com.example.project_pandora.ui.widget;

import android.content.SharedPreferences;
import android.view.View;
import com.example.project_pandora.R;
import com.example.project_pandora.core.config.BackgroundPrefs;

/** Each page has its own local photo; all pages share a single persisted appearance choice. */
public final class CompanyBackgrounds {
    public enum Scene {
        HOME(R.drawable.home_office_building), COMPANY(R.drawable.company_interior),
        LOG(R.drawable.company_workstation), AI(R.drawable.company_sky), MINE(R.drawable.company_lounge);
        final int photo;
        Scene(int photo) { this.photo = photo; }
    }
    private CompanyBackgrounds() {}

    public static CompanyBackdropDrawable bind(View root, Scene scene) { return bind(root, scene, () -> {}); }

    public static CompanyBackdropDrawable bind(View root, Scene scene, Runnable onChange) {
        CompanyBackdropDrawable drawable = new CompanyBackdropDrawable(root.getContext(), scene.photo, onChange);
        root.setBackground(drawable);
        Runnable refresh = () -> {
            int style = BackgroundPrefs.getStyle(root.getContext());
            drawable.setStyle(style);
        };
        SharedPreferences prefs = BackgroundPrefs.preferences(root.getContext());
        SharedPreferences.OnSharedPreferenceChangeListener listener = (settings, key) -> {
            if (BackgroundPrefs.KEY_STYLE.equals(key)) root.post(refresh);
        };
        root.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View view) {
                prefs.registerOnSharedPreferenceChangeListener(listener);
                refresh.run();
            }
            @Override public void onViewDetachedFromWindow(View view) {
                prefs.unregisterOnSharedPreferenceChangeListener(listener);
            }
        });
        if (root.isAttachedToWindow()) prefs.registerOnSharedPreferenceChangeListener(listener);
        refresh.run();
        return drawable;
    }
}

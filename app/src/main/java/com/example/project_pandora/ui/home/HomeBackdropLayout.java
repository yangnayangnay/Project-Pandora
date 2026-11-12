package com.example.project_pandora.ui.home;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import com.example.project_pandora.ui.widget.CompanyBackdropDrawable;
import com.example.project_pandora.ui.widget.CompanyBackgrounds;

/** Home glass panels sample the same full-page scene and obey the shared background setting. */
public final class HomeBackdropLayout extends LinearLayout {
    private final CompanyBackdropDrawable backdrop;

    public HomeBackdropLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        backdrop = CompanyBackgrounds.bind(this, CompanyBackgrounds.Scene.HOME, () -> invalidatePanels(this));
    }

    private void invalidatePanels(android.view.ViewGroup parent) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            android.view.View child = parent.getChildAt(i);
            if (child instanceof HomeGlassPanel) child.invalidate();
            else if (child instanceof android.view.ViewGroup) invalidatePanels((android.view.ViewGroup) child);
        }
    }

    void drawScene(Canvas canvas, boolean frosted) { backdrop.drawScene(canvas, frosted); }
}

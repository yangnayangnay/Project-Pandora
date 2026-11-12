package com.example.project_pandora.ui.widget;

import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/** A short native slide; layout bounds remain stable and accessibility animation scale is respected. */
public final class CalendarPageMotion {
    private CalendarPageMotion() {}
    public static void enter(View view, int direction) {
        view.animate().cancel();
        if (!view.isAttachedToWindow() || (android.os.Build.VERSION.SDK_INT >= 26 && !ValueAnimator.areAnimatorsEnabled())) {
            view.setTranslationX(0); view.setAlpha(1); return;
        }
        view.setTranslationX(direction * Math.min(view.getWidth() * .22f, 80 * view.getResources().getDisplayMetrics().density));
        view.setAlpha(.6f);
        view.animate().translationX(0).alpha(1).setDuration(200).setInterpolator(new DecelerateInterpolator()).start();
    }
}

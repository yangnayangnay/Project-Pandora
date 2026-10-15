package com.example.project_pandora.core.util;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;

import com.example.project_pandora.R;
import com.google.android.material.appbar.MaterialToolbar;

import androidx.appcompat.widget.Toolbar;

public class BackToolbarHelper {

    public static void setupBack(MaterialToolbar toolbar, Runnable onBack) {
        toolbar.setNavigationIcon(R.drawable.ic_back);
        toolbar.setNavigationOnClickListener(v -> onBack.run());

        int resourceId = toolbar.getResources().getIdentifier("status_bar_height", "dimen", "android");
        int statusBarHeight = resourceId > 0 ? toolbar.getResources().getDimensionPixelSize(resourceId) : 0;
        toolbar.setPadding(toolbar.getPaddingLeft(), statusBarHeight, toolbar.getPaddingRight(), toolbar.getPaddingBottom());

        ViewTreeObserver.OnPreDrawListener preDrawListener = new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                toolbar.getViewTreeObserver().removeOnPreDrawListener(this);
                int toolbarHeight = toolbar.getHeight();
                int contentHeight = toolbarHeight - toolbar.getPaddingTop() - toolbar.getPaddingBottom();
                if (contentHeight <= 0) contentHeight = dpToPx(toolbar, 56);
                if (toolbarHeight > 0) {
                    toolbar.setMinimumHeight(toolbarHeight);
                } else {
                    toolbar.setMinimumHeight(dpToPx(toolbar, 56) + statusBarHeight);
                }
                View navIcon = findNavigationIconView(toolbar);
                if (navIcon != null) {
                    ViewGroup.LayoutParams lp = navIcon.getLayoutParams();
                    if (lp != null && contentHeight > 0) {
                        lp.height = contentHeight;
                        int minTouchWidth = dpToPx(toolbar, 56);
                        if (lp.width < minTouchWidth) {
                            lp.width = minTouchWidth;
                        }
                        if (lp instanceof ViewGroup.MarginLayoutParams) {
                            ((ViewGroup.MarginLayoutParams) lp).topMargin = 0;
                            ((ViewGroup.MarginLayoutParams) lp).bottomMargin = 0;
                        }
                        if (lp instanceof Toolbar.LayoutParams) {
                            ((Toolbar.LayoutParams) lp).gravity = Gravity.CENTER_VERTICAL;
                        }
                        navIcon.setLayoutParams(lp);
                    }
                    navIcon.setClickable(true);
                    navIcon.setFocusable(true);
                    navIcon.setOnClickListener(v -> onBack.run());
                }
                return true;
            }
        };
        toolbar.getViewTreeObserver().addOnPreDrawListener(preDrawListener);
    }

    private static int dpToPx(View view, int dp) {
        return (int) (dp * view.getResources().getDisplayMetrics().density);
    }

    private static View findNavigationIconView(ViewGroup parent) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child instanceof ImageButton) {
                return child;
            }
            if (child instanceof ViewGroup) {
                View found = findNavigationIconView((ViewGroup) child);
                if (found != null) return found;
            }
        }
        return null;
    }
}
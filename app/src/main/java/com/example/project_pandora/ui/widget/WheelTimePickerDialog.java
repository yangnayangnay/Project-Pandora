package com.example.project_pandora.ui.widget;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project_pandora.R;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

/** Five-row, cyclic 24-hour wheels; only the confirm button commits a selection. */
public final class WheelTimePickerDialog extends Dialog {
    public interface OnTimeSelected { void accept(int hour, int minute); }
    private final String title;
    private final int initialHour, initialMinute;
    private final OnTimeSelected onSelected;
    private WheelColumn hours, minutes;
    private TextView preview;

    public WheelTimePickerDialog(Context context, String title, int hour, int minute, OnTimeSelected onSelected) {
        super(context);
        this.title = title;
        initialHour = hour; initialMinute = minute;
        this.onSelected = onSelected;
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        Window window = getWindow();
        if (window == null) return;
        window.requestFeature(Window.FEATURE_NO_TITLE);
        LinearLayout content = new LinearLayout(getContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(16));
        content.setBackground(rounded(R.color.color_surface, 28));

        LinearLayout header = new LinearLayout(getContext());
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView label = new TextView(getContext());
        label.setText(title); label.setTextSize(16); label.setTextColor(color(R.color.color_text_primary));
        header.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
        preview = new TextView(getContext());
        preview.setTextSize(16); preview.setTextColor(color(R.color.color_time_picker_accent));
        preview.setGravity(Gravity.CENTER); preview.setPadding(dp(16), dp(6), dp(16), dp(6));
        preview.setBackground(rounded(R.color.color_surface_soft, 24));
        header.addView(preview, new LinearLayout.LayoutParams(dp(120), -2));
        content.addView(header, new LinearLayout.LayoutParams(-1, dp(40)));

        // Keep buttons reachable on short landscape windows.
        int visibleRows = getContext().getResources().getConfiguration().screenHeightDp < 420 ? 3 : 5;
        int wheelHeight = dp(44) * visibleRows;
        FrameLayout wheelFrame = new FrameLayout(getContext());
        View selectedBand = new View(getContext());
        selectedBand.setBackground(rounded(R.color.color_surface_soft, 24));
        wheelFrame.addView(selectedBand, new FrameLayout.LayoutParams(-1, dp(44), Gravity.CENTER));
        LinearLayout columns = new LinearLayout(getContext());
        hours = new WheelColumn(24, initialHour, visibleRows, "小时");
        minutes = new WheelColumn(60, initialMinute, visibleRows, "分钟");
        columns.addView(hours, new LinearLayout.LayoutParams(0, -1, 1));
        columns.addView(minutes, new LinearLayout.LayoutParams(0, -1, 1));
        wheelFrame.addView(columns, new FrameLayout.LayoutParams(-1, -1));
        content.addView(wheelFrame, new LinearLayout.LayoutParams(-1, wheelHeight));

        LinearLayout buttons = new LinearLayout(getContext());
        MaterialButton cancel = button("取消", R.color.color_text_primary);
        MaterialButton confirm = button("确定", R.color.color_time_picker_accent);
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(0, dp(48), 1);
        cancelParams.setMargins(0, 0, dp(12), 0);
        buttons.addView(cancel, cancelParams);
        buttons.addView(confirm, new LinearLayout.LayoutParams(0, dp(48), 1));
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(-1, -2);
        buttonParams.topMargin = dp(12);
        content.addView(buttons, buttonParams);
        cancel.setOnClickListener(v -> cancel());
        confirm.setOnClickListener(v -> {
            hours.stopScroll(); minutes.stopScroll();
            int hour = hours.value(), minute = minutes.value();
            dismiss();
            onSelected.accept(hour, minute);
        });
        setContentView(content);
        setCanceledOnTouchOutside(true);
        window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.dimAmount = .32f;
        attributes.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        attributes.y = dp(16);
        window.setAttributes(attributes);
        window.setLayout(Math.min(dp(400), getContext().getResources().getDisplayMetrics().widthPixels - dp(24)), -2);
        updatePreview();
    }

    private void updatePreview() {
        if (preview != null && hours != null && minutes != null)
            preview.setText(String.format(Locale.ROOT, "%02d:%02d", hours.value(), minutes.value()));
    }

    private MaterialButton button(String title, int textColor) {
        MaterialButton button = new MaterialButton(getContext());
        button.setText(title); button.setTextSize(16); button.setTextColor(color(textColor));
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color(R.color.color_surface_soft)));
        button.setCornerRadius(dp(24)); button.setInsetTop(0); button.setInsetBottom(0);
        button.setMinHeight(0); button.setMinimumHeight(0);
        return button;
    }

    private final class WheelColumn extends RecyclerView {
        private final int range, initial;
        private final LinearLayoutManager layout;
        private final LinearSnapHelper snap = new LinearSnapHelper();

        WheelColumn(int range, int initial, int visibleRows, String label) {
            super(WheelTimePickerDialog.this.getContext());
            this.range = range;
            this.initial = initial;
            setContentDescription(label);
            layout = new LinearLayoutManager(getContext());
            setLayoutManager(layout);
            setPadding(0, dp(44) * (visibleRows / 2), 0, dp(44) * (visibleRows / 2));
            setClipToPadding(false);
            setOverScrollMode(OVER_SCROLL_NEVER);
            setVerticalScrollBarEnabled(false);
            setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            setAdapter(new Adapter<ViewHolder>() {
                @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
                    TextView number = new TextView(getContext());
                    number.setGravity(Gravity.CENTER);
                    number.setTextSize(20);
                    number.setLayoutParams(new RecyclerView.LayoutParams(-1, dp(44)));
                    return new ViewHolder(number) {};
                }
                @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
                    TextView number = (TextView) holder.itemView;
                    int value = position % range;
                    number.setText(String.format(Locale.ROOT, "%02d", value));
                    number.setContentDescription(value + (range == 24 ? "时" : "分"));
                    number.setTextColor(color(R.color.color_text_secondary));
                    number.setOnClickListener(v -> smoothScrollBy(0,
                            v.getTop() + v.getHeight() / 2 - getHeight() / 2));
                }
                @Override public int getItemCount() { return range * 1001; }
            });
            snap.attachToRecyclerView(this);
            layout.scrollToPositionWithOffset(range * 500 + initial, 0);
            addOnScrollListener(new OnScrollListener() {
                @Override public void onScrolled(@NonNull RecyclerView view, int dx, int dy) { refresh(); }
            });
            addOnLayoutChangeListener((v, l, t, r, b, oldL, oldT, oldR, oldB) -> refresh());
        }

        int value() {
            View centered = snap.findSnapView(layout);
            return centered == null ? initial : layout.getPosition(centered) % range;
        }

        private void refresh() {
            View selected = snap.findSnapView(layout);
            for (int index = 0; index < getChildCount(); index++) {
                TextView child = (TextView) getChildAt(index);
                float distance = Math.abs(child.getTop() + child.getHeight() / 2f - getHeight() / 2f) / dp(44);
                child.setAlpha(Math.max(.12f, 1 - distance * .34f));
                child.setTextColor(color(child == selected ? R.color.color_time_picker_accent : R.color.color_text_secondary));
                child.setSelected(child == selected);
            }
            updatePreview();
        }
    }

    private GradientDrawable rounded(int resource, int radius) {
        GradientDrawable drawable = new GradientDrawable(); drawable.setColor(color(resource)); drawable.setCornerRadius(dp(radius)); return drawable;
    }
    private int color(int resource) { return ContextCompat.getColor(getContext(), resource); }
    private int dp(float value) { return Math.round(value * getContext().getResources().getDisplayMetrics().density); }
}

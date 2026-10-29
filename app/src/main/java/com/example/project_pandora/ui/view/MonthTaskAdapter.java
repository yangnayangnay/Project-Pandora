package com.example.project_pandora.ui.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project_pandora.R;
import com.example.project_pandora.core.util.DateCompat;
import com.example.project_pandora.core.util.NumberCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 月视图任务卡适配器。全部视图由代码创建，复用现有颜色 / 色板资源。
 */
public class MonthTaskAdapter extends RecyclerView.Adapter<MonthTaskAdapter.MonthTaskViewHolder> {

    private static final String TEXT_ALL_DAY = "全天";
    private static final String TEXT_TIME_UNSET = "未设置时间";

    private final List<Map<String, Object>> tasks;
    private final Consumer<Map<String, Object>> open;
    private final Consumer<Map<String, Object>> statusClick;

    public MonthTaskAdapter(List<Map<String, Object>> tasks,
                            Consumer<Map<String, Object>> open,
                            Consumer<Map<String, Object>> statusClick) {
        this.tasks = tasks != null ? tasks : new ArrayList<>();
        this.open = open;
        this.statusClick = statusClick;
    }

    /**
     * 与 TaskTableAdapter 相同的局部刷新语义：命中 id 后改写 status 并只刷新该项。
     */
    public void updateTaskStatus(long id, String status) {
        for (int i = 0; i < tasks.size(); i++) {
            Map<String, Object> task = tasks.get(i);
            if (task == null) continue;
            Long taskId = NumberCompat.toLong(task.get("id"));
            if (taskId != null && taskId.longValue() == id) {
                task.put("status", status);
                notifyItemChanged(i);
                return;
            }
        }
    }

    @NonNull
    @Override
    public MonthTaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return MonthTaskViewHolder.create(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull MonthTaskViewHolder holder, int position) {
        holder.bind(tasks.get(position), open, statusClick);
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class MonthTaskViewHolder extends RecyclerView.ViewHolder {
        private final View accentBar;
        private final GradientDrawable accentBackground;
        private final GradientDrawable statusBackground;
        private final TextView nameView;
        private final TextView timeView;
        private final TextView metaView;
        private final TextView statusView;
        private final int[] accentPalette;

        private MonthTaskViewHolder(@NonNull LinearLayout card,
                                    @NonNull View accentBar,
                                    @NonNull GradientDrawable accentBackground,
                                    @NonNull GradientDrawable statusBackground,
                                    @NonNull TextView nameView,
                                    @NonNull TextView timeView,
                                    @NonNull TextView metaView,
                                    @NonNull TextView statusView,
                                    @NonNull int[] accentPalette) {
            super(card);
            this.accentBar = accentBar;
            this.accentBackground = accentBackground;
            this.statusBackground = statusBackground;
            this.nameView = nameView;
            this.timeView = timeView;
            this.metaView = metaView;
            this.statusView = statusView;
            this.accentPalette = accentPalette;
        }

        static MonthTaskViewHolder create(@NonNull ViewGroup parent) {
            Context context = parent.getContext();

            LinearLayout card = new LinearLayout(context);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(context, 16), dp(context, 16), dp(context, 16), dp(context, 16));
            card.setMinimumHeight(dp(context, 80));
            GradientDrawable cardBackground = new GradientDrawable();
            cardBackground.setColor(ContextCompat.getColor(context, R.color.color_calendar_bg));
            cardBackground.setStroke(dp(context, 1), ContextCompat.getColor(context, R.color.color_divider));
            cardBackground.setCornerRadius(dp(context, 18));
            card.setBackground(cardBackground);

            RecyclerView.LayoutParams cardParams = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.bottomMargin = dp(context, 12);
            card.setLayoutParams(cardParams);

            LinearLayout topRow = new LinearLayout(context);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(Gravity.TOP);
            topRow.setMinimumHeight(dp(context, 40));

            int[] palette = readAccentPalette(context);
            GradientDrawable accentBackground = new GradientDrawable();
            accentBackground.setColor(palette[0]);
            accentBackground.setCornerRadius(dp(context, 2));

            View accentBar = new View(context);
            accentBar.setBackground(accentBackground);
            LinearLayout.LayoutParams accentParams = new LinearLayout.LayoutParams(
                    dp(context, 4), dp(context, 40));
            accentParams.rightMargin = dp(context, 10);
            accentParams.gravity = Gravity.TOP;
            topRow.addView(accentBar, accentParams);

            TextView nameView = new TextView(context);
            nameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
            nameView.setTypeface(null, Typeface.BOLD);
            nameView.setTextColor(ContextCompat.getColor(context, R.color.color_text_primary));
            nameView.setGravity(Gravity.START);
            nameView.setSingleLine(false);
            nameView.setMaxLines(Integer.MAX_VALUE);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            topRow.addView(nameView, nameParams);

            TextView timeView = new TextView(context);
            timeView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            timeView.setTextColor(ContextCompat.getColor(context, R.color.color_text_secondary));
            timeView.setGravity(Gravity.TOP | Gravity.END);
            timeView.setSingleLine(true);
            LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            timeParams.leftMargin = dp(context, 8);
            topRow.addView(timeView, timeParams);

            card.addView(topRow, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView metaView = new TextView(context);
            metaView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            metaView.setTextColor(ContextCompat.getColor(context, R.color.color_text_secondary));
            metaView.setGravity(Gravity.START);
            metaView.setMaxLines(2);
            metaView.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            metaParams.topMargin = dp(context, 6);
            card.addView(metaView, metaParams);

            TextView statusView = new TextView(context);
            statusView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            statusView.setGravity(Gravity.CENTER);
            statusView.setSingleLine(true);
            statusView.setMinimumHeight(dp(context, 44));
            statusView.setPadding(dp(context, 10), dp(context, 3), dp(context, 10), dp(context, 3));
            GradientDrawable statusBackground = new GradientDrawable();
            statusBackground.setCornerRadius(dp(context, 12));
            statusView.setBackground(statusBackground);
            LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            statusParams.topMargin = dp(context, 10);
            card.addView(statusView, statusParams);

            return new MonthTaskViewHolder(card, accentBar, accentBackground, statusBackground,
                    nameView, timeView, metaView, statusView, palette);
        }

        void bind(Map<String, Object> task,
                  Consumer<Map<String, Object>> open,
                  Consumer<Map<String, Object>> statusClick) {
            Context context = itemView.getContext();

            String name = string(task != null ? task.get("name") : null);
            if (name.isEmpty()) name = "未命名任务";
            nameView.setText(name);
            nameView.setTextColor(ContextCompat.getColor(context, R.color.color_text_primary));

            String timeText = formatTimeRange(task);
            timeView.setText(timeText);
            timeView.setTextColor(ContextCompat.getColor(context, R.color.color_text_secondary));

            String deadline = task != null ? DateCompat.toLocalDate(task.get("end_time")) : null;
            String deadlineText = deadline != null && !deadline.isEmpty()
                    ? deadline.replace('-', '/') : "-";
            String assignee = "";
            if (task != null) {
                assignee = string(task.get("assignee_real_name"));
                if (assignee.isEmpty()) assignee = string(task.get("assignee_name"));
            }
            if (assignee.isEmpty()) assignee = "未指派";
            String metaText = "截止 " + deadlineText + " · " + assignee;
            metaView.setText(metaText);
            metaView.setTextColor(ContextCompat.getColor(context, R.color.color_text_secondary));

            String status = string(task != null ? task.get("status") : null);
            boolean completed = "COMPLETED".equals(status) || "CONFIRMED".equals(status);
            String statusText = completed ? "已完成" : "未完成";
            statusBackground.setColor(ContextCompat.getColor(context, completed
                    ? R.color.color_status_completed_bg : R.color.color_status_pending_bg));
            statusView.setText(statusText);
            statusView.setTextColor(ContextCompat.getColor(context, completed
                    ? R.color.color_status_completed_text : R.color.color_status_pending_text));

            int accentIndex = (name.hashCode() & Integer.MAX_VALUE) % accentPalette.length;
            accentBackground.setColor(accentPalette[accentIndex]);
            accentBar.setBackground(accentBackground);

            itemView.setContentDescription(name + "，" + timeText + "，" + metaText
                    + "，" + statusText + "，点击查看详情");

            if (task != null && open != null) {
                itemView.setOnClickListener(v -> open.accept(task));
                itemView.setClickable(true);
            } else {
                itemView.setOnClickListener(null);
                itemView.setClickable(false);
            }

            if (task != null && statusClick != null) {
                statusView.setOnClickListener(v -> statusClick.accept(task));
                statusView.setClickable(true);
                statusView.setFocusable(true);
            } else {
                statusView.setOnClickListener(null);
                statusView.setClickable(false);
                statusView.setFocusable(false);
            }
        }
    }

    private static String formatTimeRange(Map<String, Object> task) {
        if (task == null) return TEXT_TIME_UNSET;
        String start = DateCompat.toLocalDateTime(task.get("start_time"));
        String end = DateCompat.toLocalDateTime(task.get("end_time"));
        if (start == null && end == null) return TEXT_TIME_UNSET;

        String startTime = clock(start);
        String endTime = clock(end);
        if (startTime == null && endTime == null) return TEXT_ALL_DAY;
        if ("00:00".equals(startTime) && "00:00".equals(endTime)) return TEXT_ALL_DAY;
        if (startTime != null && endTime != null) return startTime + " - " + endTime;
        return startTime != null ? startTime : endTime;
    }

    /** DateCompat 的本地时间格式为 yyyy-MM-dd HH:mm，日期串则没有时间部分。 */
    private static String clock(String localDateTime) {
        if (localDateTime == null) return null;
        int separator = localDateTime.indexOf(' ');
        if (separator < 0 || separator + 6 > localDateTime.length()) return null;
        String time = localDateTime.substring(separator + 1, separator + 6);
        if (time.length() != 5 || time.charAt(2) != ':') return null;
        return time;
    }

    private static int[] readAccentPalette(Context context) {
        int fallback = ContextCompat.getColor(context, R.color.color_calendar_today);
        String[] values = context.getResources().getStringArray(R.array.calendar_month_detail_colors);
        if (values.length == 0) return new int[]{fallback};
        int[] colors = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            try {
                colors[i] = Color.parseColor(values[i]);
            } catch (IllegalArgumentException e) {
                colors[i] = fallback;
            }
        }
        return colors;
    }

    private static int dp(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static String string(Object value) {
        return value != null ? value.toString().trim() : "";
    }
}

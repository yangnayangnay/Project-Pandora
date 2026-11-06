package com.example.project_pandora.ui.task;

import android.content.Intent;
import android.content.Context;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.project_pandora.R;
import com.example.project_pandora.core.util.*;
import com.example.project_pandora.model.Task;
import java.util.*;

/** A shared column definition keeps the scrolling header and all task rows aligned. */
final class TaskDetailsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    static final String[] LABELS = {"任务名称", "正文", "标签", "状态", "优先级", "重要程度", "紧急程度", "负责人", "布置人", "开始时间", "截止时间", "创建时间", "更新时间"};
    static final int[] WIDTHS = {180, 260, 220, 96, 80, 96, 96, 120, 120, 152, 152, 152, 152};
    private final List<Map<String, Object>> rows = new ArrayList<>();
    void submit(List<Map<String, Object>> data) { rows.clear(); rows.addAll(data); notifyDataSetChanged(); }
    static int totalWidth(Context context) { int width = 0; for (int value : WIDTHS) width += dp(context, value); return width; }
    static LinearLayout row(Context context, boolean header) {
        LinearLayout row = new LinearLayout(context); row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundColor(ContextCompat.getColor(context, header ? R.color.color_surface_soft : R.color.color_surface));
        for (int i = 0; i < LABELS.length; i++) {
            TextView cell = new TextView(context); cell.setTextSize(header ? 13 : 14); cell.setTextColor(ContextCompat.getColor(context, R.color.color_text_primary));
            cell.setPadding(dp(context, 12), dp(context, 12), dp(context, 12), dp(context, 12)); cell.setGravity(Gravity.CENTER_VERTICAL); cell.setMinHeight(dp(context, 56));
            cell.setMaxLines(header ? 2 : 4); cell.setEllipsize(android.text.TextUtils.TruncateAt.END);
            if (header) { cell.setTypeface(null, Typeface.BOLD); cell.setText(LABELS[i]); }
            row.addView(cell, new LinearLayout.LayoutParams(dp(context, WIDTHS[i]), -2));
        }
        return row;
    }
    @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int type) {
        LinearLayout row = row(parent.getContext(), false); row.setLayoutParams(new RecyclerView.LayoutParams(totalWidth(parent.getContext()), -2));
        android.util.TypedValue ripple = new android.util.TypedValue(); parent.getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, ripple, true);
        row.setForeground(androidx.appcompat.content.res.AppCompatResources.getDrawable(parent.getContext(), ripple.resourceId));
        return new RecyclerView.ViewHolder(row) {};
    }
    @Override public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Map<String, Object> task = rows.get(position); LinearLayout row = (LinearLayout) holder.itemView;
        Task display = new Task(); display.setStatus(Objects.toString(TaskTags.value(task, "status"), "")); display.setPriority(Objects.toString(TaskTags.value(task, "priority"), ""));
        String[] values = {plain(task, "name"), plain(task, "content"), TaskTags.names(task), display.getStatusDisplayName(), display.getPriorityDisplayName(),
                NumberCompat.toBool(TaskTags.value(task, "is_important")) ? "重要" : "不重要", NumberCompat.toBool(TaskTags.value(task, "is_urgent")) ? "紧急" : "不紧急",
                TaskDetailFilter.person(task, "assignee"), TaskDetailFilter.person(task, "dispatcher"), time(task, "start_time"), time(task, "end_time"), time(task, "created_at"), time(task, "updated_at")};
        for (int i = 0; i < values.length; i++) ((TextView) row.getChildAt(i)).setText(values[i].isEmpty() ? "—" : values[i]);
        row.getChildAt(2).setBackgroundColor(ContextCompat.getColor(row.getContext(), R.color.color_surface));
        if (!TaskTags.of(task).isEmpty()) row.getChildAt(2).setBackgroundColor(TaskTagColors.task(row.getContext(), task));
        row.setContentDescription(values[0] + "，" + values[3] + "，负责人 " + values[7] + "，点击查看完整详情");
        row.setOnClickListener(v -> {
            Long id = NumberCompat.toLong(TaskTags.value(task, "id")); if (id == null) return;
            v.getContext().startActivity(new Intent(v.getContext(), TaskDetailActivity.class).putExtra("taskId", id.longValue()));
        });
    }
    @Override public int getItemCount() { return rows.size(); }
    private static String plain(Map<String, Object> task, String key) { return Objects.toString(TaskTags.value(task, key), ""); }
    private static String time(Map<String, Object> task, String key) { return Objects.toString(DateCompat.toLocalDateTime(TaskTags.value(task, key)), "未设置"); }
    private static int dp(Context context, int value) { return Math.round(value * context.getResources().getDisplayMetrics().density); }
}

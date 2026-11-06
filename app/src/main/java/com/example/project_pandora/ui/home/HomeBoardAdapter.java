package com.example.project_pandora.ui.home;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
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
import com.example.project_pandora.core.util.TaskTags;
import com.example.project_pandora.model.Task;
import com.example.project_pandora.ui.task.TaskDetailActivity;
import com.example.project_pandora.ui.task.TaskDetailFilter;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Full-width cards keep long event and task content readable without horizontal scrolling. */
final class HomeBoardAdapter extends RecyclerView.Adapter<HomeBoardAdapter.Holder> {
    private final boolean events;
    private final List<Map<String, Object>> rows = new ArrayList<>();

    HomeBoardAdapter(boolean events) { this.events = events; }

    void submit(List<Map<String, Object>> data) {
        rows.clear(); rows.addAll(data); notifyDataSetChanged();
    }

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        MaterialCardView card = new MaterialCardView(context);
        RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(-1, -2);
        params.bottomMargin = dp(context, 12); card.setLayoutParams(params);
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(context, 16), dp(context, 16), dp(context, 16), dp(context, 16));
        TextView title = text(content, 17, R.color.color_text_primary);
        title.setTypeface(null, Typeface.BOLD);
        TextView status = text(content, 13, R.color.color_mindmap_dispatch_text);
        TextView body = text(content, 15, R.color.color_text_primary);
        TextView tags = text(content, 13, R.color.color_accent);
        TextView details = text(content, 13, R.color.color_text_secondary);
        TextView hint = text(content, 13, R.color.color_accent);
        card.addView(content);
        return new Holder(card, title, status, body, tags, details, hint);
    }

    @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
        Map<String, Object> row = rows.get(position);
        String title;
        if (events) {
            title = plain(row, "content");
            if (title.isEmpty() || title.matches("[0-9]+")) title = "未命名";
            holder.title.setText((position + 1) + ". " + title);
            holder.status.setVisibility(View.GONE); holder.body.setVisibility(View.GONE);
            holder.tags.setVisibility(View.GONE); holder.details.setVisibility(View.GONE); holder.hint.setVisibility(View.GONE);
            holder.itemView.setOnClickListener(null); holder.itemView.setClickable(false); holder.itemView.setFocusable(false);
        } else {
            title = plain(row, "name");
            holder.title.setText(title.isEmpty() ? "未命名任务" : title);
            Task task = new Task(); task.setStatus(plain(row, "status"));
            show(holder.status, task.getStatusDisplayName());
            show(holder.body, plain(row, "content"));
            show(holder.tags, TaskTags.names(row));
            String details = "负责人：" + TaskDetailFilter.person(row, "assignee")
                    + "\n派发人：" + TaskDetailFilter.person(row, "dispatcher")
                    + "\n开始：" + time(row, "start_time") + "\n截止：" + time(row, "end_time");
            String progress = plain(row, "progress_note");
            if (!progress.isEmpty()) details += "\n进度说明：" + progress;
            show(holder.details, details);
            Long id = NumberCompat.toLong(DailyItems.value(row, "id"));
            show(holder.hint, id == null ? "" : "查看任务详情 ›");
            holder.itemView.setOnClickListener(id == null ? null : view -> view.getContext().startActivity(
                    new Intent(view.getContext(), TaskDetailActivity.class).putExtra("taskId", id.longValue())));
            holder.itemView.setClickable(id != null); holder.itemView.setFocusable(id != null);
        }
    }

    @Override public int getItemCount() { return rows.size(); }

    private static TextView text(LinearLayout parent, int size, int color) {
        TextView text = new TextView(parent.getContext());
        text.setTextSize(size); text.setTextColor(ContextCompat.getColor(parent.getContext(), color));
        text.setLineSpacing(dp(parent.getContext(), 3), 1f);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        if (parent.getChildCount() > 0) params.topMargin = dp(parent.getContext(), 10);
        parent.addView(text, params); return text;
    }

    private static void show(TextView view, String value) {
        view.setText(value); view.setVisibility(value.isEmpty() ? View.GONE : View.VISIBLE);
    }
    private static String plain(Map<String, Object> row, String key) {
        return Objects.toString(DailyItems.value(row, key), "").trim();
    }
    private static String time(Map<String, Object> row, String key) {
        return Objects.toString(DateCompat.toLocalDateTime(DailyItems.value(row, key)), "未设置");
    }
    private static int dp(Context context, int size) { return Math.round(size * context.getResources().getDisplayMetrics().density); }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView title, status, body, tags, details, hint;
        Holder(View root, TextView title, TextView status, TextView body, TextView tags, TextView details, TextView hint) {
            super(root); this.title = title; this.status = status; this.body = body;
            this.tags = tags; this.details = details; this.hint = hint;
        }
    }
}

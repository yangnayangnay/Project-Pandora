package com.example.project_pandora.ui.home;

import android.graphics.Paint;
import android.view.*;
import android.widget.*;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.project_pandora.R;
import com.google.android.material.button.MaterialButton;
import java.util.*;
import java.util.function.*;

/** Day checklist: completion and editing have separate, accessible touch targets. */
public final class DailyItemAdapter extends RecyclerView.Adapter<DailyItemAdapter.Holder> {
    private final List<Map<String, Object>> rows = new ArrayList<>();
    private final BiConsumer<Map<String, Object>, Boolean> onComplete;
    private final Consumer<Map<String, Object>> onEdit;
    private final Set<Long> busy;
    public DailyItemAdapter(Set<Long> busy, BiConsumer<Map<String, Object>, Boolean> onComplete, Consumer<Map<String, Object>> onEdit) { this.busy = busy; this.onComplete = onComplete; this.onEdit = onEdit; }
    public void submit(List<Map<String, Object>> values) { rows.clear(); rows.addAll(values); notifyDataSetChanged(); }
    @Override public Holder onCreateViewHolder(ViewGroup parent, int type) {
        android.content.Context context = parent.getContext();
        LinearLayout row = new LinearLayout(context); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(0, dp(parent, 8), 0, dp(parent, 8));
        CheckBox check = new CheckBox(context); row.addView(check, new LinearLayout.LayoutParams(dp(parent, 48), dp(parent, 48)));
        LinearLayout body = new LinearLayout(context); body.setOrientation(LinearLayout.VERTICAL);
        TextView content = new TextView(context); content.setTextSize(15); body.addView(content);
        TextView status = new TextView(context); status.setTextSize(12); status.setTextColor(ContextCompat.getColor(context, R.color.color_text_secondary)); body.addView(status);
        row.addView(body, new LinearLayout.LayoutParams(0, -2, 1));
        MaterialButton edit = new MaterialButton(context, null, com.google.android.material.R.attr.borderlessButtonStyle); edit.setText("编辑"); edit.setMinWidth(0); edit.setMinimumWidth(0); row.addView(edit, new LinearLayout.LayoutParams(dp(parent, 64), dp(parent, 48)));
        row.setLayoutParams(new RecyclerView.LayoutParams(-1, -2)); return new Holder(row, check, content, status, edit);
    }
    @Override public void onBindViewHolder(Holder holder, int position) {
        Map<String, Object> item = rows.get(position); boolean completed = DailyItems.completed(item);
        Long id = com.example.project_pandora.core.util.NumberCompat.toLong(DailyItems.value(item, "id")); boolean writing = busy.contains(id);
        holder.check.setOnCheckedChangeListener(null); holder.check.setChecked(completed); holder.check.setEnabled(!writing);
        holder.check.setContentDescription((completed ? "恢复未完成：" : "标记完成：") + DailyItems.content(item));
        holder.content.setText(DailyItems.content(item)); holder.content.setPaintFlags(completed ? holder.content.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG : holder.content.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        holder.content.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), completed ? R.color.color_text_secondary : R.color.color_text_primary));
        holder.status.setText(writing ? "保存中…" : completed ? "已完成" : "待办"); holder.edit.setEnabled(!writing);
        holder.edit.setContentDescription("编辑事项：" + DailyItems.content(item));
        holder.check.setOnCheckedChangeListener((button, checked) -> onComplete.accept(item, checked)); holder.edit.setOnClickListener(v -> onEdit.accept(item));
    }
    @Override public int getItemCount() { return rows.size(); }
    private static int dp(View view, int value) { return Math.round(value * view.getResources().getDisplayMetrics().density); }
    static final class Holder extends RecyclerView.ViewHolder {
        final CheckBox check; final TextView content, status; final MaterialButton edit;
        Holder(View row, CheckBox check, TextView content, TextView status, MaterialButton edit) { super(row); this.check = check; this.content = content; this.status = status; this.edit = edit; }
    }
}

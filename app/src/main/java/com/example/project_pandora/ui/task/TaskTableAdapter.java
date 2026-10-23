package com.example.project_pandora.ui.task;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project_pandora.R;
import com.example.project_pandora.databinding.ItemTaskTableBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TaskTableAdapter extends RecyclerView.Adapter<TaskTableAdapter.TableViewHolder> {
    private final List<Map<String, Object>> tasks;
    private final long currentUserId;
    private final OnStatusToggleListener statusToggleListener;

    /**
     * Round27 REQ-27-04: 完成状态单元格快速切换回调（null 时状态格点击无响应）
     */
    public interface OnStatusToggleListener {
        void onStatusCellClick(Map<String, Object> task);
    }

    public TaskTableAdapter(List<Map<String, Object>> tasks) {
        this(tasks, -1L, null);
    }

    /**
     * Round24 REQ-24-02: 本人任务行标识重载
     * 负责人 assignee_id == currentUserId 时负责人列红粗显示，否则维持原样
     */
    public TaskTableAdapter(List<Map<String, Object>> tasks, long currentUserId) {
        this(tasks, currentUserId, null);
    }

    /**
     * Round27 REQ-27-04: 状态格快速切换重载（原两构造保持兼容，listener 可空）
     */
    public TaskTableAdapter(List<Map<String, Object>> tasks, long currentUserId, OnStatusToggleListener listener) {
        this.tasks = tasks != null ? tasks : new ArrayList<>();
        this.currentUserId = currentUserId;
        this.statusToggleListener = listener;
    }

    public void clearData() {
        tasks.clear();
        notifyDataSetChanged();
    }

    /**
     * REQ-31-02: 切换成功后局部更新该行"完成情况"单元格（服务器已确认返回，非乐观更新）。
     * 未命中静默（等调用方整体重拉校准兜底）。
     */
    public void updateTaskStatus(long taskId, String newStatus) {
        for (int i = 0; i < tasks.size(); i++) {
            Long id = com.example.project_pandora.core.util.NumberCompat.toLong(tasks.get(i).get("id"));
            if (id != null && id == taskId) {
                tasks.get(i).put("status", newStatus);
                notifyItemChanged(i);
                return;
            }
        }
    }

    @NonNull
    @Override
    public TableViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTaskTableBinding binding = ItemTaskTableBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new TableViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TableViewHolder holder, int position) {
        Map<String, Object> task = tasks.get(position);
        ItemTaskTableBinding b = holder.binding;

        Object name = task.get("name");
        b.textColName.setText(name != null ? name.toString() : "-");

        Object endTime = task.get("end_time");
        // Round25 R25-02: UTC ISO 串按本地时区解析（原 substring 会把 00:00 截成前一天）
        String localDate = com.example.project_pandora.core.util.DateCompat.toLocalDate(endTime);
        String endStr = localDate != null ? localDate.replace("-", "/") : "-";
        b.textColDeadline.setText(endStr);

        boolean isUrgent = com.example.project_pandora.core.util.NumberCompat.toBool(task.get("is_urgent"));
        b.textColUrgency.setText(isUrgent ? "紧急" : "不紧急");
        // REQ-31-05: 状态色资源化（昼夜双套）
        b.textColUrgency.setBackgroundColor(isUrgent
                ? androidx.core.content.ContextCompat.getColor(b.getRoot().getContext(), R.color.color_urgent_bg)
                : androidx.core.content.ContextCompat.getColor(b.getRoot().getContext(), R.color.color_not_urgent_bg));

        String status = str(task.get("status"));
        boolean isCompleted = "COMPLETED".equals(status) || "CONFIRMED".equals(status);
        b.textColCompletion.setText(isCompleted ? "已完成" : "未完成");
        b.textColCompletion.setBackgroundColor(isCompleted
                ? androidx.core.content.ContextCompat.getColor(b.getRoot().getContext(), R.color.color_status_completed_bg)
                : androidx.core.content.ContextCompat.getColor(b.getRoot().getContext(), R.color.color_status_pending_bg));
        b.textColCompletion.setTextColor(isCompleted
                ? androidx.core.content.ContextCompat.getColor(b.getRoot().getContext(), R.color.color_status_completed_text)
                : androidx.core.content.ContextCompat.getColor(b.getRoot().getContext(), R.color.color_status_pending_text));

        Object assignee = task.get("assignee_real_name");
        if (assignee == null || assignee.toString().isEmpty()) assignee = task.get("assignee_name");
        if (assignee == null || assignee.toString().isEmpty()) assignee = task.get("assignee_id");
        b.textColAssignee.setText(assignee != null ? assignee.toString() : "-");

        // Round24 REQ-24-02: 负责人是本人时红粗标识（ViewHolder 复用需双向恢复默认样式）
        // Round26: 解析统一收敛至 NumberCompat
        Long assigneeId = com.example.project_pandora.core.util.NumberCompat.toLong(task.get("assignee_id"));
        if (assigneeId != null && assigneeId == currentUserId) {
            b.textColAssignee.setTypeface(null, android.graphics.Typeface.BOLD);
            b.textColAssignee.setTextColor(androidx.core.content.ContextCompat.getColor(
                    b.getRoot().getContext(), R.color.color_assignee_self));
        } else {
            b.textColAssignee.setTypeface(null, android.graphics.Typeface.NORMAL);
            b.textColAssignee.setTextColor(androidx.core.content.ContextCompat.getColor(
                    b.getRoot().getContext(), R.color.color_assignee_other));
        }

        // Round27 REQ-27-04: 完成状态单元格点击快速切换（子视图优先消费，行其余区域保持跳详情）
        if (statusToggleListener != null) {
            b.textColCompletion.setOnClickListener(v -> statusToggleListener.onStatusCellClick(task));
        } else {
            b.textColCompletion.setOnClickListener(null);
        }

        holder.itemView.setOnClickListener(v -> {
            // Round26: 行 ID 解析收敛至 NumberCompat（原 instanceof Number 判定，串形态 ID 无法跳转）
            Long taskId = com.example.project_pandora.core.util.NumberCompat.toLong(task.get("id"));
            if (taskId != null) {
                Intent intent = new Intent(v.getContext(), TaskDetailActivity.class);
                intent.putExtra("taskId", taskId);
                v.getContext().startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    private static String str(Object obj) {
        return obj != null ? obj.toString() : "";
    }

    static class TableViewHolder extends RecyclerView.ViewHolder {
        final ItemTaskTableBinding binding;

        TableViewHolder(ItemTaskTableBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
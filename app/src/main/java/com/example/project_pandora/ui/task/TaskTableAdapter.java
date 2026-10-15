package com.example.project_pandora.ui.task;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project_pandora.databinding.ItemTaskTableBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TaskTableAdapter extends RecyclerView.Adapter<TaskTableAdapter.TableViewHolder> {
    private final List<Map<String, Object>> tasks;

    public TaskTableAdapter(List<Map<String, Object>> tasks) {
        this.tasks = tasks != null ? tasks : new ArrayList<>();
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
        String endStr = endTime != null ? endTime.toString() : "-";
        if (endStr.length() >= 10) endStr = endStr.substring(0, 10).replace("-", "/");
        b.textColDeadline.setText(endStr);

        boolean isUrgent = toBool(task.get("is_urgent"));
        b.textColUrgency.setText(isUrgent ? "紧急" : "不紧急");
        b.textColUrgency.setBackgroundColor(isUrgent ? 0xFFFF6B6B : 0xFF69DB7C);

        String status = str(task.get("status"));
        boolean isCompleted = "COMPLETED".equals(status) || "CONFIRMED".equals(status);
        b.textColCompletion.setText(isCompleted ? "已完成" : "未完成");
        b.textColCompletion.setBackgroundColor(isCompleted ? 0xFF339AF0 : 0xFFFFD43B);

        Object progress = task.get("progress_note");
        b.textColProgress.setText(progress != null ? progress.toString() : "-");

        Object assignee = task.get("assignee_real_name");
        if (assignee == null || assignee.toString().isEmpty()) assignee = task.get("assignee_name");
        if (assignee == null || assignee.toString().isEmpty()) assignee = task.get("assignee_id");
        b.textColAssignee.setText(assignee != null ? assignee.toString() : "-");

        holder.itemView.setOnClickListener(v -> {
            Object idObj = task.get("id");
            if (idObj instanceof Number) {
                Intent intent = new Intent(v.getContext(), TaskDetailActivity.class);
                intent.putExtra("taskId", ((Number) idObj).longValue());
                v.getContext().startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    private static boolean toBool(Object obj) {
        if (obj == null) return false;
        if (obj instanceof Boolean) return (Boolean) obj;
        if (obj instanceof Number) return ((Number) obj).intValue() != 0;
        return false;
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
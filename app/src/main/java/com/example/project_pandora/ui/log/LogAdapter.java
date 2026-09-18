package com.example.project_pandora.ui.log;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project_pandora.R;
import com.example.project_pandora.databinding.ItemLogBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LogAdapter extends RecyclerView.Adapter<LogAdapter.LogViewHolder> {

    private final List<Map<String, Object>> logs = new ArrayList<>();
    private final OnLogClickListener listener;

    public interface OnLogClickListener {
        void onLogClick(Map<String, Object> log);
        void onLogLongClick(Map<String, Object> log);
    }

    public LogAdapter(OnLogClickListener listener) {
        this.listener = listener;
    }

    public void setLogs(List<Map<String, Object>> newLogs) {
        logs.clear();
        if (newLogs != null) {
            logs.addAll(newLogs);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemLogBinding binding = ItemLogBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new LogViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        Map<String, Object> log = logs.get(position);
        holder.bind(log);
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    class LogViewHolder extends RecyclerView.ViewHolder {
        private final ItemLogBinding binding;

        LogViewHolder(ItemLogBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Map<String, Object> log) {
            Object workItem = log.get("work_item");
            binding.textWorkItem.setText(workItem != null ? workItem.toString() : "");

            Object status = log.get("completion_status");
            String statusText = "进行中";
            int statusColor = 0xFFFF9800;
            if (status != null) {
                switch (status.toString()) {
                    case "COMPLETED":
                        statusText = "已完成";
                        statusColor = 0xFF4CAF50;
                        break;
                    case "NOT_STARTED":
                        statusText = "未开始";
                        statusColor = 0xFF9E9E9E;
                        break;
                }
            }
            binding.textStatus.setText(statusText);
            binding.textStatus.setBackgroundColor(statusColor);
            binding.textStatus.setTextColor(0xFFFFFFFF);

            Object date = log.get("log_date");
            binding.textDate.setText(date != null ? date.toString() : "");

            Object timeCost = log.get("time_cost");
            if (timeCost != null) {
                binding.textTimeCost.setText(timeCost + "h");
            } else {
                binding.textTimeCost.setText("");
            }

            itemView.setOnClickListener(v -> listener.onLogClick(log));
            itemView.setOnLongClickListener(v -> {
                listener.onLogLongClick(log);
                return true;
            });
        }
    }
}
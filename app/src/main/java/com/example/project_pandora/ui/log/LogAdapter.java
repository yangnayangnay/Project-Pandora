package com.example.project_pandora.ui.log;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.project_pandora.R;
import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.databinding.ItemLogBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LogAdapter extends RecyclerView.Adapter<LogAdapter.LogViewHolder> {

    private final List<Map<String, Object>> logs = new ArrayList<>();
    private final OnLogClickListener listener;
    private final long currentUserId;

    public interface OnLogClickListener {
        void onLogClick(Map<String, Object> log);
        void onLogLongClick(Map<String, Object> log);
    }

    public LogAdapter(OnLogClickListener listener, long currentUserId) {
        this.listener = listener;
        this.currentUserId = currentUserId;
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
            // REQ-23-06：本人日志隐藏创建人标记，他人日志显示创建人（数据源混合时规则自动成立）
            if (LogAuthorDisplay.isOwnLog(log, currentUserId)) {
                binding.textAuthor.setVisibility(android.view.View.GONE);
            } else {
                binding.textAuthor.setText(LogAuthorDisplay.resolveAuthorName(log));
                binding.textAuthor.setVisibility(android.view.View.VISIBLE);
            }

            Object workItem = log.get("work_item");
            binding.textWorkItem.setText(workItem != null ? workItem.toString() : "");

            Object workContent = log.get("work_content");
            if (workContent == null) workContent = log.get("WORK_CONTENT");
            if (workContent != null) {
                String contentStr = workContent.toString().trim();
                if (!contentStr.isEmpty()) {
                    binding.textWorkContent.setText(workContent.toString());
                    binding.textWorkContent.setVisibility(android.view.View.VISIBLE);
                } else {
                    binding.textWorkContent.setVisibility(android.view.View.GONE);
                }
            } else {
                binding.textWorkContent.setVisibility(android.view.View.GONE);
            }

            // Round19 REQ-19-09: 移除时间字段展示（后端API不变）

            Object avatar = log.get("avatar");
            if (avatar != null && !avatar.toString().isEmpty()) {
                String baseUrl = NetworkClient.getBaseUrl();
                if (baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
                String fullUrl = baseUrl + avatar;
                Glide.with(binding.imageAvatar.getContext())
                        .load(fullUrl)
                        .apply(RequestOptions.circleCropTransform())
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .into(binding.imageAvatar);
            } else {
                binding.imageAvatar.setImageResource(R.drawable.ic_profile_placeholder);
            }

            itemView.setOnClickListener(v -> listener.onLogClick(log));
            itemView.setOnLongClickListener(v -> {
                listener.onLogLongClick(log);
                return true;
            });
        }
    }
}
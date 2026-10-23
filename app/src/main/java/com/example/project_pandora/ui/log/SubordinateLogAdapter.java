package com.example.project_pandora.ui.log;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.project_pandora.R;
import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.databinding.ItemSubordinateLogBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SubordinateLogAdapter extends RecyclerView.Adapter<SubordinateLogAdapter.SubLogViewHolder> {

    private final List<Map<String, Object>> logs = new ArrayList<>();
    private final long currentUserId;

    public SubordinateLogAdapter(long currentUserId) {
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
    public SubLogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSubordinateLogBinding binding = ItemSubordinateLogBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new SubLogViewHolder(binding, currentUserId);
    }

    @Override
    public void onBindViewHolder(@NonNull SubLogViewHolder holder, int position) {
        holder.bind(logs.get(position));
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    static class SubLogViewHolder extends RecyclerView.ViewHolder {
        private final ItemSubordinateLogBinding binding;
        private final long currentUserId;

        SubLogViewHolder(ItemSubordinateLogBinding binding, long currentUserId) {
            super(binding.getRoot());
            this.binding = binding;
            this.currentUserId = currentUserId;
        }

        void bind(Map<String, Object> log) {
            // REQ-23-06：与他人日志 Tab 规则一致——本人隐藏创建人（防御性：/log/subordinate 理论不含本人）
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
            if (workContent != null && !workContent.toString().trim().isEmpty()) {
                binding.textWorkContent.setText(workContent.toString());
                binding.textWorkContent.setVisibility(android.view.View.VISIBLE);
            } else {
                binding.textWorkContent.setVisibility(android.view.View.GONE);
            }

            Object date = log.get("log_date");
            binding.textDate.setText(date != null ? date.toString() : "");

            Object avatar = log.get("avatar");
            if (avatar != null && !avatar.toString().isEmpty()) {
                String baseUrl = NetworkClient.getBaseUrl();
                if (baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
                String fullUrl = baseUrl + avatar;
                Glide.with(binding.imageAvatar.getContext())
                        .load(fullUrl)
                        .apply(RequestOptions.circleCropTransform())
                        .placeholder(R.mipmap.ic_launcher)
                        .error(R.mipmap.ic_launcher)
                        .into(binding.imageAvatar);
            } else {
                binding.imageAvatar.setImageResource(R.mipmap.ic_launcher);
            }

            itemView.setOnClickListener(v -> {
                Object id = log.get("id");
                if (id == null) id = log.get("ID");
                // Round26: 行 ID 解析收敛至 NumberCompat（原 instanceof Number 判定，串形态 ID 无法跳转）
                Long logId = com.example.project_pandora.core.util.NumberCompat.toLong(id);
                if (logId != null) {
                    Intent intent = new Intent(itemView.getContext(), LogDetailActivity.class);
                    intent.putExtra(LogDetailActivity.EXTRA_LOG_ID, logId);
                    itemView.getContext().startActivity(intent);
                }
            });
        }
    }
}
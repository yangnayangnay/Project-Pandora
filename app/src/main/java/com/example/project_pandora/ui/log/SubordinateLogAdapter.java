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
        return new SubLogViewHolder(binding);
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

        SubLogViewHolder(ItemSubordinateLogBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Map<String, Object> log) {
            Object realName = log.get("real_name");
            Object username = log.get("username");
            String author = realName != null ? realName.toString()
                    : (username != null ? username.toString() : "未知");
            binding.textAuthor.setText(author);

            Object workItem = log.get("work_item");
            binding.textWorkItem.setText(workItem != null ? workItem.toString() : "");

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
                if (id instanceof Number) {
                    Intent intent = new Intent(itemView.getContext(), LogDetailActivity.class);
                    intent.putExtra(LogDetailActivity.EXTRA_LOG_ID, ((Number) id).longValue());
                    itemView.getContext().startActivity(intent);
                }
            });
        }
    }
}
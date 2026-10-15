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

            itemView.setOnClickListener(v -> listener.onLogClick(log));
            itemView.setOnLongClickListener(v -> {
                listener.onLogLongClick(log);
                return true;
            });
        }
    }
}
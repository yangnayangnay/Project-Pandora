package com.example.project_pandora.ui.log;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project_pandora.databinding.ItemTop10Binding;

import java.util.List;
import java.util.Map;

public class Top10Adapter extends RecyclerView.Adapter<Top10Adapter.Top10ViewHolder> {

    private final List<Map<String, Object>> items;

    public Top10Adapter(List<Map<String, Object>> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public Top10ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTop10Binding binding = ItemTop10Binding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new Top10ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull Top10ViewHolder holder, int position) {
        Map<String, Object> item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class Top10ViewHolder extends RecyclerView.ViewHolder {
        private final ItemTop10Binding binding;
        private Map<String, Object> item;

        Top10ViewHolder(ItemTop10Binding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Map<String, Object> item) {
            this.item = item;
            Object rankOrder = item.get("rankOrder");
            binding.textRank.setText("第" + rankOrder + "位");
            String content = (String) item.get("content");
            binding.editContent.setText(content != null ? content : "");

            binding.editContent.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    item.put("content", s.toString());
                }
            });
        }
    }
}
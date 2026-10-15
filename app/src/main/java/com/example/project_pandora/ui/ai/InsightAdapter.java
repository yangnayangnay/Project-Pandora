package com.example.project_pandora.ui.ai;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project_pandora.databinding.ItemInsightBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InsightAdapter extends RecyclerView.Adapter<InsightAdapter.InsightViewHolder> {

    private final List<Map<String, Object>> insights = new ArrayList<>();

    public void setInsights(List<Map<String, Object>> newInsights) {
        insights.clear();
        if (newInsights != null) {
            insights.addAll(newInsights);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public InsightViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemInsightBinding binding = ItemInsightBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new InsightViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull InsightViewHolder holder, int position) {
        Map<String, Object> insight = insights.get(position);
        Object index = insight.get("index");
        if (index == null) index = insight.get("INDEX");
        Object content = insight.get("content");
        if (content == null) content = insight.get("CONTENT");

        holder.binding.textIndex.setText(index != null ? index.toString() + "." : "");
        holder.binding.textContent.setText(content != null ? content.toString() : "");
    }

    @Override
    public int getItemCount() {
        return insights.size();
    }

    static class InsightViewHolder extends RecyclerView.ViewHolder {
        final ItemInsightBinding binding;

        InsightViewHolder(ItemInsightBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
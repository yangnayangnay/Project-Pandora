package com.example.project_pandora.ui.mine;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project_pandora.core.util.RoleDisplayUtil;
import com.example.project_pandora.databinding.ItemUserRelationBinding;
import com.example.project_pandora.model.UserRelationItem;

import java.util.ArrayList;
import java.util.List;

public class UserRelationAdapter extends RecyclerView.Adapter<UserRelationAdapter.ViewHolder> {

    private final List<UserRelationItem> items = new ArrayList<>();

    public void setItems(List<UserRelationItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    public int getItemCount() {
        return items.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemUserRelationBinding binding = ItemUserRelationBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserRelationItem item = items.get(position);
        holder.binding.textName.setText(item.getRealName());
        holder.binding.textRole.setText(RoleDisplayUtil.toChinese(item.getRole()));
        String deptInfo = item.getDepartmentName();
        if (item.getTeamName() != null && !item.getTeamName().isEmpty()) {
            deptInfo = (deptInfo != null && !deptInfo.isEmpty()) ? deptInfo + " / " + item.getTeamName() : item.getTeamName();
        }
        holder.binding.textDept.setText(deptInfo != null ? deptInfo : "");
        holder.binding.textPermLevel.setText("L" + item.getPermissionLevel());
        if (item.getDepth() > 0) {
            holder.binding.textName.setText("  ".repeat(item.getDepth()) + item.getRealName() + " (第" + item.getDepth() + "层)");
        } else {
            holder.binding.textName.setText(item.getRealName());
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemUserRelationBinding binding;

        ViewHolder(ItemUserRelationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
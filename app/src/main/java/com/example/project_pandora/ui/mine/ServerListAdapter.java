package com.example.project_pandora.ui.mine;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project_pandora.R;
import com.example.project_pandora.core.network.ServerEntry;
import com.example.project_pandora.core.network.ServerUrlManager;
import com.example.project_pandora.databinding.ItemServerEntryBinding;

import java.util.ArrayList;
import java.util.List;


/**
 * R44 REQ-A: 服务端列表适配器（生效徽标/启停/上移下移/单条测试/删除）。
 * 交互统一整表 saveServerList（顺序即优先级）；删除守卫由 Activity 层回调实现。
 */
public class ServerListAdapter extends RecyclerView.Adapter<ServerListAdapter.Holder> {

    public interface Listener {
        void onListChanged(List<ServerEntry> newList);
        void onItemTest(String url);
        void onDelete(int position);
        void onError(String message);
    }

    private final List<ServerEntry> entries = new ArrayList<>();
    private final ServerUrlManager manager;
    private final Listener listener;
    private String activeUrl = "";

    public ServerListAdapter(ServerUrlManager manager, Listener listener) {
        this.manager = manager;
        this.listener = listener;
    }

    public void submit(List<ServerEntry> list) {
        entries.clear();
        entries.addAll(list);
        activeUrl = manager.getActiveUrl();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemServerEntryBinding binding = ItemServerEntryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new Holder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        ServerEntry e = entries.get(position);
        h.b.textServerUrl.setText(e.getUrl());
        h.b.badgeActive.setVisibility(e.getUrl().equals(activeUrl) ? View.VISIBLE : View.GONE);
        h.b.switchEnabled.setOnCheckedChangeListener(null);
        h.b.switchEnabled.setChecked(e.isEnabled());
        h.b.switchEnabled.setOnCheckedChangeListener((v, checked) -> {
            int pos = h.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            List<ServerEntry> updated = new ArrayList<>(entries);
            updated.set(pos, new ServerEntry(e.getUrl(), checked));
            persist(updated);
        });
        h.b.btnMoveUp.setOnClickListener(v -> move(h.getAdapterPosition(), -1));
        h.b.btnMoveDown.setOnClickListener(v -> move(h.getAdapterPosition(), 1));
        h.b.btnItemTest.setOnClickListener(v -> {
            int pos = h.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            listener.onItemTest(entries.get(pos).getUrl());
        });
        h.b.btnDelete.setOnClickListener(v -> {
            int pos = h.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            listener.onDelete(pos);
        });
    }

    private void move(int position, int delta) {
        if (position == RecyclerView.NO_POSITION) return;
        int target = position + delta;
        if (target < 0 || target >= entries.size()) return;
        List<ServerEntry> updated = new ArrayList<>(entries);
        ServerEntry moved = updated.remove(position);
        updated.add(target, moved);
        persist(updated);
    }

    private void persist(List<ServerEntry> updated) {
        try {
            manager.saveServerList(updated);
            listener.onListChanged(updated);
            submit(updated);
        } catch (ServerUrlManager.ServerListException ex) {
            listener.onError(ex.getMessage());
        }
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemServerEntryBinding b;

        Holder(ItemServerEntryBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }
    }
}
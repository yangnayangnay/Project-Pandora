package com.example.project_pandora.ui.task;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.project_pandora.databinding.FragmentQuadrantBinding;
import com.example.project_pandora.repository.TaskRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class QuadrantFragment extends Fragment {

    private FragmentQuadrantBinding binding;
    private TaskRepository taskRepository;
    private final CompositeDisposable disposables = new CompositeDisposable();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentQuadrantBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        taskRepository = new TaskRepository(requireContext());

        binding.recyclerQ1.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerQ2.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerQ3.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerQ4.setLayoutManager(new LinearLayoutManager(requireContext()));

        binding.swipeRefresh.setOnRefreshListener(this::loadQuadrant);
        loadQuadrant();
    }

    private void loadQuadrant() {
        disposables.add(taskRepository.getQuadrantTasks()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        data -> {
                            binding.swipeRefresh.setRefreshing(false);
                            binding.recyclerQ1.setAdapter(new SimpleTaskAdapter(getList(data, "importantNotUrgent")));
                            binding.recyclerQ2.setAdapter(new SimpleTaskAdapter(getList(data, "importantUrgent")));
                            binding.recyclerQ3.setAdapter(new SimpleTaskAdapter(getList(data, "notImportantUrgent")));
                            binding.recyclerQ4.setAdapter(new SimpleTaskAdapter(getList(data, "notImportantNotUrgent")));
                        },
                        error -> binding.swipeRefresh.setRefreshing(false)
                ));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getList(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof List) {
            return (List<Map<String, Object>>) value;
        }
        return new ArrayList<>();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposables.clear();
        binding = null;
    }

    private static class SimpleTaskAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final List<Map<String, Object>> tasks;

        SimpleTaskAdapter(List<Map<String, Object>> tasks) {
            this.tasks = tasks != null ? tasks : new ArrayList<>();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setPadding(8, 8, 8, 8);
            tv.setTextSize(13);
            return new RecyclerView.ViewHolder(tv) {};
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Object name = tasks.get(position).get("name");
            ((TextView) holder.itemView).setText(name != null ? name.toString() : "");
        }

        @Override
        public int getItemCount() {
            return tasks.size();
        }
    }
}
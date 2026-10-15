package com.example.project_pandora.ui.task;

import android.os.Bundle;
import android.app.AlertDialog;
import android.content.Intent;
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

import com.example.project_pandora.R;
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

        binding.toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_dispatch_task) {
                startActivity(new Intent(requireContext(), TaskDispatchActivity.class));
                return true;
            }
            if (id == R.id.action_subordinate_tasks) {
                loadSubordinateTasks();
                return true;
            }
            return false;
        });

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
                            binding.recyclerQ1.setAdapter(new SimpleTaskAdapter(getList(data, "importantUrgent")));
                            binding.recyclerQ2.setAdapter(new SimpleTaskAdapter(getList(data, "importantNotUrgent")));
                            binding.recyclerQ3.setAdapter(new SimpleTaskAdapter(getList(data, "notImportantUrgent")));
                            binding.recyclerQ4.setAdapter(new SimpleTaskAdapter(getList(data, "notImportantNotUrgent")));
                        },
                        error -> binding.swipeRefresh.setRefreshing(false)
                ));
    }

    private void loadSubordinateTasks() {
        disposables.add(taskRepository.getSubordinateTasks()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        tasks -> {
                            if (tasks.isEmpty()) {
                                new AlertDialog.Builder(requireContext())
                                        .setTitle("下属任务").setMessage("暂无下属任务")
                                        .setPositiveButton("确定", null).show();
                                return;
                            }
                            StringBuilder sb = new StringBuilder();
                            for (Map<String, Object> task : tasks) {
                                Object name = task.get("name");
                                Object assignee = task.get("assignee_real_name");
                                if (assignee == null) assignee = task.get("assignee_name");
                                Object status = task.get("status");
                                sb.append("• ").append(name != null ? name : "未命名")
                                  .append(" — ").append(assignee != null ? assignee : "未知")
                                  .append(" [").append(status != null ? status : "").append("]\n\n");
                            }
                            new AlertDialog.Builder(requireContext())
                                    .setTitle("下属任务 (" + tasks.size() + "条)")
                                    .setMessage(sb.toString())
                                    .setPositiveButton("确定", null).show();
                        },
                        error -> new AlertDialog.Builder(requireContext())
                                .setTitle("下属任务").setMessage("加载失败: " + error.getMessage())
                                .setPositiveButton("确定", null).show()
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
            Map<String, Object> task = tasks.get(position);
            Object name = task.get("name");
            ((TextView) holder.itemView).setText(name != null ? name.toString() : "");
            holder.itemView.setOnClickListener(v -> {
                Object idObj = task.get("id");
                if (idObj instanceof Number) {
                    Intent intent = new Intent(v.getContext(), TaskDetailActivity.class);
                    intent.putExtra("taskId", ((Number) idObj).longValue());
                    v.getContext().startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            return tasks.size();
        }
    }
}
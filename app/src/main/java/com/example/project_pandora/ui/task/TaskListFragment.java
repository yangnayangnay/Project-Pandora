package com.example.project_pandora.ui.task;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project_pandora.core.network.TokenManager;
import com.example.project_pandora.databinding.FragmentTaskListBinding;
import com.example.project_pandora.repository.TaskRepository;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

public class TaskListFragment extends Fragment {

    private FragmentTaskListBinding binding;
    private TaskRepository taskRepository;
    private TaskListAdapter adapter;
    private String currentStatus = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTaskListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        taskRepository = new TaskRepository(requireContext());
        adapter = new TaskListAdapter(taskId -> {
            Intent intent = new Intent(requireContext(), TaskDetailActivity.class);
            intent.putExtra("taskId", taskId);
            startActivity(intent);
        });

        binding.recyclerTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerTasks.setAdapter(adapter);

        setupTabs();
        binding.swipeRefresh.setOnRefreshListener(this::loadTasks);
        binding.btnCreateTask.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), TaskCreateActivity.class));
        });

        loadTasks();
    }

    private void setupTabs() {
        String[] tabs = {"全部", "待接收", "进行中", "已完成", "已确认"};
        for (String tab : tabs) {
            binding.tabLayout.addTab(binding.tabLayout.newTab().setText(tab));
        }

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: currentStatus = null; break;
                    case 1: currentStatus = "PENDING"; break;
                    case 2: currentStatus = "IN_PROGRESS"; break;
                    case 3: currentStatus = "COMPLETED"; break;
                    case 4: currentStatus = "CONFIRMED"; break;
                }
                loadTasks();
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadTasks() {
        Long userId = TokenManager.getInstance(requireContext()).getUserId();
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.textEmpty.setVisibility(View.GONE);

        taskRepository.listTasks(currentStatus, userId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        tasks -> {
                            binding.progressBar.setVisibility(View.GONE);
                            binding.swipeRefresh.setRefreshing(false);
                            adapter.setTasks(tasks);
                            binding.textEmpty.setVisibility(tasks.isEmpty() ? View.VISIBLE : View.GONE);
                        },
                        error -> {
                            binding.progressBar.setVisibility(View.GONE);
                            binding.swipeRefresh.setRefreshing(false);
                            binding.textEmpty.setVisibility(View.VISIBLE);
                        }
                );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
package com.example.project_pandora.ui.aimap;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.project_pandora.core.network.TokenManager;
import com.example.project_pandora.core.util.BackToolbarHelper;
import com.example.project_pandora.databinding.ActivityTaskShowBinding;
import com.example.project_pandora.repository.TaskRepository;
import com.example.project_pandora.ui.task.TaskCreateActivity;
import com.example.project_pandora.ui.task.TaskTableAdapter;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class TaskShowActivity extends AppCompatActivity {

    private ActivityTaskShowBinding binding;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private TaskRepository taskRepository;
    private Long currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTaskShowBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BackToolbarHelper.setupBack(binding.toolbar, this::finish);

        taskRepository = new TaskRepository(this);
        currentUserId = TokenManager.getInstance(this).getUserId();
        String role = TokenManager.getInstance(this).getRole();

        binding.tabTask.addTab(binding.tabTask.newTab().setText("我的任务"));
        if (role != null && !role.equals("EMPLOYEE")) {
            binding.tabTask.addTab(binding.tabTask.newTab().setText("我布置的任务"));
        }

        binding.tabTask.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                loadTasks(tab.getPosition());
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        binding.fabAddTask.setOnClickListener(v ->
                startActivity(new Intent(this, TaskCreateActivity.class)));

        loadTasks(0);
    }

    private void loadTasks(int tabPosition) {
        if (currentUserId == null || currentUserId < 0) return;

        disposables.add(
                taskRepository.listTasksExtended(null,
                        tabPosition == 0 ? currentUserId : null,
                        tabPosition == 1 ? currentUserId : null,
                        true)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                this::updateTaskTable,
                                error -> {
                                    binding.textEmpty.setVisibility(View.VISIBLE);
                                    binding.scrollTable.setVisibility(View.GONE);
                                }
                        )
        );
    }

    private void updateTaskTable(List<Map<String, Object>> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            binding.textEmpty.setVisibility(View.VISIBLE);
            binding.scrollTable.setVisibility(View.GONE);
            return;
        }

        binding.textEmpty.setVisibility(View.GONE);
        binding.scrollTable.setVisibility(View.VISIBLE);
        binding.recyclerTaskTable.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerTaskTable.setAdapter(new TaskTableAdapter(tasks));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }
}
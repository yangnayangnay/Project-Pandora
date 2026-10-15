package com.example.project_pandora.ui.task;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project_pandora.core.util.BackToolbarHelper;
import com.example.project_pandora.databinding.ActivityCompanyDispatchManageBinding;
import com.example.project_pandora.data.remote.TaskApi;
import com.example.project_pandora.data.remote.UserApi;
import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.repository.Top10Repository;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class CompanyDispatchManageActivity extends AppCompatActivity {

    private ActivityCompanyDispatchManageBinding binding;
    private Top10Repository top10Repository;
    private TaskApi taskApi;
    private UserApi userApi;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final List<Map<String, Object>> currentDispatch = new ArrayList<>();
    private final List<Map<String, Object>> allTasks = new ArrayList<>();
    private final List<Map<String, Object>> allUsers = new ArrayList<>();

    private int currentTab = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCompanyDispatchManageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        top10Repository = new Top10Repository(this);
        taskApi = NetworkClient.getInstance(this).createService(TaskApi.class);
        userApi = NetworkClient.getInstance(this).createService(UserApi.class);

        BackToolbarHelper.setupBack(binding.toolbar, this::finish);

        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("当前十大任务"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("选择已有任务"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("创建新任务"));

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                switchTab();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        setupCreateForm();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        switchTab();
    }

    private void switchTab() {
        if (currentTab == 2) {
            binding.recyclerView.setVisibility(View.GONE);
            binding.createForm.setVisibility(View.VISIBLE);
            loadUsers();
        } else {
            binding.recyclerView.setVisibility(View.VISIBLE);
            binding.createForm.setVisibility(View.GONE);
            if (currentTab == 0) {
                loadCurrentDispatch();
            } else {
                loadAllTasks();
            }
        }
    }

    private void loadCurrentDispatch() {
        disposables.add(top10Repository.getCompanyDispatch()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        list -> {
                            currentDispatch.clear();
                            currentDispatch.addAll(list);
                            showCurrentDispatch();
                        },
                        error -> Toast.makeText(this, "加载失败: " + error.getMessage(), Toast.LENGTH_SHORT).show()
                ));
    }

    private void showCurrentDispatch() {
        List<String> items = new ArrayList<>();
        for (Map<String, Object> item : currentDispatch) {
            Object taskName = item.get("task_name");
            if (taskName == null) taskName = item.get("TASK_NAME");
            Object assigneeName = item.get("assignee_name");
            if (assigneeName == null) assigneeName = item.get("ASSIGNEE_NAME");
            Object rank = item.get("rank_order");
            if (rank == null) rank = item.get("RANK_ORDER");
            String text = rank + ". " + (taskName != null ? taskName : "未命名");
            if (assigneeName != null) text += " (" + assigneeName + ")";
            items.add(text);
        }
        if (items.isEmpty()) items.add("暂无十大任务");
        binding.recyclerView.setAdapter(new SimpleTextAdapter(items, null));
    }

    private void loadAllTasks() {
        disposables.add(taskApi.listTasks(null, null)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        response -> {
                            if (response.isSuccess() && response.getData() != null) {
                                allTasks.clear();
                                allTasks.addAll(response.getData());
                                showAllTasks();
                            }
                        },
                        error -> Toast.makeText(this, "加载失败: " + error.getMessage(), Toast.LENGTH_SHORT).show()
                ));
    }

    private void showAllTasks() {
        List<String> items = new ArrayList<>();
        for (Map<String, Object> task : allTasks) {
            Object name = task.get("name");
            if (name == null) name = task.get("NAME");
            Object id = task.get("id");
            if (id == null) id = task.get("ID");
            items.add((name != null ? name : "未命名") + " [ID:" + id + "]");
        }
        if (items.isEmpty()) items.add("暂无任务");
        binding.recyclerView.setAdapter(new SimpleTextAdapter(items, position -> {
            if (position < allTasks.size()) {
                Map<String, Object> task = allTasks.get(position);
                Object id = task.get("id");
                if (id == null) id = task.get("ID");
                if (id instanceof Number) {
                    markTask(((Number) id).longValue(), currentDispatch.size() + 1);
                }
            }
        }));
    }

    private void markTask(long taskId, int rankOrder) {
        disposables.add(top10Repository.markCompanyDispatch(taskId, rankOrder)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            Toast.makeText(this, "已标记为十大任务", Toast.LENGTH_SHORT).show();
                            binding.tabLayout.getTabAt(0).select();
                        },
                        error -> Toast.makeText(this, "标记失败: " + error.getMessage(), Toast.LENGTH_SHORT).show()
                ));
    }

    private void loadUsers() {
        disposables.add(userApi.getAllUsers()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        response -> {
                            if (response.isSuccess() && response.getData() != null) {
                                allUsers.clear();
                                allUsers.addAll(response.getData());
                                updateAssigneeSpinner();
                            }
                        },
                        error -> {}
                ));
    }

    private void updateAssigneeSpinner() {
        List<String> names = new ArrayList<>();
        for (Map<String, Object> user : allUsers) {
            Object realName = user.get("real_name");
            if (realName == null) realName = user.get("REAL_NAME");
            Object username = user.get("username");
            if (username == null) username = user.get("USERNAME");
            names.add(realName != null ? realName.toString() : (username != null ? username.toString() : "未知"));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerAssignee.setAdapter(adapter);

        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[]{"LOW", "MEDIUM", "HIGH", "URGENT"});
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerPriority.setAdapter(priorityAdapter);
    }

    private void setupCreateForm() {
        binding.btnCreate.setOnClickListener(v -> {
            String name = binding.editTaskName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "任务名称不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            int assigneePos = binding.spinnerAssignee.getSelectedItemPosition();
            if (assigneePos < 0 || assigneePos >= allUsers.size()) {
                Toast.makeText(this, "请选择承接人", Toast.LENGTH_SHORT).show();
                return;
            }
            Map<String, Object> user = allUsers.get(assigneePos);
            Object assigneeId = user.get("id");
            if (assigneeId == null) assigneeId = user.get("ID");
            if (!(assigneeId instanceof Number)) {
                Toast.makeText(this, "承接人ID无效", Toast.LENGTH_SHORT).show();
                return;
            }
            String priority = binding.spinnerPriority.getSelectedItem().toString();
            String endTime = binding.editEndTime.getText().toString().trim();

            disposables.add(top10Repository.createAndMarkCompanyDispatch(
                    name, ((Number) assigneeId).longValue(), priority, currentDispatch.size() + 1, endTime)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            result -> {
                                Toast.makeText(this, "创建并标记成功", Toast.LENGTH_SHORT).show();
                                binding.editTaskName.setText("");
                                binding.editEndTime.setText("");
                                binding.tabLayout.getTabAt(0).select();
                            },
                            error -> Toast.makeText(this, "创建失败: " + error.getMessage(), Toast.LENGTH_SHORT).show()
                    ));
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }

    private static class SimpleTextAdapter extends RecyclerView.Adapter<SimpleTextAdapter.ViewHolder> {
        private final List<String> items;
        private final OnItemClickListener listener;

        interface OnItemClickListener {
            void onItemClick(int position);
        }

        SimpleTextAdapter(List<String> items, OnItemClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setTextSize(14);
            tv.setTextColor(0xFF333333);
            tv.setPadding(16, 12, 16, 12);
            return new ViewHolder(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ((TextView) holder.itemView).setText(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ViewHolder(View itemView) {
                super(itemView);
                if (listener != null) {
                    itemView.setOnClickListener(v -> listener.onItemClick(getAdapterPosition()));
                }
            }
        }
    }
}
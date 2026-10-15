package com.example.project_pandora.ui.task;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project_pandora.databinding.ActivityTaskDispatchBinding;
import com.example.project_pandora.repository.TaskRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import com.example.project_pandora.core.util.BackToolbarHelper;

public class TaskDispatchActivity extends AppCompatActivity {

    private ActivityTaskDispatchBinding binding;
    private TaskRepository taskRepository;
    private Long selectedAssigneeId = null;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private AssigneeAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTaskDispatchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        taskRepository = new TaskRepository(this);

        BackToolbarHelper.setupBack(binding.toolbar, this::finish);
        setupPrioritySpinner();
        binding.btnDispatch.setOnClickListener(v -> handleDispatch());

        adapter = new AssigneeAdapter();
        binding.recyclerAssignees.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerAssignees.setAdapter(adapter);

        loadSubordinateUsers();
    }

    private void loadSubordinateUsers() {
        disposables.add(taskRepository.getSubordinateUsers()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        users -> {
                            adapter.setUsers(users);
                            if (users.isEmpty()) {
                                binding.textSelectAssignee.setVisibility(View.GONE);
                                binding.recyclerAssignees.setVisibility(View.GONE);
                                binding.textNoSubordinates.setVisibility(View.VISIBLE);
                                binding.btnDispatch.setEnabled(false);
                            } else {
                                binding.textSelectAssignee.setVisibility(View.VISIBLE);
                                binding.recyclerAssignees.setVisibility(View.VISIBLE);
                                binding.textNoSubordinates.setVisibility(View.GONE);
                                binding.btnDispatch.setEnabled(true);
                            }
                        },
                        error -> Toast.makeText(this, "加载下属失败: " + error.getMessage(), Toast.LENGTH_SHORT).show()
                ));
    }

    private void setupPrioritySpinner() {
        String[] priorities = {"LOW - 低", "MEDIUM - 中", "HIGH - 高", "URGENT - 紧急"};
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, priorities);
        binding.spinnerPriority.setAdapter(adapter);
    }

    private void handleDispatch() {
        String name = binding.editTaskName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "请输入任务名称", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedAssigneeId == null) {
            Toast.makeText(this, "请选择责任人", Toast.LENGTH_SHORT).show();
            return;
        }

        String priority = "MEDIUM";
        switch (binding.spinnerPriority.getSelectedItemPosition()) {
            case 0: priority = "LOW"; break;
            case 1: priority = "MEDIUM"; break;
            case 2: priority = "HIGH"; break;
            case 3: priority = "URGENT"; break;
        }

        boolean isImportant = binding.checkImportant.isChecked();
        boolean isUrgent = binding.checkUrgent.isChecked();

        binding.btnDispatch.setEnabled(false);
        binding.btnDispatch.setText("分发中...");

        disposables.add(taskRepository.createTask(name, priority, selectedAssigneeId, isImportant, isUrgent, null, null)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            Toast.makeText(this, "任务分发成功", Toast.LENGTH_SHORT).show();
                            finish();
                        },
                        error -> {
                            Toast.makeText(this, "分发失败: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            binding.btnDispatch.setEnabled(true);
                            binding.btnDispatch.setText("分发任务");
                        }
                ));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }

    private class AssigneeAdapter extends RecyclerView.Adapter<AssigneeAdapter.ViewHolder> {
        private final List<Map<String, Object>> users = new ArrayList<>();

        void setUsers(List<Map<String, Object>> users) {
            this.users.clear();
            this.users.addAll(users);
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_single_choice, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Map<String, Object> user = users.get(position);
            Object idObj = user.get("id");
            Object nameObj = user.get("username");
            Object realNameObj = user.get("real_name");
            Object roleObj = user.get("role");

            String displayName = (realNameObj != null ? realNameObj.toString() : nameObj.toString());
            if (roleObj != null) displayName += " (" + com.example.project_pandora.core.util.RoleDisplayUtil.toChinese(roleObj.toString()) + ")";

            Object depthObj = user.get("depth");
            if (depthObj instanceof Number) {
                int depth = ((Number) depthObj).intValue();
                if (depth > 0) {
                    displayName = "  ".repeat(depth) + "└ " + displayName + " [第" + depth + "层]";
                }
            }

            TextView tv = (TextView) holder.itemView;
            tv.setText(displayName);
            tv.setTextSize(15);
            tv.setPadding(32, 24, 32, 24);

            if (idObj != null && idObj.equals(selectedAssigneeId)) {
                tv.setBackgroundColor(0x300000FF);
            } else {
                tv.setBackgroundColor(0);
            }

            holder.itemView.setOnClickListener(v -> {
                if (idObj instanceof Number) {
                    selectedAssigneeId = ((Number) idObj).longValue();
                    notifyDataSetChanged();
                }
            });
        }

        @Override
        public int getItemCount() { return users.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            ViewHolder(View itemView) { super(itemView); }
        }
    }
}

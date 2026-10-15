package com.example.project_pandora.ui.mine;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.core.util.BackToolbarHelper;
import com.example.project_pandora.core.util.RoleDisplayUtil;
import com.example.project_pandora.data.remote.HierarchyApi;
import com.example.project_pandora.data.remote.UserApi;
import com.example.project_pandora.databinding.ActivityHierarchyManageBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class HierarchyManageActivity extends AppCompatActivity {

    private ActivityHierarchyManageBinding binding;
    private HierarchyApi hierarchyApi;
    private UserApi userApi;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final List<Map<String, Object>> allUsers = new ArrayList<>();
    private final List<Map<String, Object>> relations = new ArrayList<>();
    private RelationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHierarchyManageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        hierarchyApi = NetworkClient.getInstance(this).createService(HierarchyApi.class);
        userApi = NetworkClient.getInstance(this).createService(UserApi.class);

        BackToolbarHelper.setupBack(binding.toolbar, this::finish);

        adapter = new RelationAdapter();
        binding.recyclerRelations.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerRelations.setAdapter(adapter);

        binding.btnAddRelation.setOnClickListener(v -> handleAddRelation());

        loadUsersAndRelations();
    }

    private void loadUsersAndRelations() {
        disposables.add(userApi.getAllUsers()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        response -> {
                            if (response.isSuccess() && response.getData() != null) {
                                allUsers.clear();
                                allUsers.addAll(response.getData());
                                setupUserSpinners();
                                loadRelations();
                            }
                        },
                        error -> Toast.makeText(this, "加载用户失败", Toast.LENGTH_SHORT).show()
                ));
    }

    private void setupUserSpinners() {
        List<String> names = new ArrayList<>();
        for (Map<String, Object> user : allUsers) {
            String name = getDisplayName(user);
            names.add(name);
        }
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names);
        binding.spinnerSuperior.setAdapter(spinnerAdapter);
        binding.spinnerSubordinate.setAdapter(spinnerAdapter);
    }

    private void loadRelations() {
        disposables.add(hierarchyApi.getHierarchyTree()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        response -> {
                            if (response.isSuccess() && response.getData() != null) {
                                relations.clear();
                                relations.addAll(response.getData());
                                adapter.notifyDataSetChanged();
                                binding.textNoRelations.setVisibility(relations.isEmpty() ? View.VISIBLE : View.GONE);
                            }
                        },
                        error -> Toast.makeText(this, "加载层级关系失败", Toast.LENGTH_SHORT).show()
                ));
    }

    private void handleAddRelation() {
        int supPos = binding.spinnerSuperior.getSelectedItemPosition();
        int subPos = binding.spinnerSubordinate.getSelectedItemPosition();
        if (supPos < 0 || subPos < 0 || supPos >= allUsers.size() || subPos >= allUsers.size()) {
            Toast.makeText(this, "请选择上级和下级", Toast.LENGTH_SHORT).show();
            return;
        }
        if (supPos == subPos) {
            Toast.makeText(this, "上级和下级不能是同一人", Toast.LENGTH_SHORT).show();
            return;
        }

        Long superiorId = getId(allUsers.get(supPos));
        Long subordinateId = getId(allUsers.get(subPos));

        Map<String, Object> request = new HashMap<>();
        request.put("superiorId", superiorId);
        request.put("subordinateId", subordinateId);
        request.put("forceReplace", false);

        binding.textError.setVisibility(View.GONE);
        binding.btnAddRelation.setEnabled(false);
        binding.btnAddRelation.setText("添加中...");

        disposables.add(hierarchyApi.addRelation(request)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        response -> {
                            binding.btnAddRelation.setEnabled(true);
                            binding.btnAddRelation.setText("添加关系");
                            if (response.isSuccess() && response.getData() != null) {
                                Object success = response.getData().get("success");
                                if (Boolean.FALSE.equals(success)) {
                                    String reason = response.getData().get("reason") != null ? response.getData().get("reason").toString() : "添加失败";
                                    if (reason.contains("替换")) {
                                        confirmReplace(superiorId, subordinateId);
                                    } else {
                                        binding.textError.setText(reason);
                                        binding.textError.setVisibility(View.VISIBLE);
                                    }
                                } else {
                                    Toast.makeText(this, "关系添加成功", Toast.LENGTH_SHORT).show();
                                    loadRelations();
                                }
                            } else {
                                binding.textError.setText(response.getMessage());
                                binding.textError.setVisibility(View.VISIBLE);
                            }
                        },
                        error -> {
                            binding.btnAddRelation.setEnabled(true);
                            binding.btnAddRelation.setText("添加关系");
                            binding.textError.setText("添加失败: " + error.getMessage());
                            binding.textError.setVisibility(View.VISIBLE);
                        }
                ));
    }

    private void confirmReplace(Long superiorId, Long subordinateId) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("确认替换")
                .setMessage("该用户已有直接上级，是否替换为新上级？")
                .setPositiveButton("确认替换", (d, w) -> {
                    Map<String, Object> request = new HashMap<>();
                    request.put("superiorId", superiorId);
                    request.put("subordinateId", subordinateId);
                    request.put("forceReplace", true);
                    disposables.add(hierarchyApi.addRelation(request)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    response -> {
                                        if (response.isSuccess()) {
                                            Toast.makeText(this, "关系替换成功", Toast.LENGTH_SHORT).show();
                                            loadRelations();
                                        }
                                    },
                                    error -> Toast.makeText(this, "替换失败", Toast.LENGTH_SHORT).show()
                            ));
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void handleDeleteRelation(Long superiorId, Long subordinateId) {
        disposables.add(hierarchyApi.removeRelation(superiorId, subordinateId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        response -> {
                            Toast.makeText(this, "关系已删除", Toast.LENGTH_SHORT).show();
                            loadRelations();
                        },
                        error -> Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show()
                ));
    }

    private String getDisplayName(Map<String, Object> user) {
        Object realName = user.get("real_name");
        if (realName == null) realName = user.get("REAL_NAME");
        Object username = user.get("username");
        if (username == null) username = user.get("USERNAME");
        Object role = user.get("role");
        if (role == null) role = user.get("ROLE");
        String name = realName != null ? realName.toString() : (username != null ? username.toString() : "未知");
        if (role != null) name += " (" + RoleDisplayUtil.toChinese(role.toString()) + ")";
        return name;
    }

    private Long getId(Map<String, Object> user) {
        Object id = user.get("id");
        if (id == null) id = user.get("ID");
        if (id instanceof Number) return ((Number) id).longValue();
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }

    private class RelationAdapter extends RecyclerView.Adapter<RelationAdapter.ViewHolder> {

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Map<String, Object> rel = relations.get(position);
            Object supName = rel.get("sup_name");
            Object supRole = rel.get("sup_role");
            Object subName = rel.get("sub_name");
            Object subRole = rel.get("sub_role");
            Object supId = rel.get("superior_id");
            Object subId = rel.get("subordinate_id");

            String line1 = (supName != null ? supName.toString() : "?") + " → " + (subName != null ? subName.toString() : "?");
            String line2 = (supRole != null ? RoleDisplayUtil.toChinese(supRole.toString()) : "") + " → " + (subRole != null ? RoleDisplayUtil.toChinese(subRole.toString()) : "");

            holder.text1.setText(line1);
            holder.text2.setText(line2);

            Long superiorId = supId instanceof Number ? ((Number) supId).longValue() : null;
            Long subordinateId = subId instanceof Number ? ((Number) subId).longValue() : null;

            holder.itemView.setOnClickListener(v -> {
                new android.app.AlertDialog.Builder(HierarchyManageActivity.this)
                        .setTitle("删除关系")
                        .setMessage("确认删除 " + line1 + " ？")
                        .setPositiveButton("删除", (d, w) -> handleDeleteRelation(superiorId, subordinateId))
                        .setNegativeButton("取消", null)
                        .show();
            });
        }

        @Override
        public int getItemCount() {
            return relations.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1;
            TextView text2;

            ViewHolder(View itemView) {
                super(itemView);
                text1 = itemView.findViewById(android.R.id.text1);
                text2 = itemView.findViewById(android.R.id.text2);
            }
        }
    }
}
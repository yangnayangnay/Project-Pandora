package com.example.project_pandora.ui.mine;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
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

/**
 * 人员关系管理页面。
 *
 * <p>TASK-R18-013: 由平铺列表改造为树形结构展示（RecyclerView 自定义树形适配器 + 层级缩进），
 * 数据源从 /hierarchy/relations 切换到 /hierarchy/tree（含 roots + orphanNodes）。</p>
 *
 * <p>TASK-R18-014: 保留 BackToolbarHelper.setupBack 返回栏（项目特色工具，不改动其状态栏适配
 * 与导航图标触达区扩展的实现）；返回时 onDestroy 的 disposables.clear() 覆盖树形适配器新增订阅。</p>
 */
public class HierarchyManageActivity extends AppCompatActivity {

    private ActivityHierarchyManageBinding binding;
    private HierarchyApi hierarchyApi;
    private UserApi userApi;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final List<Map<String, Object>> allUsers = new ArrayList<>();
    /** 树形数据缓存（用于展开/折叠时重建扁平化列表） */
    private List<Map<String, Object>> cachedRoots = new ArrayList<>();
    private List<Map<String, Object>> cachedOrphanNodes = new ArrayList<>();

    private HierarchyTreeAdapter treeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHierarchyManageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        hierarchyApi = NetworkClient.getInstance(this).createService(HierarchyApi.class);
        userApi = NetworkClient.getInstance(this).createService(UserApi.class);

        // TASK-R18-014: 保留 BackToolbarHelper 返回栏（项目特色工具，不改动其实现）
        BackToolbarHelper.setupBack(binding.toolbar, this::finish);

        treeAdapter = new HierarchyTreeAdapter();
        binding.recyclerRelations.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerRelations.setAdapter(treeAdapter);

        // 树节点点击回调：__REBUILD__ 标记表示需要重建树（展开/折叠切换）；
        // 否则为叶子节点点击触发删除关系确认
        treeAdapter.setOnNodeClickListener((superiorId, subordinateId, displayName) -> {
            if ("__REBUILD__".equals(displayName)) {
                // 展开/折叠切换后重建扁平化列表（保持展开状态集合）
                treeAdapter.setTreeData(cachedRoots, cachedOrphanNodes);
            } else if (superiorId != null && subordinateId != null) {
                confirmDeleteRelation(superiorId, subordinateId, displayName);
            }
        });

        binding.btnAddRelation.setOnClickListener(v -> handleAddRelation());

        loadUsersAndTree();
    }

    /** 加载用户列表（用于 Spinner 选择）和树形数据 */
    private void loadUsersAndTree() {
        disposables.add(userApi.getAllUsers()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        response -> {
                            if (response.isSuccess() && response.getData() != null) {
                                allUsers.clear();
                                allUsers.addAll(response.getData());
                                setupUserSpinners();
                                loadTree();
                            }
                        },
                        error -> Toast.makeText(this, com.example.project_pandora.core.util.NetworkErrorClassifier.classify(error), Toast.LENGTH_SHORT).show()
                ));
    }

    /** TASK-R18-013: 加载树形数据（/hierarchy/tree） */
    @SuppressWarnings("unchecked")
    private void loadTree() {
        disposables.add(hierarchyApi.getTree()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        response -> {
                            if (response.isSuccess() && response.getData() != null) {
                                Map<String, Object> data = response.getData();
                                // 解析 roots 和 orphanNodes（双取兜底，lowercaseKeys 转换后为小写）
                                Object rootsObj = data.get("roots");
                                if (rootsObj == null) rootsObj = data.get("ROOTS");
                                Object orphanObj = data.get("orphanNodes");
                                if (orphanObj == null) orphanObj = data.get("orphannodes");
                                if (orphanObj == null) orphanObj = data.get("ORPHANNODES");

                                cachedRoots = rootsObj instanceof List ? (List<Map<String, Object>>) rootsObj : new ArrayList<>();
                                cachedOrphanNodes = orphanObj instanceof List ? (List<Map<String, Object>>) orphanObj : new ArrayList<>();

                                treeAdapter.setTreeData(cachedRoots, cachedOrphanNodes);
                                binding.textNoRelations.setVisibility(treeAdapter.isEmpty() ? View.VISIBLE : View.GONE);
                            } else {
                                binding.textNoRelations.setVisibility(View.VISIBLE);
                            }
                        },
                        error -> {
                            Toast.makeText(this, com.example.project_pandora.core.util.NetworkErrorClassifier.classify(error), Toast.LENGTH_SHORT).show();
                            binding.textNoRelations.setVisibility(View.VISIBLE);
                        }
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
                                    // TASK-R18-013: 添加成功后重新拉取 /hierarchy/tree 重建树并保持展开状态
                                    treeAdapter.markNotFirstLoad();
                                    loadTree();
                                }
                            } else {
                                binding.textError.setText(response.getMessage());
                                binding.textError.setVisibility(View.VISIBLE);
                            }
                        },
                        error -> {
                            binding.btnAddRelation.setEnabled(true);
                            binding.btnAddRelation.setText("添加关系");
                            String errorMsg;
                            if (error instanceof com.example.project_pandora.core.exception.BusinessException) {
                                com.example.project_pandora.core.exception.BusinessException be =
                                        (com.example.project_pandora.core.exception.BusinessException) error;
                                errorMsg = com.example.project_pandora.core.util.ErrorMessageMapper.map(be.getCode(), be.getMessage());
                            } else {
                                errorMsg = com.example.project_pandora.core.util.NetworkErrorClassifier.classify(error);
                            }
                            binding.textError.setText(errorMsg);
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
                                            // TASK-R18-013: 替换成功后重建树并保持展开状态
                                            treeAdapter.markNotFirstLoad();
                                            loadTree();
                                        }
                                    },
                                    error -> Toast.makeText(this, com.example.project_pandora.core.util.NetworkErrorClassifier.classify(error), Toast.LENGTH_SHORT).show()
                            ));
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /** TASK-R18-013: 叶子节点点击触发删除关系确认 */
    private void confirmDeleteRelation(Long superiorId, Long subordinateId, String displayName) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("删除关系")
                .setMessage("确认删除与 " + displayName + " 的上下级关系？")
                .setPositiveButton("删除", (d, w) -> handleDeleteRelation(superiorId, subordinateId))
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
                            // TASK-R18-013: 删除成功后重建树并保持展开状态
                            treeAdapter.markNotFirstLoad();
                            loadTree();
                        },
                        error -> Toast.makeText(this, com.example.project_pandora.core.util.NetworkErrorClassifier.classify(error), Toast.LENGTH_SHORT).show()
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
        // TASK-R18-014: disposables.clear() 覆盖树形适配器新增订阅，避免返回后回调更新已销毁页面
        disposables.clear();
    }
}
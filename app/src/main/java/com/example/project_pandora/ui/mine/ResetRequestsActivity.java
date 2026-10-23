package com.example.project_pandora.ui.mine;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.project_pandora.R;
import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.core.util.BackToolbarHelper;
import com.example.project_pandora.data.remote.UserApi;
import com.example.project_pandora.databinding.ActivityResetRequestsBinding;
import com.example.project_pandora.databinding.ItemResetRequestBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ResetRequestsActivity extends AppCompatActivity {

    private ActivityResetRequestsBinding binding;
    private UserApi userApi;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final List<Map<String, Object>> records = new ArrayList<>();
    private ResetRequestAdapter adapter;
    private String currentStatus = "PENDING";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityResetRequestsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        userApi = NetworkClient.getInstance(this).createService(UserApi.class);

        BackToolbarHelper.setupBack(binding.toolbar, this::finish);
        binding.tabStatus.addTab(binding.tabStatus.newTab().setText("待审批"));
        binding.tabStatus.addTab(binding.tabStatus.newTab().setText("已批准"));
        binding.tabStatus.addTab(binding.tabStatus.newTab().setText("已拒绝"));
        binding.tabStatus.addTab(binding.tabStatus.newTab().setText("全部"));
        binding.tabStatus.addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) {
                int p = tab.getPosition();
                currentStatus = p == 0 ? "PENDING" : p == 1 ? "APPROVED" : p == 2 ? "REJECTED" : null;
                loadData();
            }
            @Override public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
            @Override public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
        });

        adapter = new ResetRequestAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);

        binding.swipeRefresh.setOnRefreshListener(this::loadData);
        loadData();
    }

    private void loadData() {
        binding.progressBar.setVisibility(View.VISIBLE);
        disposables.add(userApi.listResetRequests(currentStatus)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        response -> {
                            binding.progressBar.setVisibility(View.GONE);
                            binding.swipeRefresh.setRefreshing(false);
                            if (response.isSuccess() && response.getData() != null) {
                                List<Map<String, Object>> list = (List<Map<String, Object>>) response.getData().get("records");
                                records.clear();
                                if (list != null) records.addAll(list);
                                adapter.notifyDataSetChanged();
                                if (records.isEmpty()) {
                                    Toast.makeText(this, "暂无记录", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(this, response.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        },
                        error -> {
                            binding.progressBar.setVisibility(View.GONE);
                            binding.swipeRefresh.setRefreshing(false);
                            Toast.makeText(this, "网络错误: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                ));
    }

    private void approve(Long id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("批准重置申请");
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint(R.string.reset_request_custom_pwd);
        builder.setView(input);
        builder.setPositiveButton("确定", (d, w) -> {
            String pwd = input.getText().toString().trim();
            Map<String, String> req = new HashMap<>();
            if (!pwd.isEmpty()) req.put("newPassword", pwd);
            disposables.add(userApi.approveResetRequest(id, req)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            resp -> {
                                if (resp.isSuccess()) {
                                    String msg = resp.getData() != null && resp.getData().get("message") != null
                                            ? resp.getData().get("message").toString() : "已批准";
                                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                                    loadData();
                                } else {
                                    Toast.makeText(this, resp.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            },
                            e -> Toast.makeText(this, "操作失败: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    ));
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void reject(Long id) {
        disposables.add(userApi.rejectResetRequest(id, new HashMap<>())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        resp -> {
                            Toast.makeText(this, resp.isSuccess() ? "已拒绝" : resp.getMessage(), Toast.LENGTH_SHORT).show();
                            loadData();
                        },
                        e -> Toast.makeText(this, "操作失败", Toast.LENGTH_SHORT).show()
                ));
    }

    private class ResetRequestAdapter extends RecyclerView.Adapter<ResetRequestAdapter.VH> {
        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            ItemResetRequestBinding b = ItemResetRequestBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new VH(b);
        }

        @Override
        public void onBindViewHolder(VH h, int position) {
            Map<String, Object> r = records.get(position);
            h.b.textUsername.setText(str(r, "USERNAME"));
            String status = str(r, "STATUS");
            h.b.textStatus.setText(status);
            int color = "PENDING".equals(status) ? 0xFFFF9800 : "APPROVED".equals(status) ? 0xFF4CAF50 : 0xFFF44336;
            h.b.textStatus.setTextColor(color);
            String realName = str(r, "REAL_NAME");
            String email = str(r, "EMAIL");
            h.b.textRealName.setText((realName.isEmpty() ? "" : realName) + (email.isEmpty() ? "" : "  " + email));
            String reason = str(r, "REQUEST_REASON");
            h.b.textReason.setText(reason.isEmpty() ? "（无申请原因）" : "原因: " + reason);
            h.b.textTime.setText(str(r, "CREATED_AT"));
            if ("PENDING".equals(status)) {
                h.b.btnContainer.setVisibility(View.VISIBLE);
                Long id = num(r, "ID");
                h.b.btnApprove.setOnClickListener(v -> approve(id));
                h.b.btnReject.setOnClickListener(v -> reject(id));
            } else {
                h.b.btnContainer.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() { return records.size(); }

        class VH extends RecyclerView.ViewHolder {
            ItemResetRequestBinding b;
            VH(ItemResetRequestBinding b) { super(b.getRoot()); this.b = b; }
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? "" : v.toString();
    }

    private Long num(Map<String, Object> m, String k) {
        // Round26: 解析统一收敛至 NumberCompat（原非 Number 实例一律 null 语义保持）
        return com.example.project_pandora.core.util.NumberCompat.toLong(m.get(k));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }
}
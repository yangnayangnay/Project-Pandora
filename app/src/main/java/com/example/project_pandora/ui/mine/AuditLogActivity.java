package com.example.project_pandora.ui.mine;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.data.remote.AuditApi;
import com.example.project_pandora.databinding.ActivityAuditLogBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

public class AuditLogActivity extends AppCompatActivity {

    private ActivityAuditLogBinding binding;
    private AuditApi auditApi;

    private int currentPage = 1;
    private long totalRecords = 0;
    private final int pageSize = 20;

    private final String[] operationTypes = {
            "全部", "TASK_CREATE", "TASK_PROGRESS_UPDATE", "TASK_TRANSITION",
            "LOG_CREATE", "LOG_UPDATE", "LOG_DELETE", "LOG_AUTO_GENERATE", "TOP10_UPDATE",
            "USER_REGISTER", "USER_APPROVE", "USER_REJECT", "USER_PROFILE_UPDATE", "USER_PASSWORD_CHANGE",
            "PASSWORD_RESET_REQUEST", "PASSWORD_RESET_APPROVE", "PASSWORD_RESET_REJECT", "PASSWORD_RESET_EXECUTE",
            "EMPLOYEE_RECORD_CREATE", "EMPLOYEE_RECORD_UPDATE", "EMPLOYEE_RECORD_DELETE",
            "PERM_INVITATION_CREATE", "PERM_INVITATION_RESPOND", "PERM_AUDIT_RULE_CREATE",
            "AI_MBTI_SUBMIT", "AI_KEYWORD_ANALYSIS", "DATA_SYNC"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAuditLogBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auditApi = NetworkClient.getInstance(this).createService(AuditApi.class);

        binding.spinnerOperationType.setAdapter(
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, operationTypes));

        binding.btnSearch.setOnClickListener(v -> {
            currentPage = 1;
            loadLogs();
        });

        binding.btnPrevPage.setOnClickListener(v -> {
            if (currentPage > 1) { currentPage--; loadLogs(); }
        });

        binding.btnNextPage.setOnClickListener(v -> {
            if (currentPage * pageSize < totalRecords) { currentPage++; loadLogs(); }
        });

        loadLogs();
    }

    private void loadLogs() {
        binding.progressBar.setVisibility(View.VISIBLE);

        Long operatorId = null;
        String opStr = binding.etOperatorId.getText().toString().trim();
        if (!opStr.isEmpty()) {
            try { operatorId = Long.parseLong(opStr); } catch (NumberFormatException ignored) {}
        }

        int typePos = binding.spinnerOperationType.getSelectedItemPosition();
        String operationType = typePos == 0 ? null : operationTypes[typePos];

        String keyword = binding.etKeyword.getText().toString().trim();
        if (keyword.isEmpty()) keyword = null;

        auditApi.listAuditLogs(currentPage, pageSize, operatorId, null, null, operationType, keyword)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        response -> {
                            binding.progressBar.setVisibility(View.GONE);
                            if (response.getData() != null) {
                                displayLogs(response.getData());
                            }
                        },
                        error -> {
                            binding.progressBar.setVisibility(View.GONE);
                        }
                );
    }

    @SuppressWarnings("unchecked")
    private void displayLogs(Map<String, Object> data) {
        List<Map<String, Object>> records = (List<Map<String, Object>>) data.get("records");
        if (records == null) records = new ArrayList<>();

        Object totalObj = data.get("total");
        totalRecords = totalObj instanceof Number ? ((Number) totalObj).longValue() : 0;

        binding.tvPageInfo.setText("第" + currentPage + "页 / 共" + totalRecords + "条");

        AuditLogAdapter adapter = new AuditLogAdapter(records);
        binding.recyclerAuditLogs.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerAuditLogs.setAdapter(adapter);
    }

    private static class AuditLogAdapter extends RecyclerView.Adapter<AuditLogAdapter.LogViewHolder> {
        private final List<Map<String, Object>> items;

        AuditLogAdapter(List<Map<String, Object>> items) {
            this.items = items != null ? items : new ArrayList<>();
        }

        @NonNull
        @Override
        public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout row = new LinearLayout(parent.getContext());
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(24, 16, 24, 16);
            row.setBackgroundColor(0xFFF8F0);
            return new LogViewHolder(row);
        }

        @Override
        public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
            Map<String, Object> item = items.get(position);
            LinearLayout row = (LinearLayout) holder.itemView;
            row.removeAllViews();

            Object opType = item.get("operation_type") != null ? item.get("operation_type") : item.get("OPERATION_TYPE");
            Object action = item.get("action") != null ? item.get("action") : item.get("ACTION");
            Object opContent = item.get("operation_content") != null ? item.get("operation_content") : item.get("OPERATION_CONTENT");
            Object operatorId = item.get("operator_id") != null ? item.get("operator_id") : item.get("OPERATOR_ID");
            Object operatedAt = item.get("operated_at") != null ? item.get("operated_at") : item.get("OPERATED_AT");

            addTextView(row, "操作类型: " + (opType != null ? opType : action));
            addTextView(row, "操作人: " + (operatorId != null ? operatorId : "?"));
            if (opContent != null) addTextView(row, "内容: " + opContent);
            addTextView(row, "时间: " + (operatedAt != null ? operatedAt : ""));
        }

        private void addTextView(LinearLayout parent, String text) {
            TextView tv = new TextView(parent.getContext());
            tv.setText(text);
            tv.setTextSize(13);
            tv.setPadding(0, 4, 0, 4);
            parent.addView(tv);
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class LogViewHolder extends RecyclerView.ViewHolder {
            LogViewHolder(View itemView) { super(itemView); }
        }
    }
}
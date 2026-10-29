package com.example.project_pandora.ui.task;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.project_pandora.R;
import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.core.util.BackToolbarHelper;
import com.example.project_pandora.data.remote.TaskApi;
import com.example.project_pandora.databinding.ActivityTaskReviewsBinding;
import com.example.project_pandora.databinding.ItemTaskReviewBinding;
import com.google.android.material.tabs.TabLayout;
import java.util.*;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.SerialDisposable;

public class TaskReviewsActivity extends AppCompatActivity {
    private ActivityTaskReviewsBinding binding;
    private TaskApi api;
    private boolean mine;
    private boolean deciding;
    private final List<Map<String, Object>> rows = new ArrayList<>();
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final SerialDisposable loading = new SerialDisposable();
    private final ReviewAdapter adapter = new ReviewAdapter();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        binding = ActivityTaskReviewsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom); return insets;
        });
        BackToolbarHelper.setupBack(binding.toolbar, this::finish);
        api = NetworkClient.getInstance(this).createService(TaskApi.class);
        binding.recyclerReviews.setAdapter(adapter);
        binding.tabs.addTab(binding.tabs.newTab().setText("待我审核"));
        binding.tabs.addTab(binding.tabs.newTab().setText("我的申请"));
        binding.tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { mine = tab.getPosition() == 1; load(); }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
        binding.btnRefresh.setOnClickListener(v -> load());
        load();
    }

    private void load() {
        rows.clear(); adapter.notifyDataSetChanged();
        binding.progress.setVisibility(View.VISIBLE); binding.textEmpty.setVisibility(View.GONE);
        loading.set(api.getReviews(mine).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
            .subscribe(response -> {
                binding.progress.setVisibility(View.GONE);
                if (!response.isSuccess()) { showError(response.getMessage()); return; }
                if (response.getData() != null) rows.addAll(response.getData());
                adapter.notifyDataSetChanged();
                binding.textEmpty.setText(mine ? "暂无申请记录" : "暂无待你审核的申请");
                binding.textEmpty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
            }, error -> showError(error.getMessage())));
    }

    private void showError(String message) {
        binding.progress.setVisibility(View.GONE);
        binding.textEmpty.setText("加载失败：" + message + "。请点击刷新重试。");
        binding.textEmpty.setVisibility(View.VISIBLE);
    }

    private void confirm(Map<String, Object> row, boolean approved) {
        if (deciding) return;
        EditText reason = new EditText(this);
        reason.setHint(approved ? "审核说明（选填）" : "请填写退回原因（必填）");
        reason.setMaxLines(5);
        reason.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(500)});
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(approved ? "通过该申请？" : "退回该申请？")
            .setView(reason).setNegativeButton("取消", null).setPositiveButton("确认", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String text = reason.getText().toString().trim();
            if (!approved && text.isEmpty()) { reason.setError("请填写退回原因"); return; }
            Map<String, Object> body = new HashMap<>(); body.put("approved", approved); body.put("reason", text);
            deciding = true; dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            long id = ((Number) row.get("id")).longValue();
            disposables.add(api.review(id, body).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(response -> {
                deciding = false;
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                if (response.isSuccess()) { dialog.dismiss(); Toast.makeText(this, approved ? "已通过，操作生效" : "已退回", Toast.LENGTH_SHORT).show(); load(); }
                else Toast.makeText(this, response.getMessage(), Toast.LENGTH_LONG).show();
            }, error -> { deciding = false; dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true); Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show(); }));
        }));
        dialog.show();
    }

    private String text(Map<String, Object> row, String key) { return Objects.toString(row.get(key), ""); }
    private class ReviewAdapter extends RecyclerView.Adapter<ReviewHolder> {
        @NonNull @Override public ReviewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            return new ReviewHolder(ItemTaskReviewBinding.inflate(getLayoutInflater(), parent, false));
        }
        @Override public int getItemCount() { return rows.size(); }
        @Override @SuppressWarnings("unchecked") public void onBindViewHolder(@NonNull ReviewHolder holder, int position) {
            Map<String, Object> row = rows.get(position);
            Map<String, Object> payload = row.get("payload") instanceof Map ? (Map<String, Object>) row.get("payload") : Collections.emptyMap();
            String operation = text(row, "operation");
            String label = "CREATE".equals(operation) ? "创建任务" : "DELETE".equals(operation) ? "删除任务" : "编辑任务";
            String status = text(row, "status");
            holder.b.textTitle.setText(label + " · " + Objects.toString(payload.get("name"), Objects.toString(row.get("task_name"), "任务申请")));
            holder.b.textMeta.setText("申请人：" + text(row, "requester_name") + " · " + ("PENDING".equals(status) ? "待审核" : "APPROVED".equals(status) ? "已通过" : "已退回") + "\n" + text(row, "created_at"));
            StringBuilder content = new StringBuilder();
            String[][] fields = {{"content", "正文"}, {"assigneeId", "责任人 ID"}, {"priority", "优先级"}, {"startTime", "开始日期"}, {"endTime", "截止日期"}, {"isImportant", "重要"}, {"isUrgent", "紧急"}, {"progressNote", "进度说明"}};
            for (String[] field : fields) if (payload.containsKey(field[0])) content.append(field[1]).append("：").append(payload.get(field[0])).append('\n');
            if (!text(row, "reason").isEmpty()) content.append("审核说明：").append(text(row, "reason")).append('\n');
            if (!text(row, "routing_error").isEmpty()) content.append(text(row, "routing_error")).append('\n');
            if ("PENDING".equals(status)) content.append("审核通过后生效");
            holder.b.textContent.setText(content.toString());
            holder.b.actions.setVisibility(!mine && "PENDING".equals(status) ? View.VISIBLE : View.GONE);
            holder.b.btnApprove.setOnClickListener(v -> confirm(row, true));
            holder.b.btnReject.setOnClickListener(v -> confirm(row, false));
        }
    }
    private static class ReviewHolder extends RecyclerView.ViewHolder {
        final ItemTaskReviewBinding b;
        ReviewHolder(ItemTaskReviewBinding b) { super(b.getRoot()); this.b = b; }
    }
    @Override protected void onDestroy() { loading.dispose(); disposables.dispose(); super.onDestroy(); }
}

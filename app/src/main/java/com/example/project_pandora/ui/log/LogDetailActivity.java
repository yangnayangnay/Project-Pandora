package com.example.project_pandora.ui.log;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.project_pandora.core.network.TokenManager;
import com.example.project_pandora.core.util.BackToolbarHelper;
import com.example.project_pandora.databinding.ActivityLogDetailBinding;
import com.example.project_pandora.repository.LogRepository;


import java.util.Map;
import java.util.List;
import java.util.Collections;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.SerialDisposable;

public class LogDetailActivity extends AppCompatActivity {

    public static final String EXTRA_LOG_ID = "logId";

    private ActivityLogDetailBinding binding;
    private LogRepository logRepository;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final SerialDisposable detailRequest = new SerialDisposable(), commentRequest = new SerialDisposable();
    private final LogCommentAdapter commentAdapter = new LogCommentAdapter();
    private boolean sendingComment, commentsFailed;

    private long logId;
    private String currentRole;
    private Long currentUserId;
    private String editWorkItem;
    private String editWorkContent;

    private String editLogDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLogDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        logId = getIntent().getLongExtra(EXTRA_LOG_ID, -1);
        if (logId <= 0) {
            finish();
            return;
        }

        logRepository = new LogRepository(this);
        TokenManager tm = TokenManager.getInstance(this);
        currentRole = tm.getRole();
        currentUserId = tm.getUserId();

        BackToolbarHelper.setupBack(binding.toolbar, this::finish);
        binding.toolbar.setTitle("日志详情");

        binding.btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(this, LogCreateActivity.class);
            intent.putExtra("editMode", true);
            intent.putExtra("logId", logId);
            intent.putExtra("workItem", editWorkItem);
            intent.putExtra("workContent", editWorkContent);

            intent.putExtra("logDate", editLogDate);
            startActivity(intent);
        });

        binding.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("删除日志")
                    .setMessage("确定删除这条日志吗？")
                    .setPositiveButton("删除", (d, w) -> deleteLog())
                    .setNegativeButton("取消", null)
                    .show();
        });

        binding.recyclerComments.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerComments.setNestedScrollingEnabled(false);
        binding.recyclerComments.setAdapter(commentAdapter);
        binding.btnSendComment.setOnClickListener(v -> sendComment());
        binding.textCommentsState.setOnClickListener(v -> { if (commentsFailed) loadComments(); });
    }

    private void loadDetail() {
        binding.progressBar.setVisibility(View.VISIBLE);
        detailRequest.set(logRepository.getLogDetail(logId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        this::bindDetail,
                        error -> {
                            binding.progressBar.setVisibility(View.GONE);
                            Toast.makeText(this, "加载失败: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                ));
    }

    private void bindDetail(Map<String, Object> log) {
        binding.progressBar.setVisibility(View.GONE);

        Object workItem = log.get("work_item");
        binding.textWorkItem.setText(workItem != null ? workItem.toString() : "");
        editWorkItem = workItem != null ? workItem.toString() : null;

        Object workContent = log.get("work_content");
        String contentStr = workContent != null ? workContent.toString().trim() : "";
        if (contentStr.isEmpty()) {
            binding.textWorkContent.setText("暂无详细内容");
            binding.textWorkContent.setTextColor(androidx.core.content.ContextCompat.getColor(this, com.example.project_pandora.R.color.color_text_secondary));
        } else {
            binding.textWorkContent.setText(workContent.toString());
            binding.textWorkContent.setTextColor(androidx.core.content.ContextCompat.getColor(this, com.example.project_pandora.R.color.color_text_primary));
        }
        editWorkContent = workContent != null ? workContent.toString() : null;

        Object date = log.get("log_date");
        editLogDate = date != null ? date.toString() : null;

        Object username = log.get("username");
        Object realName = log.get("real_name");
        String nameStr = realName != null ? realName.toString() : (username != null ? username.toString() : "");
        binding.textUsername.setText(nameStr);

        Object logUserIdObj = log.get("user_id");
        // Round26: 解析统一收敛至 NumberCompat（原 instanceof Number 判定语义保持）
        Long logUserId = com.example.project_pandora.core.util.NumberCompat.toLong(logUserIdObj);
        boolean isOwner = logUserId != null && logUserId.equals(currentUserId);
        boolean canManage = isOwner;
        boolean writable = com.example.project_pandora.core.util.LogDateWindow.isWritable(editLogDate);
        binding.btnEdit.setEnabled(writable);
        binding.btnEdit.setText(writable ? "编辑" : "超出可编辑日期范围");
        if (!canManage) {
            binding.btnEdit.setVisibility(View.GONE);
            binding.btnDelete.setVisibility(View.GONE);
        }
        loadComments();
    }

    @SuppressWarnings("unchecked")
    private void loadComments() {
        commentsFailed = false;
        binding.progressComments.setVisibility(View.VISIBLE);
        binding.textCommentsState.setVisibility(View.GONE);
        binding.commentComposer.setVisibility(View.GONE);
        commentRequest.set(logRepository.getLogComments(logId).observeOn(AndroidSchedulers.mainThread()).subscribe(data -> {
            binding.progressComments.setVisibility(View.GONE);
            Object raw = data.get("comments");
            List<Map<String, Object>> comments = raw instanceof List ? (List<Map<String, Object>>) raw : Collections.emptyList();
            commentAdapter.setComments(comments);
            binding.textCommentsState.setText("暂无评论");
            binding.textCommentsState.setVisibility(comments.isEmpty() ? View.VISIBLE : View.GONE);
            binding.commentComposer.setVisibility(Boolean.TRUE.equals(data.get("cancomment")) ? View.VISIBLE : View.GONE);
        }, error -> {
            commentsFailed = true; binding.progressComments.setVisibility(View.GONE);
            commentAdapter.setComments(Collections.emptyList());
            binding.textCommentsState.setText("评论加载失败，点击重试"); binding.textCommentsState.setVisibility(View.VISIBLE);
        }));
    }

    private void sendComment() {
        if (sendingComment) return;
        String content = binding.editComment.getText() == null ? "" : binding.editComment.getText().toString().trim();
        if (content.isEmpty()) { binding.commentInputLayout.setError("请输入评论内容"); return; }
        if (content.length() > 2000) { binding.commentInputLayout.setError("评论不能超过2000字"); return; }
        binding.commentInputLayout.setError(null); sendingComment = true;
        binding.btnSendComment.setEnabled(false); binding.btnSendComment.setText("发送中…");
        binding.editComment.setEnabled(false);
        disposables.add(logRepository.addLogComment(logId, content).observeOn(AndroidSchedulers.mainThread()).subscribe(result -> {
            finishCommentSend(); binding.editComment.setText(""); loadComments();
            Toast.makeText(this, "评论已发表", Toast.LENGTH_SHORT).show();
        }, error -> {
            finishCommentSend(); binding.commentInputLayout.setError("发送失败：" + error.getMessage());
            if (error instanceof com.example.project_pandora.core.exception.BusinessException
                    && ((com.example.project_pandora.core.exception.BusinessException) error).getCode() == 403) loadComments();
        }));
    }

    private void finishCommentSend() {
        sendingComment = false; binding.btnSendComment.setEnabled(true); binding.btnSendComment.setText("发表评论");
        binding.editComment.setEnabled(true);
    }

    private void deleteLog() {
        disposables.add(logRepository.deleteLog(logId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        v -> {
                            Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                            finish();
                        },
                        error -> Toast.makeText(this, "删除失败: " + error.getMessage(), Toast.LENGTH_SHORT).show()
                ));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (logId > 0) {
            loadDetail();
        }
    }

    @Override

    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
        detailRequest.dispose(); commentRequest.dispose();
    }
}

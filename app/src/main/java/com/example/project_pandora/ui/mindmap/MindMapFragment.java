package com.example.project_pandora.ui.mindmap;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;


import com.example.project_pandora.core.network.TokenManager;
import com.example.project_pandora.databinding.FragmentMindmapBinding;
import com.example.project_pandora.repository.ViewRepository;
import com.example.project_pandora.ui.log.Top10WorkEditActivity;

import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class MindMapFragment extends Fragment {

    private FragmentMindmapBinding binding;
    private ViewRepository viewRepository;

    private final CompositeDisposable disposables = new CompositeDisposable();
    private String currentRole;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMindmapBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewRepository = new ViewRepository(requireContext());

        currentRole = TokenManager.getInstance(requireContext()).getRole();

        // Round26 B1：个人十大编辑入口（跳转恢复页，编辑对象恒为本人条目，越权由后端 4046 兜底）
        binding.btnEditPersonalTop10.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), Top10WorkEditActivity.class);
            intent.putExtra("workType", "PERSONAL_IMPORTANT");
            startActivity(intent);
        });

        loadMindMapData();
    }

    private void loadMindMapData() {
        disposables.add(viewRepository.getMindMap()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        this::displayMindMap,
                        error -> {
                            if (binding == null) return;
                            String errorMsg;
                            if (error instanceof com.example.project_pandora.core.exception.BusinessException) {
                                com.example.project_pandora.core.exception.BusinessException be =
                                        (com.example.project_pandora.core.exception.BusinessException) error;
                                errorMsg = com.example.project_pandora.core.util.ErrorMessageMapper.map(be.getCode(), be.getMessage());
                            } else {
                                errorMsg = com.example.project_pandora.core.util.NetworkErrorClassifier.classify(error);
                            }
                            showEmpty(binding.listCompanyImportant, errorMsg);
                            showEmpty(binding.listCompanyDispatch, errorMsg);
                            showEmpty(binding.listPersonalImportant, errorMsg);
                            showEmpty(binding.listPersonalLog, errorMsg);
                        }
                ));
    }

    @SuppressWarnings("unchecked")
    private void displayMindMap(Map<String, Object> data) {
        if (binding == null) return;
        List<Map<String, Object>> companyImportant = (List<Map<String, Object>>) data.get("companyImportant");
        List<Map<String, Object>> companyDispatch = (List<Map<String, Object>>) data.get("companyDispatch");
        List<Map<String, Object>> personalImportant = (List<Map<String, Object>>) data.get("personalImportant");
        List<Map<String, Object>> personalLog = (List<Map<String, Object>>) data.get("personalLog");

        populateCompanyImportant(binding.listCompanyImportant, companyImportant);
        populateCompanyDispatch(binding.listCompanyDispatch, companyDispatch);
        populatePersonalImportant(binding.listPersonalImportant, personalImportant);
        populatePersonalLog(binding.listPersonalLog, personalLog);
    }

    private void populateCompanyImportant(LinearLayout container, List<Map<String, Object>> items) {
        container.removeAllViews();
        if (items == null || items.isEmpty()) {
            showEmpty(container, "\u6682\u65E0\u91CD\u8981\u4E8B\u9879");
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            Map<String, Object> item = items.get(i);
            Object content = item.get("content");
            if (content == null) content = item.get("CONTENT");
            // TASK-R18-010: 纯数字/空字符串防御（三层防御之三：展示层兜底）
            // content 为 null、空字符串或纯数字时显示"未命名"占位，保留"序号. "前缀
            String displayContent;
            if (content == null) {
                displayContent = "\u672A\u547D\u540D";
            } else {
                String contentStr = content.toString();
                if (contentStr.trim().isEmpty() || contentStr.trim().matches("^[0-9]+$")) {
                    displayContent = "\u672A\u547D\u540D";
                } else {
                    displayContent = contentStr;
                }
            }
            TextView tv = createItemTextView((i + 1) + ". " + displayContent);
            container.addView(tv);
            if (i < items.size() - 1) container.addView(createDivider());
        }
    }

    private void populateCompanyDispatch(LinearLayout container, List<Map<String, Object>> items) {
        container.removeAllViews();
        if (items == null || items.isEmpty()) {
            showEmpty(container, "\u6682\u65E0\u4EFB\u52A1\u6D3E\u53D1");
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            Map<String, Object> item = items.get(i);
            Object taskName = item.get("task_name");
            if (taskName == null) taskName = item.get("TASK_NAME");
            Object assigneeName = item.get("assignee_name");
            if (assigneeName == null) assigneeName = item.get("ASSIGNEE_NAME");
            Object content = item.get("content");
            if (content == null) content = item.get("CONTENT");

            String display = (i + 1) + ". ";
            if (taskName != null && !taskName.toString().isEmpty()) {
                display += taskName.toString();
            } else if (content != null && !content.toString().isEmpty()) {
                display += content.toString();
            } else {
                display += "\u672A\u547D\u540D";
            }
            if (assigneeName != null && !assigneeName.toString().isEmpty()) {
                display += " (" + assigneeName + ")";
            }
            container.addView(createItemTextView(display));
            if (i < items.size() - 1) container.addView(createDivider());
        }
    }

    private void populatePersonalImportant(LinearLayout container, List<Map<String, Object>> items) {
        container.removeAllViews();
        if (items == null || items.isEmpty()) {
            showEmpty(container, "\u6682\u65E0\u4E2A\u4EBA\u4FE1\u606F");
            return;
        }

        int rank = 1;
        for (Map<String, Object> item : items) {
            Object content = item.get("content");
            if (content == null) content = item.get("CONTENT");
            Object workType = item.get("work_type");
            if (workType == null) workType = item.get("WORK_TYPE");
            Object sourceNote = item.get("source_note");
            if (sourceNote == null) sourceNote = item.get("SOURCE_NOTE");
            String text = rank + ". " + (content != null ? content.toString() : "\u672A\u547D\u540D");
            String label = Top10TypeLabelMapper.resolveLabel(
                    workType != null ? workType.toString() : null,
                    sourceNote != null ? sourceNote.toString() : null);
            if (label != null) text += "\uFF08" + label + "\uFF09";
            TextView tv = createItemTextView(text);
            tv.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), com.example.project_pandora.R.color.color_text_primary));
            container.addView(tv);
            container.addView(createDivider());
            rank++;
        }
    }


    private void populatePersonalLog(LinearLayout container, List<Map<String, Object>> items) {
        container.removeAllViews();
        if (items == null || items.isEmpty()) {
            showEmpty(container, "\u6682\u65E0\u65E5\u5FD7");
            return;
        }
        boolean showAuthor = !"EMPLOYEE".equals(currentRole);
        for (int i = 0; i < items.size(); i++) {
            Map<String, Object> item = items.get(i);
            Object workItem = item.get("work_item");
            if (workItem == null) workItem = item.get("WORK_ITEM");
            Object workContent = item.get("work_content");
            if (workContent == null) workContent = item.get("WORK_CONTENT");
            Object realName = item.get("real_name");
            if (realName == null) realName = item.get("REAL_NAME");
            Object username = item.get("username");
            if (username == null) username = item.get("USERNAME");
            Object logDate = item.get("log_date");
            if (logDate == null) logDate = item.get("LOG_DATE");

            String title = (workItem != null ? workItem.toString() : "\u672A\u547D\u540D");
            // Round27 REQ-27-14: 他人日志"人名:"独立成行，标题+日期换行排列（本人日志保持单行）
            String authorPrefix = null;
            if (showAuthor) {
                String author = realName != null ? realName.toString() : (username != null ? username.toString() : "");
                if (!author.isEmpty()) authorPrefix = author + ":";
            }
            String titleLine = title;
            if (logDate != null) titleLine += " [" + logDate + "]";
            if (authorPrefix != null) {
                container.addView(createLogTitleTextView(authorPrefix));
                container.addView(createLogTitleTextView(titleLine));
            } else {
                container.addView(createLogTitleTextView(titleLine));
            }

            String contentStr = workContent != null ? workContent.toString().trim() : "";
            if (!contentStr.isEmpty()) {
                container.addView(createLogContentTextView(contentStr));
            }
            if (i < items.size() - 1) container.addView(createDivider());
        }
    }

    /**
     * Round24 REQ-24-03: 日志象限标题行——13sp 加粗，无行数上限，
     * 文本规则（作者前缀 + [log_date]）与 Round18 既有行为保持一致
     */
    private TextView createLogTitleTextView(String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(13);
        tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tv.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), com.example.project_pandora.R.color.color_text_primary));
        tv.setPadding(8, 6, 8, 6);
        return tv;
    }

    /**
     * Round24 REQ-24-03: 日志象限内容行——12sp 深棕，
     * 不设 maxLines/ellipsize、不做字数截断（全文完整展示，滚动由外层 NestedScrollView 承载）
     */
    private TextView createLogContentTextView(String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(12);
        tv.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), com.example.project_pandora.R.color.color_text_primary));
        tv.setPadding(8, 0, 8, 6);
        return tv;
    }


    private TextView createItemTextView(String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(12);
        tv.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), com.example.project_pandora.R.color.color_text_primary));
        tv.setPadding(8, 6, 8, 6);
        tv.setMaxLines(2);
        return tv;
    }

    /**
     * Round19 REQ-19-04: 统一分隔横线粗细为 1dp
     */
    private View createDivider() {
        View divider = new View(requireContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        divider.setLayoutParams(lp);
        divider.setBackgroundColor(androidx.core.content.ContextCompat.getColor(requireContext(), com.example.project_pandora.R.color.color_divider));
        return divider;
    }

    private void showEmpty(LinearLayout container, String text) {
        container.removeAllViews();
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(11);
        tv.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), com.example.project_pandora.R.color.color_text_secondary));
        tv.setPadding(8, 16, 8, 16);
        container.addView(tv);
    }


    @Override
    public void onResume() {
        super.onResume();
        loadMindMapData();
        // TASK-R18-017: 导图页 onResume 触发前台即时通知轮询
        com.example.project_pandora.notification.NotificationPollWorker.enqueueOneTimeWork(requireContext());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposables.clear();
        binding = null;
    }
}

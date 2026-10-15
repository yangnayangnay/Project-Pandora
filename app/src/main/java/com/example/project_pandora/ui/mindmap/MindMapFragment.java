package com.example.project_pandora.ui.mindmap;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.project_pandora.R;
import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.core.network.TokenManager;
import com.example.project_pandora.core.util.RoleDisplayUtil;
import com.example.project_pandora.databinding.FragmentMindmapBinding;
import com.example.project_pandora.data.remote.UserApi;
import com.example.project_pandora.repository.LogRepository;
import com.example.project_pandora.repository.ViewRepository;
import com.example.project_pandora.ui.log.Top10WorkEditActivity;
import com.example.project_pandora.ui.task.CompanyDispatchManageActivity;
import com.example.project_pandora.ui.task.TaskDispatchActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class MindMapFragment extends Fragment {

    private FragmentMindmapBinding binding;
    private ViewRepository viewRepository;
    private UserApi userApi;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private String currentRole;
    private boolean canEditCompany = false;

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
        userApi = NetworkClient.getInstance(requireContext()).createService(UserApi.class);
        currentRole = TokenManager.getInstance(requireContext()).getRole();
        canEditCompany = "ADMIN".equals(currentRole) || "FOUNDER".equals(currentRole);

        binding.quadrantBottomLeft.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), Top10WorkEditActivity.class);
            intent.putExtra("workType", "PERSONAL_IMPORTANT");
            startActivity(intent);
        });

        binding.quadrantBottomLeft.setOnLongClickListener(v -> {
            showInjectDialog();
            return true;
        });

        binding.quadrantBottomRight.setOnClickListener(v -> {
            try {
                Navigation.findNavController(v).navigate(R.id.logFragment);
            } catch (Exception e) {
                Intent intent = new Intent(requireContext(), com.example.project_pandora.ui.log.LogCreateActivity.class);
                startActivity(intent);
            }
        });

        binding.quadrantTopLeft.setOnClickListener(v -> {
            if (canEditCompany) {
                Intent intent = new Intent(requireContext(), Top10WorkEditActivity.class);
                intent.putExtra("workType", "COMPANY_IMPORTANT");
                startActivity(intent);
            } else {
                Toast.makeText(requireContext(), "权限不足，仅管理员可操作", Toast.LENGTH_SHORT).show();
            }
        });

        binding.quadrantTopRight.setOnClickListener(v -> {
            if (canEditCompany) {
                startActivity(new Intent(requireContext(), CompanyDispatchManageActivity.class));
            } else {
                Toast.makeText(requireContext(), "权限不足，仅管理员可操作", Toast.LENGTH_SHORT).show();
            }
        });

        binding.fabAdd.setOnClickListener(v -> {
            String[] options = {"录入日志", "创建任务", "自动生成日志"};
            new AlertDialog.Builder(requireContext())
                    .setTitle("快速操作")
                    .setItems(options, (dialog, which) -> {
                        switch (which) {
                            case 0:
                                startActivity(new Intent(requireContext(), com.example.project_pandora.ui.log.LogCreateActivity.class));
                                break;
                            case 1:
                                startActivity(new Intent(requireContext(), com.example.project_pandora.ui.task.TaskCreateActivity.class));
                                break;
                            case 2:
                                performAutoGenerate();
                                break;
                        }
                    })
                    .show();
        });

        applyCompanyEditPermission(canEditCompany);

        loadMindMapData();
    }

    private void applyCompanyEditPermission(boolean canEdit) {
        float alpha = canEdit ? 1.0f : 0.6f;
        binding.quadrantTopLeft.setAlpha(alpha);
        binding.quadrantTopRight.setAlpha(alpha);
    }

    private void loadMindMapData() {
        disposables.add(viewRepository.getMindMap()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        this::displayMindMap,
                        error -> {
                            showEmpty(binding.listCompanyImportant, "\u52A0\u8F7D\u5931\u8D25");
                            showEmpty(binding.listCompanyDispatch, "\u52A0\u8F7D\u5931\u8D25");
                            showEmpty(binding.listPersonalImportant, "\u52A0\u8F7D\u5931\u8D25");
                            showEmpty(binding.listPersonalLog, "\u52A0\u8F7D\u5931\u8D25");
                        }
                ));
    }

    @SuppressWarnings("unchecked")
    private void displayMindMap(Map<String, Object> data) {
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
            TextView tv = createItemTextView((i + 1) + ". " + (content != null ? content.toString() : "\u672A\u547D\u540D"));
            container.addView(tv);
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
        }
    }

    private void populatePersonalImportant(LinearLayout container, List<Map<String, Object>> items) {
        container.removeAllViews();
        if (items == null || items.isEmpty()) {
            showEmpty(container, "\u6682\u65E0\u4E2A\u4EBA\u4FE1\u606F");
            return;
        }

        List<Map<String, Object>> injectedList = new ArrayList<>();
        List<Map<String, Object>> autoList = new ArrayList<>();
        for (Map<String, Object> item : items) {
            Object isAutoFilled = item.get("is_auto_filled");
            if (isAutoFilled == null) isAutoFilled = item.get("IS_AUTO_FILLED");
            if (isAutoFilled != null && Boolean.TRUE.equals(isAutoFilled)) {
                autoList.add(item);
            } else {
                injectedList.add(item);
            }
        }

        int rank = 1;
        for (Map<String, Object> item : injectedList) {
            Object content = item.get("content");
            if (content == null) content = item.get("CONTENT");
            String text = rank + ". " + (content != null ? content.toString() : "\u672A\u547D\u540D");
            TextView tv = createItemTextView(text);
            tv.setTextColor(0xFF333333);

            Object idObj = item.get("id");
            if (idObj == null) idObj = item.get("ID");
            final Long itemId = idObj != null ? ((Number) idObj).longValue() : null;
            tv.setOnLongClickListener(v -> {
                if (itemId != null) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("\u5220\u9664\u6CE8\u5165\u6761\u76EE")
                            .setMessage("\u786E\u5B9A\u5220\u9664\u8BE5\u6761\u4E2A\u4EBA\u5341\u5927\u4FE1\u606F\uFF1F")
                            .setPositiveButton("\u5220\u9664", (d, w) -> deleteInjectedItem(itemId))
                            .setNegativeButton("\u53D6\u6D88", null)
                            .show();
                }
                return true;
            });
            container.addView(tv);
            rank++;
        }

        for (Map<String, Object> item : autoList) {
            Object content = item.get("content");
            if (content == null) content = item.get("CONTENT");
            String text = rank + ". " + (content != null ? content.toString() : "\u672A\u547D\u540D");

            Object sourceNote = item.get("source_note");
            if (sourceNote == null) sourceNote = item.get("SOURCE_NOTE");
            Object sourceLogDate = item.get("source_log_date");
            if (sourceLogDate == null) sourceLogDate = item.get("SOURCE_LOG_DATE");

            if (sourceLogDate != null && !sourceLogDate.toString().isEmpty()) {
                text += " [\u6765\u6E90:" + sourceLogDate + "]";
            } else if (sourceNote != null && !sourceNote.toString().isEmpty()) {
                text += " [\u81EA\u52A8]";
            } else {
                text += " [\u81EA\u52A8]";
            }

            TextView tv = createItemTextView(text);
            tv.setTextColor(0xFF6750A4);

            Object autoType = item.get("auto_type");
            if (autoType != null && "BIRTHDAY_WISH".equals(autoType.toString())) {
                tv.setTextSize(13);
                tv.setOnClickListener(v -> showBirthdayPicker());
            }
            container.addView(tv);
            rank++;
        }
    }

    private void showInjectDialog() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        EditText etRank = new EditText(requireContext());
        etRank.setHint("\u6392\u5E8F\u53F7(1-10)");
        etRank.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(etRank);

        EditText etContent = new EditText(requireContext());
        etContent.setHint("\u5185\u5BB9");
        layout.addView(etContent);

        new AlertDialog.Builder(requireContext())
                .setTitle("\u6CE8\u5165\u4E2A\u4EBA\u5341\u5927\u4FE1\u606F")
                .setView(layout)
                .setPositiveButton("\u6CE8\u5165", (d, w) -> {
                    String rankStr = etRank.getText().toString().trim();
                    String content = etContent.getText().toString().trim();
                    if (content.isEmpty()) {
                        Toast.makeText(requireContext(), "\u5185\u5BB9\u4E0D\u80FD\u4E3A\u7A7A", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int rankOrder = 1;
                    try {
                        if (!rankStr.isEmpty()) rankOrder = Integer.parseInt(rankStr);
                    } catch (NumberFormatException ignored) {}
                    injectPersonalItem(rankOrder, content);
                })
                .setNegativeButton("\u53D6\u6D88", null)
                .show();
    }

    private void injectPersonalItem(int rankOrder, String content) {
        Map<String, Object> request = new HashMap<>();
        request.put("rankOrder", rankOrder);
        request.put("content", content);
        disposables.add(viewRepository.injectPersonalTop10(request)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            Toast.makeText(requireContext(), "\u6CE8\u5165\u6210\u529F", Toast.LENGTH_SHORT).show();
                            loadMindMapData();
                        },
                        error -> Toast.makeText(requireContext(), "\u6CE8\u5165\u5931\u8D25: " + error.getMessage(), Toast.LENGTH_SHORT).show()
                ));
    }

    private void deleteInjectedItem(Long id) {
        disposables.add(viewRepository.deletePersonalTop10(id)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            Toast.makeText(requireContext(), "\u5220\u9664\u6210\u529F", Toast.LENGTH_SHORT).show();
                            loadMindMapData();
                        },
                        error -> Toast.makeText(requireContext(), "\u5220\u9664\u5931\u8D25: " + error.getMessage(), Toast.LENGTH_SHORT).show()
                ));
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
            Object realName = item.get("real_name");
            if (realName == null) realName = item.get("REAL_NAME");
            Object username = item.get("username");
            if (username == null) username = item.get("USERNAME");
            Object logDate = item.get("log_date");
            if (logDate == null) logDate = item.get("LOG_DATE");

            String text = (workItem != null ? workItem.toString() : "\u672A\u547D\u540D");
            if (showAuthor) {
                String author = realName != null ? realName.toString() : (username != null ? username.toString() : "");
                if (!author.isEmpty()) text = author + ": " + text;
            }
            if (logDate != null) text += " [" + logDate + "]";
            container.addView(createItemTextView(text));
        }
    }

    private void showBirthdayPicker() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(requireContext(),
                (view, year, month, day) -> {
                    String birthday = String.format("%04d-%02d-%02d", year, month + 1, day);
                    updateBirthday(birthday);
                },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    private void updateBirthday(String birthday) {
        Map<String, Object> request = new HashMap<>();
        request.put("birthday", birthday);
        disposables.add(userApi.updateBirthday(request)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            Toast.makeText(requireContext(), "\u751F\u65E5\u5DF2\u8BBE\u7F6E", Toast.LENGTH_SHORT).show();
                            loadMindMapData();
                        },
                        error -> Toast.makeText(requireContext(), "\u8BBE\u7F6E\u5931\u8D25: " + error.getMessage(), Toast.LENGTH_SHORT).show()
                ));
    }

    private TextView createItemTextView(String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(12);
        tv.setTextColor(0xFF333333);
        tv.setPadding(8, 6, 8, 6);
        tv.setMaxLines(2);
        return tv;
    }

    private void showEmpty(LinearLayout container, String text) {
        container.removeAllViews();
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(11);
        tv.setTextColor(0xFF999999);
        tv.setPadding(8, 16, 8, 16);
        container.addView(tv);
    }

    private void performAutoGenerate() {
        LogRepository logRepository = new LogRepository(requireContext());
        disposables.add(logRepository.autoGenerateLogs()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            Toast.makeText(requireContext(), "自动生成完成", Toast.LENGTH_SHORT).show();
                            loadMindMapData();
                        },
                        error -> Toast.makeText(requireContext(), "生成失败: " + error.getMessage(), Toast.LENGTH_SHORT).show()
                ));
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMindMapData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposables.clear();
        binding = null;
    }
}

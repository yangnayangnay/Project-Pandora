package com.example.project_pandora.ui.log;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.project_pandora.R;
import com.example.project_pandora.core.config.LogDisplayLimitConfig;
import com.example.project_pandora.core.network.TokenManager;
import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.data.remote.UserApi;
import com.example.project_pandora.databinding.FragmentLogBinding;
import com.example.project_pandora.repository.LogRepository;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.SerialDisposable;

public class LogFragment extends Fragment {

    private static final int TAB_MY_LOG = 0;
    private static final int TAB_OTHER_LOG = 1;

    private FragmentLogBinding binding;
    private LogRepository logRepository;
    private LogAdapter logAdapter;
    private SubordinateLogAdapter subordinateLogAdapter;
    private LogDisplayLimitConfig logDisplayLimitConfig;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final SerialDisposable logLoad = new SerialDisposable();
    private final SerialDisposable directoryLoad = new SerialDisposable();
    private final SerialDisposable roleRefresh = new SerialDisposable();

    private String currentRole;
    private boolean isMyLogTab = true;
    private boolean hasOtherLogTab = false;
    private boolean keyboardVisible = false;
    private long currentUserId = -1L;

    private final List<Map<String, Object>> subordinateList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLogBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        logRepository = new LogRepository(requireContext());
        logDisplayLimitConfig = new LogDisplayLimitConfig(requireContext());
        currentRole = TokenManager.getInstance(requireContext()).getRole();
        Long uid = TokenManager.getInstance(requireContext()).getUserId();
        currentUserId = uid != null ? uid : -1L;
        isMyLogTab = true;
        keyboardVisible = false;

        setupAdapters();
        setupTabs();
        setupSearch();
        setupFab();
        setupSwipeRefresh();
        setupKeyboardFabToggle();
        setHasOptionsMenu(true);
        setupToolbarMenu();

        if (binding.tabLayout.getTabCount() > 0) {
            binding.tabLayout.getTabAt(TAB_MY_LOG).select();
        }
    }

    private void setupAdapters() {
        logAdapter = new LogAdapter(new LogAdapter.OnLogClickListener() {
            @Override
            public void onLogClick(Map<String, Object> log) {
                // Round26: 行 ID 解析收敛至 NumberCompat（原 instanceof Number 判定，串形态 ID 无法跳转）
                Long logId = com.example.project_pandora.core.util.NumberCompat.toLong(log.get("id"));
                if (logId != null) {
                    Intent intent = new Intent(requireContext(), LogDetailActivity.class);
                    intent.putExtra(LogDetailActivity.EXTRA_LOG_ID, logId);
                    startActivity(intent);
                }
            }

            @Override
            public void onLogLongClick(Map<String, Object> log) {
                Long logId = com.example.project_pandora.core.util.NumberCompat.toLong(log.get("id"));
                if (logId != null) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("删除日志")
                            .setMessage("确定删除这条日志吗？")
                            .setPositiveButton("删除", (d, w) -> deleteLog(logId))
                            .setNegativeButton("取消", null)
                            .show();
                }
            }
        }, currentUserId);

        binding.recyclerLogs.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerLogs.setAdapter(logAdapter);

        subordinateLogAdapter = new SubordinateLogAdapter(currentUserId);
        binding.recyclerSubordinateLogs.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerSubordinateLogs.setAdapter(subordinateLogAdapter);
    }

    private void setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("我的日志"));

        hasOtherLogTab = canViewOtherLogs();
        if (hasOtherLogTab) {
            binding.tabLayout.addTab(binding.tabLayout.newTab().setText("他人日志"));
        }

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == TAB_MY_LOG) {
                    isMyLogTab = true;
                    binding.recyclerLogs.setVisibility(View.VISIBLE);
                    binding.recyclerSubordinateLogs.setVisibility(View.GONE);
                    binding.spinnerSubordinate.setVisibility(View.GONE);
                    updateFabVisibility();
                    clearSearchInputs();
                    applyFilters();
                } else {
                    isMyLogTab = false;
                    binding.recyclerLogs.setVisibility(View.GONE);
                    binding.recyclerSubordinateLogs.setVisibility(View.VISIBLE);
                    binding.spinnerSubordinate.setVisibility(View.VISIBLE);
                    updateFabVisibility();
                    clearSearchInputs();
                    loadSubordinateList();
                    applyFilters();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private boolean canViewOtherLogs() {
        return "ADMIN".equals(currentRole) || "FOUNDER".equals(currentRole)
                || "DEPT_HEAD".equals(currentRole) || "TEAM_LEADER".equals(currentRole);
    }

    private void refreshRoleTabs() {
        boolean allowed = canViewOtherLogs();
        if (allowed == hasOtherLogTab) return;
        hasOtherLogTab = allowed;
        directoryLoad.set(null);
        subordinateList.clear();
        updateSubordinateSpinner();
        if (allowed) {
            binding.tabLayout.addTab(binding.tabLayout.newTab().setText("他人日志"));
        } else {
            binding.tabLayout.getTabAt(TAB_MY_LOG).select();
            binding.tabLayout.removeTabAt(TAB_OTHER_LOG);
            subordinateLogAdapter.setLogs(new ArrayList<>());
        }
    }

    private void setupSearch() {
        binding.editStartDate.setOnClickListener(v -> pickFilterDate(binding.editStartDate));
        binding.editEndDate.setOnClickListener(v -> pickFilterDate(binding.editEndDate));
        binding.btnSearch.setOnClickListener(v -> applyFilters());
        binding.btnResetFilters.setOnClickListener(v -> {
            clearSearchInputs(); binding.spinnerSubordinate.setSelection(0);
            binding.chipFilterToday.setChecked(false); binding.chipFilterWeek.setChecked(false); binding.chipFilterAll.setChecked(true);
            applyFilters();
        });
        binding.chipFilterToday.setOnClickListener(v -> setDateRange(0));
        binding.chipFilterWeek.setOnClickListener(v -> setDateRange(6));
        binding.chipFilterAll.setOnClickListener(v -> { binding.editStartDate.setText(""); binding.editEndDate.setText(""); applyFilters(); });
        binding.spinnerSubordinate.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) { if (!isMyLogTab) applyFilters(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void pickFilterDate(android.widget.EditText field) {
        Calendar now = Calendar.getInstance();
        android.app.DatePickerDialog picker = new android.app.DatePickerDialog(requireContext(), (view, year, month, day) -> {
            field.setText(String.format(Locale.ROOT, "%04d-%02d-%02d", year, month + 1, day));
            binding.chipFilterToday.setChecked(false); binding.chipFilterWeek.setChecked(false); binding.chipFilterAll.setChecked(false);
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
        picker.show();
    }

    private void setDateRange(int days) {
        binding.editStartDate.setText(com.example.project_pandora.core.util.LogDateWindow.daysAgo(days));
        binding.editEndDate.setText(com.example.project_pandora.core.util.LogDateWindow.daysAgo(0));
        applyFilters();
    }

    private void applyFilters() {
        if (binding == null) return;
        String start = binding.editStartDate.getText().toString().trim();
        String end = binding.editEndDate.getText().toString().trim();
        String keyword = binding.editKeyword.getText().toString().trim();
        if (!start.isEmpty() && !end.isEmpty() && start.compareTo(end) > 0) {
            Toast.makeText(requireContext(), "开始日期不能晚于结束日期", Toast.LENGTH_SHORT).show();
            binding.swipeRefresh.setRefreshing(false); return;
        }
        binding.textFilterSummary.setText((isMyLogTab ? "我的日志" : "授权范围内的他人日志")
                + " · " + (start.isEmpty() ? "不限起始日期" : start) + " 至 " + (end.isEmpty() ? "不限结束日期" : end)
                + (keyword.isEmpty() ? "" : " · 关键词：" + keyword));
        if (isMyLogTab) loadMyLogs(start.isEmpty() ? null : start, end.isEmpty() ? null : end, keyword.isEmpty() ? null : keyword);
        else loadSubordinateLogs(getSelectedSubordinateId(), start.isEmpty() ? null : start, end.isEmpty() ? null : end, keyword.isEmpty() ? null : keyword);
    }

    private void setupToolbarMenu() {
        binding.toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_stat) {
                showLogStats();
                return true;
            } else if (id == R.id.action_template) {
                navigateToLogEntry();
                return true;
            }
            return false;
        });
    }

    private void setupFab() {
        binding.fabAddLog.setOnClickListener(v -> navigateToLogEntry());
    }

    /** REQ-31-07: FAB/模板入口当日路由防双击标志 */
    private boolean logEntryNavigating = false;

    /**
     * REQ-31-07: "添加日志"统一入口路由——当日已有本人日志（唯一约束至多一条）跳编辑模式，
     * 无/查询失败降级新建（点击时单独查询，规避列表截断与搜索态数据替换风险）。
     * 顺带根治 DEF-31-01：入口侧消除当日重复新建撞 4030 的场景。
     */
    private void navigateToLogEntry() {
        if (logEntryNavigating) return;
        logEntryNavigating = true;
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(new java.util.Date());
        disposables.add(logRepository.listLogs(today, today, null)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        logs -> {
                            logEntryNavigating = false;
                            Intent intent = new Intent(requireContext(), LogCreateActivity.class);
                            if (logs != null && !logs.isEmpty()) {
                                Map<String, Object> todayLog = logs.get(0);
                                intent.putExtra("editMode", true);
                                intent.putExtra("logId", com.example.project_pandora.core.util.NumberCompat.toLong(todayLog.get("id")));
                                intent.putExtra("workItem", todayLog.get("work_item") != null ? todayLog.get("work_item").toString() : "");
                                intent.putExtra("workContent", todayLog.get("work_content") != null ? todayLog.get("work_content").toString() : "");
                                intent.putExtra("logDate", today);
                            }
                            startActivity(intent);
                        },
                        error -> {
                            logEntryNavigating = false;
                            startActivity(new Intent(requireContext(), LogCreateActivity.class));
                        }
                ));
    }

    private void setupKeyboardFabToggle() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            boolean kb = insets.isVisible(WindowInsetsCompat.Type.ime())
                    && insets.getInsets(WindowInsetsCompat.Type.ime()).bottom > 0;
            if (kb != keyboardVisible) {
                keyboardVisible = kb;
                updateFabVisibility();
            }
            return insets;
        });
    }

    private void updateFabVisibility() {
        if (binding == null) return;
        if (keyboardVisible || !isMyLogTab) {
            binding.fabAddLog.setVisibility(View.GONE);
        } else {
            binding.fabAddLog.setVisibility(View.VISIBLE);
        }
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(this::applyFilters);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_stat) {
            showLogStats();
            return true;
        } else if (id == R.id.action_template) {
            navigateToLogEntry();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showLogStats() {
        disposables.add(logRepository.getLogStats()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        stats -> {
                            StringBuilder sb = new StringBuilder();
                            Object total = stats.get("total");
                            if (total == null) total = stats.get("TOTAL");
                            Object pending = stats.get("pending");
                            if (pending == null) pending = stats.get("PENDING");
                            Object completed = stats.get("completed");
                            if (completed == null) completed = stats.get("COMPLETED");
                            Object totalTime = stats.get("totalTime");
                            if (totalTime == null) totalTime = stats.get("TOTAL_TIME");
                            sb.append("总日志数: ").append(total != null ? total : 0).append("\n");
                            sb.append("待完成: ").append(pending != null ? pending : 0).append("\n");
                            sb.append("已完成: ").append(completed != null ? completed : 0).append("\n");
                            sb.append("总工时: ").append(totalTime != null ? totalTime : 0).append("小时");
                            new AlertDialog.Builder(requireContext())
                                    .setTitle("日志统计")
                                    .setMessage(sb.toString())
                                    .setPositiveButton("确定", null)
                                    .show();
                        },
                        error -> Toast.makeText(requireContext(), "统计加载失败: " + error.getMessage(), Toast.LENGTH_SHORT).show()
                ));
    }

    private void clearSearchInputs() {
        binding.editStartDate.setText("");
        binding.editEndDate.setText("");
        binding.editKeyword.setText("");
    }

    private void loadMyLogs(String startDate, String endDate, String keyword) {
        boolean isUnsearched = startDate == null && endDate == null && keyword == null;
        String effectiveStartDate = applyDisplayLimitIfNeeded(startDate, isUnsearched);
        logLoad.set(logRepository.listLogs(effectiveStartDate, endDate, keyword)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        logs -> {
                            if (binding == null) return;
                            binding.swipeRefresh.setRefreshing(false);
                            List<Map<String, Object>> displayLogs = truncateByCountLimit(logs, isUnsearched);
                            logAdapter.setLogs(displayLogs);
                            binding.textEmpty.setVisibility(displayLogs.isEmpty() ? View.VISIBLE : View.GONE);
                            binding.textEmpty.setText("暂无日志");
                        },
                        error -> {
                            if (binding == null) return;
                            binding.swipeRefresh.setRefreshing(false);
                            Toast.makeText(requireContext(), "加载失败: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                ));
    }

    private String applyDisplayLimitIfNeeded(String startDate, boolean isUnsearched) {
        if (!isUnsearched) {
            return startDate;
        }
        int timeLimitDays = logDisplayLimitConfig.getTimeLimitDays();
        if (timeLimitDays == 0) {
            return null;
        }
        try {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -timeLimitDays);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            return sdf.format(cal.getTime());
        } catch (Exception e) {
            return null;
        }
    }

    private List<Map<String, Object>> truncateByCountLimit(List<Map<String, Object>> logs, boolean isUnsearched) {
        if (!isUnsearched) {
            return logs;
        }
        int countLimit = logDisplayLimitConfig.getCountLimit();
        if (countLimit == 0 || logs.size() <= countLimit) {
            return logs;
        }
        return new ArrayList<>(logs.subList(0, countLimit));
    }

    private void loadSubordinateList() {
        directoryLoad.set(logRepository.getSubordinates()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        list -> {
                            subordinateList.clear();
                            subordinateList.add(null);
                            subordinateList.addAll(list);
                            updateSubordinateSpinner();
                        },
                        error -> {
                            subordinateList.clear();
                            subordinateList.add(null);
                            updateSubordinateSpinner();
                        }
                ));
    }

    private void updateSubordinateSpinner() {
        if (binding == null) return;
        List<String> names = new ArrayList<>();
        names.add("全部可查看人员");
        for (Map<String, Object> sub : subordinateList) {
            if (sub == null) continue;
            Object realName = sub.get("real_name");
            Object username = sub.get("username");
            names.add(realName != null ? realName.toString()
                    : (username != null ? username.toString() : "未知"));
        }
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, names);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerSubordinate.setAdapter(spinnerAdapter);
    }

    private Long getSelectedSubordinateId() {
        int pos = binding.spinnerSubordinate.getSelectedItemPosition();
        if (pos <= 0) return null;
        if (pos < subordinateList.size()) {
            Map<String, Object> sub = subordinateList.get(pos);
            if (sub != null) {
                // Round26: 解析统一收敛至 NumberCompat（原 instanceof Number 判定语义保持）
                return com.example.project_pandora.core.util.NumberCompat.toLong(sub.get("id"));
            }
        }
        return null;
    }

    private void loadSubordinateLogs(Long subordinateId, String startDate, String endDate, String keyword) {
        logLoad.set(logRepository.getSubordinateLogs(subordinateId, startDate, endDate, keyword)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        logs -> {
                            if (binding == null) return;
                            binding.swipeRefresh.setRefreshing(false);
                            subordinateLogAdapter.setLogs(logs);
                            binding.textEmpty.setVisibility(logs.isEmpty() ? View.VISIBLE : View.GONE);
                            binding.textEmpty.setText("暂无下属日志");
                        },
                        error -> {
                            if (binding == null) return;
                            binding.swipeRefresh.setRefreshing(false);
                            String msg = error.getMessage();
                            if (msg != null && msg.contains("403")) {
                                Toast.makeText(requireContext(), "无权访问他人日志", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(requireContext(), "加载失败: " + msg, Toast.LENGTH_SHORT).show();
                            }
                        }
                ));
    }

    private void deleteLog(long logId) {
        disposables.add(logRepository.deleteLog(logId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        v -> {
                            Toast.makeText(requireContext(), "已删除", Toast.LENGTH_SHORT).show();
                            applyFilters();
                        },
                        error -> Toast.makeText(requireContext(), "删除失败: " + error.getMessage(), Toast.LENGTH_SHORT).show()
                ));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding == null) return;
        UserApi api = NetworkClient.getInstance(requireContext()).createService(UserApi.class);
        roleRefresh.set(api.getCurrentUser().observeOn(AndroidSchedulers.mainThread()).subscribe(response -> {
            if (binding == null) return;
            if (response.isSuccess() && response.getData() != null) {
                Object role = response.getData().get("role");
                if (role != null) currentRole = role.toString();
                refreshRoleTabs();
            }
            if (!isMyLogTab) loadSubordinateList();
            applyFilters();
        }, error -> {
            if (binding != null) applyFilters();
        }));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposables.clear();
        logLoad.set(null);
        directoryLoad.set(null);
        roleRefresh.set(null);
        binding = null;
    }
}

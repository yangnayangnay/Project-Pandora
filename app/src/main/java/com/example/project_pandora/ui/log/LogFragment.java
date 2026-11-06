package com.example.project_pandora.ui.log;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Outline;
import android.graphics.Path;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
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
    private static final String STATE_FILTERS_EXPANDED = "filtersExpanded";

    private FragmentLogBinding binding;
    private LogRepository logRepository;
    private LogAdapter logAdapter;
    private SubordinateLogAdapter subordinateLogAdapter;
    private LogDisplayLimitConfig logDisplayLimitConfig;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final SerialDisposable logLoad = new SerialDisposable();
    private final SerialDisposable roleRefresh = new SerialDisposable();

    private String currentRole;
    private boolean isMyLogTab = true;
    private boolean hasOtherLogTab = false;
    private boolean keyboardVisible = false;
    private boolean filtersExpanded = true;
    private long currentUserId = -1L;

    private Long selectedSubordinateId, selectedDepartmentId;
    private String selectedPersonLabel = "全部可查看人员";
    private final ActivityResultLauncher<Intent> peoplePicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != android.app.Activity.RESULT_OK || result.getData() == null) return;
                Intent data = result.getData();
                selectedSubordinateId = data.hasExtra(LogPeoplePickerActivity.EXTRA_USER) ? data.getLongExtra(LogPeoplePickerActivity.EXTRA_USER, -1) : null;
                selectedDepartmentId = data.hasExtra(LogPeoplePickerActivity.EXTRA_DEPARTMENT) ? data.getLongExtra(LogPeoplePickerActivity.EXTRA_DEPARTMENT, -1) : null;
                selectedPersonLabel = data.getStringExtra(LogPeoplePickerActivity.EXTRA_LABEL);
                if (binding != null) { updatePersonSelection(); applyFilters(); }
            });

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
        if (savedInstanceState != null) {
            filtersExpanded = savedInstanceState.getBoolean(STATE_FILTERS_EXPANDED, true);
            selectedSubordinateId = savedInstanceState.containsKey("filterUser") ? savedInstanceState.getLong("filterUser") : null;
            selectedDepartmentId = savedInstanceState.containsKey("filterDepartment") ? savedInstanceState.getLong("filterDepartment") : null;
            selectedPersonLabel = savedInstanceState.getString("filterPersonLabel", "全部可查看人员");
        }

        setupAdapters();
        setupTabs();
        setupSearch();
        setupFilterToggle();
        setupFab();
        setupSwipeRefresh();
        setupKeyboardFabToggle();

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
                    binding.buttonSelectLogPerson.setVisibility(View.GONE);
                    updateFabVisibility();
                    clearSearchInputs();
                    applyFilters();
                } else {
                    isMyLogTab = false;
                    binding.recyclerLogs.setVisibility(View.GONE);
                    binding.recyclerSubordinateLogs.setVisibility(View.VISIBLE);
                    binding.buttonSelectLogPerson.setVisibility(View.VISIBLE);
                    updatePersonSelection();
                    updateFabVisibility();
                    clearSearchInputs();
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
        selectedSubordinateId = null; selectedDepartmentId = null; selectedPersonLabel = "全部可查看人员";
        updatePersonSelection();
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
            clearSearchInputs(); selectedSubordinateId = null; selectedDepartmentId = null;
            selectedPersonLabel = "全部可查看人员"; updatePersonSelection();
            binding.chipFilterToday.setChecked(false); binding.chipFilterWeek.setChecked(false); binding.chipFilterAll.setChecked(true);
            applyFilters();
        });
        binding.chipFilterToday.setOnClickListener(v -> setDateRange(0));
        binding.chipFilterWeek.setOnClickListener(v -> setDateRange(6));
        binding.chipFilterAll.setOnClickListener(v -> { binding.editStartDate.setText(""); binding.editEndDate.setText(""); applyFilters(); });
        binding.buttonSelectLogPerson.setOnClickListener(v -> peoplePicker.launch(new Intent(requireContext(), LogPeoplePickerActivity.class)));
    }

    private void setupFilterToggle() {
        binding.buttonToggleLogFilters.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                int width = view.getWidth();
                int handleHeight = view.getHeight() - view.getPaddingBottom();
                if (width <= 0 || handleHeight <= 0) {
                    outline.setEmpty();
                    return;
                }
                // Match the visible semicircle, excluding the transparent touch area below it.
                Path semicircle = new Path();
                semicircle.moveTo(0f, 0f);
                semicircle.lineTo(width, 0f);
                semicircle.arcTo(0f, -handleHeight, width, handleHeight, 0f, 180f, false);
                semicircle.close();
                outline.setConvexPath(semicircle);
            }
        });
        updateFilterVisibility();
        binding.buttonToggleLogFilters.setOnClickListener(v -> {
            filtersExpanded = !filtersExpanded;
            if (!filtersExpanded) {
                binding.editKeyword.clearFocus();
                WindowCompat.getInsetsController(requireActivity().getWindow(), binding.getRoot())
                        .hide(WindowInsetsCompat.Type.ime());
            }
            updateFilterVisibility();
        });
    }

    private void updateFilterVisibility() {
        binding.cardLogFilters.setVisibility(filtersExpanded ? View.VISIBLE : View.GONE);
        binding.buttonToggleLogFilters.setImageLevel(filtersExpanded ? 0 : 10000);
        binding.buttonToggleLogFilters.setContentDescription(getString(filtersExpanded
                ? R.string.log_filters_collapse : R.string.log_filters_expand));
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
        binding.textFilterSummary.setText((isMyLogTab ? "我的日志" : selectedPersonLabel)
                + " · " + (start.isEmpty() ? "不限起始日期" : start) + " 至 " + (end.isEmpty() ? "不限结束日期" : end)
                + (keyword.isEmpty() ? "" : " · 关键词：" + keyword));
        if (isMyLogTab) loadMyLogs(start.isEmpty() ? null : start, end.isEmpty() ? null : end, keyword.isEmpty() ? null : keyword);
        else loadSubordinateLogs(selectedSubordinateId, start.isEmpty() ? null : start, end.isEmpty() ? null : end, keyword.isEmpty() ? null : keyword);
    }

    private void setupFab() {
        binding.fabAddLog.setOnClickListener(v -> navigateToLogEntry());
    }

    /** The + entry starts a fresh date-selectable log form. */
    private void navigateToLogEntry() {
        startActivity(new Intent(requireContext(), LogCreateActivity.class));
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

    private void updatePersonSelection() {
        if (binding != null) binding.buttonSelectLogPerson.setText(selectedPersonLabel + " · 选择部门/人员");
    }

    private void loadSubordinateLogs(Long subordinateId, String startDate, String endDate, String keyword) {
        logLoad.set(logRepository.getSubordinateLogs(subordinateId, selectedDepartmentId, startDate, endDate, keyword)
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
            applyFilters();
        }, error -> {
            if (binding != null) applyFilters();
        }));
    }

    @Override public void onSaveInstanceState(@NonNull Bundle state) {
        state.putBoolean(STATE_FILTERS_EXPANDED, filtersExpanded);
        if (selectedSubordinateId != null) state.putLong("filterUser", selectedSubordinateId);
        if (selectedDepartmentId != null) state.putLong("filterDepartment", selectedDepartmentId);
        state.putString("filterPersonLabel", selectedPersonLabel);
        super.onSaveInstanceState(state);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposables.clear();
        logLoad.set(null);
        roleRefresh.set(null);
        binding = null;
    }
}

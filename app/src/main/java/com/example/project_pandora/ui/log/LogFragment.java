package com.example.project_pandora.ui.log;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.project_pandora.R;
import com.example.project_pandora.core.config.LogDisplayLimitConfig;
import com.example.project_pandora.databinding.FragmentLogBinding;
import com.example.project_pandora.repository.LogRepository;

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

    private static final String STATE_FILTERS_EXPANDED = "filtersExpanded";

    private FragmentLogBinding binding;
    private LogRepository logRepository;
    private LogAdapter logAdapter;
    private LogDisplayLimitConfig logDisplayLimitConfig;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final SerialDisposable logLoad = new SerialDisposable();
    private boolean keyboardVisible = false;
    private boolean filtersExpanded = false;
    private long currentUserId = -1L;
    private com.google.android.material.button.MaterialButton filterToggle;
    private androidx.appcompat.app.AlertDialog filterDatePicker;
    private int activeFilters;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLogBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        com.example.project_pandora.ui.widget.CompanyBackgrounds.bind(view,
                com.example.project_pandora.ui.widget.CompanyBackgrounds.Scene.LOG);
        binding.toolbar.setBackgroundColor(android.graphics.Color.TRANSPARENT);

        logRepository = new LogRepository(requireContext());
        logDisplayLimitConfig = new LogDisplayLimitConfig(requireContext());
        Long uid = com.example.project_pandora.core.network.TokenManager.getInstance(requireContext()).getUserId();
        currentUserId = uid != null ? uid : -1L;
        keyboardVisible = false;
        if (savedInstanceState != null) {
            filtersExpanded = savedInstanceState.getBoolean(STATE_FILTERS_EXPANDED, false);
        }

        setupAdapters();
        setupSearch();
        setupFilterToggle();
        setupFab();
        setupSwipeRefresh();
        setupKeyboardFabToggle();
        binding.buttonRetryLog.setOnClickListener(v -> applyFilters());
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

    }

    private void setupSearch() {
        binding.editStartDate.setOnClickListener(v -> pickFilterDate(binding.editStartDate));
        binding.editEndDate.setOnClickListener(v -> pickFilterDate(binding.editEndDate));
        binding.btnSearch.setOnClickListener(v -> applyFilters());
        binding.btnResetFilters.setOnClickListener(v -> {
            clearSearchInputs();
            binding.chipFilterToday.setChecked(false); binding.chipFilterWeek.setChecked(false); binding.chipFilterAll.setChecked(true);
            applyFilters();
        });
        binding.chipFilterToday.setOnClickListener(v -> setDateRange(0));
        binding.chipFilterWeek.setOnClickListener(v -> setDateRange(6));
        binding.chipFilterAll.setOnClickListener(v -> { binding.editStartDate.setText(""); binding.editEndDate.setText(""); applyFilters(); });
    }

    private void setupFilterToggle() {
        android.view.MenuItem item = binding.toolbar.getMenu().findItem(R.id.actionLogFilters);
        filterToggle = new com.google.android.material.button.MaterialButton(requireContext(), null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        filterToggle.setLayoutParams(new android.view.ViewGroup.LayoutParams(
                Math.round(104 * getResources().getDisplayMetrics().density),
                Math.round(48 * getResources().getDisplayMetrics().density)));
        filterToggle.setMinWidth(0);
        filterToggle.setMinHeight(Math.round(48 * getResources().getDisplayMetrics().density));
        filterToggle.setTextSize(14);
        filterToggle.setCornerRadius(Math.round(14 * getResources().getDisplayMetrics().density));
        filterToggle.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                androidx.core.content.ContextCompat.getColor(requireContext(), R.color.color_accent)));
        filterToggle.setStrokeColor(android.content.res.ColorStateList.valueOf(
                androidx.core.content.ContextCompat.getColor(requireContext(), R.color.color_accent)));
        filterToggle.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.color_on_accent));
        filterToggle.setIconGravity(com.google.android.material.button.MaterialButton.ICON_GRAVITY_TEXT_END);
        filterToggle.setIconSize(Math.round(16 * getResources().getDisplayMetrics().density));
        filterToggle.setIconPadding(Math.round(4 * getResources().getDisplayMetrics().density));
        filterToggle.setIconTint(filterToggle.getTextColors());
        filterToggle.setStateListAnimator(android.animation.AnimatorInflater.loadStateListAnimator(requireContext(), R.animator.pandora_button_press));
        filterToggle.setStrokeWidth(Math.max(1, Math.round(getResources().getDisplayMetrics().density)));
        item.setActionView(filterToggle);
        filterToggle.setOnClickListener(view -> {
            filtersExpanded = !filtersExpanded;
            if (!filtersExpanded) {
                binding.editKeyword.clearFocus();
                WindowCompat.getInsetsController(requireActivity().getWindow(), binding.getRoot())
                        .hide(WindowInsetsCompat.Type.ime());
            }
            updateFilterVisibility();
        });
        updateFilterVisibility();
    }

    private void updateFilterVisibility() {
        binding.cardLogFilters.setVisibility(filtersExpanded ? View.VISIBLE : View.GONE);
        android.view.MenuItem item = binding.toolbar.getMenu().findItem(R.id.actionLogFilters);
        if (item != null) item.setTitle(filtersExpanded ? "收起筛选" : "筛选");
        if (filterToggle != null) {
            filterToggle.setText("筛选" + (activeFilters == 0 ? "" : " (" + activeFilters + ")"));
            filterToggle.setIconResource(filtersExpanded ? R.drawable.ic_triangle_up : R.drawable.ic_triangle_down);
            filterToggle.setIconTint(filterToggle.getTextColors());
            filterToggle.setContentDescription(filtersExpanded ? "收起日志筛选" : "展开日志筛选");
        }
    }

    private void pickFilterDate(android.widget.EditText field) {
        Calendar now = Calendar.getInstance();
        try { if (!field.getText().toString().isEmpty()) now.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).parse(field.getText().toString())); } catch (Exception ignored) { }
        filterDatePicker = com.example.project_pandora.ui.widget.QuickDatePickerDialog.create(requireContext(), now, chosen -> {
            field.setText(String.format(Locale.ROOT, "%04d-%02d-%02d", chosen.get(Calendar.YEAR), chosen.get(Calendar.MONTH) + 1, chosen.get(Calendar.DATE)));
            binding.chipFilterToday.setChecked(false); binding.chipFilterWeek.setChecked(false); binding.chipFilterAll.setChecked(false);
        });
        filterDatePicker.setButton(android.content.DialogInterface.BUTTON_NEUTRAL, "清除日期", (dialog, which) -> field.setText(""));
        filterDatePicker.show();
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
        activeFilters = (start.isEmpty() ? 0 : 1) + (end.isEmpty() ? 0 : 1) + (keyword.isEmpty() ? 0 : 1);
        updateFilterVisibility();
        if (!start.isEmpty() && !end.isEmpty() && start.compareTo(end) > 0) {
            Toast.makeText(requireContext(), "开始日期不能晚于结束日期", Toast.LENGTH_SHORT).show();
            binding.swipeRefresh.setRefreshing(false); return;
        }
        binding.textFilterSummary.setText("我的日志 · " + (start.isEmpty() ? "不限起始日期" : start) + " 至 " + (end.isEmpty() ? "不限结束日期" : end)
                + (keyword.isEmpty() ? "" : " · 关键词：" + keyword));
        loadMyLogs(start.isEmpty() ? null : start, end.isEmpty() ? null : end, keyword.isEmpty() ? null : keyword);
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
        if (keyboardVisible) {
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
        binding.buttonRetryLog.setVisibility(View.GONE);
        binding.textEmpty.setVisibility(View.GONE);
        binding.swipeRefresh.setRefreshing(true);
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
                            binding.textEmpty.setText(isUnsearched ? "暂无日志，点击右下角新增" : "没有符合筛选条件的日志，可调整或重置筛选");
                        },
                        error -> {
                            if (binding == null) return;
                            binding.swipeRefresh.setRefreshing(false);
                            binding.textEmpty.setText(logAdapter.getItemCount() == 0 ? "日志加载失败，请稍后重试" : "刷新失败，已保留原有日志");
                            binding.textEmpty.setVisibility(logAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                            if (logAdapter.getItemCount() > 0) binding.textFilterSummary.setText("刷新失败，已保留原有日志；请重新加载");
                            binding.buttonRetryLog.setVisibility(View.VISIBLE);
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
        applyFilters();
    }

    @Override public void onSaveInstanceState(@NonNull Bundle state) {
        state.putBoolean(STATE_FILTERS_EXPANDED, filtersExpanded);
        super.onSaveInstanceState(state);
    }

    @Override
    public void onDestroyView() {
        if (filterDatePicker != null) filterDatePicker.dismiss();
        super.onDestroyView();
        disposables.clear();
        logLoad.set(null);
        binding = null;
    }
}

package com.example.project_pandora.ui.log;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.project_pandora.R;
import com.example.project_pandora.core.config.LogDisplayLimitConfig;
import com.example.project_pandora.core.network.TokenManager;
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

public class LogFragment extends Fragment {

    private static final int TAB_MY_LOG = 0;
    private static final int TAB_OTHER_LOG = 1;

    private FragmentLogBinding binding;
    private LogRepository logRepository;
    private LogAdapter logAdapter;
    private SubordinateLogAdapter subordinateLogAdapter;
    private LogDisplayLimitConfig logDisplayLimitConfig;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private String currentRole;
    private boolean isMyLogTab = true;
    private boolean hasOtherLogTab = false;

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

        setupAdapters();
        setupTabs();
        setupSearch();
        setupFab();
        setupSwipeRefresh();
        setHasOptionsMenu(true);

        if (binding.tabLayout.getTabCount() > 0) {
            binding.tabLayout.getTabAt(TAB_MY_LOG).select();
        }
        loadMyLogs(null, null, null);
    }

    private void setupAdapters() {
        logAdapter = new LogAdapter(new LogAdapter.OnLogClickListener() {
            @Override
            public void onLogClick(Map<String, Object> log) {
                Object id = log.get("id");
                if (id instanceof Number) {
                    Intent intent = new Intent(requireContext(), LogDetailActivity.class);
                    intent.putExtra(LogDetailActivity.EXTRA_LOG_ID, ((Number) id).longValue());
                    startActivity(intent);
                }
            }

            @Override
            public void onLogLongClick(Map<String, Object> log) {
                Object id = log.get("id");
                if (id instanceof Number) {
                    long logId = ((Number) id).longValue();
                    new AlertDialog.Builder(requireContext())
                            .setTitle("删除日志")
                            .setMessage("确定删除这条日志吗？")
                            .setPositiveButton("删除", (d, w) -> deleteLog(logId))
                            .setNegativeButton("取消", null)
                            .show();
                }
            }
        });

        binding.recyclerLogs.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerLogs.setAdapter(logAdapter);

        subordinateLogAdapter = new SubordinateLogAdapter();
        binding.recyclerSubordinateLogs.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerSubordinateLogs.setAdapter(subordinateLogAdapter);
    }

    private void setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("我的日志"));

        hasOtherLogTab = !"EMPLOYEE".equals(currentRole);
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
                    binding.fabAddLog.setVisibility(View.VISIBLE);
                    clearSearchInputs();
                    loadMyLogs(null, null, null);
                } else {
                    isMyLogTab = false;
                    binding.recyclerLogs.setVisibility(View.GONE);
                    binding.recyclerSubordinateLogs.setVisibility(View.VISIBLE);
                    binding.spinnerSubordinate.setVisibility(View.VISIBLE);
                    binding.fabAddLog.setVisibility(View.GONE);
                    clearSearchInputs();
                    loadSubordinateList();
                    loadSubordinateLogs(null, null, null, null);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupSearch() {
        binding.btnSearch.setOnClickListener(v -> {
            String startDate = binding.editStartDate.getText().toString().trim();
            String endDate = binding.editEndDate.getText().toString().trim();
            String keyword = binding.editKeyword.getText().toString().trim();

            String sDate = TextUtils.isEmpty(startDate) ? null : startDate;
            String eDate = TextUtils.isEmpty(endDate) ? null : endDate;
            String kw = TextUtils.isEmpty(keyword) ? null : keyword;

            if (isMyLogTab) {
                loadMyLogs(sDate, eDate, kw);
            } else {
                Long subId = getSelectedSubordinateId();
                loadSubordinateLogs(subId, sDate, eDate, kw);
            }
        });
    }

    private void setupFab() {
        binding.fabAddLog.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), LogCreateActivity.class)));
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(() -> {
            String startDate = binding.editStartDate.getText().toString().trim();
            String endDate = binding.editEndDate.getText().toString().trim();
            String keyword = binding.editKeyword.getText().toString().trim();

            String sDate = TextUtils.isEmpty(startDate) ? null : startDate;
            String eDate = TextUtils.isEmpty(endDate) ? null : endDate;
            String kw = TextUtils.isEmpty(keyword) ? null : keyword;

            if (isMyLogTab) {
                loadMyLogs(sDate, eDate, kw);
            } else {
                Long subId = getSelectedSubordinateId();
                loadSubordinateLogs(subId, sDate, eDate, kw);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_stat) {
            showLogStats();
            return true;
        } else if (id == R.id.action_template) {
            startActivity(new Intent(requireContext(), LogCreateActivity.class));
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
        disposables.add(logRepository.listLogs(effectiveStartDate, endDate, keyword)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        logs -> {
                            binding.swipeRefresh.setRefreshing(false);
                            List<Map<String, Object>> displayLogs = truncateByCountLimit(logs, isUnsearched);
                            logAdapter.setLogs(displayLogs);
                            binding.textEmpty.setVisibility(displayLogs.isEmpty() ? View.VISIBLE : View.GONE);
                            binding.textEmpty.setText("暂无日志");
                        },
                        error -> {
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
        disposables.add(logRepository.getSubordinates()
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
        List<String> names = new ArrayList<>();
        names.add("全部下属");
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
                Object id = sub.get("id");
                if (id instanceof Number) return ((Number) id).longValue();
            }
        }
        return null;
    }

    private void loadSubordinateLogs(Long subordinateId, String startDate, String endDate, String keyword) {
        disposables.add(logRepository.getSubordinateLogs(subordinateId, startDate, endDate, keyword)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        logs -> {
                            binding.swipeRefresh.setRefreshing(false);
                            subordinateLogAdapter.setLogs(logs);
                            binding.textEmpty.setVisibility(logs.isEmpty() ? View.VISIBLE : View.GONE);
                            binding.textEmpty.setText("暂无下属日志");
                        },
                        error -> {
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
                            loadMyLogs(null, null, null);
                        },
                        error -> Toast.makeText(requireContext(), "删除失败: " + error.getMessage(), Toast.LENGTH_SHORT).show()
                ));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding == null) return;
        String startDate = binding.editStartDate.getText().toString().trim();
        String endDate = binding.editEndDate.getText().toString().trim();
        String keyword = binding.editKeyword.getText().toString().trim();
        String sDate = TextUtils.isEmpty(startDate) ? null : startDate;
        String eDate = TextUtils.isEmpty(endDate) ? null : endDate;
        String kw = TextUtils.isEmpty(keyword) ? null : keyword;
        if (isMyLogTab) {
            loadMyLogs(sDate, eDate, kw);
        } else {
            Long subId = getSelectedSubordinateId();
            loadSubordinateLogs(subId, sDate, eDate, kw);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposables.clear();
        binding = null;
    }
}

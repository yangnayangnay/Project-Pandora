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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.project_pandora.core.logger.PandoraLogger;
import com.example.project_pandora.core.smoketest.SmokeTestEngine;
import com.example.project_pandora.databinding.FragmentLogBinding;
import com.example.project_pandora.repository.LogRepository;

import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class LogFragment extends Fragment {

    private FragmentLogBinding binding;
    private LogRepository logRepository;
    private LogAdapter adapter;
    private final CompositeDisposable disposables = new CompositeDisposable();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLogBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        runMorningSmokeTestIfNeeded();

        logRepository = new LogRepository(requireContext());

        adapter = new LogAdapter(new LogAdapter.OnLogClickListener() {
            @Override
            public void onLogClick(Map<String, Object> log) {
                Intent intent = new Intent(requireContext(), LogCreateActivity.class);
                intent.putExtra("editMode", true);
                Object id = log.get("id");
                if (id instanceof Number) {
                    intent.putExtra("logId", ((Number) id).longValue());
                }
                Object workItem = log.get("work_item");
                if (workItem != null) intent.putExtra("workItem", workItem.toString());
                Object status = log.get("completion_status");
                if (status != null) intent.putExtra("completionStatus", status.toString());
                Object timeCost = log.get("time_cost");
                if (timeCost != null) intent.putExtra("timeCost", timeCost.toString());
                Object logDate = log.get("log_date");
                if (logDate != null) intent.putExtra("logDate", logDate.toString());
                startActivity(intent);
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
        binding.recyclerLogs.setAdapter(adapter);

        binding.fabAddLog.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), LogCreateActivity.class)));

        binding.swipeRefresh.setOnRefreshListener(this::loadLogs);

        loadLogs();
    }

    private void loadLogs() {
        disposables.add(logRepository.listLogs()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        logs -> {
                            binding.swipeRefresh.setRefreshing(false);
                            adapter.setLogs(logs);
                            binding.textEmpty.setVisibility(logs.isEmpty() ? View.VISIBLE : View.GONE);
                        },
                        error -> {
                            binding.swipeRefresh.setRefreshing(false);
                            Toast.makeText(requireContext(), "加载失败: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                ));
    }

    private void deleteLog(long logId) {
        disposables.add(logRepository.deleteLog(logId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        v -> {
                            Toast.makeText(requireContext(), "已删除", Toast.LENGTH_SHORT).show();
                            loadLogs();
                        },
                        error -> Toast.makeText(requireContext(), "删除失败: " + error.getMessage(), Toast.LENGTH_SHORT).show()
                ));
    }

    private void runMorningSmokeTestIfNeeded() {
        SmokeTestEngine smokeTestEngine = new SmokeTestEngine(requireContext());
        if (smokeTestEngine.shouldRunToday()) {
            PandoraLogger.getInstance().info("LogFragment", "当天首次进入日志页，执行晨星冒烟测试");
            SmokeTestEngine.SmokeTestResult result = smokeTestEngine.runSmokeTest();
            if (result.isPassed()) {
                PandoraLogger.getInstance().info("LogFragment", result.getMessage());
            } else {
                PandoraLogger.getInstance().warn("LogFragment", result.getMessage());
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadLogs();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposables.clear();
        binding = null;
    }
}

package com.example.project_pandora.ui.log;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.project_pandora.core.logger.PandoraLogger;
import com.example.project_pandora.core.smoketest.SmokeTestEngine;
import com.example.project_pandora.databinding.FragmentLogBinding;

public class LogFragment extends Fragment {

    private FragmentLogBinding binding;

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

        binding.textLogTitle.setText("工作日志");
        binding.textLogHint.setText("点击右下角按钮添加日志");
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
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
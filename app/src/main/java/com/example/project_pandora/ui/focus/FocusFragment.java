package com.example.project_pandora.ui.focus;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.project_pandora.R;
import com.example.project_pandora.databinding.FragmentFocusBinding;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class FocusFragment extends Fragment {

    private static final long WORK_DURATION_MS = 25 * 60 * 1000L;
    private static final long BREAK_DURATION_MS = 5 * 60 * 1000L;
    private static final long INTERVAL_MS = 1000L;

    private FragmentFocusBinding binding;
    private CountDownTimer countDownTimer;
    private final AtomicBoolean isWorkMode = new AtomicBoolean(true);
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private int sessionCount = 0;
    private long remainingMs = WORK_DURATION_MS;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFocusBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        updateTimerDisplay(remainingMs);

        binding.btnStart.setOnClickListener(v -> {
            if (!isRunning.get()) {
                startTimer();
            }
        });

        binding.btnPause.setOnClickListener(v -> {
            if (isRunning.get()) {
                pauseTimer();
            }
        });

        binding.btnReset.setOnClickListener(v -> resetTimer());
    }

    private void startTimer() {
        isRunning.set(true);
        binding.btnStart.setEnabled(false);

        countDownTimer = new CountDownTimer(remainingMs, INTERVAL_MS) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingMs = millisUntilFinished;
                updateTimerDisplay(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                remainingMs = 0;
                updateTimerDisplay(0);
                isRunning.set(false);
                binding.btnStart.setEnabled(true);

                if (isWorkMode.get()) {
                    sessionCount++;
                    binding.textSessionCount.setText(
                            String.format(Locale.getDefault(), "已完成 %d 个专注周期", sessionCount));
                    switchToBreak();
                } else {
                    switchToWork();
                }
            }
        }.start();
    }

    private void pauseTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isRunning.set(false);
        binding.btnStart.setEnabled(true);
    }

    private void resetTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isRunning.set(false);
        binding.btnStart.setEnabled(true);
        isWorkMode.set(true);
        remainingMs = WORK_DURATION_MS;
        binding.textFocusMode.setText(R.string.focus_mode_work);
        updateTimerDisplay(WORK_DURATION_MS);
    }

    private void switchToBreak() {
        isWorkMode.set(false);
        remainingMs = BREAK_DURATION_MS;
        binding.textFocusMode.setText(R.string.focus_mode_break);
        updateTimerDisplay(BREAK_DURATION_MS);
    }

    private void switchToWork() {
        isWorkMode.set(true);
        remainingMs = WORK_DURATION_MS;
        binding.textFocusMode.setText(R.string.focus_mode_work);
        updateTimerDisplay(WORK_DURATION_MS);
    }

    private void updateTimerDisplay(long ms) {
        int totalSeconds = (int) (ms / 1000);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        binding.textTimer.setText(
                String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        binding = null;
    }
}
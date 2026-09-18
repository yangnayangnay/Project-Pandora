package com.example.project_pandora.ui.mine;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.project_pandora.core.moon.MoonPhaseCalculator;
import com.example.project_pandora.core.theme.ThemeManager;
import com.example.project_pandora.databinding.FragmentMineBinding;
import com.example.project_pandora.ui.focus.FocusActivity;

public class MineFragment extends Fragment {

    private FragmentMineBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMineBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MoonPhaseCalculator calculator = new MoonPhaseCalculator();
        MoonPhaseCalculator.MoonPhase currentPhase = calculator.calculateCurrentMoonPhase();
        binding.textMoonPhase.setText("当前月相: " + currentPhase.getDisplayName());

        ThemeManager themeManager = new ThemeManager(requireContext());
        ThemeManager.ThemeMode mode = themeManager.getThemeMode();
        binding.textThemeMode.setText("主题模式: " + (mode == ThemeManager.ThemeMode.AUTO ? "跟随月相" : "手动"));

        binding.btnUserProfile.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ProfileActivity.class)));

        binding.btnSettings.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ThemeSettingActivity.class)));

        binding.btnFocus.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), FocusActivity.class)));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

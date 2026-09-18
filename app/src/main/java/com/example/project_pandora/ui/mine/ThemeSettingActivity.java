package com.example.project_pandora.ui.mine;

import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project_pandora.core.moon.MoonPhaseCalculator.MoonPhase;
import com.example.project_pandora.core.theme.ThemeManager;
import com.example.project_pandora.databinding.ActivityThemeSettingBinding;

public class ThemeSettingActivity extends AppCompatActivity {

    private ActivityThemeSettingBinding binding;
    private ThemeManager themeManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityThemeSettingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        themeManager = new ThemeManager(this);

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        ThemeManager.ThemeMode currentMode = themeManager.getThemeMode();
        if (currentMode == ThemeManager.ThemeMode.AUTO) {
            binding.radioAuto.setChecked(true);
        } else {
            binding.radioManual.setChecked(true);
        }

        RadioGroup themeGroup = new RadioGroup(this);
        themeGroup.setOrientation(RadioGroup.VERTICAL);

        MoonPhase currentTheme = themeManager.getCurrentTheme();
        for (MoonPhase phase : MoonPhase.values()) {
            RadioButton rb = new RadioButton(this);
            rb.setText(phase.getDisplayName());
            rb.setTag(phase);
            if (currentMode == ThemeManager.ThemeMode.MANUAL && phase == currentTheme) {
                rb.setChecked(true);
            }
            themeGroup.addView(rb);
        }

        binding.recyclerThemes.setVisibility(android.view.View.GONE);
        if (currentMode == ThemeManager.ThemeMode.MANUAL) {
            binding.recyclerThemes.setVisibility(android.view.View.VISIBLE);
        }

        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setPadding(32, 16, 32, 16);
        container.addView(themeGroup);
        binding.recyclerThemes.setVisibility(android.view.View.GONE);

        binding.radioGroupMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == binding.radioAuto.getId()) {
                binding.recyclerThemes.setVisibility(android.view.View.GONE);
            } else {
                binding.recyclerThemes.setVisibility(android.view.View.VISIBLE);
            }
        });

        binding.btnSave.setOnClickListener(v -> {
            if (binding.radioAuto.isChecked()) {
                themeManager.setThemeMode(ThemeManager.ThemeMode.AUTO);
                themeManager.applyTheme();
            } else {
                for (int i = 0; i < themeGroup.getChildCount(); i++) {
                    RadioButton rb = (RadioButton) themeGroup.getChildAt(i);
                    if (rb.isChecked()) {
                        MoonPhase selected = (MoonPhase) rb.getTag();
                        themeManager.setManualTheme(selected);
                        themeManager.applyTheme();
                        break;
                    }
                }
            }
            Toast.makeText(this, "主题设置已保存", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}

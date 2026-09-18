package com.example.project_pandora.ui.ai;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project_pandora.core.divination.DivinationEngine;
import com.example.project_pandora.databinding.ActivityDivinationBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DivinationActivity extends AppCompatActivity {

    private static final String PREF_NAME = "divination_pref";
    private static final String KEY_DATE = "divination_date";
    private static final String KEY_ORIGINAL_NAME = "divination_original_name";
    private static final String KEY_MUTUAL_NAME = "divination_mutual_name";
    private static final String KEY_CHANGED_NAME = "divination_changed_name";
    private static final String KEY_ORIGINAL_TEXT = "divination_original_text";
    private static final String KEY_MUTUAL_TEXT = "divination_mutual_text";
    private static final String KEY_CHANGED_TEXT = "divination_changed_text";
    private static final String KEY_MOVING_LINE = "divination_moving_line";

    private ActivityDivinationBinding binding;
    private SharedPreferences prefs;
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDivinationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        String today = new SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.CHINA).format(new Date());
        binding.textDate.setText(today);

        prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        if (hasDivinedToday()) {
            showSavedResult();
        } else {
            binding.btnDivine.setOnClickListener(v -> performDivination());
        }
    }

    private boolean hasDivinedToday() {
        String savedDate = prefs.getString(KEY_DATE, "");
        String today = dateFmt.format(new Date());
        return today.equals(savedDate);
    }

    private void showSavedResult() {
        binding.btnDivine.setVisibility(View.GONE);

        binding.textOriginalName.setText(prefs.getString(KEY_ORIGINAL_NAME, ""));
        binding.textMutualName.setText(prefs.getString(KEY_MUTUAL_NAME, ""));
        binding.textChangedName.setText(prefs.getString(KEY_CHANGED_NAME, ""));

        binding.textOriginalText.setText(prefs.getString(KEY_ORIGINAL_TEXT, ""));
        binding.textMutualText.setText(prefs.getString(KEY_MUTUAL_TEXT, ""));
        binding.textChangedText.setText(prefs.getString(KEY_CHANGED_TEXT, ""));

        int movingLine = prefs.getInt(KEY_MOVING_LINE, 0);
        if (movingLine > 0) {
            binding.textMovingLine.setText(String.format(Locale.getDefault(),
                    "动爻：第%d爻", movingLine));
            binding.textMovingLine.setVisibility(View.VISIBLE);
        }

        binding.layoutResult.setVisibility(View.VISIBLE);
    }

    private void performDivination() {
        binding.btnDivine.setVisibility(View.GONE);
        binding.progressBar.setVisibility(View.VISIBLE);

        DivinationEngine.DivinationResult result = new DivinationEngine().divine(new Date());

        binding.textOriginalName.setText(result.getOriginalName());
        binding.textMutualName.setText(result.getMutualName());
        binding.textChangedName.setText(result.getChangedName());

        binding.textOriginalText.setText(buildHexagramText(
                result.getOriginalJudgment(), result.getOriginalLines()));
        binding.textMutualText.setText(buildHexagramText(
                result.getMutualJudgment(), result.getMutualLines()));
        binding.textChangedText.setText(buildHexagramText(
                result.getChangedJudgment(), result.getChangedLines()));

        binding.textMovingLine.setText(String.format(Locale.getDefault(),
                "动爻：第%d爻", result.getMovingLine()));
        binding.textMovingLine.setVisibility(View.VISIBLE);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_DATE, dateFmt.format(new Date()));
        editor.putString(KEY_ORIGINAL_NAME, result.getOriginalName());
        editor.putString(KEY_MUTUAL_NAME, result.getMutualName());
        editor.putString(KEY_CHANGED_NAME, result.getChangedName());
        editor.putString(KEY_ORIGINAL_TEXT, buildHexagramText(
                result.getOriginalJudgment(), result.getOriginalLines()));
        editor.putString(KEY_MUTUAL_TEXT, buildHexagramText(
                result.getMutualJudgment(), result.getMutualLines()));
        editor.putString(KEY_CHANGED_TEXT, buildHexagramText(
                result.getChangedJudgment(), result.getChangedLines()));
        editor.putInt(KEY_MOVING_LINE, result.getMovingLine());
        editor.apply();

        binding.progressBar.setVisibility(View.GONE);
        binding.layoutResult.setVisibility(View.VISIBLE);
    }

    private String buildHexagramText(String judgment, String[] lines) {
        StringBuilder sb = new StringBuilder();
        sb.append(judgment).append("\n");
        for (String line : lines) {
            sb.append(line).append("\n");
        }
        return sb.toString().trim();
    }
}

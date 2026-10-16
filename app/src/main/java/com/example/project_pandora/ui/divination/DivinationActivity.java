package com.example.project_pandora.ui.divination;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project_pandora.R;
import com.example.project_pandora.core.divination.DivinationEngine;
import com.example.project_pandora.databinding.ActivityDivinationBinding;

/**
 * Round19 REQ-19-12: 梅花易数占卜页面
 * 用户输入正整数，基于数字起卦算法计算本卦/互卦/变卦并展示
 */
public class DivinationActivity extends AppCompatActivity {

    private ActivityDivinationBinding binding;
    private final DivinationEngine engine = new DivinationEngine();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDivinationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        binding.btnDivine.setOnClickListener(v -> performDivination());
    }

    private void performDivination() {
        String input = binding.editNumber.getText().toString().trim();

        // 校验输入非空
        if (input.isEmpty()) {
            Toast.makeText(this, "请输入正整数", Toast.LENGTH_SHORT).show();
            return;
        }

        int number;
        try {
            number = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "请输入正整数", Toast.LENGTH_SHORT).show();
            return;
        }

        // 校验为正整数
        if (number <= 0) {
            Toast.makeText(this, "请输入正整数", Toast.LENGTH_SHORT).show();
            return;
        }

        // 调用数字起卦算法
        DivinationEngine.DivinationResult result = engine.divine(number);

        // 展示结果
        displayResult(result, number);
    }

    private void displayResult(DivinationEngine.DivinationResult result, int inputNumber) {
        // 结果标题
        binding.textResultTitle.setText("输入数字 " + inputNumber + " 的起卦结果");
        binding.textResultTitle.setVisibility(View.VISIBLE);

        // 本卦
        binding.cardOriginal.setVisibility(View.VISIBLE);
        binding.textOriginalName.setText(result.getOriginalName());
        binding.textOriginalJudgment.setText(result.getOriginalJudgment());

        // 互卦
        binding.cardMutual.setVisibility(View.VISIBLE);
        binding.textMutualName.setText(result.getMutualName());
        binding.textMutualJudgment.setText(result.getMutualJudgment());

        // 变卦
        binding.cardChanged.setVisibility(View.VISIBLE);
        binding.textChangedName.setText(result.getChangedName());
        binding.textChangedJudgment.setText(result.getChangedJudgment());

        // 动爻
        binding.textMovingLine.setText("动爻：第 " + result.getMovingLine() + " 爻");
        binding.textMovingLine.setVisibility(View.VISIBLE);
    }
}
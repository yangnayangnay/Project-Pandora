package com.example.project_pandora.ui.aimap;

import android.content.Intent;
import android.os.Bundle;

import com.example.project_pandora.ui.PandoraActivity;

import com.example.project_pandora.core.util.BackToolbarHelper;
import com.example.project_pandora.databinding.ActivityAiAnalysisBinding;

import com.example.project_pandora.ui.ai.MBTITestActivity;
import com.example.project_pandora.ui.ai.SuggestionActivity;

public class AIAnalysisActivity extends PandoraActivity {

    private ActivityAiAnalysisBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAiAnalysisBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        com.example.project_pandora.ui.widget.CompanyBackgrounds.bind(binding.getRoot(), com.example.project_pandora.ui.widget.CompanyBackgrounds.Scene.AI);
        binding.toolbar.setBackgroundColor(android.graphics.Color.TRANSPARENT);

        BackToolbarHelper.setupBack(binding.toolbar, this::finish);

        binding.btnSuggestion.setOnClickListener(v ->
                startActivity(new Intent(this, SuggestionActivity.class)));

        binding.btnMbtiTest.setOnClickListener(v ->
                startActivity(new Intent(this, MBTITestActivity.class)));

        // Round23 REQ-23-08: 梅花易数入口（自"我的"页迁移而来）
        binding.btnDivination.setOnClickListener(v ->
                startActivity(new Intent(this, DivinationActivity.class)));
        binding.btnBirthFortune.setOnClickListener(v ->
                startActivity(new Intent(this, BirthFortuneActivity.class)));

    }
}

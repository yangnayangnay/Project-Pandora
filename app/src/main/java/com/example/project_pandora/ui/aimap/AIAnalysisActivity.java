package com.example.project_pandora.ui.aimap;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project_pandora.core.util.BackToolbarHelper;
import com.example.project_pandora.databinding.ActivityAiAnalysisBinding;

import com.example.project_pandora.ui.ai.MBTITestActivity;
import com.example.project_pandora.ui.ai.SuggestionActivity;

public class AIAnalysisActivity extends AppCompatActivity {

    private ActivityAiAnalysisBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAiAnalysisBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BackToolbarHelper.setupBack(binding.toolbar, this::finish);

        binding.btnSuggestion.setOnClickListener(v ->
                startActivity(new Intent(this, SuggestionActivity.class)));

        binding.btnMbtiTest.setOnClickListener(v ->
                startActivity(new Intent(this, MBTITestActivity.class)));

    }
}
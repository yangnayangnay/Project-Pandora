package com.example.project_pandora.ui.ai;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project_pandora.databinding.ActivitySuggestionBinding;
import com.example.project_pandora.repository.AIRepository;

import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

public class SuggestionActivity extends AppCompatActivity {

    private ActivitySuggestionBinding binding;
    private AIRepository aiRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySuggestionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        aiRepository = new AIRepository(this);

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        loadSuggestion();
    }

    private void loadSuggestion() {
        binding.progressBar.setVisibility(View.VISIBLE);
        aiRepository.getSuggestion()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        suggestion -> {
                            binding.progressBar.setVisibility(View.GONE);
                            displaySuggestion(suggestion);
                        },
                        error -> {
                            binding.progressBar.setVisibility(View.GONE);
                            Toast.makeText(this, "获取建议失败", Toast.LENGTH_SHORT).show();
                        }
                );
    }

    private void displaySuggestion(Map<String, Object> suggestion) {
        Object workAdvice = suggestion.get("workAdvice");
        Object careerAdvice = suggestion.get("careerAdvice");
        Object developmentPlan = suggestion.get("developmentPlan");

        binding.textWorkAdvice.setText(workAdvice != null ? workAdvice.toString() : "暂无建议");
        binding.textCareerAdvice.setText(careerAdvice != null ? careerAdvice.toString() : "暂无建议");
        binding.textDevelopmentPlan.setText(developmentPlan != null ? developmentPlan.toString() : "暂无计划");
    }
}
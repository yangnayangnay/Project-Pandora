package com.example.project_pandora.ui.aimap;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.project_pandora.databinding.FragmentAimapBinding;
import com.example.project_pandora.ui.ai.BirthdayWishActivity;
import com.example.project_pandora.ui.ai.DivinationActivity;
import com.example.project_pandora.ui.ai.KeywordAnalysisActivity;
import com.example.project_pandora.ui.ai.MBTITestActivity;
import com.example.project_pandora.ui.ai.SuggestionActivity;

public class AIMapFragment extends Fragment {

    private FragmentAimapBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAimapBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.toolbar.setNavigationOnClickListener(null);

        binding.btnKeywordAnalysis.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), KeywordAnalysisActivity.class)));

        binding.btnSuggestion.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SuggestionActivity.class)));

        binding.btnMbtiTest.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), MBTITestActivity.class)));

        binding.btnDivination.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), DivinationActivity.class)));

        binding.btnBirthdayWish.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), BirthdayWishActivity.class)));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

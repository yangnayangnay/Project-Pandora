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

        binding.btnDataCollect.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), DataCollectActivity.class)));

        binding.btnTaskShow.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), TaskShowActivity.class)));

        binding.btnAiAnalysis.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AIAnalysisActivity.class)));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

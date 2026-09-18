package com.example.project_pandora.ui.mindmap;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.project_pandora.databinding.FragmentMindmapBinding;

public class MindMapFragment extends Fragment {

    private FragmentMindmapBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMindmapBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.textSectionCompanyImportant.setText("公司10大重要工作");
        binding.textSectionCompanyDispatch.setText("公司10大派发任务");
        binding.textSectionPersonalImportant.setText("个人10大重要工作");
        binding.textSectionPersonalLog.setText("个人日志");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
package com.example.project_pandora.ui.focus;

import android.os.Bundle;

import com.example.project_pandora.ui.PandoraActivity;
import androidx.fragment.app.FragmentTransaction;

import com.example.project_pandora.R;
import com.example.project_pandora.databinding.ActivityFocusBinding;
import com.example.project_pandora.core.util.BackToolbarHelper;

public class FocusActivity extends PandoraActivity {

    private ActivityFocusBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFocusBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BackToolbarHelper.setupBack(binding.toolbar, this::finish);

        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.focusContainer, new FocusFragment());
            transaction.commit();
        }
    }
}
package com.example.project_pandora.ui.focus;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.example.project_pandora.R;
import com.example.project_pandora.databinding.ActivityFocusBinding;

public class FocusActivity extends AppCompatActivity {

    private ActivityFocusBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFocusBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.focusContainer, new FocusFragment());
            transaction.commit();
        }
    }
}
package com.example.project_pandora.ui.mine;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project_pandora.core.network.TokenManager;
import com.example.project_pandora.databinding.ActivityProfileBinding;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        TokenManager tokenManager = TokenManager.getInstance(this);

        binding.textRole.setText(tokenManager.getRole());

        binding.btnSave.setOnClickListener(v -> {
            Toast.makeText(this, "个人信息保存成功", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
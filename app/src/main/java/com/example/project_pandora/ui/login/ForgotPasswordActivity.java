package com.example.project_pandora.ui.login;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project_pandora.R;
import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.core.util.BackToolbarHelper;
import com.example.project_pandora.data.remote.AuthApi;
import com.example.project_pandora.databinding.ActivityForgotPasswordBinding;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ForgotPasswordActivity extends AppCompatActivity {

    private ActivityForgotPasswordBinding binding;
    private AuthApi authApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authApi = NetworkClient.getInstance(this).createService(AuthApi.class);

        BackToolbarHelper.setupBack(binding.toolbar, this::finish);
        binding.btnSubmit.setOnClickListener(v -> submit());
    }

    private void submit() {
        String username = binding.editUsername.getText().toString().trim();
        if (username.isEmpty()) {
            Toast.makeText(this, "请输入用户名或邮箱", Toast.LENGTH_SHORT).show();
            return;
        }
        String reason = binding.editReason.getText().toString().trim();

        binding.btnSubmit.setEnabled(false);
        binding.progressBar.setVisibility(View.VISIBLE);

        Map<String, String> request = new HashMap<>();
        request.put("username", username);
        if (!reason.isEmpty()) {
            request.put("reason", reason);
        }

        authApi.forgotPassword(request)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        response -> {
                            binding.btnSubmit.setEnabled(true);
                            binding.progressBar.setVisibility(View.GONE);
                            if (response.isSuccess()) {
                                Map<String, Object> data = response.getData();
                                String msg = data != null && data.get("message") != null
                                        ? data.get("message").toString() : "申请已提交";
                                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                                finish();
                            } else {
                                Toast.makeText(this, response.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        },
                        error -> {
                            binding.btnSubmit.setEnabled(true);
                            binding.progressBar.setVisibility(View.GONE);
                            Toast.makeText(this, "网络错误: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                );
    }
}
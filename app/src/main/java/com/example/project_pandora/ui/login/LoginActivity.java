package com.example.project_pandora.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project_pandora.MainActivity;
import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.core.network.TokenManager;
import com.example.project_pandora.data.remote.AuthApi;
import com.example.project_pandora.databinding.ActivityLoginBinding;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private AuthApi authApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authApi = NetworkClient.getInstance(this).createService(AuthApi.class);

        binding.btnLogin.setOnClickListener(v -> handleLogin());
        binding.btnGoRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }

    private void handleLogin() {
        String username = binding.editUsername.getText().toString().trim();
        String password = binding.editPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "请输入用户名和密码", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnLogin.setEnabled(false);
        binding.btnLogin.setText("登录中...");

        Map<String, String> request = new HashMap<>();
        request.put("username", username);
        request.put("password", password);

        authApi.login(request)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        response -> {
                            if (response.isSuccess() && response.getData() != null) {
                                Map<String, Object> data = response.getData();
                                String token = (String) data.get("token");
                                TokenManager tm = TokenManager.getInstance(this);
                                tm.saveToken(token);

                                Map<String, Object> user = (Map<String, Object>) data.get("user");
                                if (user != null) {
                                    Long userId = ((Number) user.get("id")).longValue();
                                    String role = (String) user.get("role");
                                    tm.saveUserInfo(userId, role);
                                }

                                startActivity(new Intent(this, MainActivity.class));
                                finish();
                            } else {
                                Toast.makeText(this, response.getMessage(), Toast.LENGTH_SHORT).show();
                                binding.btnLogin.setEnabled(true);
                                binding.btnLogin.setText("登 录");
                            }
                        },
                        error -> {
                            Toast.makeText(this, "登录失败: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            binding.btnLogin.setEnabled(true);
                            binding.btnLogin.setText("登 录");
                        }
                );
    }
}
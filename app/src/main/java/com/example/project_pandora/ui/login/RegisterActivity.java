package com.example.project_pandora.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project_pandora.MainActivity;
import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.core.network.TokenManager;
import com.example.project_pandora.data.remote.AuthApi;
import com.example.project_pandora.databinding.ActivityRegisterBinding;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private AuthApi authApi;

    private final String[] roleNames = {"普通员工", "团队负责人", "部门负责人", "联合创始人"};
    private final String[] roleValues = {"EMPLOYEE", "TEAM_LEADER", "DEPT_HEAD", "FOUNDER"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authApi = NetworkClient.getInstance(this).createService(AuthApi.class);

        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, roleNames);
        binding.spinnerRole.setAdapter(roleAdapter);
        binding.spinnerRole.setText(roleNames[0], false);

        binding.btnRegister.setOnClickListener(v -> handleRegister());
        binding.btnGoLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void handleRegister() {
        String username = binding.editUsername.getText().toString().trim();
        String password = binding.editPassword.getText().toString().trim();
        String confirmPassword = binding.editConfirmPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "请输入用户名和密码", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "密码长度不能少于6位", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "两次输入的密码不一致", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedPos = 0;
        String currentText = binding.spinnerRole.getText().toString();
        for (int i = 0; i < roleNames.length; i++) {
            if (roleNames[i].equals(currentText)) {
                selectedPos = i;
                break;
            }
        }
        String role = roleValues[selectedPos];

        binding.btnRegister.setEnabled(false);
        binding.btnRegister.setText("注册中...");

        Map<String, String> request = new HashMap<>();
        request.put("username", username);
        request.put("password", password);
        request.put("role", role);

        authApi.register(request)
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
                                    String userRole = (String) user.get("role");
                                    tm.saveUserInfo(userId, userRole);
                                }

                                startActivity(new Intent(this, MainActivity.class));
                                finish();
                            } else {
                                Toast.makeText(this, response.getMessage(), Toast.LENGTH_SHORT).show();
                                binding.btnRegister.setEnabled(true);
                                binding.btnRegister.setText("注 册");
                            }
                        },
                        error -> {
                            Toast.makeText(this, "注册失败: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            binding.btnRegister.setEnabled(true);
                            binding.btnRegister.setText("注 册");
                        }
                );
    }
}
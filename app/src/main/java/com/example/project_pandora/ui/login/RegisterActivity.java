package com.example.project_pandora.ui.login;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project_pandora.MainActivity;
import com.example.project_pandora.R;
import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.core.network.TokenManager;
import com.example.project_pandora.data.remote.AuthApi;
import com.example.project_pandora.databinding.ActivityRegisterBinding;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import retrofit2.HttpException;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private AuthApi authApi;

    private final String[] roleNames = {"员工", "团队长", "部门老总", "创始人", "管理员"};
    private final String[] roleValues = {"EMPLOYEE", "TEAM_LEADER", "DEPT_HEAD", "FOUNDER", "ADMIN"};

    private final BroadcastReceiver fieldSetter = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String field = intent.getStringExtra("field");
            String value = intent.getStringExtra("value");
            if (field == null || value == null) return;
            switch (field) {
                case "username": binding.editUsername.setText(value); break;
                case "password": binding.editPassword.setText(value); break;
                case "confirm_password": binding.editConfirmPassword.setText(value); break;
                case "email": binding.editEmail.setText(value); break;
                case "real_name": binding.editRealName.setText(value); break;
                case "id_card": binding.editIdCard.setText(value); break;
                case "phone": binding.editPhone.setText(value); break;
            }
        }
    };

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

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter("com.example.project_pandora.SET_FIELD");
        registerReceiver(fieldSetter, filter, Context.RECEIVER_EXPORTED);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(fieldSetter);
    }

    private void handleRegister() {
        String username = binding.editUsername.getText().toString().trim();
        String password = binding.editPassword.getText().toString().trim();
        String confirmPassword = binding.editConfirmPassword.getText().toString().trim();
        String email = binding.editEmail.getText().toString().trim();
        String realName = binding.editRealName.getText().toString().trim();
        String idCard = binding.editIdCard.getText().toString().trim();
        String phone = binding.editPhone.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.register_empty_input, Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, R.string.password_too_short, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, R.string.register_password_mismatch, Toast.LENGTH_SHORT).show();
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
        binding.btnRegister.setText(R.string.register_loading);

        Map<String, Object> request = new HashMap<>();
        request.put("username", username);
        request.put("password", password);
        request.put("role", role);
        if (!email.isEmpty()) request.put("email", email);
        if (!realName.isEmpty()) request.put("realName", realName);
        if (!idCard.isEmpty()) request.put("idCard", idCard);
        if (!phone.isEmpty()) request.put("phone", phone);

        authApi.register(request)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        response -> {
                            if (response.isSuccess() && response.getData() != null) {
                                Map<String, Object> data = response.getData();
                                Map<String, Object> user = (Map<String, Object>) data.get("user");
                                String approvalStatus = user != null ? (String) user.get("approvalStatus") : null;
                                String token = (String) data.get("token");

                                if (token != null && approvalStatus != null && "APPROVED".equals(approvalStatus)) {
                                    TokenManager tm = TokenManager.getInstance(this);
                                    tm.saveToken(token);
                                    if (user != null) {
                                        Long userId = ((Number) user.get("id")).longValue();
                                        String userRole = (String) user.get("role");
                                        tm.saveUserInfo(userId, userRole);
                                    }
                                    Toast.makeText(this, R.string.register_approved, Toast.LENGTH_LONG).show();
                                    startActivity(new Intent(this, MainActivity.class));
                                    finish();
                                } else {
                                    Toast.makeText(this, R.string.register_pending, Toast.LENGTH_LONG).show();
                                    startActivity(new Intent(this, LoginActivity.class));
                                    finish();
                                }
                            } else {
                                Toast.makeText(this, response.getMessage(), Toast.LENGTH_SHORT).show();
                                binding.btnRegister.setEnabled(true);
                                binding.btnRegister.setText(R.string.btn_register);
                            }
                        },
                        error -> {
                            String errMsg;
                            if (error instanceof HttpException) {
                                try {
                                    String body = ((HttpException) error).response().errorBody().string();
                                    Type type = new TypeToken<Map<String, Object>>(){}.getType();
                                    Map<String, Object> errMap = new Gson().fromJson(body, type);
                                    errMsg = errMap != null && errMap.get("message") != null
                                            ? errMap.get("message").toString() : "注册失败";
                                } catch (Exception e) {
                                    errMsg = "注册失败: " + error.getMessage();
                                }
                            } else {
                                errMsg = "网络错误: " + error.getMessage();
                            }
                            Toast.makeText(this, errMsg, Toast.LENGTH_SHORT).show();
                            binding.btnRegister.setEnabled(true);
                            binding.btnRegister.setText(R.string.btn_register);
                        }
                );
    }
}

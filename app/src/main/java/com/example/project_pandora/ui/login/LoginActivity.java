package com.example.project_pandora.ui.login;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.example.project_pandora.MainActivity;
import com.example.project_pandora.R;
import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.core.network.NetworkStateMonitor;
import com.example.project_pandora.core.network.ServerConnectivityMonitor;
import com.example.project_pandora.core.network.TokenManager;
import com.example.project_pandora.core.sync.ServerConnectivity;

import com.example.project_pandora.data.remote.AuthApi;
import com.example.project_pandora.data.remote.UserApi;
import com.example.project_pandora.databinding.ActivityLoginBinding;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;

import retrofit2.HttpException;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private AuthApi authApi;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private static final String CHANNEL_ID = "pandora_notifications";
    private static final int NOTIF_SERVER_DISCONNECTED = 1;
    private static final int NOTIF_APPROVAL_RESULT = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authApi = NetworkClient.getInstance(this).createService(AuthApi.class);
        createNotificationChannel();


        autoLogin();

        binding.btnLogin.setOnClickListener(v -> handleLogin());

        binding.btnForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(this, ForgotPasswordActivity.class));
        });
        binding.btnRefreshServer.setOnClickListener(v -> {
            binding.btnRefreshServer.setVisibility(android.view.View.GONE);
            binding.textServerStatus.setText(R.string.server_checking);
            binding.textServerStatus.setTextColor(0xFFFF9800);
            ServerConnectivityMonitor.getInstance().checkNow();
        });

        // Round29 T7 修复: 生效地址不可达时的自救入口（ServerConfigActivity 无登录依赖）
        binding.btnLoginServerConfig.setOnClickListener(v ->
                startActivity(new Intent(this, com.example.project_pandora.ui.mine.ServerConfigActivity.class)));

        ServerConnectivityMonitor.getInstance().startMonitoring(this);
        disposables.add(ServerConnectivityMonitor.getInstance().stateStream()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        state -> updateServerStatusUI(state),
                        error -> {}
                ));
    }

    @Override
    protected void onResume() {
        super.onResume();
        ServerConnectivityMonitor.getInstance().onAppForeground();
    }

    @Override
    protected void onPause() {
        super.onPause();
        ServerConnectivityMonitor.getInstance().onAppBackground();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }

    private void updateServerStatusUI(ServerConnectivity state) {
        switch (state) {
            case CONNECTED:
                binding.textServerStatus.setText(R.string.server_connected);
                binding.textServerStatus.setTextColor(0xFF4CAF50);
                binding.btnRefreshServer.setVisibility(android.view.View.GONE);
                binding.btnLoginServerConfig.setVisibility(android.view.View.GONE);
                break;
            case DISCONNECTED:
                binding.textServerStatus.setText(R.string.server_retry_failed);
                binding.textServerStatus.setTextColor(0xFFF44336);
                binding.btnRefreshServer.setVisibility(android.view.View.VISIBLE);
                binding.btnLoginServerConfig.setVisibility(android.view.View.VISIBLE);
                showNotification(NOTIF_SERVER_DISCONNECTED,
                        getString(R.string.notification_server_status_title),
                        getString(R.string.notification_server_disconnected));
                break;
            case CHECKING:
                binding.textServerStatus.setText(R.string.server_checking);
                binding.textServerStatus.setTextColor(0xFFFF9800);
                break;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private void showNotification(int id, String title, String content) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(true);
        getSystemService(NotificationManager.class).notify(id, builder.build());
    }

    private void handleLogin() {
        if (ServerConnectivityMonitor.getInstance().getCurrentState() == ServerConnectivity.DISCONNECTED) {
            Toast.makeText(this, "无法连接服务器，请检查网络", Toast.LENGTH_SHORT).show();
            return;
        }

        String username = binding.editUsername.getText().toString().trim();
        String password = binding.editPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.login_empty_input, Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnLogin.setEnabled(false);
        binding.btnLogin.setText(R.string.login_loading);

        Map<String, String> request = new HashMap<>();
        request.put("username", username);
        request.put("password", password);
        request.put("clientType", "APP");

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

                                tm.saveLastUsername(username);

                                ServerConnectivity conn = ServerConnectivityMonitor.getInstance().getCurrentState();
                                NetworkStateMonitor.getInstance().onServerConnectivityChanged(conn);

                                startActivity(new Intent(this, MainActivity.class));
        com.example.project_pandora.notification.PollingForegroundService.startIfLoggedIn(this);
                                finish();
                            } else {
                                String msg = response.getMessage();
                                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                                if (msg.contains("待审批")) {
                                    showNotification(NOTIF_APPROVAL_RESULT,
                                            getString(R.string.notification_approval_title),
                                            msg);
                                }
                                binding.btnLogin.setEnabled(true);
                                binding.btnLogin.setText(R.string.btn_login);
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
                                            ? errMap.get("message").toString() : "登录失败";
                                } catch (Exception e) {
                                    errMsg = "登录失败: " + error.getMessage();
                                }
                            } else {
                                errMsg = "网络错误: " + error.getMessage();
                            }
                            Toast.makeText(this, errMsg, Toast.LENGTH_SHORT).show();
                            binding.btnLogin.setEnabled(true);
                            binding.btnLogin.setText(R.string.btn_login);
                        }
                );
    }

    private void autoLogin() {
        TokenManager tm = TokenManager.getInstance(this);
        if (tm.getToken() == null) {
            return;
        }
        if (ServerConnectivityMonitor.getInstance().getCurrentState() == ServerConnectivity.DISCONNECTED) {
            return;
        }

        UserApi userApi = NetworkClient.getInstance(this).createService(UserApi.class);
        disposables.add(userApi.getCurrentUser()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        response -> {
                            if (response.isSuccess()) {
                                startActivity(new Intent(this, MainActivity.class));
        com.example.project_pandora.notification.PollingForegroundService.startIfLoggedIn(this);
                                finish();
                            }
                        },
                        error -> {
                            if (error instanceof HttpException && ((HttpException) error).code() == 401) {
                                tm.clear();
                            }
                        }
                ));
    }
}

package com.example.project_pandora.ui.mine;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project_pandora.R;
import com.example.project_pandora.core.network.ServerUrlManager;
import com.example.project_pandora.core.util.BackToolbarHelper;
import com.example.project_pandora.databinding.ActivityServerConfigBinding;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Round29: 服务器地址（BaseUrl）配置页。
 * 单字段编辑 + 测试连接（当前输入值即测，与保存解耦）+ 保存 + 恢复默认。
 * 地址明文回显（非敏感数据，区别于 AI Key 掩码处理）。
 * 保存归一化后持久化，NetworkClient 经 BaseUrlInterceptor 全局即时生效。
 */
public class ServerConfigActivity extends AppCompatActivity {

    private ActivityServerConfigBinding binding;
    private ServerUrlManager serverUrlManager;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private boolean isTesting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityServerConfigBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        serverUrlManager = ServerUrlManager.getInstance(this);

        BackToolbarHelper.setupBack(binding.toolbar, this::finish);

        loadCurrentConfig();

        binding.btnTest.setOnClickListener(v -> testConnection());
        binding.btnSave.setOnClickListener(v -> saveConfig());
        binding.btnReset.setOnClickListener(v -> resetConfig());
    }

    @Override
    protected void onDestroy() {
        disposables.clear();
        super.onDestroy();
    }

    /** 回显当前持久化配置（地址明文，存储时已归一化） */
    private void loadCurrentConfig() {
        binding.editServerUrl.setText(serverUrlManager.getConfiguredUrl());
    }

    /** 保存：校验 http/https 前缀 → 归一化 → 持久化 */
    private void saveConfig() {
        String raw = binding.editServerUrl.getText().toString().trim();

        if (!raw.isEmpty() && !(raw.startsWith("http://") || raw.startsWith("https://"))) {
            Toast.makeText(this, R.string.toast_server_url_invalid, Toast.LENGTH_SHORT).show();
            return;
        }

        serverUrlManager.saveUrl(raw);
        // 归一化后回写输入框（让用户看到生效形态）
        binding.editServerUrl.setText(serverUrlManager.getConfiguredUrl());
        Toast.makeText(this, R.string.toast_server_saved, Toast.LENGTH_SHORT).show();
    }

    /** 恢复默认：清空配置 = 回退默认双轨地址 */
    private void resetConfig() {
        serverUrlManager.clear();
        binding.editServerUrl.setText("");
        Toast.makeText(this, R.string.toast_server_reset, Toast.LENGTH_SHORT).show();
    }

    /** 测试连接：当前输入值（未保存也可测），对归一化地址发 GET /actuator/health */
    private void testConnection() {
        if (isTesting) return;
        String raw = binding.editServerUrl.getText().toString().trim();

        if (!(raw.startsWith("http://") || raw.startsWith("https://"))) {
            Toast.makeText(this, R.string.toast_server_url_invalid, Toast.LENGTH_SHORT).show();
            return;
        }

        isTesting = true;
        binding.btnTest.setEnabled(false);
        binding.btnTest.setText(R.string.server_testing);

        final String normalized = ServerUrlManager.normalizeBaseUrl(raw);
        disposables.add(io.reactivex.rxjava3.core.Single.<String>fromCallable(() -> probe(normalized))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        message -> {
                            resetTestButton();
                            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                        },
                        error -> {
                            resetTestButton();
                            Toast.makeText(this, getString(R.string.toast_server_test_fail, briefError(error)), Toast.LENGTH_LONG).show();
                        }
                ));
    }

    private void resetTestButton() {
        isTesting = false;
        binding.btnTest.setEnabled(true);
        binding.btnTest.setText(R.string.btn_test_server);
    }

    /**
     * 同步探测（io 线程执行）：GET {归一化地址}/actuator/health，HTTP 2xx 即连通。
     * 独立 OkHttpClient（不经 NetworkClient，防 BaseUrlInterceptor 重写自环——沿 Round28 独立探测模式）。
     */
    private String probe(String normalizedBaseUrl) throws Exception {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url(normalizedBaseUrl + "actuator/health")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                return getString(R.string.toast_server_test_ok, response.code());
            }
            return getString(R.string.toast_server_test_fail, "HTTP " + response.code());
        }
    }

    /** 异常归类为中文提示（沿 Round28 briefError 模式） */
    private String briefError(Throwable error) {
        if (error instanceof java.net.SocketTimeoutException) {
            return getString(R.string.err_server_timeout);
        }
        if (error instanceof java.net.UnknownHostException) {
            return getString(R.string.err_server_unknown_host);
        }
        if (error instanceof java.net.ConnectException) {
            return getString(R.string.err_server_connect);
        }
        String msg = error.getMessage();
        if (msg == null) {
            return error.getClass().getSimpleName();
        }
        if (msg.length() > 60) {
            msg = msg.substring(0, 60) + "...";
        }
        return msg;
    }
}
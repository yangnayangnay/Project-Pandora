package com.example.project_pandora.ui.ai;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project_pandora.core.network.AiClientConfig;
import com.example.project_pandora.core.network.AiConfigManager;
import com.example.project_pandora.core.util.BackToolbarHelper;
import com.example.project_pandora.databinding.ActivityAiConfigBinding;
import com.google.gson.JsonObject;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Round28 REQ-28-01: 客户端 AI 模型配置页。
 * 四字段（端点/Key/模型/启用）编辑 + 测试连接（当前输入值即测，与保存解耦）+ 保存。
 * Key 回显掩码，掩码未被改动时保存沿用原 Key。
 */
public class AiConfigActivity extends AppCompatActivity {

    private ActivityAiConfigBinding binding;
    private AiConfigManager aiConfigManager;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private boolean isTesting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAiConfigBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        aiConfigManager = AiConfigManager.getInstance(this);

        BackToolbarHelper.setupBack(binding.toolbar, this::finish);

        loadCurrentConfig();

        binding.btnTest.setOnClickListener(v -> testConnection());
        binding.btnSave.setOnClickListener(v -> saveConfig());
    }

    /** 回显当前持久化配置（Key 以掩码展示） */
    private void loadCurrentConfig() {
        AiClientConfig c = aiConfigManager.getConfig();
        binding.editApiUrl.setText(c.apiUrl);
        binding.editApiKey.setText(AiConfigManager.maskKey(c.apiKey));
        binding.editModel.setText(c.model);
        binding.switchEnable.setChecked(c.enabled);
    }

    /** 测试连接：用当前输入值直发 OpenAI 格式探测（独立 OkHttpClient，不经 NetworkClient 防自环） */
    private void testConnection() {

        if (isTesting) return;
        String url = binding.editApiUrl.getText().toString().trim();
        String key = resolveInputKey();
        String model = binding.editModel.getText().toString().trim();

        if (!(url.startsWith("http://") || url.startsWith("https://"))) {
            Toast.makeText(this, "请先填写合法的接口地址", Toast.LENGTH_SHORT).show();
            return;
        }
        if (model.isEmpty()) {
            Toast.makeText(this, "请先填写模型名称", Toast.LENGTH_SHORT).show();
            return;
        }

        isTesting = true;
        binding.btnTest.setEnabled(false);
        binding.btnTest.setText("测试中...");

        final String fUrl = url;
        final String fKey = key;
        final String fModel = model;

        disposables.add(io.reactivex.rxjava3.core.Single.<String>fromCallable(() -> probe(fUrl, fKey, fModel))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        message -> {
                            resetTestButton();
                            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                        },
                        error -> {
                            resetTestButton();
                            Toast.makeText(this, "连接失败：" + briefError(error), Toast.LENGTH_LONG).show();
                        }
                ));
    }

    private void resetTestButton() {
        isTesting = false;
        binding.btnTest.setEnabled(true);
        binding.btnTest.setText("测试连接");
    }

    /** 同步探测（io 线程执行）：HTTP 2xx 即连通 */
    private String probe(String url, String key, String model) throws Exception {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .build();

        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", "ping");
        com.google.gson.JsonArray messages = new com.google.gson.JsonArray();
        messages.add(message);
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.add("messages", messages);
        body.addProperty("max_tokens", 8);

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + key)
                .post(RequestBody.create(body.toString(), MediaType.parse("application/json; charset=utf-8")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                return "连接成功（HTTP " + response.code() + "）";
            }
            return "接口可达但返回异常（HTTP " + response.code() + "），请核对地址与鉴权";
        }
    }

    /** 保存：校验后落盘；Key 掩码未被改动时沿用原 Key */
    private void saveConfig() {
        String url = binding.editApiUrl.getText().toString().trim();
        String key = resolveInputKey();
        String model = binding.editModel.getText().toString().trim();
        boolean enabled = binding.switchEnable.isChecked();

        if (!url.isEmpty() && !(url.startsWith("http://") || url.startsWith("https://"))) {
            Toast.makeText(this, "接口地址须以 http:// 或 https:// 开头", Toast.LENGTH_SHORT).show();
            return;
        }
        if (enabled && url.isEmpty()) {
            Toast.makeText(this, "启用前请填写接口地址", Toast.LENGTH_SHORT).show();
            return;
        }
        if (enabled && model.isEmpty()) {
            Toast.makeText(this, "启用前请填写模型名称", Toast.LENGTH_SHORT).show();
            return;
        }

        aiConfigManager.saveConfig(new AiClientConfig(url, key, model, enabled));
        Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
    }

    /** 输入框 Key 解析：与已存 Key 的掩码一致视为未修改，沿用原值 */
    private String resolveInputKey() {
        String input = binding.editApiKey.getText().toString();
        String stored = aiConfigManager.getConfig().apiKey;
        if (!stored.trim().isEmpty() && input.trim().equals(AiConfigManager.maskKey(stored))) {
            return stored;
        }
        return input.trim();
    }

    private String briefError(Throwable error) {
        String name = error.getClass().getSimpleName();
        if (error instanceof java.net.SocketTimeoutException) {
            return "连接超时";
        }
        if (error instanceof java.net.UnknownHostException || error instanceof java.net.ConnectException) {
            return "无法连接到该地址";
        }
        String msg = error.getMessage();
        if (msg == null) {
            return name;
        }
        if (msg.length() > 60) {
            msg = msg.substring(0, 60) + "...";
        }
        return msg;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }
}
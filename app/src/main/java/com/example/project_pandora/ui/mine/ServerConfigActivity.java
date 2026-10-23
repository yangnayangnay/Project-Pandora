package com.example.project_pandora.ui.mine;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.project_pandora.R;
import com.example.project_pandora.core.network.ServerEntry;
import com.example.project_pandora.core.network.ServerUrlManager;
import com.example.project_pandora.core.util.BackToolbarHelper;
import com.example.project_pandora.databinding.ActivityServerConfigBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Round29: 服务器地址（BaseUrl）配置页。
 * R44 REQ-A: 升级为服务端列表管理——RecyclerView 条目（生效徽标/启停/上移下移/单条测试/删除）
 * + 底部添加区（校验/归一化/去重/上限）+ 删除守卫（空列表回退默认预置）。
 * 保存经 ServerUrlManager.saveServerList，BaseUrlInterceptor 次请求即生效。
 */
public class ServerConfigActivity extends AppCompatActivity implements ServerListAdapter.Listener {

    private ActivityServerConfigBinding binding;
    private ServerUrlManager serverUrlManager;
    private ServerListAdapter adapter;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private boolean isTesting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityServerConfigBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        serverUrlManager = ServerUrlManager.getInstance(this);

        BackToolbarHelper.setupBack(binding.toolbar, this::finish);

        adapter = new ServerListAdapter(serverUrlManager, this);
        binding.recyclerServerList.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerServerList.setAdapter(adapter);
        reloadList();

        binding.btnTest.setOnClickListener(v -> testConnection());
        binding.btnAdd.setOnClickListener(v -> addServer());
    }

    @Override
    protected void onDestroy() {
        disposables.clear();
        super.onDestroy();
    }

    private void reloadList() {
        adapter.submit(serverUrlManager.getServerList());
    }

    /** 添加：校验前缀 → 归一化 → 去重 → 上限 → 入列表整表保存 */
    private void addServer() {
        String raw = binding.editServerUrl.getText().toString().trim();
        if (!(raw.startsWith("http://") || raw.startsWith("https://"))) {
            Toast.makeText(this, R.string.toast_server_url_invalid, Toast.LENGTH_SHORT).show();
            return;
        }
        String normalized = ServerUrlManager.normalizeBaseUrl(raw);
        if (normalized.isEmpty()) {
            Toast.makeText(this, R.string.toast_server_url_invalid, Toast.LENGTH_SHORT).show();
            return;
        }
        List<ServerEntry> list = new ArrayList<>(serverUrlManager.getServerList());
        if (list.size() >= ServerUrlManager.MAX_SERVERS) {
            Toast.makeText(this, R.string.toast_server_limit, Toast.LENGTH_SHORT).show();
            return;
        }
        for (ServerEntry e : list) {
            if (e.getUrl().equals(normalized)) {
                Toast.makeText(this, R.string.toast_server_duplicate, Toast.LENGTH_SHORT).show();
                return;
            }
        }
        list.add(new ServerEntry(normalized, true));
        try {
            serverUrlManager.saveServerList(list);
        } catch (ServerUrlManager.ServerListException ex) {
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }
        binding.editServerUrl.setText("");
        reloadList();
        Toast.makeText(this, R.string.toast_server_saved, Toast.LENGTH_SHORT).show();
    }

    // ==================== ServerListAdapter.Listener ====================

    @Override
    public void onListChanged(List<ServerEntry> newList) {
        reloadList();
    }

    @Override
    public void onItemTest(String url) {
        runProbe(url);
    }

    @Override
    public void onDelete(int position) {
        List<ServerEntry> list = new ArrayList<>(serverUrlManager.getServerList());
        if (position < 0 || position >= list.size()) return;
        list.remove(position);
        if (list.isEmpty()) {
            // 删除守卫：空列表回退默认预置（REQ-A 规则 7）
            list.add(new ServerEntry(serverUrlManager.getEffectiveBaseUrl(), true));
            serverUrlManager.clear();
            Toast.makeText(this, R.string.toast_server_reset, Toast.LENGTH_SHORT).show();
        } else {
            try {
                serverUrlManager.saveServerList(list);
            } catch (ServerUrlManager.ServerListException ex) {
                Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
        }
        reloadList();
    }

    @Override
    public void onError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        reloadList();
    }

    // ==================== 测试连接（添加区，与保存解耦） ====================

    private void testConnection() {
        if (isTesting) return;
        String raw = binding.editServerUrl.getText().toString().trim();
        if (!(raw.startsWith("http://") || raw.startsWith("https://"))) {
            Toast.makeText(this, R.string.toast_server_url_invalid, Toast.LENGTH_SHORT).show();
            return;
        }
        runProbe(ServerUrlManager.normalizeBaseUrl(raw));
    }

    private void runProbe(String normalized) {
        if (isTesting || normalized == null || normalized.isEmpty()) return;
        isTesting = true;
        binding.btnTest.setEnabled(false);
        binding.btnTest.setText(R.string.server_testing);

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
     * 同步探测（io 线程）：GET {归一化地址}/actuator/health，2xx 即连通。
     * 独立 OkHttpClient（防 BaseUrlInterceptor 自环——沿 Round28 模式）。
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
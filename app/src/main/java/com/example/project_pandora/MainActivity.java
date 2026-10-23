package com.example.project_pandora;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.project_pandora.core.crash.CrashHandler;
import com.example.project_pandora.core.logger.PandoraLogger;
import com.example.project_pandora.core.network.NetworkStateMonitor;
import com.example.project_pandora.core.network.ServerConnectivityMonitor;
import com.example.project_pandora.core.network.TokenManager;
import com.example.project_pandora.core.sync.NetworkMode;
import com.example.project_pandora.core.sync.SyncManager;
import com.example.project_pandora.core.sync.SyncProgress;
import com.example.project_pandora.core.sync.SyncState;
import com.example.project_pandora.core.sync.SyncWorker;
import com.example.project_pandora.databinding.ActivityMainBinding;
import com.example.project_pandora.notification.NotificationPollWorker;
import com.example.project_pandora.notification.NotificationPermissionHelper;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private final CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        PandoraLogger.init(this);
        CrashHandler.register(this);

        if (TokenManager.getInstance(this).isLoggedIn()) {
            SyncWorker.enqueueOneTime(this);
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.mainContainer, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupBottomNavigation();
        adjustBottomNavHeight();
        initSyncStatusBar();
    }

    private void initSyncStatusBar() {
        SyncManager syncManager = SyncManager.getInstance();

        disposables.add(syncManager.statusStream()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(state -> updateSyncBar()));

        disposables.add(syncManager.progressStream()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(progress -> updateSyncBar()));

        disposables.add(NetworkStateMonitor.getInstance().modeStream()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mode -> updateSyncBar()));
    }

    private void updateSyncBar() {
        SyncManager syncManager = SyncManager.getInstance();
        SyncState state = syncManager.getCurrentStatus();
        SyncProgress progress = syncManager.getCurrentProgress();
        NetworkMode mode = NetworkStateMonitor.getInstance().getCurrentMode();

        if (mode == NetworkMode.OFFLINE) {
            binding.syncStatusBar.setVisibility(View.VISIBLE);
            binding.textSyncIndicator.setText("离线模式 - 数据将保存至本地");
            binding.syncStatusBar.setBackgroundColor(0xFFFFF3E0);
            binding.textSyncIndicator.setTextColor(0xFFE65100);
            return;
        }

        switch (state) {
            case SYNCING:
                binding.syncStatusBar.setVisibility(View.VISIBLE);
                if (progress != null && progress.getTotal() > 0) {
                    binding.textSyncIndicator.setText("同步中: " + progress.displayText());
                } else {
                    binding.textSyncIndicator.setText("同步中...");
                }
                binding.syncStatusBar.setBackgroundColor(0xFFE3F2FD);
                binding.textSyncIndicator.setTextColor(0xFF1565C0);
                break;
            case PARTIAL_FAILED:
                binding.syncStatusBar.setVisibility(View.VISIBLE);
                if (progress != null) {
                    binding.textSyncIndicator.setText(progress.summaryText());
                } else {
                    binding.textSyncIndicator.setText("部分同步失败");
                }
                binding.syncStatusBar.setBackgroundColor(0xFFFFF3E0);
                binding.textSyncIndicator.setTextColor(0xFFE65100);
                break;
            case TOKEN_EXPIRED:
                binding.syncStatusBar.setVisibility(View.VISIBLE);
                binding.textSyncIndicator.setText("登录已过期，请重新登录");
                binding.syncStatusBar.setBackgroundColor(0xFFFFEBEE);
                binding.textSyncIndicator.setTextColor(0xFFC62828);
                break;
            default:
                binding.syncStatusBar.setVisibility(View.GONE);
                break;
        }
    }

    private void setupBottomNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
        }
    }

    private void adjustBottomNavHeight() {
        binding.bottomNavigation.post(() -> {
            int screenHeight = getResources().getDisplayMetrics().heightPixels;
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int maxHeight = (int) (screenHeight * 0.12);

            int widthSpec = View.MeasureSpec.makeMeasureSpec(screenWidth, View.MeasureSpec.EXACTLY);
            int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            binding.bottomNavigation.measure(widthSpec, heightSpec);
            int desiredHeight = binding.bottomNavigation.getMeasuredHeight();

            if (desiredHeight > maxHeight && maxHeight > 0) {
                ViewGroup.LayoutParams params = binding.bottomNavigation.getLayoutParams();
                params.height = maxHeight;
                binding.bottomNavigation.setLayoutParams(params);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        ServerConnectivityMonitor.getInstance().onAppForeground();
    }

    /**
     * TASK-R18-017: 应用切回前台时触发一次性立即通知轮询（OneTimeWorkRequest，REPLACE 防重复入队）
     * 实现后台 15 分钟 / 前台立即的通知及时性
     * Round24 REQ-24-09: 已登录分支同时触发通知权限运行时申请（会话内去重，覆盖登录后/冷启已登录两条路径）
     */
    @Override
    protected void onStart() {
        super.onStart();
        if (TokenManager.getInstance(this).isLoggedIn()) {
            NotificationPollWorker.enqueueOneTimeWork(this);
            com.example.project_pandora.notification.PollingForegroundService.startIfLoggedIn(this);
            NotificationPermissionHelper.requestOnce(this);
        }
    }

    /**
     * Round24 REQ-24-09: 通知权限结果不做业务分支——拒绝时应用内轮询/业务全正常
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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
}

package com.example.project_pandora.ui.mine;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project_pandora.core.config.LogCountLimit;
import com.example.project_pandora.core.config.LogDisplayLimitConfig;
import com.example.project_pandora.core.config.LogTimeLimit;
import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.core.network.NetworkStateMonitor;
import com.example.project_pandora.core.sync.NetworkMode;
import com.example.project_pandora.core.sync.SyncManager;
import com.example.project_pandora.core.sync.SyncProgress;
import com.example.project_pandora.core.sync.SyncState;
import com.example.project_pandora.core.util.BackToolbarHelper;
import com.example.project_pandora.data.remote.UserApi;
import com.example.project_pandora.databinding.ActivitySettingsBinding;
import com.example.project_pandora.repository.SyncRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private UserApi userApi;
    private LogDisplayLimitConfig logDisplayLimitConfig;
    private SyncManager syncManager;
    private SyncRepository syncRepository;
    private final CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        userApi = NetworkClient.getInstance(this).createService(UserApi.class);
        logDisplayLimitConfig = new LogDisplayLimitConfig(this);
        syncManager = SyncManager.getInstance();
        syncRepository = new SyncRepository(this);

        BackToolbarHelper.setupBack(binding.toolbar, this::finish);

        loadSettings();
        loadLocalLogDisplaySettings();
        initSyncCard();

        binding.switchShowCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) return;
            updateShowCompleted(isChecked);
        });

        binding.switchShowYesterdayLog.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) return;
            updateShowYesterdayLog(isChecked);
        });

        binding.cardLogTimeLimit.setOnClickListener(v -> showTimeLimitSelector());
        binding.cardLogCountLimit.setOnClickListener(v -> showCountLimitSelector());
    }

    private void loadSettings() {
        userApi.getUserSettings()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        response -> {
                            if (response.isSuccess() && response.getData() != null) {
                                Map<String, Object> data = response.getData();
                                Object showCompleted = data.get("showCompletedTasks");
                                if (showCompleted == null) showCompleted = data.get("SHOW_COMPLETED_TASKS");
                                binding.switchShowCompleted.setChecked(
                                        showCompleted == null || Integer.parseInt(showCompleted.toString()) == 1);

                                Object showYesterday = data.get("showYesterdayLog");
                                if (showYesterday == null) showYesterday = data.get("SHOW_YESTERDAY_LOG");
                                binding.switchShowYesterdayLog.setChecked(
                                        showYesterday == null || Integer.parseInt(showYesterday.toString()) == 1);
                            }
                        },
                        error -> {
                            binding.switchShowCompleted.setChecked(true);
                            binding.switchShowYesterdayLog.setChecked(true);

                        }
                );
    }

    private void loadLocalLogDisplaySettings() {
        int timeDays = logDisplayLimitConfig.getTimeLimitDays();
        int countLimit = logDisplayLimitConfig.getCountLimit();
        binding.textLogTimeLimitValue.setText(LogTimeLimit.fromDays(timeDays).getDisplayName());
        binding.textLogCountLimitValue.setText(LogCountLimit.fromCount(countLimit).getDisplayName());
    }

    private void showTimeLimitSelector() {
        List<LogTimeLimit> options = LogTimeLimit.orderedValues();
        int currentDays = logDisplayLimitConfig.getTimeLimitDays();
        int checkedItem = 0;
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).getDays() == currentDays) {
                checkedItem = i;
                break;
            }
        }
        String[] items = new String[options.size()];
        for (int i = 0; i < options.size(); i++) {
            items[i] = options.get(i).getDisplayName();
        }
        new AlertDialog.Builder(this)
                .setTitle("直接展示的日志限制时间")
                .setSingleChoiceItems(items, checkedItem, (dialog, which) -> {
                    LogTimeLimit selected = options.get(which);
                    boolean success = logDisplayLimitConfig.setTimeLimitDays(selected.getDays());
                    if (success) {
                        binding.textLogTimeLimitValue.setText(selected.getDisplayName());
                    } else {
                        Toast.makeText(this, "保存失败，请重试", Toast.LENGTH_SHORT).show();
                    }
                    dialog.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showCountLimitSelector() {
        List<LogCountLimit> options = LogCountLimit.orderedValues();
        int currentCount = logDisplayLimitConfig.getCountLimit();
        int checkedItem = 0;
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).getCount() == currentCount) {
                checkedItem = i;
                break;
            }
        }
        String[] items = new String[options.size()];
        for (int i = 0; i < options.size(); i++) {
            items[i] = options.get(i).getDisplayName();
        }
        new AlertDialog.Builder(this)
                .setTitle("直接展示的日志限制数量")
                .setSingleChoiceItems(items, checkedItem, (dialog, which) -> {
                    LogCountLimit selected = options.get(which);
                    boolean success = logDisplayLimitConfig.setCountLimit(selected.getCount());
                    if (success) {
                        binding.textLogCountLimitValue.setText(selected.getDisplayName());
                    } else {
                        Toast.makeText(this, "保存失败，请重试", Toast.LENGTH_SHORT).show();
                    }
                    dialog.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void updateShowCompleted(boolean showCompleted) {
        Map<String, Object> request = new HashMap<>();
        request.put("showCompletedTasks", showCompleted);

        userApi.updateUserSettings(request)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        response -> {},
                        error -> {
                            binding.switchShowCompleted.setChecked(!showCompleted);
                            Toast.makeText(this, "\u4FDD\u5B58\u5931\u8D25\uFF0C\u8BF7\u91CD\u8BD5", Toast.LENGTH_SHORT).show();
                        }
                );
    }

    private void updateShowYesterdayLog(boolean showYesterday) {
        Map<String, Object> request = new HashMap<>();
        request.put("showYesterdayLog", showYesterday);

        userApi.updateUserSettings(request)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        response -> {},
                        error -> {
                            binding.switchShowYesterdayLog.setChecked(!showYesterday);
                            Toast.makeText(this, "\u4FDD\u5B58\u5931\u8D25\uFF0C\u8BF7\u91CD\u8BD5", Toast.LENGTH_SHORT).show();
                        }
                );
    }

    private void initSyncCard() {
        disposables.add(syncRepository.hasUnsyncedData()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(hasUnsynced -> {
                    if (hasUnsynced) {
                        binding.btnResync.setVisibility(View.VISIBLE);
                        updateSyncStatusText();
                    } else {
                        binding.textSyncStatus.setText("所有数据已同步");
                        binding.btnResync.setVisibility(View.GONE);
                        binding.textSyncProgress.setVisibility(View.GONE);
                    }
                }, error -> binding.textSyncStatus.setText("同步状态未知")));

        binding.btnResync.setOnClickListener(v -> {
            if (NetworkStateMonitor.getInstance().getCurrentMode() == NetworkMode.OFFLINE) {
                Toast.makeText(this, "请先连接网络", Toast.LENGTH_SHORT).show();
                return;
            }
            binding.btnResync.setEnabled(false);
            binding.btnResync.setText("同步中...");
            disposables.add(syncManager.manualResync()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            progress -> {
                                if (progress.isCompleted()) {
                                    binding.textSyncProgress.setText(progress.summaryText());
                                    binding.btnResync.setEnabled(true);
                                    binding.btnResync.setText("重新同步");
                                } else {
                                    binding.textSyncProgress.setVisibility(View.VISIBLE);
                                    binding.textSyncProgress.setText(progress.displayText());
                                }
                            },
                            error -> {
                                binding.btnResync.setEnabled(true);
                                binding.btnResync.setText("重新同步");
                                Toast.makeText(this, "同步失败: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                    ));
        });

        disposables.add(syncManager.statusStream()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(state -> {
                    updateSyncStatusText();
                }));

        disposables.add(syncManager.progressStream()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(progress -> {
                    if (!progress.isCompleted() && progress.getTotal() > 0) {
                        binding.textSyncProgress.setVisibility(View.VISIBLE);
                        binding.textSyncProgress.setText(progress.displayText());
                    } else if (progress.isCompleted() && progress.getTotal() > 0) {
                        binding.textSyncProgress.setText(progress.summaryText());
                        binding.btnResync.setEnabled(true);
                        binding.btnResync.setText("重新同步");
                    }
                }));
    }

    private void updateSyncStatusText() {
        SyncState state = syncManager.getCurrentStatus();
        NetworkMode mode = NetworkStateMonitor.getInstance().getCurrentMode();
        if (mode == NetworkMode.OFFLINE) {
            binding.textSyncStatus.setText("离线模式");
            return;
        }
        if (state == SyncState.SYNCING) {
            binding.textSyncStatus.setText("同步中...");
            return;
        }
        disposables.add(syncRepository.hasUnsyncedData()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(hasUnsynced -> {
                    if (!hasUnsynced) {
                        binding.textSyncStatus.setText("所有数据已同步");
                    } else if (state == SyncState.PARTIAL_FAILED) {
                        binding.textSyncStatus.setText("部分同步失败，可重新同步");
                    } else if (state == SyncState.TOKEN_EXPIRED) {
                        binding.textSyncStatus.setText("登录已过期，请重新登录");
                    } else {
                        binding.textSyncStatus.setText("有数据待同步");
                    }
                }, error -> binding.textSyncStatus.setText("同步状态未知")));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }
}

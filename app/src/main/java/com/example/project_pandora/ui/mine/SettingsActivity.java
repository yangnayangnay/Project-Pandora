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
import com.example.project_pandora.core.sync.SyncWorker;
import com.example.project_pandora.core.util.BackToolbarHelper;
import com.example.project_pandora.data.remote.NotificationApi;
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
    private NotificationApi notificationApi;
    private LogDisplayLimitConfig logDisplayLimitConfig;
    private SyncManager syncManager;
    private SyncRepository syncRepository;
    private final CompositeDisposable disposables = new CompositeDisposable();

    /** TASK-R18-018: 5 类通知类型与开关 view id 映射 */
    private static final String[][] NOTIF_SWITCH_MAP = {
            {"NEW_TASK", "switchNotifNewTask"},
            {"STATUS_CHANGE", "switchNotifStatusChange"},
            {"LOG_COMPLETED", "switchNotifLogCompleted"},
            {"TOP10_DISPATCH_CHANGED", "switchNotifTop10Dispatch"},
            {"TOP10_IMPORTANT_CHANGED", "switchNotifTop10Important"}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        userApi = NetworkClient.getInstance(this).createService(UserApi.class);
        notificationApi = NetworkClient.getInstance(this).createService(NotificationApi.class);
        logDisplayLimitConfig = new LogDisplayLimitConfig(this);
        syncManager = SyncManager.getInstance();
        syncRepository = new SyncRepository(this);

        BackToolbarHelper.setupBack(binding.toolbar, this::finish);

        loadSettings();
        loadLocalLogDisplaySettings();
        initSyncCard();
        // TASK-R18-018: 加载通知偏好并初始化开关
        loadNotificationPreferences();

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

        // TASK-R18-018: 5 类通知开关切换监听
        binding.switchNotifNewTask.setOnCheckedChangeListener((b, checked) -> {
            if (!b.isPressed()) return;
            updateNotificationPreference("NEW_TASK", checked, binding.switchNotifNewTask);
        });
        binding.switchNotifStatusChange.setOnCheckedChangeListener((b, checked) -> {
            if (!b.isPressed()) return;
            updateNotificationPreference("STATUS_CHANGE", checked, binding.switchNotifStatusChange);
        });
        binding.switchNotifLogCompleted.setOnCheckedChangeListener((b, checked) -> {
            if (!b.isPressed()) return;
            updateNotificationPreference("LOG_COMPLETED", checked, binding.switchNotifLogCompleted);
        });
        binding.switchNotifTop10Dispatch.setOnCheckedChangeListener((b, checked) -> {
            if (!b.isPressed()) return;
            updateNotificationPreference("TOP10_DISPATCH_CHANGED", checked, binding.switchNotifTop10Dispatch);
        });
        binding.switchNotifTop10Important.setOnCheckedChangeListener((b, checked) -> {
            if (!b.isPressed()) return;
            updateNotificationPreference("TOP10_IMPORTANT_CHANGED", checked, binding.switchNotifTop10Important);
        });
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

    /**
     * TASK-R18-018: 加载通知偏好（GET /notification/preferences）初始化 5 个开关状态
     * 接口失败默认全部开启
     */
    private void loadNotificationPreferences() {
        disposables.add(notificationApi.getPreferences()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        response -> {
                            if (response.isSuccess() && response.getData() != null) {
                                Map<String, Object> data = response.getData();
                                binding.switchNotifNewTask.setChecked(getPrefBool(data, "NEW_TASK", true));
                                binding.switchNotifStatusChange.setChecked(getPrefBool(data, "STATUS_CHANGE", true));
                                binding.switchNotifLogCompleted.setChecked(getPrefBool(data, "LOG_COMPLETED", true));
                                binding.switchNotifTop10Dispatch.setChecked(getPrefBool(data, "TOP10_DISPATCH_CHANGED", true));
                                binding.switchNotifTop10Important.setChecked(getPrefBool(data, "TOP10_IMPORTANT_CHANGED", true));
                            } else {
                                setAllNotifSwitches(true);
                            }
                        },
                        error -> setAllNotifSwitches(true)
                ));
    }

    private boolean getPrefBool(Map<String, Object> data, String key, boolean defaultVal) {
        Object value = data.get(key);
        if (value instanceof Boolean) return (Boolean) value;
        if (value != null) {
            try { return Boolean.parseBoolean(value.toString()); } catch (Exception ignored) {}
        }
        return defaultVal;
    }

    private void setAllNotifSwitches(boolean enabled) {
        binding.switchNotifNewTask.setChecked(enabled);
        binding.switchNotifStatusChange.setChecked(enabled);
        binding.switchNotifLogCompleted.setChecked(enabled);
        binding.switchNotifTop10Dispatch.setChecked(enabled);
        binding.switchNotifTop10Important.setChecked(enabled);
    }

    /**
     * TASK-R18-018: 保存通知偏好（PUT /notification/preferences）并更新本地 SharedPreferences 偏好缓存
     * 保存失败时开关回弹至保存前状态并提示"保存失败，请重试"（复用现有交互模式）
     */
    private void updateNotificationPreference(String type, boolean enabled,
                                              com.google.android.material.materialswitch.MaterialSwitch switchView) {
        Map<String, Object> request = new HashMap<>();
        request.put(type, enabled);

        disposables.add(notificationApi.updatePreferences(request)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        response -> {
                            // 更新本地 SharedPreferences 偏好缓存（轮询 Worker 读取该缓存）
                            android.content.SharedPreferences prefs = getSharedPreferences("notification_prefs", Context.MODE_PRIVATE);
                            prefs.edit().putBoolean("pref_type_" + type, enabled)
                                    .putLong("pref_timestamp", System.currentTimeMillis())
                                    .apply();
                        },
                        error -> {
                            // 失败回弹至保存前状态
                            switchView.setChecked(!enabled);
                            Toast.makeText(this, "\u4FDD\u5B58\u5931\u8D25\uFF0C\u8BF7\u91CD\u8BD5", Toast.LENGTH_SHORT).show();
                        }
                ));
    }
}

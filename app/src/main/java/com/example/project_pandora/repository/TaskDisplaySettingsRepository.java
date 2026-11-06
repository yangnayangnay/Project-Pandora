package com.example.project_pandora.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.project_pandora.core.exception.BusinessException;
import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.core.network.ServerUrlManager;
import com.example.project_pandora.core.network.TokenManager;
import com.example.project_pandora.core.util.NumberCompat;
import com.example.project_pandora.data.remote.UserApi;

import java.util.Collections;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/** Shares the account's server-backed task visibility between settings and company lists. */
public final class TaskDisplaySettingsRepository {
    private final UserApi api;
    private final BooleanSupplier readCache;
    private final Consumer<Boolean> writeCache;

    public TaskDisplaySettingsRepository(Context context) {
        this(NetworkClient.getInstance(context).createService(UserApi.class),
                context.getApplicationContext().getSharedPreferences("task_display_settings", Context.MODE_PRIVATE),
                cacheKey(ServerUrlManager.getInstance(context).getActiveUrl(), TokenManager.getInstance(context).getUserId()));
    }

    private TaskDisplaySettingsRepository(UserApi api, SharedPreferences prefs, String key) {
        this(api, () -> prefs.getBoolean(key, true), value -> prefs.edit().putBoolean(key, value).apply());
    }

    TaskDisplaySettingsRepository(UserApi api, BooleanSupplier readCache, Consumer<Boolean> writeCache) {
        this.api = api; this.readCache = readCache; this.writeCache = writeCache;
    }

    static String cacheKey(String server, Long userId) {
        return server + "|" + userId + "|showCompletedTasks";
    }

    public boolean cachedShowCompleted() { return readCache.getAsBoolean(); }

    public Observable<Boolean> loadShowCompleted() {
        return Observable.defer(api::getUserSettings).subscribeOn(Schedulers.io()).map(response -> {
            if (!response.isSuccess()) throw new BusinessException(response.getCode(), response.getMessage());
            Map<String, Object> data = response.getData();
            if (data == null) throw new IllegalStateException("读取任务显示设置失败");
            Object value = data.get("showCompletedTasks");
            if (value == null) value = data.get("SHOW_COMPLETED_TASKS");
            if (value == null) value = data.get("show_completed_tasks");
            if (value instanceof Boolean) return (Boolean) value;
            if ("true".equalsIgnoreCase(String.valueOf(value))) return true;
            if ("false".equalsIgnoreCase(String.valueOf(value))) return false;
            Integer number = NumberCompat.toInt(value);
            return number == null ? cachedShowCompleted() : number != 0;
        }).doOnNext(writeCache::accept);
    }

    /** Lists remain usable during a settings outage without discarding the last saved choice. */
    public Observable<Boolean> showCompletedForLists() {
        return loadShowCompleted().onErrorReturn(error -> cachedShowCompleted());
    }

    public Observable<Boolean> saveShowCompleted(boolean show) {
        return Observable.defer(() -> api.updateUserSettings(Collections.singletonMap("showCompletedTasks", show)))
                .subscribeOn(Schedulers.io()).map(response -> {
                    if (!response.isSuccess()) throw new BusinessException(response.getCode(), response.getMessage());
                    return show;
                }).doOnNext(writeCache::accept);
    }
}

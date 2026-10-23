package com.example.project_pandora.core.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

/**
 * Round28 REQ-28-01: 客户端 AI 配置管理器（设备级，登出不清除）。
 * EncryptedSharedPreferences 独立存储（ai_config_prefs，与 TokenManager 的 secure_prefs 隔离）；
 * 初始化失败降级普通 SharedPreferences + log.warn 不崩溃（防御式）。
 * 单例内存缓存化，无每请求 IO。
 */
public class AiConfigManager {

    private static final String TAG = "AiConfigManager";
    private static final String PREF_NAME = "ai_config_prefs";
    private static final String KEY_API_URL = "ai_api_url";
    private static final String KEY_API_KEY = "ai_api_key";
    private static final String KEY_MODEL = "ai_model";
    private static final String KEY_ENABLED = "ai_enabled";

    private static AiConfigManager instance;

    private final SharedPreferences prefs;
    private volatile AiClientConfig cached;

    private AiConfigManager(Context context) {
        SharedPreferences sp;
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            sp = EncryptedSharedPreferences.create(
                    context,
                    PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            Log.w(TAG, "EncryptedSharedPreferences 初始化失败，降级普通 SharedPreferences: " + e.getMessage());
            sp = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
        this.prefs = sp;
    }

    public static synchronized AiConfigManager getInstance(Context context) {
        if (instance == null) {
            instance = new AiConfigManager(context.getApplicationContext());
        }
        return instance;
    }

    /** 读取配置快照（内存缓存化） */
    public AiClientConfig getConfig() {
        AiClientConfig snap = cached;
        if (snap != null) {
            return snap;
        }
        synchronized (this) {
            if (cached == null) {
                cached = new AiClientConfig(
                        prefs.getString(KEY_API_URL, ""),
                        prefs.getString(KEY_API_KEY, ""),
                        prefs.getString(KEY_MODEL, ""),
                        prefs.getBoolean(KEY_ENABLED, false));
            }
            return cached;
        }
    }

    /** 原子写四键并刷新内存缓存 */
    public void saveConfig(AiClientConfig config) {
        prefs.edit()
                .putString(KEY_API_URL, config.apiUrl)
                .putString(KEY_API_KEY, config.apiKey)
                .putString(KEY_MODEL, config.model)
                .putBoolean(KEY_ENABLED, config.enabled)
                .apply();
        cached = config;
    }

    /** 透传就绪判定：开关开 + URL 协议合法 + 模型名非空 */
    public boolean isTransmitReady() {
        AiClientConfig c = getConfig();
        return c.enabled
                && (c.apiUrl.startsWith("http://") || c.apiUrl.startsWith("https://"))
                && !c.model.trim().isEmpty();
    }

    /** API Key 脱敏回显：前 3 后 3 掩码；长度 ≤6 全掩码 */
    public static String maskKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return "";
        }
        String k = key.trim();
        if (k.length() <= 6) {
            return "******";
        }
        return k.substring(0, 3) + "****" + k.substring(k.length() - 3);
    }

    /** 清空配置（内存缓存与持久化同步清除） */
    public void clear() {
        prefs.edit().clear().apply();
        cached = new AiClientConfig("", "", "", false);
    }
}
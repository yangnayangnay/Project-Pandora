package com.example.project_pandora.core.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

/**
 * Round29: 服务器地址（BaseUrl）配置管理器（设备级，登出不清除）。
 * 普通 SharedPreferences 独立存储（server_config_prefs）——地址非敏感数据，
 * 与 LogDisplayLimitConfig 等既有普通配置口径一致（区别于 AiConfigManager 的加密存储）。
 * 唯一事实源：BaseUrlInterceptor（出站 URL 重写）、ServerConfigActivity（配置界面）、
 * NetworkClient.getBaseUrl()（委托）三方共同消费。
 * 单例内存缓存化，无每请求 IO。
 */
public class ServerUrlManager {

    private static final String PREF_NAME = "server_config_prefs";
    private static final String KEY_BASE_URL = "server_base_url";

    /** 默认双轨地址（自 NetworkClient 迁入，避免反向循环依赖） */
    static final String EMULATOR_BASE_URL = "http://10.0.2.2:8080/api/";
    /** R33: 真机默认轨硬编码阿里云 ECS 公网地址（用户确认后续服务端常驻此地址） */
    static final String DEVICE_BASE_URL = "http://123.56.153.138:8080/api/";

    private static volatile ServerUrlManager instance;

    private final SharedPreferences prefs;
    private volatile String cachedConfigured;

    private ServerUrlManager(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized ServerUrlManager getInstance(Context context) {
        if (instance == null) {
            instance = new ServerUrlManager(context);
        }
        return instance;
    }

    /** 读取持久化配置串（存储时已归一化；内存缓存化） */
    public String getConfiguredUrl() {
        String snap = cachedConfigured;
        if (snap != null) {
            return snap;
        }
        synchronized (this) {
            if (cachedConfigured == null) {
                cachedConfigured = prefs.getString(KEY_BASE_URL, "");
            }
            return cachedConfigured;
        }
    }

    /** 归一化后写键 + 刷新内存缓存 */
    public void saveUrl(String raw) {
        String normalized = normalizeBaseUrl(raw);
        prefs.edit().putString(KEY_BASE_URL, normalized).apply();
        cachedConfigured = normalized;
    }

    /** 清空配置（内存缓存与持久化同步清除）= 回退默认双轨 */
    public void clear() {
        prefs.edit().remove(KEY_BASE_URL).apply();
        cachedConfigured = "";
    }

    /**
     * 生效地址：已配置（非空且归一化合法）→ 归一化配置地址；
     * 未配置/配置为空/协议非法 → 按 isEmulator() 返回默认双轨地址。
     */
    public String getEffectiveBaseUrl() {
        String configured = normalizeBaseUrl(getConfiguredUrl());
        if (!configured.isEmpty()) {
            return configured;
        }
        return isEmulator() ? EMULATOR_BASE_URL : DEVICE_BASE_URL;
    }

    /**
     * 静态无参便捷入口（与 NetworkClient.getBaseUrl() 保持相同签名，
     * 8 处既有调用点零改动）。单例已初始化时委托 getEffectiveBaseUrl()，
     * 未初始化时防御式回退默认双轨、不抛异常。
     */
    public static String getBaseUrl() {
        ServerUrlManager m = instance;
        if (m != null) {
            return m.getEffectiveBaseUrl();
        }
        return isEmulator() ? EMULATOR_BASE_URL : DEVICE_BASE_URL;
    }

    /**
     * 地址归一化（D-1 方案 A：自动补 /api/）。
     * 去首尾空白 → 校验 http/https 前缀（非法返回空串=视为未配置）→
     * 提取 scheme://authority，路径统一补全为 /api/：
     * 三种输入形态（http://IP:8080 / http://IP:8080/api / http://IP:8080/api/）
     * 归一化结果一致（保证尾斜杠满足 Retrofit baseUrl 合法性）。
     */
    public static String normalizeBaseUrl(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        if (!s.startsWith("http://") && !s.startsWith("https://")) {
            return "";
        }
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        if (s.endsWith("/api")) {
            return s + "/";
        }
        return s + "/api/";
    }

    /** 模拟器判定（自 NetworkClient 迁入） */
    private static boolean isEmulator() {
        return "google_sdk".equals(Build.PRODUCT)
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MODEL.contains("sdk_gphone")
                || Build.PRODUCT.contains("sdk_gphone")
                || Build.FINGERPRINT.contains("sdk_gphone");
    }
}
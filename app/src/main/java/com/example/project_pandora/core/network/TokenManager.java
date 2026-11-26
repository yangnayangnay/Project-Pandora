package com.example.project_pandora.core.network;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

/**
 * Token 管理器——负责存储和管理用户认证 Token、用户信息。
 * 使用 Android 加密存储（EncryptedSharedPreferences）保护敏感数据。
 * 采用单例模式，全局唯一实例。
 */
public class TokenManager {

    private static final String PREF_NAME = "secure_prefs";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_ROLE = "user_role";
    private static final String KEY_LAST_USERNAME = "last_username";

    private static TokenManager instance;
    private final SharedPreferences prefs;

    private TokenManager(Context context) {
        try {
            // 使用 AES256-GCM 加密主密钥
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            // 创建加密 SharedPreferences
            prefs = EncryptedSharedPreferences.create(
                    context,
                    PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to create EncryptedSharedPreferences", e);
        }
    }

    /**
     * 获取 TokenManager 单例实例。
     * 使用 Application Context 防止内存泄漏。
     */
    public static synchronized TokenManager getInstance(Context context) {
        if (instance == null) {
            instance = new TokenManager(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * 保存认证 Token。
     */
    public void saveToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    /**
     * 获取认证 Token。
     */
    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    /**
     * 保存用户信息（ID 和角色）。
     */
    public void saveUserInfo(Long userId, String role) {
        prefs.edit()
             .putLong(KEY_USER_ID, userId)
             .putString(KEY_ROLE, role)
             .apply();
    }

    /**
     * 获取用户ID。
     */
    public Long getUserId() {
        return prefs.getLong(KEY_USER_ID, -1);
    }

    /**
     * 获取用户角色。
     */
    public String getRole() {
        return prefs.getString(KEY_ROLE, null);
    }

    /**
     * 保存上次登录的用户名。
     */
    public void saveLastUsername(String username) {
        prefs.edit().putString(KEY_LAST_USERNAME, username).apply();
    }

    /**
     * 获取上次登录的用户名。
     */
    public String getLastUsername() {
        return prefs.getString(KEY_LAST_USERNAME, null);
    }

    /**
     * 检查用户是否已登录（Token 是否存在）。
     */
    public boolean isLoggedIn() {
        return getToken() != null;
    }

    /**
     * 清除所有认证信息（登出时调用）。
     */
    public void clear() {
        prefs.edit().remove(KEY_TOKEN).remove(KEY_USER_ID).remove(KEY_ROLE).apply();
    }
}
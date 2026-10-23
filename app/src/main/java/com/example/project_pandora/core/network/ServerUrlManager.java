package com.example.project_pandora.core.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * Round29: 服务器地址（BaseUrl）配置管理器（设备级，登出不清除）。
 * R44 REQ-A: 升级为有序服务端列表（≤5 条，顺序即故障转移优先级）+ 活跃地址双键。
 * 键布局：server_list（JSON 数组 [{"u":url,"e":true},...]）+ active_url（当前生效条目）。
 * R29 单地址迁移：首次读 server_list 缺失 → 读旧键 server_base_url 非空 → 迁移为列表[0]
 * （旧键保留只写不读——saveServerList 时镜像写活跃地址进旧键，供降级回滚到旧 APK 版本）。
 * 静态 getBaseUrl()/getEffectiveBaseUrl()/normalizeBaseUrl() 签名冻结（8 处调用点+
 * BaseUrlInterceptor 零改动）；单例缓存化，无每请求 IO。
 */
public class ServerUrlManager {

    private static final String TAG = "ServerUrlMgr";
    private static final String PREF_NAME = "server_config_prefs";
    private static final String KEY_BASE_URL = "server_base_url";
    /** R44: 服务端列表（JSON 数组）与活跃地址 */
    private static final String KEY_SERVER_LIST = "server_list";
    private static final String KEY_ACTIVE_URL = "active_url";
    /** R44 REQ-A: 列表上限 */
    public static final int MAX_SERVERS = 5;

    /** 默认双轨地址（自 NetworkClient 迁入，避免反向循环依赖） */
    static final String EMULATOR_BASE_URL = "http://10.0.2.2:8080/api/";
    /** R33: 真机默认轨硬编码阿里云 ECS 公网地址（用户确认后续服务端常驻此地址） */
    static final String DEVICE_BASE_URL = "http://123.56.153.138:8080/api/";
    /** R44: 云权威地址（中继/回切判定基准）= 真机默认轨 */
    static final String CLOUD_URL = DEVICE_BASE_URL;

    /** 列表操作业务异常（超限/重复，供 UI Toast 提示） */
    public static class ServerListException extends RuntimeException {
        public ServerListException(String message) {
            super(message);
        }
    }

    private static volatile ServerUrlManager instance;

    private final SharedPreferences prefs;
    private volatile String cachedConfigured;
    /** R44: 列表与活跃地址内存缓存（写路径 synchronized 双键一致性） */
    private volatile List<ServerEntry> cachedList;
    private volatile String cachedActive;

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

    // ==================== R44: 服务端列表 API（五件套） ====================

    /**
     * 读取服务端列表（必非空）。迁移链：server_list 键缺失 → 旧单值键迁移 → 双轨预置。
     * JSON 解析异常 → 回退预置（REQ-A 异常场景：不崩溃）。
     */
    public synchronized List<ServerEntry> getServerList() {
        List<ServerEntry> snap = cachedList;
        if (snap != null) {
            return new ArrayList<>(snap);
        }
        String raw = prefs.getString(KEY_SERVER_LIST, null);
        if (raw != null) {
            List<ServerEntry> parsed = parseListJson(raw);
            if (!parsed.isEmpty()) {
                cachedList = parsed;
                return new ArrayList<>(parsed);
            }
        }
        // 迁移：旧单值键（R29）→ 列表[0]
        String legacy = prefs.getString(KEY_BASE_URL, "");
        String normalizedLegacy = normalizeBaseUrl(legacy);
        List<ServerEntry> migrated = new ArrayList<>();
        if (!normalizedLegacy.isEmpty()) {
            migrated.add(new ServerEntry(normalizedLegacy, true));
        } else {
            migrated.add(new ServerEntry(defaultUrl(), true));
        }
        persistList(migrated, migrated.get(0).getUrl());
        cachedList = migrated;
        Log.i(TAG, "server list migrated/init, size=" + migrated.size());
        return new ArrayList<>(migrated);
    }

    /**
     * 整表保存（顺序即优先级）。校验：≤5 上限、归一化去重（违规抛 ServerListException 供 UI 提示）；
     * 活跃地址不在新列表 → 重置为列表头；活跃地址与镜像同步写旧键（降级回滚路径）。
     */
    public synchronized void saveServerList(List<ServerEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            throw new ServerListException("列表不能为空");
        }
        if (entries.size() > MAX_SERVERS) {
            throw new ServerListException("最多 " + MAX_SERVERS + " 台服务器");
        }
        List<ServerEntry> normalized = new ArrayList<>();
        for (ServerEntry e : entries) {
            String u = e.getUrl();
            for (ServerEntry existed : normalized) {
                if (existed.getUrl().equals(u)) {
                    throw new ServerListException("地址重复：" + u);
                }
            }
            normalized.add(e);
        }
        String active = getActiveUrl();
        boolean activeInList = false;
        for (ServerEntry e : normalized) {
            if (e.getUrl().equals(active)) {
                activeInList = true;
                break;
            }
        }
        if (!activeInList) {
            active = normalized.get(0).getUrl();
        }
        persistList(normalized, active);
        cachedList = normalized;
        cachedActive = active;
    }

    /** 活跃地址（未设置 → 列表头） */
    public String getActiveUrl() {
        String snap = cachedActive;
        if (snap != null) {
            return snap;
        }
        synchronized (this) {
            if (cachedActive == null) {
                String stored = prefs.getString(KEY_ACTIVE_URL, "");
                if (stored.isEmpty()) {
                    stored = getServerList().get(0).getUrl();
                }
                cachedActive = stored;
            }
            return cachedActive;
        }
    }

    /** 切换活跃地址（故障转移/回切唯一切换动作；须为列表内启用条目，否则静默忽略+日志） */
    public synchronized void setActiveUrl(String url) {
        String normalized = normalizeBaseUrl(url);
        for (ServerEntry e : getServerList()) {
            if (e.getUrl().equals(normalized)) {
                if (!e.isEnabled()) {
                    Log.w(TAG, "setActiveUrl ignored (disabled): " + normalized);
                    return;
                }
                prefs.edit().putString(KEY_ACTIVE_URL, normalized)
                        .putString(KEY_BASE_URL, normalized) // 镜像旧键（降级回滚）
                        .apply();
                cachedActive = normalized;
                cachedConfigured = normalized;
                Log.i(TAG, "active url switched to " + normalized);
                return;
            }
        }
        Log.w(TAG, "setActiveUrl ignored (not in list): " + url);
    }

    /** 云权威判定（中继/回切判定用） */
    public boolean isCloudActive() {
        return CLOUD_URL.equals(getActiveUrl());
    }

    // ==================== 既有单地址语义（签名冻结） ====================

    /** R44: 兼容保留——单地址保存=写列表唯一条目（供旧路径/降级回滚） */
    public void saveUrl(String raw) {
        String normalized = normalizeBaseUrl(raw);
        if (normalized.isEmpty()) {
            return;
        }
        List<ServerEntry> list = new ArrayList<>();
        list.add(new ServerEntry(normalized, true));
        saveServerList(list);
    }

    /** 兼容保留——清空=回退默认预置 */
    public void clear() {
        List<ServerEntry> list = new ArrayList<>();
        list.add(new ServerEntry(defaultUrl(), true));
        persistList(list, list.get(0).getUrl());
        cachedList = list;
        cachedActive = list.get(0).getUrl();
        cachedConfigured = "";
    }

    /** 读取配置串（R44 语义=活跃地址；空串=未配置走默认轨） */
    public String getConfiguredUrl() {
        String active = getActiveUrl();
        if (!active.equals(defaultUrl())) {
            return active;
        }
        return "";
    }

    /** 生效地址（=活跃地址；未配置 → 默认双轨） */
    public String getEffectiveBaseUrl() {
        String active = getActiveUrl();
        if (!active.isEmpty()) {
            return active;
        }
        return defaultUrl();
    }

    /** 静态无参便捷入口（签名冻结） */
    public static String getBaseUrl() {
        ServerUrlManager m = instance;
        if (m != null) {
            return m.getEffectiveBaseUrl();
        }
        return isEmulator() ? EMULATOR_BASE_URL : DEVICE_BASE_URL;
    }

    /** 地址归一化（D-1 方案 A：自动补 /api/；非法返回空串） */
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

    // ==================== 内部 ====================

    private String defaultUrl() {
        return isEmulator() ? EMULATOR_BASE_URL : DEVICE_BASE_URL;
    }

    private void persistList(List<ServerEntry> list, String active) {
        // 手工构建 JSON（键值均为受控字符——URL 经归一化白名单、布尔字面量，无注入面）
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append("{\"u\":\"").append(escape(list.get(i).getUrl()))
                    .append("\",\"e\":").append(list.get(i).isEnabled()).append('}');
        }
        sb.append(']');
        prefs.edit()
                .putString(KEY_SERVER_LIST, sb.toString())
                .putString(KEY_ACTIVE_URL, active)
                .putString(KEY_BASE_URL, active) // 镜像旧键（降级回滚）
                .apply();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private List<ServerEntry> parseListJson(String raw) {
        List<ServerEntry> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject o = arr.getJSONObject(i);
                String u = normalizeBaseUrl(o.optString("u", ""));
                if (!u.isEmpty()) {
                    out.add(new ServerEntry(u, o.optBoolean("e", true)));
                }
            }
        } catch (JSONException e) {
            Log.w(TAG, "server list json corrupted, fallback default: " + e.getMessage());
        }
        return out;
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
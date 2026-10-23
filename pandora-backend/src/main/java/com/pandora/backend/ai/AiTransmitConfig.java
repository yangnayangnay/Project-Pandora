package com.pandora.backend.ai;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Round28 REQ-28-02: 客户端透传 AI 配置值对象（不可变）。
 * 由 AiTransmitConfig.fromHeaders(request) 从四个透传请求头防御式解析：
 * X-AI-Api-Url / X-AI-Api-Key / X-AI-Model / X-AI-Enabled。
 * 头缺失/值空白/URL 非 http(s) 前缀/model 空 → isUsable()==false 的"未配置"实例，全程不抛异常。
 */
public final class AiTransmitConfig {

    private final String apiUrl;
    private final String apiKey;
    private final String model;
    private final boolean enabled;

    private AiTransmitConfig(String apiUrl, String apiKey, String model, boolean enabled) {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.enabled = enabled;
    }

    public static AiTransmitConfig fromHeaders(HttpServletRequest request) {
        if (request == null) {
            return unusable();
        }
        String apiUrl = trimToNull(request.getHeader("X-AI-Api-Url"));
        String apiKey = trimToNull(request.getHeader("X-AI-Api-Key"));
        String model = trimToNull(request.getHeader("X-AI-Model"));
        String enabledStr = trimToNull(request.getHeader("X-AI-Enabled"));
        boolean enabled = enabledStr == null || "true".equalsIgnoreCase(enabledStr) || "1".equals(enabledStr);

        if (apiUrl == null || model == null || !enabled) {
            return unusable();
        }
        if (!apiUrl.startsWith("http://") && !apiUrl.startsWith("https://")) {
            return unusable();
        }
        return new AiTransmitConfig(apiUrl, apiKey == null ? "" : apiKey, model, true);
    }

    private static AiTransmitConfig unusable() {
        return new AiTransmitConfig(null, null, null, false);
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** 配置是否可用于外部调用（enabled 且 URL 协议合法且 model 非空，解析时已保证） */
    public boolean isUsable() {
        return enabled && apiUrl != null && model != null;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getModel() {
        return model;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
package com.example.project_pandora.core.network;

/**
 * Round28 REQ-28-01: 客户端 AI 配置不可变快照（四字段）。
 * 由 AiConfigManager 持久化与读取；透传拦截器据此附加请求头。
 */
public final class AiClientConfig {

    public final String apiUrl;
    public final String apiKey;
    public final String model;
    public final boolean enabled;

    public AiClientConfig(String apiUrl, String apiKey, String model, boolean enabled) {
        this.apiUrl = apiUrl == null ? "" : apiUrl;
        this.apiKey = apiKey == null ? "" : apiKey;
        this.model = model == null ? "" : model;
        this.enabled = enabled;
    }
}
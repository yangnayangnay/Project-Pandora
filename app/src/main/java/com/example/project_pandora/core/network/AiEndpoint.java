package com.example.project_pandora.core.network;

import java.net.URI;
import java.util.Locale;

/** Accepts service origins, versioned base URLs and explicit compatible endpoints. */
public final class AiEndpoint {
    private AiEndpoint() {}
    public static String chat(String input) {
        String value = input == null ? "" : input.trim();
        URI uri;
        try { uri = URI.create(value); } catch (RuntimeException error) { throw new IllegalArgumentException("接口地址格式不正确"); }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("https") || scheme.equalsIgnoreCase("http"))
                || uri.getHost() == null || uri.getRawUserInfo() != null || uri.getRawFragment() != null) {
            throw new IllegalArgumentException("请填写有效的 http:// 或 https:// 接口地址");
        }
        String path = uri.getRawPath() == null ? "" : uri.getRawPath().replaceAll("/+$", "");
        String lower = path.toLowerCase(Locale.ROOT);
        if (path.isEmpty()) path = "api.openai.com".equalsIgnoreCase(uri.getHost()) ? "/v1/chat/completions" : "/chat/completions";
        else if (lower.matches(".*/v[0-9]+") || lower.equals("/api")) path += "/chat/completions";
        // Preserve explicit custom paths, including provider gateways and query parameters.
        return uri.getScheme() + "://" + uri.getRawAuthority() + path
                + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
    }
    public static String models(String input) {
        String chat = chat(input);
        URI uri = URI.create(chat);
        String path = uri.getRawPath();
        if (!path.endsWith("/chat/completions")) throw new IllegalArgumentException("自定义端点请按服务商说明填写模型名称");
        return uri.getScheme() + "://" + uri.getRawAuthority()
                + path.substring(0, path.length() - "/chat/completions".length()) + "/models"
                + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
    }
    public static String error(int status) {
        switch (status) {
            case 400: return "请求或模型参数不受支持，请检查模型名称";
            case 401: return "API Key 无效、已过期或未填写";
            case 402: return "模型服务余额不足";
            case 403: return "该密钥没有访问权限";
            case 404: return "接口路径或模型不存在，请核对端点并获取可用模型";
            case 429: return "请求过于频繁或额度不足，请稍后重试";
            default: return status >= 500 ? "模型服务暂时不可用，请稍后重试" : "模型服务拒绝了请求，请检查配置";
        }
    }
}

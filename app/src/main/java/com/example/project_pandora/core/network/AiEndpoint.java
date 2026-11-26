package com.example.project_pandora.core.network;

import java.net.URI;
import java.util.Locale;

/**
 * AI 端点工具类——处理 AI 接口地址的构建和错误码映射。
 * 支持 OpenAI 兼容格式的 API 端点。
 * 被 AI 配置和调用模块使用。
 */
public final class AiEndpoint {
    private AiEndpoint() {}

    /**
     * 构建聊天接口地址。
     * 规则：
     * - 必须为有效的 http:// 或 https:// URL
     * - 自动补齐路径：OpenAI 默认 /v1/chat/completions，其他默认 /chat/completions
     * - 保留自定义路径和查询参数
     *
     * @param input 用户输入的接口地址
     * @return 完整的聊天接口地址
     * @throws IllegalArgumentException 地址格式无效时抛出
     */
    public static String chat(String input) {
        String value = input == null ? "" : input.trim();
        URI uri;
        try { uri = URI.create(value); } catch (RuntimeException error) { throw new IllegalArgumentException("接口地址格式不正确"); }
        String scheme = uri.getScheme();
        // 校验协议、主机、不允许用户信息或片段
        if (scheme == null || !(scheme.equalsIgnoreCase("https") || scheme.equalsIgnoreCase("http"))
                || uri.getHost() == null || uri.getRawUserInfo() != null || uri.getRawFragment() != null) {
            throw new IllegalArgumentException("请填写有效的 http:// 或 https:// 接口地址");
        }
        String path = uri.getRawPath() == null ? "" : uri.getRawPath().replaceAll("/+$", "");
        String lower = path.toLowerCase(Locale.ROOT);
        // 自动补齐路径
        if (path.isEmpty()) path = "api.openai.com".equalsIgnoreCase(uri.getHost()) ? "/v1/chat/completions" : "/chat/completions";
        else if (lower.matches(".*/v[0-9]+") || lower.equals("/api")) path += "/chat/completions";
        // 保留自定义路径和查询参数
        return uri.getScheme() + "://" + uri.getRawAuthority() + path
                + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
    }

    /**
     * 构建模型列表接口地址。
     * 从聊天地址推导模型列表地址。
     *
     * @param input 聊天接口地址
     * @return 模型列表接口地址
     */
    public static String models(String input) {
        String chat = chat(input);
        URI uri = URI.create(chat);
        String path = uri.getRawPath();
        if (!path.endsWith("/chat/completions")) throw new IllegalArgumentException("自定义端点请按服务商说明填写模型名称");
        return uri.getScheme() + "://" + uri.getRawAuthority()
                + path.substring(0, path.length() - "/chat/completions".length()) + "/models"
                + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
    }

    /**
     * HTTP 状态码转错误消息。
     * 用于向用户展示友好的错误提示。
     *
     * @param status HTTP 状态码
     * @return 错误消息
     */
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
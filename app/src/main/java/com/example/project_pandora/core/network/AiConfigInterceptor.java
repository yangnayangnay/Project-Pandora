package com.example.project_pandora.core.network;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Round28 REQ-28-02: AI 配置透传拦截器。
 * 请求 URL path 命中白名单（/ai/、/view/mindmap、/log——覆盖 7 处 AI 触发链）且透传就绪时，
 * 附加 X-AI-Api-Url / X-AI-Api-Key / X-AI-Model / X-AI-Enabled 四头；
 * 不满足条件不附加任何头（等同未配置）；内部 try-catch 全吞降级为不附加（网络主链路优先）。
 */
public class AiConfigInterceptor implements Interceptor {

    private final Context appContext;

    public AiConfigInterceptor(Context context) {
        this.appContext = context.getApplicationContext();
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        try {
            AiConfigManager manager = AiConfigManager.getInstance(appContext);
            if (manager.isTransmitReady() && isAiPath(request)) {
                AiClientConfig config = manager.getConfig();
                Request.Builder builder = request.newBuilder()
                        .header("X-AI-Api-Url", config.apiUrl)
                        .header("X-AI-Api-Key", config.apiKey)
                        .header("X-AI-Model", config.model.trim())
                        .header("X-AI-Enabled", "true");
                request = builder.build();
            }
        } catch (Exception ignored) {
            // 配置读取异常时降级为不附加透传头，网络主链路优先
        }
        return chain.proceed(request);
    }

    /** 白名单判定：AI 端点 / 导图（个人十大 AI 点评）/ 日志（创建触发自动分析） */
    private static boolean isAiPath(Request request) {
        String path = request.url().encodedPath();
        return path.contains("/ai/")
                || path.contains("/view/mindmap")
                || path.endsWith("/log")
                || path.contains("/log/");
    }
}
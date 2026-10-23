package com.example.project_pandora.core.network;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Round29 生效机制（组 2.1 定案方案 B）：动态 baseUrl 应用拦截器。
 * 全部 Retrofit service 代理共享 NetworkClient 唯一 OkHttpClient，在拦截器层按
 * ServerUrlManager 当前生效地址统一重写出站 URL——保存后对在用页面、后台轮询
 * （NotificationPollWorker）、离线同步（SyncService）一次性全局即时生效，零持有方改造。
 * 挂载顺序：首位应用拦截器（TokenInterceptor 之前——URL 重写先行，鉴权头/透传头/日志
 * 均基于重写后 URL 工作）。
 * 生效地址与占位地址同目标（未配置态）时零开销直通；内部 try-catch 全吞降级为原请求
 * （网络主链路优先，沿 AiConfigInterceptor 防御模式）。
 */
public class BaseUrlInterceptor implements Interceptor {

    /** Retrofit 构建时的占位 baseUrl（App 生命周期内固定），用于剥离业务相对路径 */
    private final HttpUrl placeholder;

    public BaseUrlInterceptor(String placeholderBaseUrl) {
        this.placeholder = HttpUrl.parse(placeholderBaseUrl);
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        try {
            HttpUrl effective = HttpUrl.parse(ServerUrlManager.getBaseUrl());
            if (effective == null || placeholder == null) {
                return chain.proceed(request);
            }
            HttpUrl reqUrl = request.url();
            if (sameTarget(reqUrl, effective)) {
                return chain.proceed(request);
            }
            HttpUrl newUrl = rewrite(reqUrl, effective);
            if (newUrl == null) {
                return chain.proceed(request);
            }
            request = request.newBuilder().url(newUrl).build();
        } catch (Exception ignored) {
            // 地址读取/重写异常时降级为原请求直通，网络主链路优先
        }
        return chain.proceed(request);
    }

    /** 同目标判定：scheme/host/port 一致且请求路径以生效 base 路径为前缀（未配置态直通） */
    private static boolean sameTarget(HttpUrl reqUrl, HttpUrl effective) {
        if (!reqUrl.scheme().equals(effective.scheme())
                || !reqUrl.host().equals(effective.host())
                || reqUrl.port() != effective.port()) {
            return false;
        }
        String effPath = effective.encodedPath();
        String prefix = effPath.endsWith("/") ? effPath : effPath + "/";
        return reqUrl.encodedPath().startsWith(prefix);
    }

    /**
     * URL 重写：以生效地址 HttpUrl 为基准（scheme/host/port + 路径前缀），
     * 保留原请求去掉占位前导路径后的业务相对路径与全部 query 参数。
     */
    private HttpUrl rewrite(HttpUrl reqUrl, HttpUrl effective) {
        String phPath = placeholder.encodedPath();
        String reqPath = reqUrl.encodedPath();
        String bizPath;
        if (phPath.endsWith("/") && reqPath.startsWith(phPath)) {
            bizPath = reqPath.substring(phPath.length() - 1);
        } else {
            bizPath = reqPath;
        }
        String effPath = effective.encodedPath();
        String newFullPath = (effPath.endsWith("/") ? effPath.substring(0, effPath.length() - 1) : effPath) + bizPath;
        return reqUrl.newBuilder()
                .scheme(effective.scheme())
                .host(effective.host())
                .port(effective.port())
                .encodedPath(newFullPath)
                .build();
    }
}
package com.example.project_pandora.core.network;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class TokenInterceptor implements Interceptor {

    private final TokenManager tokenManager;
    private final Context appContext;

    public TokenInterceptor(Context context) {
        this.tokenManager = TokenManager.getInstance(context);
        this.appContext = context.getApplicationContext();
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request original = chain.request();
        String token = tokenManager.getToken();

        Request request = original;
        if (token != null) {
            request = original.newBuilder()
                    .header("Authorization", "Bearer " + token)
                    .build();
        }

        Response response = chain.proceed(request);
        String liveRole = response.header("X-User-Role");
        if (response.isSuccessful() && liveRole != null && tokenManager.getUserId() != null && tokenManager.getUserId() > 0) {
            tokenManager.saveUserInfo(tokenManager.getUserId(), liveRole);
        }

        // R40 REQ-40-A: 401 会话过期治理——清 token + 弹"会话已过期"通知（一次性，id=900001 系统去重）。
        // 根因（R39 部署实证）：后端重启清 SessionStore → 旧 token 全部 401 → 轮询静默失败，
        // 用户表现为"没收到通知/任务"却无任何提示。token 清除后 MainActivity 既有
        // isLoggedIn 检查自动跳登录页，重新登录即恢复——根治静默期。
        // 三重限定防误弹：①仅带 token 的请求（登录/注册等匿名请求 401 不触发）
        // ②仅 API 请求（URL 含 /api/ 前缀，防其他 host 干扰）③登录端点自身除外
        if (response.code() == 401
                && token != null
                && request.url().encodedPath().contains("/api/")
                && !request.url().encodedPath().contains("/auth/login")) {
            tokenManager.clear();
            android.util.Log.w("TokenInterceptor", "401 received, session expired, token cleared: "
                    + request.url().encodedPath());
            try {
                com.example.project_pandora.notification.TaskNotificationService.showNotification(
                        appContext, 900001L, "SESSION_EXPIRED",
                        appContext.getString(com.example.project_pandora.R.string.session_expired_title),
                        appContext.getString(com.example.project_pandora.R.string.session_expired_content),
                        null);
            } catch (Exception e) {
                android.util.Log.w("TokenInterceptor", "session expired notify failed: " + e.getMessage());
            }
        }

        return response;
    }
}

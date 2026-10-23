package com.example.project_pandora.core.network;

import com.example.project_pandora.core.exception.BusinessException;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.HttpException;

/**
 * DEF-31-01: Retrofit HttpException 统一解析器。
 * 读取 errorBody 的 ApiResponse JSON（{code, message}）构造 BusinessException，
 * 使 UI 层拿到后端原始语义（如 4030 当日已存在日志）而非 "HTTP 400 Bad Request" 笼统文案。
 * 解析失败（非 JSON/空体）时回退构造带 HTTP 状态码的 BusinessException，保留可定位信息。
 */
public final class HttpErrorParser {

    private HttpErrorParser() {}

    /**
     * HttpException → BusinessException。
     * 非 HttpException 原样透传。
     */
    public static Throwable parse(Throwable error) {
        if (!(error instanceof HttpException)) {
            return error;
        }
        HttpException httpEx = (HttpException) error;
        ResponseBody errorBody = httpEx.response() != null ? httpEx.response().errorBody() : null;
        if (errorBody != null) {
            try {
                String body = errorBody.string();
                if (body != null && !body.trim().isEmpty()) {
                    JSONObject json = new JSONObject(body);
                    int code = json.optInt("code", httpEx.code());
                    String message = json.optString("message", "");
                    if (!message.isEmpty()) {
                        return new BusinessException(code, message);
                    }
                    return new BusinessException(code, "HTTP " + httpEx.code() + " " + httpEx.message());
                }
            } catch (IOException | org.json.JSONException ignored) {
                // errorBody 已读或非 JSON —— 走下方回退
            }
        }
        return new BusinessException(httpEx.code(), "HTTP " + httpEx.code() + " " + httpEx.message());
    }
}
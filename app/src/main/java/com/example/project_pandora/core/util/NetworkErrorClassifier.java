package com.example.project_pandora.core.util;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * 网络异常分类器。
 * 根据异常类型分类返回精确的网络错误提示文案。
 */
public final class NetworkErrorClassifier {

    private NetworkErrorClassifier() {}

    /**
     * 根据异常类型分类返回精确提示文案。
     *
     * @param error 捕获的异常
     * @return 分类后的用户友好提示文案
     */
    public static String classify(Throwable error) {
        if (error instanceof SocketTimeoutException) {
            return "请求超时，请稍后重试";
        }
        if (error instanceof ConnectException) {
            return "无法连接服务器，请检查网络连接或确认服务是否正常运行";
        }
        if (error instanceof UnknownHostException) {
            return "网络不可达，请检查网络设置";
        }
        if (error instanceof IOException) {
            return "网络异常，请稍后重试";
        }
        return "操作失败，请稍后重试";
    }
}
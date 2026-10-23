package com.pandora.backend.common;

import lombok.Data;

@Data
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;
    private String traceId;
    /**
     * D-33-02 修复：通知轮询专用可选字段（全局通知表 MAX(id) 基线）。
     * 仅 /notification/poll 填充；其他端点保持 null（Jackson 默认不序列化 null，对存量客户端零影响）。
     * App 端据此比对本地游标，游标超前（迁移基线差异）时自适应重置，避免永远拉空。
     */
    private Long maxNotificationId;

    public ApiResponse() {}

    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data);
    }

    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(200, "success", null);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), null);
    }
}
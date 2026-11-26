package com.pandora.backend.common;

import lombok.Data;

/**
 * 统一 API 响应包装类。
 * 所有 Controller 接口的返回值均应使用此类包装，确保前后端交互格式一致。
 * 包含状态码、消息、数据体和可选的追踪ID、通知轮询基线等扩展字段。
 *
 * @param <T> 响应数据的类型
 */
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
    private Long unreadCount;

    public ApiResponse() {}

    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 创建成功响应（带数据）。
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 状态码 200、消息 "success" 的响应对象
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data);
    }

    /**
     * 创建成功响应（无数据）。
     *
     * @param <T> 数据类型
     * @return 状态码 200、消息 "success"、data 为 null 的响应对象
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(200, "success", null);
    }

    /**
     * 创建错误响应（自定义状态码和消息）。
     *
     * @param code    错误码
     * @param message 错误信息
     * @param <T>     数据类型
     * @return 错误响应对象
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    /**
     * 创建错误响应（使用预定义错误码枚举）。
     *
     * @param errorCode 错误码枚举
     * @param <T>       数据类型
     * @return 错误响应对象
     */
    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), null);
    }
}

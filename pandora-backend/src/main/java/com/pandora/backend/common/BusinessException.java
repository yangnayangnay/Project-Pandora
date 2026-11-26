package com.pandora.backend.common;

import lombok.Getter;

/**
 * 业务异常类——用于封装可预期的业务错误。
 * 区别于系统异常（如数据库连接失败），业务异常表示正常业务流程中的错误情况（如用户名已存在）。
 * 被 GlobalExceptionHandler 捕获后转换为统一错误响应。
 */
@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    /**
     * 构造业务异常，使用预定义错误码。
     *
     * @param errorCode 错误码枚举
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 构造业务异常，使用预定义错误码并自定义错误消息。
     * 用于需要动态消息的场景（如"工号与密码错误"）。
     *
     * @param errorCode 错误码枚举
     * @param message 自定义错误消息
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
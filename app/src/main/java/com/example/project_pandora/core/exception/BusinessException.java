package com.example.project_pandora.core.exception;

/**
 * 客户端业务异常类，携带后端错误码。
 * 用于 Repository 层向后端发起请求后，后端返回业务错误时抛出，
 * UI 层捕获后可通过 ErrorMessageMapper 映射精确提示文案。
 */
public class BusinessException extends RuntimeException {
    private final int code;
    private final String message;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
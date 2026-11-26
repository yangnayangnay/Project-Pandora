package com.pandora.backend.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器——统一捕获并处理所有 Controller 抛出的异常。
 * 将不同类型的异常转换为标准的 ApiResponse 响应格式，确保前后端交互一致性。
 * 被 Spring 容器自动注册，无需手动调用。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理权限不足异常（@PreAuthorize 拒绝访问）。
     * 返回 HTTP 403 Forbidden。
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(org.springframework.security.access.AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(ErrorCode.FORBIDDEN));
    }

    /**
     * 处理业务异常。
     * 根据错误码映射 HTTP 状态码，支持自定义错误消息。
     * 若 BusinessException 携带了自定义消息（与枚举默认消息不同），则使用自定义消息。
     *
     * @param e 业务异常
     * @return 标准错误响应
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("Business exception: code={}, message={}", e.getErrorCode().getCode(), e.getMessage());
        HttpStatus status = mapErrorCodeToHttpStatus(e.getErrorCode());
        // 如果 BusinessException 携带了自定义 message（与枚举默认 message 不同），使用自定义 message
        String message = e.getMessage();
        if (message == null || message.equals(e.getErrorCode().getMessage())) {
            return ResponseEntity.status(status).body(ApiResponse.error(e.getErrorCode()));
        } else {
            return ResponseEntity.status(status).body(ApiResponse.error(e.getErrorCode().getCode(), message));
        }
    }

    /**
     * 处理参数校验异常（@Valid 注解触发）。
     * 返回 HTTP 400 Bad Request。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("Validation failed: {}", e.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.error(ErrorCode.MISSING_REQUIRED_FIELD));
    }

    /**
     * 处理数据绑定异常（表单参数绑定失败）。
     * 返回 HTTP 400 Bad Request。
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException e) {
        log.warn("Bind failed: {}", e.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.error(ErrorCode.MISSING_REQUIRED_FIELD));
    }

    /**
     * 处理接口不存在异常。
     * 返回 HTTP 404 Not Found，提示客户端检查版本。
     */
    @ExceptionHandler({org.springframework.web.servlet.resource.NoResourceFoundException.class,
            org.springframework.web.servlet.NoHandlerFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleMissingEndpoint(Exception e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ErrorCode.NOT_FOUND.getCode(), "接口或资源不存在，请确认服务版本与客户端一致"));
    }

    /**
     * 处理所有未捕获的异常。
     * 返回 HTTP 500 Internal Server Error，记录错误日志。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.internalServerError().body(ApiResponse.error(ErrorCode.INTERNAL_ERROR));
    }

    /**
     * 将业务错误码映射为 HTTP 状态码。
     * 用于决定响应头中的状态码。
     *
     * @param errorCode 业务错误码
     * @return HTTP 状态码
     */
    private HttpStatus mapErrorCodeToHttpStatus(ErrorCode errorCode) {
        return switch (errorCode) {
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN, FORBIDDEN_ACTION -> HttpStatus.FORBIDDEN;
            case NOT_FOUND, USER_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT, TASK_CONFLICT, RULE_CONFLICT,
                 TOP10_TASK_ALREADY_MARKED, TOP10_CONTENT_DUPLICATE,
                 HIERARCHY_CYCLE_DETECTED -> HttpStatus.CONFLICT;
            case AI_TIMEOUT -> HttpStatus.GATEWAY_TIMEOUT;
            case AI_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
package com.pandora.backend.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("Business exception: code={}, message={}", e.getErrorCode().getCode(), e.getMessage());
        HttpStatus status = mapErrorCodeToHttpStatus(e.getErrorCode());
        return ResponseEntity.status(status).body(ApiResponse.error(e.getErrorCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("Validation failed: {}", e.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.error(ErrorCode.MISSING_REQUIRED_FIELD));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException e) {
        log.warn("Bind failed: {}", e.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.error(ErrorCode.MISSING_REQUIRED_FIELD));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.internalServerError().body(ApiResponse.error(ErrorCode.INTERNAL_ERROR));
    }

    private HttpStatus mapErrorCodeToHttpStatus(ErrorCode errorCode) {
        return switch (errorCode) {
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN, FORBIDDEN_ACTION -> HttpStatus.FORBIDDEN;
            case NOT_FOUND, USER_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT, TASK_CONFLICT, RULE_CONFLICT -> HttpStatus.CONFLICT;
            case AI_TIMEOUT -> HttpStatus.GATEWAY_TIMEOUT;
            case AI_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
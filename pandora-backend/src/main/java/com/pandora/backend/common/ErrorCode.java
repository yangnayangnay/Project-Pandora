package com.pandora.backend.common;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS(200, "success"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "冲突"),

    INVALID_CREDENTIALS(4001, "用户名或密码错误"),
    ACCOUNT_DISABLED(4002, "账号已禁用"),
    MISSING_REQUIRED_FIELD(4003, "缺少必填字段"),
    INVALID_TIME_RANGE(4004, "时间范围无效"),
    USER_NOT_FOUND(4005, "用户不存在"),
    FORBIDDEN_ACTION(4006, "无操作权限"),
    INVALID_TRANSITION(4007, "非法状态跳转"),
    TASK_CONFLICT(4008, "任务并发冲突"),
    RULE_CONFLICT(4009, "审核规则冲突"),
    USERNAME_ALREADY_EXISTS(4010, "用户名已存在"),
    PASSWORD_TOO_SHORT(4011, "密码长度不能少于6位"),
    INVALID_ROLE(4012, "无效的角色"),

    AI_TIMEOUT(5001, "AI服务超时"),
    AI_UNAVAILABLE(5002, "AI服务暂不可用"),

    INTERNAL_ERROR(500, "服务器内部错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
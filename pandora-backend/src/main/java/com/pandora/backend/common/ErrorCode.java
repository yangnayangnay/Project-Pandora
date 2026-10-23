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
    ACCOUNT_PENDING_APPROVAL(4013, "账号待审批，请等待管理员审核"),
    ACCOUNT_REJECTED(4014, "账号已被拒绝，请联系管理员"),
    EMAIL_ALREADY_EXISTS(4015, "邮箱已被注册"),
    INVALID_EMAIL_FORMAT(4016, "邮箱格式无效"),
    INVALID_ID_CARD(4017, "身份证号无效"),
    ID_CARD_ALREADY_EXISTS(4018, "身份证号已被使用"),
    REAL_NAME_MISMATCH(4019, "实名信息与预登记不匹配"),
    EMPLOYEE_RECORD_NOT_FOUND(4020, "预登记员工记录不存在"),
    EMPLOYEE_RECORD_ALREADY_REGISTERED(4021, "该员工已注册"),
    APPROVAL_NOT_FOUND(4022, "审批记录不存在"),

    RESET_REQUEST_NOT_FOUND(4023, "重置申请不存在"),
    RESET_REQUEST_ALREADY_PROCESSED(4024, "该重置申请已处理"),
    RESET_REQUEST_DUPLICATE(4025, "您已有待审批的重置申请，请等待管理员处理"),

    INVALID_PHONE_FORMAT(4026, "手机号格式不正确"),
    FILE_TOO_LARGE(4027, "图片过大，请选择小于2MB的图片"),
    UNSUPPORTED_IMAGE_FORMAT(4028, "仅支持 JPG/PNG/WEBP 格式"),

    AI_TIMEOUT(5001, "AI服务超时"),
    AI_UNAVAILABLE(5002, "AI服务暂不可用"),

    LOG_DATE_CONFLICT(4030, "当日已存在日志，请编辑已有日志"),

    TOP10_LIMIT_EXCEEDED(4040, "十大事项数量已满"),
    TOP10_CONTENT_EMPTY(4041, "内容不能为空"),
    TOP10_TASK_NOT_FOUND(4042, "任务不存在"),
    FUNCTION_CODE_NOT_FOUND(4043, "功能客体不存在"),
    PERMISSION_SUBJECT_NOT_FOUND(4044, "权限主体不存在"),
    LAST_ADMIN_PROTECTION(4045, "至少保留一位管理员权限"),
    PERSONAL_TOP10_INJECT_FORBIDDEN(4046, "禁止篡改他人信息"),

    TOP10_TASK_ALREADY_MARKED(4047, "该任务已在十大派发中，不可重复选择"),
    TOP10_CONTENT_DUPLICATE(4048, "该事项内容已存在，请勿重复添加"),
    TOP10_RANK_OUT_OF_RANGE(4049, "排名序号必须在1-10之间"),
    TOP10_SAVE_VERIFY_FAILED(4050, "保存失败，请重试。若多次失败请联系管理员"),
    INVALID_PARAM_LENGTH(4051, "参数长度超限"),
    HIERARCHY_CYCLE_DETECTED(4060, "检测到循环引用，已截断"),
    HIERARCHY_ORPHAN_NODE(4061, "存在孤儿节点（上级已删除），已单独展示"),

    AI_CONFIG_INVALID(5003, "AI配置参数无效"),
    AI_QUOTA_LOW(5004, "AI额度不足"),
    AI_FALLBACK_UNAVAILABLE(5005, "兜底AI不可用"),
    EXTERNAL_AI_FORMAT_ERROR(5006, "外部AI响应格式异常"),

    INTERNAL_ERROR(500, "服务器内部错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
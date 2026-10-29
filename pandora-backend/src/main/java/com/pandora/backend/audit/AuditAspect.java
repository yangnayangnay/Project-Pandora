package com.pandora.backend.audit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;

@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    @Autowired
    private AuditService auditService;

    @Around("@within(org.springframework.web.bind.annotation.RestController) && " +
            "(@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PatchMapping))")
    public Object auditWriteOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();

        try {
            recordAuditLog(joinPoint);
        } catch (Exception e) {
            log.error("审计日志写入失败: {}", e.getMessage());
        }

        return result;
    }

    private void recordAuditLog(ProceedingJoinPoint joinPoint) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long)) return;

        Long operatorId = (Long) auth.getPrincipal();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getName();

        Auditable auditable = method.getAnnotation(Auditable.class);
        String operationType = null;
        String targetType = "UNKNOWN";

        if (auditable != null) {
            operationType = auditable.operationType();
            targetType = auditable.targetType();
        }

        Long targetId = extractTargetId(method, joinPoint.getArgs());
        boolean sensitive = methodName.toLowerCase(java.util.Locale.ROOT).contains("password")
                || methodName.equals("login") || methodName.equals("register") || methodName.equals("createUser");
        String operationContent = sensitive ? "账户安全操作（不记录密码）" : buildOperationContent(joinPoint.getArgs());
        String detail = sensitive ? "[REDACTED]" : Arrays.toString(joinPoint.getArgs());
        if (detail.length() > 500) detail = detail.substring(0, 500) + "...";

        auditService.record(operatorId, methodName, targetType, targetId, detail, operationType, operationContent);
    }

    private Long extractTargetId(Method method, Object[] args) {
        Parameter[] params = method.getParameters();
        for (int i = 0; i < params.length && i < args.length; i++) {
            Object arg = args[i];
            if (arg == null) continue;
            if (arg instanceof Long) return (Long) arg;
            if (arg instanceof Number) return ((Number) arg).longValue();
        }
        return null;
    }

    private String buildOperationContent(Object[] args) {
        if (args == null || args.length == 0) return null;
        StringBuilder sb = new StringBuilder();
        for (Object arg : args) {
            if (arg == null) continue;
            if (sb.length() > 0) sb.append(", ");
            String str = arg.toString();
            if (str.length() > 200) str = str.substring(0, 200) + "...";
            sb.append(str);
        }
        return sb.length() == 0 ? null : sb.toString();
    }
}

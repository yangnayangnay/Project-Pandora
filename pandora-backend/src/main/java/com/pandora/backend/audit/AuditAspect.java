package com.pandora.backend.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    @Autowired
    private AuditService auditService;

    @Autowired
    private ObjectMapper objectMapper;

    @Around("@within(org.springframework.web.bind.annotation.RestController) && " +
            "(@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PatchMapping))")
    public Object auditWriteOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof Long) {
                Long operatorId = (Long) auth.getPrincipal();
                String methodName = joinPoint.getSignature().getName();
                String args = Arrays.toString(joinPoint.getArgs());
                if (args.length() > 500) {
                    args = args.substring(0, 500) + "...";
                }
                auditService.record(operatorId, methodName, "UNKNOWN", null, args);
            }
        } catch (Exception e) {
            log.warn("Failed to record audit log: {}", e.getMessage());
        }

        return result;
    }
}
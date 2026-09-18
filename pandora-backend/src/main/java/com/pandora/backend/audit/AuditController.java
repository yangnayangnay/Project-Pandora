package com.pandora.backend.audit;

import com.pandora.backend.common.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/audit")
public class AuditController {

    @Autowired
    private AuditService auditService;

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listAuditLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long operatorId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String role = auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(403).body(ApiResponse.error(403, "仅管理员可查询审计日志"));
        }
        return ResponseEntity.ok(ApiResponse.success(auditService.queryAuditLogs(page, size, operatorId)));
    }
}
package com.pandora.backend.audit;

import com.pandora.backend.common.ApiResponse;
import com.pandora.backend.common.RoleChecker;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/audit")
public class AuditController {

    @Autowired
    private AuditService auditService;

    @GetMapping("/my-today")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyTodayAuditLogs() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(auditService.queryMyTodayAuditLogs(userId)));
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listAuditLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String keyword) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String role = auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        // Round19 REQ-19-02: 系统日志查看权限收紧为仅 ADMIN
        if (!RoleChecker.isAdminOnly(role)) {
            return ResponseEntity.status(403).body(ApiResponse.error(403, "仅管理员可查询审计日志"));
        }
        if (startTime != null && endTime != null && !startTime.isEmpty() && !endTime.isEmpty()
                && startTime.compareTo(endTime) > 0) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "起始时间不能晚于结束时间"));
        }
        return ResponseEntity.ok(ApiResponse.success(auditService.queryAuditLogs(
                page, size, operatorId, startTime, endTime, operationType, keyword)));
    }

    @GetMapping("/export")
    public void exportAuditLogs(
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String keyword,
            HttpServletResponse response) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String role = auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        // Round19 REQ-19-02: 系统日志导出权限收紧为仅 ADMIN
        if (!RoleChecker.isAdminOnly(role)) {
            response.setStatus(403);
            return;
        }

        List<Map<String, Object>> logs = auditService.exportAuditLogs(operatorId, startTime, endTime, operationType, keyword);

        String filename = "audit_logs_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".csv";
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);

        Writer writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8);
        writer.write('\uFEFF');
        writer.write("操作人,操作类型,目标类型,目标ID,操作内容,详情,操作时间\n");
        for (Map<String, Object> log : logs) {
            String operatorName = str(log.get("operator_real_name"));
            if (operatorName.isEmpty()) operatorName = str(log.get("operator_name"));
            if (operatorName.isEmpty()) operatorName = str(log.get("operator_id"));
            writer.write(csvEscape(operatorName));
            writer.write(",");
            writer.write(csvEscape(AuditLabelMapper.toChineseOperationType(str(log.get("operation_type")))));
            writer.write(",");
            writer.write(csvEscape(AuditLabelMapper.toChineseTargetType(str(log.get("target_type")))));
            writer.write(",");
            writer.write(csvEscape(str(log.get("target_id"))));
            writer.write(",");
            writer.write(csvEscape(str(log.get("operation_content"))));
            writer.write(",");
            writer.write(csvEscape(str(log.get("detail"))));
            writer.write(",");
            writer.write(csvEscape(str(log.get("operated_at"))));
            writer.write("\n");
        }
        writer.flush();
    }

    private static String str(Object obj) {
        return obj != null ? obj.toString() : "";
    }

    private static String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
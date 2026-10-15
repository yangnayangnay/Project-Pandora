package com.pandora.backend.permission;

import com.pandora.backend.audit.Auditable;
import com.pandora.backend.common.ApiResponse;
import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/permission/function")
public class PermissionController {

    @Autowired
    private FunctionPermissionService functionPermissionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/codes")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCodes() {
        checkAdmin();
        List<Map<String, Object>> result = new ArrayList<>();
        for (FunctionCode fc : FunctionCode.values()) {
            Map<String, Object> item = new HashMap<>();
            item.put("code", fc.getCode());
            item.put("name", fc.getName());
            result.add(item);
        }
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/subjects")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSubjects(@RequestParam String functionCode) {
        checkAdmin();
        return ResponseEntity.ok(ApiResponse.success(functionPermissionService.getSubjects(functionCode)));
    }

    @PostMapping("/subjects")
    @Auditable(operationType = "PERMISSION_SUBJECT_ADD", targetType = "PERMISSION")
    public ResponseEntity<ApiResponse<Void>> addSubjects(@RequestBody Map<String, Object> request) {
        Long operatorId = getCurrentUserId();
        checkAdmin();
        String functionCode = (String) request.get("functionCode");
        List<Long> userIds = extractUserIds(request.get("userIds"));
        functionPermissionService.addSubjects(functionCode, userIds, operatorId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @DeleteMapping("/subjects")
    @Auditable(operationType = "PERMISSION_SUBJECT_REMOVE", targetType = "PERMISSION")
    public ResponseEntity<ApiResponse<Void>> removeSubjects(@RequestBody Map<String, Object> request) {
        Long operatorId = getCurrentUserId();
        checkAdmin();
        String functionCode = (String) request.get("functionCode");
        List<Long> userIds = extractUserIds(request.get("userIds"));
        functionPermissionService.removeSubjects(functionCode, userIds, operatorId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PutMapping("/subjects")
    @Auditable(operationType = "PERMISSION_SUBJECT_REPLACE", targetType = "PERMISSION")
    public ResponseEntity<ApiResponse<Void>> replaceSubjects(@RequestBody Map<String, Object> request) {
        Long operatorId = getCurrentUserId();
        checkAdmin();
        String functionCode = (String) request.get("functionCode");
        List<Long> userIds = extractUserIds(request.get("userIds"));
        functionPermissionService.replaceSubjects(functionCode, userIds, operatorId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getHistory(
            @RequestParam String functionCode,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        checkAdmin();
        return ResponseEntity.ok(ApiResponse.success(functionPermissionService.getHistory(functionCode, startDate, endDate)));
    }

    private void checkAdmin() {
        Long userId = getCurrentUserId();
        try {
            Map<String, Object> user = jdbcTemplate.queryForMap("SELECT role FROM users WHERE id = ?", userId);
            String role = (String) user.get("ROLE");
            if (role == null) role = (String) user.get("role");
            if (!"ADMIN".equals(role)) {
                throw new BusinessException(ErrorCode.FORBIDDEN_ACTION);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACTION);
        }
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }

    @SuppressWarnings("unchecked")
    private List<Long> extractUserIds(Object obj) {
        if (obj instanceof List) {
            List<Long> result = new ArrayList<>();
            for (Object item : (List<Object>) obj) {
                result.add(((Number) item).longValue());
            }
            return result;
        }
        throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD, "userIds不能为空");
    }
}
package com.pandora.backend.template;

import com.pandora.backend.audit.Auditable;
import com.pandora.backend.common.ApiResponse;
import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/template")
public class TemplateController {

    @Autowired
    private TemplateService templateService;

    @PostMapping
    @Auditable(operationType = "TEMPLATE_SAVE", targetType = "TEMPLATE")
    public ResponseEntity<ApiResponse<Map<String, Object>>> saveTemplate(@RequestBody Map<String, Object> request) {
        Long userId = getCurrentUserId();
        String templateType = (String) request.get("templateType");
        String name = (String) request.get("name");
        if (templateType == null) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD, "templateType不能为空");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> content = (Map<String, Object>) request.get("content");
        return ResponseEntity.ok(ApiResponse.success(templateService.saveTemplate(userId, templateType, name, content)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listTemplates(@RequestParam String templateType) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(templateService.listTemplates(userId, templateType)));
    }

    @PutMapping("/{templateId}")
    @Auditable(operationType = "TEMPLATE_UPDATE", targetType = "TEMPLATE")
    public ResponseEntity<ApiResponse<Void>> updateTemplate(
            @PathVariable Long templateId, @RequestBody Map<String, Object> request) {
        Long userId = getCurrentUserId();
        String name = (String) request.get("name");
        @SuppressWarnings("unchecked")
        Map<String, Object> content = (Map<String, Object>) request.get("content");
        templateService.updateTemplate(userId, templateId, name, content);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @DeleteMapping("/{templateId}")
    @Auditable(operationType = "TEMPLATE_DELETE", targetType = "TEMPLATE")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(@PathVariable Long templateId) {
        Long userId = getCurrentUserId();
        templateService.deleteTemplate(userId, templateId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/migrate")
    @Auditable(operationType = "TEMPLATE_MIGRATE", targetType = "TEMPLATE")
    public ResponseEntity<ApiResponse<Map<String, Object>>> migrateTemplates(@RequestBody Map<String, Object> request) {
        Long userId = getCurrentUserId();
        String templateType = (String) request.get("templateType");
        if (templateType == null) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD, "templateType不能为空");
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> templates = (List<Map<String, Object>>) request.get("templates");
        if (templates == null) {
            templates = List.of();
        }
        return ResponseEntity.ok(ApiResponse.success(templateService.migrateTemplates(userId, templateType, templates)));
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
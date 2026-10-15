package com.pandora.backend.draft;

import com.pandora.backend.audit.Auditable;
import com.pandora.backend.common.ApiResponse;
import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/draft")
public class DraftController {

    @Autowired
    private DraftService draftService;

    @PutMapping
    @Auditable(operationType = "DRAFT_SAVE", targetType = "DRAFT")
    public ResponseEntity<ApiResponse<Map<String, Object>>> saveDraft(@RequestBody Map<String, Object> request) {
        Long userId = getCurrentUserId();
        String draftType = (String) request.get("draftType");
        if (draftType == null) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD, "draftType不能为空");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> content = (Map<String, Object>) request.get("content");
        return ResponseEntity.ok(ApiResponse.success(draftService.saveDraft(userId, draftType, content)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDraft(@RequestParam String draftType) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(draftService.getDraft(userId, draftType)));
    }

    @DeleteMapping
    @Auditable(operationType = "DRAFT_DELETE", targetType = "DRAFT")
    public ResponseEntity<ApiResponse<Void>> deleteDraft(@RequestParam String draftType) {
        Long userId = getCurrentUserId();
        draftService.deleteDraft(userId, draftType);
        return ResponseEntity.ok(ApiResponse.success());
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
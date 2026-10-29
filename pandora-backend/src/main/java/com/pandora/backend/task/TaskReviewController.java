package com.pandora.backend.task;

import com.pandora.backend.audit.Auditable;
import com.pandora.backend.common.ApiResponse;
import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/task/reviews")
public class TaskReviewController {
    private final TaskChangeService changes;
    public TaskReviewController(TaskChangeService changes) { this.changes = changes; }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(Authentication auth, @RequestParam(defaultValue = "false") boolean mine) {
        return ApiResponse.success(changes.requests((Long) auth.getPrincipal(), mine));
    }

    @PostMapping("/{id}/decision")
    @Auditable(operationType = "TASK_REVIEW", targetType = "TASK_CHANGE")
    public ApiResponse<Map<String, Object>> decide(Authentication auth, @PathVariable long id, @RequestBody Map<String, Object> body) {
        if (!(body.get("approved") instanceof Boolean)) throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择通过或退回");
        return ApiResponse.success(changes.decide((Long) auth.getPrincipal(), id, Boolean.TRUE.equals(body.get("approved")), Objects.toString(body.get("reason"), "")));
    }
}

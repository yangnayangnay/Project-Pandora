package com.pandora.backend.log;

import com.pandora.backend.audit.Auditable;
import com.pandora.backend.common.ApiResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/log/{logId}/comments")
public class LogCommentController {
    private final LogCommentService comments;

    public LogCommentController(LogCommentService comments) { this.comments = comments; }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@PathVariable long logId) {
        return ApiResponse.success(comments.list(logId, userId()));
    }

    @PostMapping
    @Auditable(operationType = "LOG_COMMENT", targetType = "WORK_LOG")
    public ApiResponse<Map<String, Object>> add(@PathVariable long logId, @RequestBody Map<String, Object> body) {
        return ApiResponse.success(comments.add(logId, userId(), body.get("content")));
    }

    private long userId() { return ((Number) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).longValue(); }
}

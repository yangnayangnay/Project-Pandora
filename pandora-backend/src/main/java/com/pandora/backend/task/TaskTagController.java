package com.pandora.backend.task;

import com.pandora.backend.audit.Auditable;
import com.pandora.backend.common.ApiResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/task-tags")
public class TaskTagController {
    private final TaskTagService tags;
    public TaskTagController(TaskTagService tags) { this.tags = tags; }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() { return ApiResponse.success(tags.list()); }

    @PostMapping
    @Auditable(operationType = "TASK_TAG_CREATE", targetType = "TASK_TAG")
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> request) { return ApiResponse.success(tags.save(actor(), null, request)); }

    @PatchMapping("/{id}")
    @Auditable(operationType = "TASK_TAG_UPDATE", targetType = "TASK_TAG")
    public ApiResponse<Map<String, Object>> update(@PathVariable long id, @RequestBody Map<String, Object> request) { return ApiResponse.success(tags.save(actor(), id, request)); }

    @DeleteMapping("/{id}")
    @Auditable(operationType = "TASK_TAG_DELETE", targetType = "TASK_TAG")
    public ApiResponse<Void> delete(@PathVariable long id) { tags.delete(actor(), id); return ApiResponse.success(); }

    private long actor() { return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal(); }
}

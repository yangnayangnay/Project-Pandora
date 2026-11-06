package com.pandora.backend.daily;

import com.pandora.backend.common.ApiResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/daily-items")
public class DailyItemController {
    private final DailyItemService items;
    public DailyItemController(DailyItemService items) { this.items = items; }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(@RequestParam String month) { return ApiResponse.success(items.list(actor(), month)); }
    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body) { return ApiResponse.success(items.create(actor(), body)); }
    @PatchMapping("/{id}")
    public ApiResponse<Map<String, Object>> update(@PathVariable long id, @RequestBody Map<String, Object> body) { return ApiResponse.success(items.update(actor(), id, body)); }
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable long id) { items.delete(actor(), id); return ApiResponse.success(); }
    private long actor() { return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal(); }
}

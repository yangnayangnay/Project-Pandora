package com.pandora.backend.sync;

import com.pandora.backend.common.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/sync")
public class SyncController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> sync(@RequestBody Map<String, Object> request) {
        Long userId = getCurrentUserId();
        List<Map<String, Object>> items = (List<Map<String, Object>>) request.get("items");

        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> synced = new ArrayList<>();
        List<Map<String, Object>> conflicts = new ArrayList<>();

        if (items != null) {
            for (Map<String, Object> item : items) {
                String type = (String) item.get("type");
                String action = (String) item.get("action");

                if ("task".equals(type)) {
                    Map<String, Object> syncResult = syncTask(item, userId);
                    if ("CONFLICT".equals(syncResult.get("status"))) {
                        conflicts.add(syncResult);
                    } else {
                        synced.add(syncResult);
                    }
                } else if ("log".equals(type)) {
                    Map<String, Object> syncResult = syncLog(item, userId);
                    synced.add(syncResult);
                }
            }
        }

        result.put("synced", synced);
        result.put("conflicts", conflicts);
        result.put("syncTime", new Date());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private Map<String, Object> syncTask(Map<String, Object> item, Long userId) {
        Map<String, Object> result = new HashMap<>();
        result.put("clientId", item.get("clientTempId"));
        result.put("type", "task");

        Object idObj = item.get("id");
        if (idObj != null) {
            Long taskId = ((Number) idObj).longValue();
            try {
                Map<String, Object> existing = jdbcTemplate.queryForMap(
                        "SELECT version FROM tasks WHERE id = ? AND deleted = 0", taskId);

                Object clientVersionObj = item.get("version");
                Integer serverVersion = ((Number) existing.get("version")).intValue();
                Integer clientVersion = clientVersionObj != null ? ((Number) clientVersionObj).intValue() : 0;

                if (!serverVersion.equals(clientVersion)) {
                    result.put("status", "CONFLICT");
                    result.put("serverVersion", serverVersion);
                    return result;
                }

                result.put("status", "SYNCED");
                result.put("id", taskId);
            } catch (Exception e) {
                result.put("status", "NOT_FOUND");
            }
        } else {
            result.put("status", "SYNCED");
        }

        return result;
    }

    private Map<String, Object> syncLog(Map<String, Object> item, Long userId) {
        Map<String, Object> result = new HashMap<>();
        result.put("clientId", item.get("clientTempId"));
        result.put("type", "log");
        result.put("status", "SYNCED");
        return result;
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
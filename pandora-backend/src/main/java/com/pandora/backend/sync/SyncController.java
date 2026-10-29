package com.pandora.backend.sync;

import com.pandora.backend.common.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.pandora.backend.audit.Auditable;

import java.util.*;

/**
 * Round11 起的中继端点；R44 REQ-D 升级为落库契约：
 * - clientKey 幂等去重（sync_relay_keys 唯一索引——重发返回 SKIPPED 零副作用）
 * - 跨库 id 映射（sourceId→cloudId；日志类优先按自然键 (userId,logDate) 定位兜底）
 * - 删除传播软删；单条失败（NO_MAPPING）不阻断批次
 * 契约：请求 {items:[{clientKey,type,action?,sourceId,content}]}；
 * 响应 data.synced[] 逐条 {clientId,status:SYNCED/SKIPPED/NO_MAPPING,cloudId}。
 */
@RestController
@RequestMapping("/sync")
public class SyncController {

    private static final Logger log = LoggerFactory.getLogger(SyncController.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private com.pandora.backend.task.TaskChangeService taskChanges;

    @PostMapping
    @Auditable(operationType = "DATA_SYNC", targetType = "SYNC")
    @SuppressWarnings("unchecked")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sync(@RequestBody Map<String, Object> request) {
        Long userId = getCurrentUserId();
        List<Map<String, Object>> items = (List<Map<String, Object>>) request.get("items");

        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> synced = new ArrayList<>();

        if (items != null && !items.isEmpty()) {
            for (Map<String, Object> item : items) {
                synced.add(processItem(item, userId));
            }
        }

        result.put("synced", synced);
        result.put("conflicts", new ArrayList<>()); // 兼容旧契约字段
        result.put("syncTime", new Date());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /** 单条处理：幂等检查 → 落库 → 插映射 */
    private Map<String, Object> processItem(Map<String, Object> item, Long userId) {
        String clientKey = item.get("clientKey") != null ? item.get("clientKey").toString() : "";
        String type = item.get("type") != null ? item.get("type").toString() : "";
        Map<String, Object> out = new HashMap<>();
        out.put("clientId", clientKey);
        out.put("type", type);
        if (clientKey.isEmpty()) {
            out.put("status", "NO_MAPPING");
            return out;
        }

        // ① 幂等：clientKey 已处理 → SKIPPED（零副作用）
        try {
            List<Map<String, Object>> existing = jdbcTemplate.queryForList(
                    "SELECT cloud_id FROM sync_relay_keys WHERE client_key = ?", clientKey);
            if (!existing.isEmpty()) {
                out.put("status", "SKIPPED");
                out.put("cloudId", firstVal(existing.get(0), "cloud_id"));
                return out;
            }
        } catch (Exception e) {
            log.warn("idempotent check failed for {}: {}", clientKey, e.getMessage());
        }

        // ② 落库
        try {
            Long cloudId = "log".equals(type)
                    ? applyLog(item, userId)
                    : ("task".equals(type) ? applyTask(item, userId) : null);
            if (cloudId == null) {
                out.put("status", "NO_MAPPING");
                return out;
            }
            // ③ 插映射（幂等防线：唯一索引冲突时按已处理对待）
            jdbcTemplate.update(
                    "INSERT INTO sync_relay_keys (client_key, entity_type, source_id, cloud_id) VALUES (?, ?, ?, ?)",
                    clientKey, type, toLong(item.get("sourceId")), cloudId);
            out.put("status", "SYNCED");
            out.put("cloudId", cloudId);
        } catch (org.springframework.dao.DuplicateKeyException dup) {
            out.put("status", "SKIPPED");
        } catch (Exception e) {
            log.warn("apply failed for {}: {}", clientKey, e.getMessage());
            out.put("status", "NO_MAPPING");
            out.put("message", e.getMessage());
        }
        return out;
    }

    /** 日志类：按 (userId,logDate) 自然键定位（唯一索引天然防重）；EDIT=UPDATE；DELETE=软删 */
    private Long applyLog(Map<String, Object> item, Long userId) {
        Map<String, Object> content = parseContent(item.get("content"));
        String operation = content.get("operation") != null ? content.get("operation").toString() : "CREATE";
        String workItem = str(content.get("workItem"));
        String workContent = str(content.get("workContent"));
        String logDate = str(content.get("logDate"));
        if (!"DELETE".equals(operation)) {
            com.pandora.backend.log.LogDatePolicy.requireWritable(logDate);
        }

        if (!List.of("CREATE", "EDIT", "DELETE").contains(operation)) throw new IllegalArgumentException("不支持的日志操作");
        if (!"DELETE".equals(operation) && (workItem == null || workItem.trim().isEmpty() || workItem.trim().length() > 512)) {
            throw new IllegalArgumentException("日志标题不能为空且不能超过512字符");
        }
        List<Map<String, Object>> existing = jdbcTemplate.queryForList(
                "SELECT id, deleted FROM work_logs WHERE user_id = ? AND log_date = ?", userId, logDate);
        if (!existing.isEmpty()) {
            Long existingId = toLong(firstVal(existing.get(0), "id"));
            boolean deleted = ((Number) firstVal(existing.get(0), "deleted")).intValue() != 0;
            if ("CREATE".equals(operation) && !deleted) throw new IllegalArgumentException("该日期已有日志，请打开原日志编辑，不能直接覆盖");
            if ("EDIT".equals(operation) && deleted) throw new IllegalArgumentException("原日志已删除，请重新创建");
            if ("DELETE".equals(operation)) jdbcTemplate.update("UPDATE work_logs SET deleted = 1 WHERE id = ?", existingId);
            else jdbcTemplate.update(
                    "UPDATE work_logs SET work_item = ?, work_content = ?, deleted = 0, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                    workItem, workContent, existingId);
            return existingId;
        }
        if ("DELETE".equals(operation)) return 0L;
        if ("EDIT".equals(operation)) throw new IllegalArgumentException("未找到原日志，请确认日期和账号后重试");
        jdbcTemplate.update(
                "INSERT INTO work_logs (user_id, work_item, work_content, log_date, completion_status, created_at) VALUES (?, ?, ?, ?, 'PENDING', CURRENT_TIMESTAMP)",
                userId, workItem, workContent, logDate);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM work_logs WHERE user_id = ? AND log_date = ?", Long.class, userId, logDate);
    }

    /** 任务类：状态流转按 sourceId→cloudId 映射定位云端任务 */
    private Long applyTask(Map<String, Object> item, Long userId) {
        Map<String, Object> content = parseContent(item.get("content"));
        Long sourceId = toLong(item.get("sourceId"));

        Long cloudId = null;
        if (sourceId != null) {
            List<Map<String, Object>> mapped = jdbcTemplate.queryForList(
                    "SELECT cloud_id FROM sync_relay_keys WHERE entity_type = 'task' AND source_id = ? ORDER BY id DESC LIMIT 1",
                    sourceId);
            if (!mapped.isEmpty()) {
                cloudId = toLong(firstVal(mapped.get(0), "cloud_id"));
            }
        }
        if (cloudId == null) {
            return null; // NO_MAPPING
        }
        String targetStatus = str(content.get("targetStatus"));
        if (targetStatus != null && !targetStatus.isEmpty()) {
            taskChanges.updateExecution(userId, cloudId, targetStatus, str(content.get("progressNote")));
        }
        return cloudId;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseContent(Object raw) {
        if (raw instanceof Map) {
            return (Map<String, Object>) raw;
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> parsed = mapper.readValue(raw != null ? raw.toString() : "{}", Map.class);
            return parsed != null ? parsed : new HashMap<>();
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private static String str(Object o) {
        return o != null ? o.toString() : null;
    }

    private static Long toLong(Object o) {
        if (o == null) return null;
        try {
            return Long.parseLong(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Object firstVal(Map<String, Object> row, String key) {
        if (row.containsKey(key)) return row.get(key);
        for (Map.Entry<String, Object> e : row.entrySet()) {
            if (e.getKey().equalsIgnoreCase(key)) return e.getValue();
        }
        return null;
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}

package com.pandora.backend.log;

import com.pandora.backend.audit.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class LogDraftGenerator {

    @Autowired
    private AuditService auditService;

    public Map<String, Object> generateDraft(Long userId) {
        List<Map<String, Object>> logs = auditService.queryMyTodayAuditLogs(userId);
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        Map<String, Object> result = new HashMap<>();
        result.put("date", today);
        result.put("sourceCount", logs.size());

        if (logs.isEmpty()) {
            result.put("draftText", "");
            return result;
        }

        Map<String, List<String>> grouped = new LinkedHashMap<>();
        grouped.put("任务操作", new ArrayList<>());
        grouped.put("日志操作", new ArrayList<>());
        grouped.put("用户操作", new ArrayList<>());
        grouped.put("视图操作", new ArrayList<>());
        grouped.put("其他操作", new ArrayList<>());

        for (Map<String, Object> log : logs) {
            String opType = (String) log.get("OPERATION_TYPE");
            String content = (String) log.get("OPERATION_CONTENT");
            String detail = (String) log.get("DETAIL");
            String action = (String) log.get("ACTION");

            String desc = content != null ? content : (detail != null ? detail : action);
            if (desc == null) desc = action;

            String groupKey;
            if (opType == null) {
                groupKey = "其他操作";
            } else if (opType.startsWith("TASK_")) {
                groupKey = "任务操作";
            } else if (opType.startsWith("LOG_")) {
                groupKey = "日志操作";
            } else if (opType.startsWith("USER_")) {
                groupKey = "用户操作";
            } else if (opType.startsWith("VIEW_")) {
                groupKey = "视图操作";
            } else {
                groupKey = "其他操作";
            }
            grouped.get(groupKey).add(desc);
        }

        StringBuilder sb = new StringBuilder("【今日操作汇总】\n");
        int idx = 1;
        for (Map.Entry<String, List<String>> entry : grouped.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            sb.append(idx++).append(". ").append(entry.getKey()).append("：\n");
            for (String item : entry.getValue()) {
                sb.append("   - ").append(item).append("\n");
            }
        }

        result.put("draftText", sb.toString().trim());
        return result;
    }
}
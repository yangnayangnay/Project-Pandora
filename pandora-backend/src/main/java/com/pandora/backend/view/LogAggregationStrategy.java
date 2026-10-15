package com.pandora.backend.view;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.util.*;

@Component
public class LogAggregationStrategy {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> aggregate(Long userId, int rangeDays) {
        Date today = new Date(System.currentTimeMillis());
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -rangeDays);
        Date startDate = new Date(cal.getTimeInMillis());

        List<Map<String, Object>> logs = jdbcTemplate.queryForList(
                "SELECT * FROM work_logs WHERE user_id = ? AND deleted = 0 AND log_date BETWEEN ? AND ? ORDER BY log_date DESC",
                userId, startDate, today);

        if (logs.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, Integer> workItemCount = new HashMap<>();
        for (Map<String, Object> log : logs) {
            String workItem = getStr(log, "WORK_ITEM", "work_item");
            workItemCount.merge(workItem, 1, Integer::sum);
        }

        String todayStr = today.toString();
        List<Map<String, Object>> todayLogs = new ArrayList<>();
        List<Map<String, Object>> incompleteLogs = new ArrayList<>();
        List<Map<String, Object>> frequentLogs = new ArrayList<>();
        List<Map<String, Object>> recentLogs = new ArrayList<>();

        for (Map<String, Object> log : logs) {
            String logDate = getStr(log, "LOG_DATE", "log_date");
            String status = getStr(log, "COMPLETION_STATUS", "completion_status");
            String workItem = getStr(log, "WORK_ITEM", "work_item");

            Map<String, Object> item = new HashMap<>();
            item.put("content", workItem);
            item.put("is_auto_filled", true);
            item.put("work_type", "AUTO_FROM_LOG");
            item.put("source_note", "来源: " + logDate + " 工作日志");
            item.put("source_log_date", logDate);

            if (todayStr.equals(logDate)) {
                todayLogs.add(item);
            } else if (!"COMPLETED".equals(status) && !"DONE".equals(status)) {
                incompleteLogs.add(item);
            } else if (workItemCount.getOrDefault(workItem, 0) >= 2) {
                frequentLogs.add(item);
            } else {
                recentLogs.add(item);
            }
        }

        frequentLogs.sort((a, b) -> {
            int ca = workItemCount.getOrDefault((String) a.get("content"), 0);
            int cb = workItemCount.getOrDefault((String) b.get("content"), 0);
            return cb - ca;
        });

        List<Map<String, Object>> result = new ArrayList<>();
        result.addAll(todayLogs);
        result.addAll(incompleteLogs);
        result.addAll(frequentLogs);
        result.addAll(recentLogs);

        for (int i = 0; i < result.size(); i++) {
            result.get(i).put("rank_order", i + 1);
        }

        return result;
    }

    private String getStr(Map<String, Object> map, String upper, String lower) {
        Object val = map.get(upper);
        if (val == null) val = map.get(lower);
        return val != null ? val.toString() : "";
    }
}
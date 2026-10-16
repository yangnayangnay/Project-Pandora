package com.pandora.backend.top10;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 应用启动时自动清理 top10_works 表中的测试脏数据。
 *
 * <p>清理范围：</p>
 * <ul>
 *   <li>公司重要事项（COMPANY_IMPORTANT）超过 10 条的部分</li>
 *   <li>公司派发（COMPANY_DISPATCH）同一 related_task_id 的重复记录（保留 id 最小者）</li>
 *   <li>content 为空或纯数字的测试条目</li>
 *   <li>rank_order 重复的条目（同一 work_type + rank_order 保留 id 最小者）</li>
 * </ul>
 *
 * <p>全部采用软删除（deleted = 1），保留数据可追溯。启动时执行一次，
 * 防止测试产生的脏数据累积导致真实用户创建时触发"数量已满"。</p>
 */
@Component
@Order(100)
public class Top10StartupCleaner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(Top10StartupCleaner.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        int cleaned = 0;
        cleaned += cleanNumericTestEntries();
        cleaned += cleanEmptyContentEntries();
        cleaned += cleanDuplicateRank();
        cleaned += cleanOverLimitEntries();
        cleaned += cleanDuplicateDispatch();
        if (cleaned > 0) {
            log.info("Top10启动清理完成，共软删除 {} 条脏数据", cleaned);
        } else {
            log.info("Top10启动清理完成，无脏数据");
        }
    }

    private int cleanNumericTestEntries() {
        List<Map<String, Object>> dirty = jdbcTemplate.queryForList(
                "SELECT id FROM top10_works WHERE deleted = 0 AND content REGEXP '^[0-9]+$'");
        return softDelete(dirty, "纯数字测试条目");
    }

    private int cleanEmptyContentEntries() {
        List<Map<String, Object>> dirty = jdbcTemplate.queryForList(
                "SELECT id FROM top10_works WHERE deleted = 0 AND (content IS NULL OR TRIM(content) = '')");
        return softDelete(dirty, "空内容条目");
    }

    private int cleanDuplicateRank() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, work_type, rank_order FROM top10_works " +
                "WHERE deleted = 0 AND rank_order IS NOT NULL ORDER BY work_type, rank_order, id ASC");
        int count = 0;
        Map<String, Long> seen = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object idObj = row.get("ID") != null ? row.get("ID") : row.get("id");
            Object wtObj = row.get("WORK_TYPE") != null ? row.get("WORK_TYPE") : row.get("work_type");
            Object roObj = row.get("RANK_ORDER") != null ? row.get("RANK_ORDER") : row.get("rank_order");
            String key = wtObj + "#" + roObj;
            Long id = ((Number) idObj).longValue();
            if (seen.containsKey(key)) {
                jdbcTemplate.update("UPDATE top10_works SET deleted = 1, updated_at = CURRENT_TIMESTAMP WHERE id = ?", id);
                count++;
            } else {
                seen.put(key, id);
            }
        }
        if (count > 0) {
            log.info("清理 {} 条重复 rank_order 条目", count);
        }
        return count;
    }

    private int cleanOverLimitEntries() {
        int count = 0;
        for (String workType : new String[]{"COMPANY_IMPORTANT", "COMPANY_DISPATCH"}) {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT id FROM top10_works WHERE work_type = ? AND deleted = 0 " +
                    "ORDER BY rank_order ASC, id ASC LIMIT 100 OFFSET 10", workType);
            count += softDelete(rows, workType + " 超量条目");
        }
        return count;
    }

    private int cleanDuplicateDispatch() {
        List<Map<String, Object>> groups = jdbcTemplate.queryForList(
                "SELECT related_task_id, COUNT(*) AS cnt FROM top10_works " +
                "WHERE work_type = 'COMPANY_DISPATCH' AND deleted = 0 AND related_task_id IS NOT NULL " +
                "GROUP BY related_task_id HAVING COUNT(*) > 1");
        int count = 0;
        for (Map<String, Object> group : groups) {
            Object taskIdObj = group.get("RELATED_TASK_ID") != null ? group.get("RELATED_TASK_ID") : group.get("related_task_id");
            if (taskIdObj == null) {
                continue;
            }
            Long taskId = ((Number) taskIdObj).longValue();
            List<Map<String, Object>> marks = jdbcTemplate.queryForList(
                    "SELECT id FROM top10_works WHERE work_type = 'COMPANY_DISPATCH' AND related_task_id = ? AND deleted = 0 " +
                    "ORDER BY id ASC OFFSET 1", taskId);
            count += softDelete(marks, "taskId=" + taskId + " 的重复派发标记");
        }
        return count;
    }

    private int softDelete(List<Map<String, Object>> rows, String reason) {
        if (rows.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (Map<String, Object> row : rows) {
            Object idObj = row.get("ID") != null ? row.get("ID") : row.get("id");
            if (idObj == null) {
                continue;
            }
            Long id = ((Number) idObj).longValue();
            jdbcTemplate.update("UPDATE top10_works SET deleted = 1, updated_at = CURRENT_TIMESTAMP WHERE id = ?", id);
            count++;
        }
        if (count > 0) {
            log.info("清理 {} 条{}脏数据", count, reason);
        }
        return count;
    }
}
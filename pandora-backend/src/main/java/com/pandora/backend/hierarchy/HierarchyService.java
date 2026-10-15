package com.pandora.backend.hierarchy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class HierarchyService {

    private static final Logger log = LoggerFactory.getLogger(HierarchyService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${hierarchy.max-depth:10}")
    private int maxDepth;

    @Value("${hierarchy.cycle-detect:true}")
    private boolean cycleDetect;

    public List<Map<String, Object>> getSubordinates(Long userId) {
        String role = getUserRole(userId);
        if ("ADMIN".equals(role)) {
            return jdbcTemplate.queryForList(
                    "SELECT id, username, real_name, role, 0 as depth FROM users WHERE deleted = 0 AND id != ? AND approval_status = 'APPROVED' ORDER BY id", userId);
        }
        if ("FOUNDER".equals(role)) {
            return jdbcTemplate.queryForList(
                    "SELECT id, username, real_name, role, 0 as depth FROM users WHERE deleted = 0 AND id != ? AND role != 'ADMIN' AND approval_status = 'APPROVED' ORDER BY id", userId);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        visited.add(userId);
        Queue<long[]> queue = new LinkedList<>();
        queue.add(new long[]{userId, 0});
        while (!queue.isEmpty()) {
            long[] current = queue.poll();
            long currentId = current[0];
            int depth = (int) current[1];
            if (depth >= maxDepth) continue;
            List<Map<String, Object>> directSubs = jdbcTemplate.queryForList(
                    "SELECT h.subordinate_id, u.username, u.real_name, u.role FROM user_hierarchy h JOIN users u ON h.subordinate_id = u.id WHERE h.superior_id = ? AND u.deleted = 0 AND u.approval_status = 'APPROVED'", currentId);
            for (Map<String, Object> sub : directSubs) {
                Long subId = (Long) sub.get("SUBORDINATE_ID");
                if (subId == null) subId = (Long) sub.get("subordinate_id");
                if (!visited.contains(subId)) {
                    visited.add(subId);
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("id", subId);
                    entry.put("username", sub.get("USERNAME") != null ? sub.get("USERNAME") : sub.get("username"));
                    entry.put("real_name", sub.get("REAL_NAME") != null ? sub.get("REAL_NAME") : sub.get("real_name"));
                    entry.put("role", sub.get("ROLE") != null ? sub.get("ROLE") : sub.get("role"));
                    entry.put("depth", depth + 1);
                    result.add(entry);
                    queue.add(new long[]{subId, depth + 1});
                }
            }
        }
        return result;
    }

    public List<Long> getSubordinateIds(Long userId) {
        List<Map<String, Object>> subs = getSubordinates(userId);
        List<Long> ids = new ArrayList<>();
        for (Map<String, Object> sub : subs) {
            ids.add((Long) sub.get("id"));
        }
        return ids;
    }

    public List<Map<String, Object>> getSuperiors(Long userId) {
        List<Map<String, Object>> result = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        visited.add(userId);
        Queue<long[]> queue = new LinkedList<>();
        queue.add(new long[]{userId, 0});
        while (!queue.isEmpty()) {
            long[] current = queue.poll();
            long currentId = current[0];
            int depth = (int) current[1];
            if (depth >= maxDepth) continue;
            List<Map<String, Object>> directSups = jdbcTemplate.queryForList(
                    "SELECT h.superior_id, u.username, u.real_name, u.role FROM user_hierarchy h JOIN users u ON h.superior_id = u.id WHERE h.subordinate_id = ? AND u.deleted = 0", currentId);
            for (Map<String, Object> sup : directSups) {
                Long supId = (Long) sup.get("SUPERIOR_ID");
                if (supId == null) supId = (Long) sup.get("superior_id");
                if (!visited.contains(supId)) {
                    visited.add(supId);
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("id", supId);
                    entry.put("username", sup.get("USERNAME") != null ? sup.get("USERNAME") : sup.get("username"));
                    entry.put("real_name", sup.get("REAL_NAME") != null ? sup.get("REAL_NAME") : sup.get("real_name"));
                    entry.put("role", sup.get("ROLE") != null ? sup.get("ROLE") : sup.get("role"));
                    entry.put("depth", depth + 1);
                    result.add(entry);
                    queue.add(new long[]{supId, depth + 1});
                }
            }
        }
        return result;
    }

    public boolean isSubordinate(Long superiorId, Long subordinateId) {
        if (superiorId.equals(subordinateId)) return false;
        List<Long> subIds = getSubordinateIds(superiorId);
        return subIds.contains(subordinateId);
    }

    public boolean checkNoCycle(Long superiorId, Long subordinateId) {
        if (!cycleDetect) return true;
        if (superiorId.equals(subordinateId)) return false;
        List<Long> supIds = new ArrayList<>();
        for (Map<String, Object> sup : getSuperiors(superiorId)) {
            supIds.add((Long) sup.get("id"));
        }
        return !supIds.contains(subordinateId) && !superiorId.equals(subordinateId);
    }

    public boolean checkSingleSuperior(Long subordinateId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_hierarchy WHERE subordinate_id = ?", Integer.class, subordinateId);
        return count != null && count > 0;
    }

    public Map<String, Object> addRelation(Long superiorId, Long subordinateId, boolean forceReplace) {
        if (!checkNoCycle(superiorId, subordinateId)) {
            Map<String, Object> r = new HashMap<>();
            r.put("success", false);
            r.put("reason", "不能形成循环关系");
            return r;
        }
        if (checkSingleSuperior(subordinateId)) {
            if (forceReplace) {
                jdbcTemplate.update("DELETE FROM user_hierarchy WHERE subordinate_id = ?", subordinateId);
            } else {
                Map<String, Object> r = new HashMap<>();
                r.put("success", false);
                r.put("reason", "该用户已有直接上级，需确认替换");
                return r;
            }
        }
        jdbcTemplate.update("INSERT INTO user_hierarchy (superior_id, subordinate_id) VALUES (?, ?)", superiorId, subordinateId);
        Map<String, Object> r = new HashMap<>();
        r.put("success", true);
        return r;
    }

    public void removeRelation(Long superiorId, Long subordinateId) {
        jdbcTemplate.update("DELETE FROM user_hierarchy WHERE superior_id = ? AND subordinate_id = ?", superiorId, subordinateId);
    }

    public List<Map<String, Object>> getHierarchyTree() {
        List<Map<String, Object>> allRelations = jdbcTemplate.queryForList(
                "SELECT h.superior_id, h.subordinate_id, COALESCE(u1.real_name, u1.username) as sup_name, u1.role as sup_role, COALESCE(u2.real_name, u2.username) as sub_name, u2.role as sub_role FROM user_hierarchy h JOIN users u1 ON h.superior_id = u1.id JOIN users u2 ON h.subordinate_id = u2.id WHERE u1.deleted = 0 AND u2.deleted = 0 ORDER BY h.superior_id");
        List<Map<String, Object>> roots = jdbcTemplate.queryForList(
                "SELECT id, username, real_name, role FROM users WHERE deleted = 0 AND approval_status = 'APPROVED' AND id NOT IN (SELECT subordinate_id FROM user_hierarchy) ORDER BY id");
        return allRelations;
    }

    /**
     * B10：查询孤儿节点 - 上级已软删除但下属仍有效的用户
     */
    public List<Map<String, Object>> getOrphanNodes() {
        try {
            List<Map<String, Object>> orphans = jdbcTemplate.queryForList(
                    "SELECT DISTINCT u.id, u.username, u.real_name, u.role FROM users u " +
                    "JOIN user_hierarchy h ON u.id = h.subordinate_id " +
                    "JOIN users sup ON h.superior_id = sup.id " +
                    "WHERE sup.deleted = 1 AND u.deleted = 0 AND u.approval_status = 'APPROVED'");
            if (!orphans.isEmpty()) {
                log.warn("检测到 {} 个孤儿节点（上级已删除）", orphans.size());
            }
            return orphans;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String getUserRole(Long userId) {
        try {
            return jdbcTemplate.queryForObject("SELECT role FROM users WHERE id = ? AND deleted = 0", String.class, userId);
        } catch (Exception e) {
            return null;
        }
    }
}
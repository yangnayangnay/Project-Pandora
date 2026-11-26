package com.pandora.backend.user;

import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;

/**
 * 组织层级服务——从已审批岗位推导汇报关系。
 * 基于 users 表中的角色和组织归属，计算上下级关系。
 * 不将 ADMIN 视为岗位（系统管理员不参与组织架构）。
 */
public final class OrganizationHierarchy {
    private final JdbcTemplate db;
    public OrganizationHierarchy(JdbcTemplate db) { this.db = db; }

    /**
     * 获取当前所有已审批岗位的快照。
     * 仅包含业务角色（FOUNDER、DEPT_HEAD、TEAM_LEADER、EMPLOYEE）。
     *
     * @return 岗位列表
     */
    public List<Map<String, Object>> snapshot() {
        return db.queryForList("SELECT id,username,real_name,role,department_id,team_id FROM users WHERE deleted=0 "
                + "AND approval_status='APPROVED' AND role IN ('FOUNDER','DEPT_HEAD','TEAM_LEADER','EMPLOYEE') ORDER BY id");
    }

    /**
     * 计算指定人员的直属上级 ID 列表。
     * 汇报关系规则：
     * - DEPT_HEAD -> FOUNDER（同部门）
     * - TEAM_LEADER -> DEPT_HEAD（同部门）
     * - EMPLOYEE -> TEAM_LEADER（同部门+同团队）
     *
     * @param person 人员数据
     * @param people 岗位快照
     * @return 上级 ID 列表
     */
    public static List<Long> superiors(Map<String, Object> person, List<Map<String, Object>> people) {
        String expected = switch (Objects.toString(person.get("role"), "")) {
            case "DEPT_HEAD" -> "FOUNDER";
            case "TEAM_LEADER" -> "DEPT_HEAD";
            case "EMPLOYEE" -> "TEAM_LEADER";
            default -> "";
        };
        return people.stream().filter(candidate -> expected.equals(candidate.get("role")))
                .filter(candidate -> !Objects.equals(person.get("id"), candidate.get("id")))
                .filter(candidate -> "FOUNDER".equals(expected) || sameUnit(person, candidate, "department_id"))
                .filter(candidate -> !"TEAM_LEADER".equals(expected) || sameUnit(person, candidate, "team_id"))
                .map(candidate -> ((Number) candidate.get("id")).longValue()).sorted().toList();
    }

    /**
     * 判断两个人员是否属于同一组织单元（部门/团队）。
     */
    private static boolean sameUnit(Map<String, Object> a, Map<String, Object> b, String key) {
        return a.get(key) != null && Objects.equals(a.get(key), b.get(key));
    }

    /**
     * 获取所有汇报关系（用于组织架构图展示）。
     * 每个关系包含上级ID、下级ID、上级姓名、下级姓名。
     *
     * @return 汇报关系列表
     */
    public List<Map<String, Object>> relations() {
        var people = snapshot();
        Map<Long, Map<String, Object>> byId = new HashMap<>();
        people.forEach(person -> byId.put(((Number) person.get("id")).longValue(), person));
        List<Map<String, Object>> result = new ArrayList<>();
        for (var person : people) for (long parentId : superiors(person, people)) {
            var parent = byId.get(parentId);
            result.add(Map.of("superior_id", parentId, "subordinate_id", person.get("id"),
                    "superior_name", Objects.toString(parent.get("real_name"), parent.get("username").toString()),
                    "subordinate_name", Objects.toString(person.get("real_name"), person.get("username").toString())));
        }
        return result;
    }

    /**
     * 获取指定人员的亲属（上级或下级）。
     * 使用 BFS 遍历组织层级图，返回所有可达节点。
     *
     * @param id 人员ID
     * @param upward true 表示获取上级，false 表示获取下级
     * @return 亲属列表（按层级深度排序）
     */
    public List<Map<String, Object>> relatives(long id, boolean upward) {
        var people = snapshot();
        Map<Long, Map<String, Object>> byId = new HashMap<>();
        people.forEach(person -> byId.put(((Number) person.get("id")).longValue(), person));
        if (!byId.containsKey(id)) return List.of();
        // A founder's company-wide scope must survive a vacant department/team head post.
        // Direct reporting lines still come from superiors(); this only completes the visible subtree.
        if (!upward && "FOUNDER".equals(byId.get(id).get("role"))) {
            List<Map<String,Object>> company = new ArrayList<>();
            for (var person : people) {
                String role = Objects.toString(person.get("role"), "");
                if ("FOUNDER".equals(role)) continue;
                Map<String,Object> entry = new LinkedHashMap<>();
                for (String key : List.of("id", "username", "real_name", "role")) entry.put(key,person.get(key));
                entry.put("depth", "DEPT_HEAD".equals(role) ? 1 : "TEAM_LEADER".equals(role) ? 2 : 3);
                company.add(entry);
            }
            return company;
        }
        Map<Long, List<Long>> edges = new HashMap<>();
        for (var person : people) for (long parent : superiors(person, people)) {
            long child = ((Number) person.get("id")).longValue();
            edges.computeIfAbsent(upward ? child : parent, key -> new ArrayList<>()).add(upward ? parent : child);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        Set<Long> visited = new HashSet<>(Set.of(id));
        Queue<long[]> queue = new ArrayDeque<>();
        queue.add(new long[]{id, 0});
        while (!queue.isEmpty()) {
            var current = queue.remove();
            for (long next : edges.getOrDefault(current[0], List.of())) if (visited.add(next)) {
                var source = byId.get(next);
                Map<String, Object> entry = new LinkedHashMap<>();
                for (String key : List.of("id", "username", "real_name", "role")) entry.put(key, source.get(key));
                entry.put("depth", (int) current[1] + 1);
                result.add(entry);
                queue.add(new long[]{next, current[1] + 1});
            }
        }
        return result;
    }

    /**
     * 同步组织层级关系到数据库。
     * 在人员调动或角色变更后调用，确保 user_hierarchy 表与当前组织状态一致。
     * 流程：
     * 1. 清理无效关系（已删除/未审批/非业务角色）
     * 2. 计算每个人员的预期上级列表
     * 3. 对比当前关系，差异部分进行增删
     *
     * @param before 变更前的岗位快照
     * @param changedId 变更的人员ID
     */
    public void synchronize(List<Map<String, Object>> before, long changedId) {
        List<Map<String, Object>> after = snapshot();
        // 清理无效关系
        db.update("DELETE FROM user_hierarchy WHERE superior_id NOT IN (SELECT id FROM users WHERE deleted=0 "
                + "AND approval_status='APPROVED' AND role IN ('FOUNDER','DEPT_HEAD','TEAM_LEADER','EMPLOYEE')) "
                + "OR subordinate_id NOT IN (SELECT id FROM users WHERE deleted=0 "
                + "AND approval_status='APPROVED' AND role IN ('FOUNDER','DEPT_HEAD','TEAM_LEADER','EMPLOYEE'))");
        for (Map<String, Object> person : after) {
            long id = ((Number) person.get("id")).longValue();
            List<Long> expected = superiors(person, after);
            List<Long> current = db.queryForList("SELECT superior_id FROM user_hierarchy WHERE subordinate_id=? ORDER BY superior_id", Long.class, id);
            if (current.equals(expected)) continue;
            db.update("DELETE FROM user_hierarchy WHERE subordinate_id=?", id);
            expected.forEach(parent -> db.update("INSERT INTO user_hierarchy(superior_id,subordinate_id) VALUES(?,?)", parent, id));
        }
    }
}
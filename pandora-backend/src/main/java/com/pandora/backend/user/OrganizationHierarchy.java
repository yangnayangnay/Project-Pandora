package com.pandora.backend.user;

import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;

/** Derives business reporting lines from approved posts; never treats ADMIN as a post. */
public final class OrganizationHierarchy {
    private final JdbcTemplate db;
    public OrganizationHierarchy(JdbcTemplate db) { this.db = db; }

    public List<Map<String, Object>> snapshot() {
        return db.queryForList("SELECT id,username,real_name,role,department_id,team_id FROM users WHERE deleted=0 "
                + "AND approval_status='APPROVED' AND role IN ('FOUNDER','DEPT_HEAD','TEAM_LEADER','EMPLOYEE') ORDER BY id");
    }

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

    private static boolean sameUnit(Map<String, Object> a, Map<String, Object> b, String key) {
        return a.get(key) != null && Objects.equals(a.get(key), b.get(key));
    }

    /** All directory clients project the same approved posts, even with legacy stored links. */
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

    /** Call in the personnel write transaction. Reconcile legacy links too, including
     * administrator, peer-founder and no-longer-approved endpoints. */
    public void synchronize(List<Map<String, Object>> before, long changedId) {
        List<Map<String, Object>> after = snapshot();
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

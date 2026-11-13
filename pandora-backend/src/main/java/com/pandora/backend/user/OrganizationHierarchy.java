package com.pandora.backend.user;

import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;

/** Derives business reporting lines from approved posts; never treats ADMIN as a post. */
public final class OrganizationHierarchy {
    private final JdbcTemplate db;
    public OrganizationHierarchy(JdbcTemplate db) { this.db = db; }

    public List<Map<String, Object>> snapshot() {
        return db.queryForList("SELECT id,role,department_id,team_id FROM users WHERE deleted=0 "
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

    /** Call in the same transaction as the personnel write, using its pre-write snapshot.
     * Rebind the edited user, former children and scopes whose responsible post changed.
     * Unrelated reporting lines and all account/profile data remain untouched. */
    public void synchronize(List<Map<String, Object>> before, long changedId) {
        List<Map<String, Object>> after = snapshot();
        Map<Long, Map<String, Object>> old = new HashMap<>();
        before.forEach(person -> old.put(((Number) person.get("id")).longValue(), person));
        Set<Long> affected = new HashSet<>(db.queryForList(
                "SELECT subordinate_id FROM user_hierarchy WHERE superior_id=?", Long.class, changedId));
        affected.add(changedId);
        for (Map<String, Object> person : after) {
            long id = ((Number) person.get("id")).longValue();
            Map<String, Object> previous = old.get(id);
            if (previous == null || !superiors(previous, before).equals(superiors(person, after))) affected.add(id);
        }
        for (Map<String, Object> person : after) {
            long id = ((Number) person.get("id")).longValue();
            if (!affected.contains(id)) continue;
            List<Long> expected = superiors(person, after);
            List<Long> current = db.queryForList("SELECT superior_id FROM user_hierarchy WHERE subordinate_id=? ORDER BY superior_id", Long.class, id);
            if (current.equals(expected)) continue;
            db.update("DELETE FROM user_hierarchy WHERE subordinate_id=?", id);
            expected.forEach(parent -> db.update("INSERT INTO user_hierarchy(superior_id,subordinate_id) VALUES(?,?)", parent, id));
        }
    }
}

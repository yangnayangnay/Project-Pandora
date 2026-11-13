package com.pandora.backend.user;

import java.util.List;
import java.util.Map;
import java.util.Locale;

/** Role structure also projects legacy affiliations safely before a database migration. */
public final class OrganizationRules {
    private OrganizationRules() {}
    public static boolean isBusinessRole(String role) { return List.of("FOUNDER", "DEPT_HEAD", "TEAM_LEADER", "EMPLOYEE").contains(role); }
    public static boolean hasDepartment(String role) { return !"FOUNDER".equals(role) && !"ADMIN".equals(role); }
    public static boolean hasTeam(String role) { return hasDepartment(role) && !"DEPT_HEAD".equals(role); }

    public static Map<String, Object> normalize(Map<String, Object> person) {
        String role = person.entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase("role"))
                .map(e -> String.valueOf(e.getValue())).findFirst().orElse("");
        for (String key : List.copyOf(person.keySet())) {
            String field = key.replace("_", "").toLowerCase(Locale.ROOT);
            if ((!hasDepartment(role) && List.of("departmentid", "departmentname", "deptname").contains(field))
                    || (!hasTeam(role) && List.of("teamid", "teamname").contains(field))) person.put(key, null);
        }
        return person;
    }

    public static List<Map<String, Object>> normalize(List<Map<String, Object>> people) {
        people.forEach(OrganizationRules::normalize);
        return people;
    }
}

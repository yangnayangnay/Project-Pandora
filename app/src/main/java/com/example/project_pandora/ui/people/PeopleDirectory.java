package com.example.project_pandora.ui.people;

import com.example.project_pandora.core.util.NumberCompat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Shared grouping/search rules. Input is already restricted by the caller's authorized endpoint. */
public final class PeopleDirectory {
    private PeopleDirectory() {}

    public static long departmentId(Map<String, Object> person) {
        if (isFounder(person)) return -1L;
        Long id = NumberCompat.toLong(value(person, "department_id"));
        return id == null ? -1L : id;
    }

    public static boolean isMobilePerson(Map<String, Object> person) {
        return !"ADMIN".equalsIgnoreCase(text(person, "role"));
    }

    public static String name(Map<String, Object> person) {
        String realName = text(person, "real_name");
        String name = !realName.isEmpty() ? realName : text(person, "username");
        return name.isEmpty() ? "未命名人员" : name;
    }

    public static String text(Map<String, Object> person, String key) {
        String field = key.replace("_", "").toLowerCase(Locale.ROOT);
        if (field.startsWith("department") && isFounder(person)
                || field.startsWith("team") && !hasTeam(person)) return "";
        return java.util.Objects.toString(value(person, key), "").trim();
    }

    public static boolean isFounder(Map<String, Object> person) { return "FOUNDER".equalsIgnoreCase(text(person, "role")); }
    public static boolean hasTeam(Map<String, Object> person) {
        return !isFounder(person) && !"DEPT_HEAD".equalsIgnoreCase(text(person, "role"));
    }

    public static boolean matches(Map<String, Object> person, String query) {
        return (name(person) + " " + text(person, "username") + " " + text(person, "team_name"))
                .toLowerCase(Locale.ROOT).contains(query.trim().toLowerCase(Locale.ROOT));
    }

    public static Object value(Map<String, Object> person, String key) {
        Object value = person.get(key);
        if (value == null) value = person.get(key.toUpperCase(Locale.ROOT));
        if (value == null && key.contains("_")) {
            StringBuilder camel = new StringBuilder();
            boolean uppercase = false;
            for (char c : key.toCharArray()) {
                if (c == '_') uppercase = true;
                else { camel.append(uppercase ? Character.toUpperCase(c) : c); uppercase = false; }
            }
            value = person.get(camel.toString());
            if (value == null) value = person.get(camel.toString().toUpperCase(Locale.ROOT));
        }
        return value;
    }

    public static final class Department {
        public final long id;
        public final String key;
        public final String name;
        public final List<Map<String, Object>> people = new ArrayList<>();
        private Department(long id, String key, String name) { this.id = id; this.key = key; this.name = name; }
    }

    public static List<Department> departments(List<Map<String, Object>> people) {
        Map<String, Department> grouped = new LinkedHashMap<>();
        for (Map<String, Object> person : people) {
            if (!isMobilePerson(person) || isFounder(person)) continue;
            long id = departmentId(person);
            String name = text(person, "department_name");
            // Organization membership is required. Never invent an unassigned department.
            if (name.isEmpty()) continue;
            // Older relation endpoints contain department names but no IDs; retain those groups.
            String key = id == -1L && !name.isEmpty() ? "name:" + name : "id:" + id;
            Department department = grouped.get(key);
            if (department == null) {
                department = new Department(id, key, name);
                grouped.put(key, department);
            }
            department.people.add(person);
        }
        List<Department> result = new ArrayList<>(grouped.values());
        result.sort(Comparator.comparing((Department department) -> department.name).thenComparingLong(department -> department.id));
        for (Department department : result) department.people.sort(Comparator.comparing(PeopleDirectory::name)
                .thenComparing(person -> text(person, "username")));
        return result;
    }

    public static List<Map<String, Object>> search(Department department, String keyword) {
        String query = keyword.trim().toLowerCase(Locale.ROOT);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> person : department.people) {
            String searchable = name(person) + " " + text(person, "username") + " " + text(person, "team_name");
            if (searchable.toLowerCase(Locale.ROOT).contains(query)) result.add(person);
        }
        return result;
    }
}

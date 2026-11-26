package com.pandora.backend.user;

import java.util.List;
import java.util.Map;
import java.util.Locale;

/**
 * 组织规则工具类——定义角色与组织归属的关系。
 * 包含业务角色判断、部门/团队归属规则、数据规范化处理。
 * 被用户管理、任务管理等模块调用。
 */
public final class OrganizationRules {
    private OrganizationRules() {}

    /**
     * 判断是否为业务角色（参与组织架构的角色）。
     * 业务角色：FOUNDER、DEPT_HEAD、TEAM_LEADER、EMPLOYEE
     * 非业务角色：ADMIN（系统管理员，不参与公司组织架构）
     *
     * @param role 角色字符串
     * @return true 表示业务角色
     */
    public static boolean isBusinessRole(String role) { return List.of("FOUNDER", "DEPT_HEAD", "TEAM_LEADER", "EMPLOYEE").contains(role); }

    /**
     * 判断角色是否需要归属部门。
     * 创始人、管理员不需要归属部门。
     *
     * @param role 角色字符串
     * @return true 表示需要归属部门
     */
    public static boolean hasDepartment(String role) { return !"FOUNDER".equals(role) && !"ADMIN".equals(role); }

    /**
     * 判断角色是否需要归属团队。
     * 需要部门归属且不是部门负责人。
     *
     * @param role 角色字符串
     * @return true 表示需要归属团队
     */
    public static boolean hasTeam(String role) { return hasDepartment(role) && !"DEPT_HEAD".equals(role); }

    /**
     * 规范化用户数据——根据角色清除不需要的部门/团队字段。
     * 防止前端显示无意义的组织信息。
     *
     * @param person 用户数据 Map
     * @return 规范化后的用户数据
     */
    public static Map<String, Object> normalize(Map<String, Object> person) {
        String role = person.entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase("role"))
                .map(e -> String.valueOf(e.getValue())).findFirst().orElse("");
        for (String key : List.copyOf(person.keySet())) {
            String field = key.replace("_", "").toLowerCase(Locale.ROOT);
            // 非业务角色清除部门字段
            if ((!hasDepartment(role) && List.of("departmentid", "departmentname", "deptname").contains(field)))
                    person.put(key, null);
            // 非团队角色清除团队字段
            if ((!hasTeam(role) && List.of("teamid", "teamname").contains(field)))
                    person.put(key, null);
        }
        return person;
    }

    /**
     * 批量规范化用户数据。
     *
     * @param people 用户数据列表
     * @return 规范化后的用户数据列表
     */
    public static List<Map<String, Object>> normalize(List<Map<String, Object>> people) {
        people.forEach(OrganizationRules::normalize);
        return people;
    }
}
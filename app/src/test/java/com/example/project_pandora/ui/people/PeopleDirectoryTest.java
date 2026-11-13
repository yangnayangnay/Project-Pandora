package com.example.project_pandora.ui.people;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class PeopleDirectoryTest {
    private Map<String, Object> person(long id, Long department, String departmentName, String name, String username, String team) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", id); result.put("department_id", department); result.put("department_name", departmentName);
        result.put("real_name", name); result.put("username", username); result.put("team_name", team); return result;
    }
    @Test public void groupsByDepartmentIdWithoutInventingUnassignedDepartment() {
        List<PeopleDirectory.Department> groups = PeopleDirectory.departments(Arrays.asList(
                person(1, 10L, "研发", "甲", "a", "一组"), person(2, 10L, "研发", "乙", "b", "二组"),
                person(3, 20L, "研发", "丙", "c", ""), person(4, null, null, "丁", "d", "")));
        assertEquals(2, groups.size()); assertEquals(2, groups.get(0).people.size());
        assertTrue(groups.stream().allMatch(group -> !group.name.equals("未分配部门")));
    }
    @Test public void searchesNamesAccountsAndTeamsWithinSelectedDepartment() {
        var groups = PeopleDirectory.departments(Arrays.asList(
                person(1, 10L, "研发", "张三", "Alice", "客户端组"), person(2, 10L, "研发", "李四", "Bob", "服务端组"),
                person(3, 20L, "产品", "王五", "AliceOther", "客户端组")));
        var department = groups.stream().filter(group -> group.id == 10).findFirst().get();
        assertEquals(1, PeopleDirectory.search(department, "张").size());
        assertEquals(1, PeopleDirectory.search(department, " ALICE ").size());
        assertEquals(1, PeopleDirectory.search(department, "服务端").size());
        assertTrue(PeopleDirectory.search(department, "不存在").isEmpty());
    }
    @Test public void handlesUppercaseKeysStringIdsAndMissingNames() {
        Map<String, Object> row = new HashMap<>(); row.put("ID", "1"); row.put("DEPARTMENT_ID", "10");
        row.put("DEPARTMENT_NAME", "研发"); row.put("REAL_NAME", " "); row.put("USERNAME", "account");
        assertEquals(10L, PeopleDirectory.departmentId(row)); assertEquals("account", PeopleDirectory.name(row));
        assertEquals(1, PeopleDirectory.departments(Collections.singletonList(row)).size());
    }
    @Test public void emptyScopeStaysEmpty() { assertTrue(PeopleDirectory.departments(Collections.emptyList()).isEmpty()); }

    @Test public void foundersStayAtCompanyLevelAndDepartmentHeadsHaveNoTeam() {
        var founder = person(1, 10L, "研发", "创始人", "founder", "旧团队"); founder.put("role", "FOUNDER");
        assertTrue(PeopleDirectory.isFounder(founder)); assertEquals(-1L, PeopleDirectory.departmentId(founder));
        assertEquals("", PeopleDirectory.text(founder, "department_name")); assertEquals("", PeopleDirectory.text(founder, "team_name"));
        assertTrue(PeopleDirectory.departments(List.of(founder)).isEmpty());
        var head = person(2, 10L, "研发", "负责人", "head", "旧团队"); head.put("role", "DEPT_HEAD");
        assertFalse(PeopleDirectory.hasTeam(head)); assertEquals("", PeopleDirectory.text(head, "team_name"));
        assertEquals(1, PeopleDirectory.departments(List.of(founder, head)).get(0).people.size());
        assertTrue(PeopleDirectory.matches(founder, "founder"));
    }

    @Test public void relationCamelCaseAndLogSnakeCaseUseTheSameGroupsAndSearch() {
        Map<String, Object> relation = new HashMap<>();
        relation.put("id", "2"); relation.put("departmentId", "10"); relation.put("departmentName", "研发");
        relation.put("realName", "李四"); relation.put("username", "lisi"); relation.put("teamName", "客户端");
        var groups = PeopleDirectory.departments(Arrays.asList(person(1, 10L, "研发", "张三", "zs", "服务端"), relation));
        assertEquals(1, groups.size()); assertEquals(2, groups.get(0).people.size());
        assertEquals(1, PeopleDirectory.search(groups.get(0), "客户端").size());
        assertEquals("李四", PeopleDirectory.name(relation));
    }
    @Test public void legacyNamesStaySeparateFromUnassignedAndSameNameIdsStayDistinct() {
        Map<String, Object> legacy = new HashMap<>(); legacy.put("departmentName", "研发"); legacy.put("realName", "旧接口");
        var groups = PeopleDirectory.departments(Arrays.asList(legacy, person(1, null, null, "未分配", "a", ""),
                person(2, 10L, "研发", "甲", "b", ""), person(3, 20L, "研发", "乙", "c", "")));
        assertEquals(3, groups.size());
        assertTrue(groups.stream().noneMatch(group -> group.key.equals("id:-1")));
        assertTrue(groups.stream().anyMatch(group -> group.key.equals("name:研发")));
    }
    @Test public void administratorsAreExcludedEvenWhenTheyHaveDepartmentsOrUppercaseKeys() {
        var employee = person(1, 10L, "研发", "员工", "staff", "一组");
        employee.put("role", "EMPLOYEE");
        var admin = person(2, 10L, "研发", "管理员", "admin", "一组");
        admin.put("ROLE", " admin ");
        var otherAdmin = person(3, null, null, "后台管理员", "admin2", "");
        otherAdmin.put("role", "ADMIN");
        var groups = PeopleDirectory.departments(Arrays.asList(admin, employee, otherAdmin));
        assertEquals(1, groups.size());
        assertEquals(Collections.singletonList(employee), groups.get(0).people);
        assertTrue(PeopleDirectory.search(groups.get(0), "admin").isEmpty());
        assertTrue(PeopleDirectory.departments(Arrays.asList(admin, otherAdmin)).isEmpty());
    }
    @Test public void missingDepartmentNameDoesNotCreateAPlaceholderGroupEvenWithAnId() {
        assertTrue(PeopleDirectory.departments(Arrays.asList(
                person(1, 10L, " ", "员工", "a", ""), person(2, null, null, "员工2", "b", ""))).isEmpty());
    }
}

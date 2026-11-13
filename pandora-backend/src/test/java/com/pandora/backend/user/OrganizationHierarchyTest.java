package com.pandora.backend.user;

import com.pandora.backend.common.BusinessException;
import org.junit.jupiter.api.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class OrganizationHierarchyTest {
    JdbcTemplate db;
    PersonnelController personnel;
    final UsernamePasswordAuthenticationToken admin = new UsernamePasswordAuthenticationToken(1L, null, List.of());

    @BeforeEach void setup() {
        var source = new DriverManagerDataSource("jdbc:h2:mem:organization-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("db/h2_schema.sql")).execute(source);
        db = new JdbcTemplate(source);
        db.update("DELETE FROM user_hierarchy"); db.update("DELETE FROM users");
        db.update("INSERT INTO departments(id,name) VALUES(10,'研发'),(20,'产品')");
        db.update("INSERT INTO teams(id,name,department_id) VALUES(11,'客户端',10),(12,'服务端',10),(21,'设计',20)");
        user(1, "ADMIN", 10L, 11L); user(2, "FOUNDER", 10L, 11L);
        user(3, "DEPT_HEAD", 10L, 11L); user(4, "TEAM_LEADER", 10L, 11L);
        user(5, "EMPLOYEE", 10L, 11L); user(6, "DEPT_HEAD", 20L, 21L);
        user(7, "TEAM_LEADER", 20L, 21L); user(8, "EMPLOYEE", 20L, 21L);
        db.update("INSERT INTO user_hierarchy(superior_id,subordinate_id) VALUES(2,3),(3,4),(4,5),(2,6),(6,7),(7,8)");
        personnel = new PersonnelController(db);
    }
    void user(long id, String role, Long department, Long team) {
        db.update("INSERT INTO users(id,username,password_hash,role,approval_status,department_id,team_id) VALUES(?,?,'untouched-hash',?,'APPROVED',?,?)",
                id, "00000" + id, role, department, team);
    }
    List<Long> parents(long id) { return db.queryForList("SELECT superior_id FROM user_hierarchy WHERE subordinate_id=? ORDER BY superior_id", Long.class, id); }

    @Test void founderAndHeadSaveWithoutInapplicableUnitsAndCannotUseManualSuperior() {
        personnel.employment(admin, 3L, Map.of("role", "FOUNDER", "departmentId", "invalid", "teamId", "invalid", "superiorId", 1));
        assertNull(db.queryForObject("SELECT department_id FROM users WHERE id=3", Long.class));
        assertNull(db.queryForObject("SELECT team_id FROM users WHERE id=3", Long.class));
        assertEquals(List.of(), parents(3));
        personnel.employment(admin, 3L, Map.of("role", "DEPT_HEAD", "departmentId", 10, "superiorId", 6));
        assertEquals(List.of(2L), parents(3)); assertEquals(List.of(3L), parents(4));
        assertNull(db.queryForObject("SELECT team_id FROM users WHERE id=3", Long.class));
        assertEquals("000003", db.queryForObject("SELECT username FROM users WHERE id=3", String.class));
        assertEquals("untouched-hash", db.queryForObject("SELECT password_hash FROM users WHERE id=3", String.class));
    }
    @Test void departmentAndTeamDetermineSuperiorsInsteadOfCallerInput() {
        personnel.employment(admin, 5L, Map.of("role", "EMPLOYEE", "departmentId", 20, "teamId", 21, "superiorId", 4));
        assertEquals(List.of(7L), parents(5));
        assertEquals(List.of(3L), parents(4));
        assertThrows(BusinessException.class, () -> personnel.employment(admin, 5L,
                Map.of("role", "EMPLOYEE", "departmentId", 20, "teamId", 11)));
        assertEquals(List.of(7L), parents(5));
    }
    @Test void transferringLeaderReassignsFormerAndNewTeamMembersWithinScope() {
        user(9, "TEAM_LEADER", 10L, 11L);
        personnel.employment(admin, 4L, Map.of("role", "TEAM_LEADER", "departmentId", 20, "teamId", 21));
        assertEquals(List.of(6L), parents(4));
        assertEquals(List.of(9L), parents(5));
        assertEquals(List.of(4L, 7L), parents(8));
        assertEquals(List.of(2L), parents(3));
        var relationships = new OrganizationHierarchy(db);
        relationships.synchronize(relationships.snapshot(), 4);
        assertEquals(List.of(4L, 7L), parents(8));
    }
    @Test void demotingAHeadRebindsChildrenInsteadOfRetainingAnInvalidRoleChain() {
        user(9, "DEPT_HEAD", 10L, null);
        personnel.employment(admin, 3L, Map.of("role", "EMPLOYEE", "departmentId", 10, "teamId", 11));
        assertEquals(List.of(4L), parents(3)); assertEquals(List.of(9L), parents(4));
        assertFalse(parents(4).contains(3L));
    }
    @Test void administratorIsNeitherAnEditableBusinessPostNorAnAutomaticSuperior() {
        assertThrows(BusinessException.class, () -> personnel.employment(admin, 1L, Map.of("role", "FOUNDER")));
        assertThrows(BusinessException.class, () -> personnel.employment(admin, 5L, Map.of("role", "ADMIN")));
        assertEquals("ADMIN", db.queryForObject("SELECT role FROM users WHERE id=1", String.class));
        assertEquals("EMPLOYEE", db.queryForObject("SELECT role FROM users WHERE id=5", String.class));
        personnel.employment(admin, 3L, Map.of("role", "DEPT_HEAD", "departmentId", 10));
        assertEquals(List.of(2L), parents(3));
    }
    @Test void pendingAndDeletedLeadersCannotReceiveReportingAuthority() {
        db.update("UPDATE users SET approval_status='PENDING' WHERE id=4");
        user(9, "TEAM_LEADER", 10L, 11L); db.update("UPDATE users SET deleted=1 WHERE id=9");
        personnel.employment(admin, 5L, Map.of("role", "EMPLOYEE", "departmentId", 10, "teamId", 11));
        assertTrue(parents(5).isEmpty());
    }
    @Test void requiredUnitsDependOnTheChosenPostRatherThanStaleHiddenFormFields() {
        assertThrows(BusinessException.class, () -> personnel.employment(admin, 3L, Map.of("role", "DEPT_HEAD")));
        for (String role : List.of("TEAM_LEADER", "EMPLOYEE")) {
            assertThrows(BusinessException.class, () -> personnel.employment(admin, 5L, Map.of("role", role, "departmentId", 10)));
        }
        personnel.employment(admin, 3L, Map.of("role", "DEPT_HEAD", "departmentId", 10));
        assertNull(db.queryForObject("SELECT team_id FROM users WHERE id=3", Long.class));
        personnel.employment(admin, 3L, Map.of("role", "FOUNDER"));
        assertNull(db.queryForObject("SELECT department_id FROM users WHERE id=3", Long.class));
    }
    @Test void legacyFounderAndHeadAffiliationsDoNotBlockEmptyUnitManagement() {
        db.update("INSERT INTO departments(id,name) VALUES(30,'空部门')");
        db.update("INSERT INTO teams(id,name,department_id) VALUES(31,'空团队',30)");
        db.update("UPDATE users SET department_id=30,team_id=31 WHERE role IN ('ADMIN','FOUNDER')");
        db.update("UPDATE users SET team_id=31 WHERE id=3");
        personnel.deleteTeam(31); personnel.deleteDepartment(30);
        assertEquals(1, db.queryForObject("SELECT deleted FROM departments WHERE id=30", Integer.class));
        assertEquals("untouched-hash", db.queryForObject("SELECT password_hash FROM users WHERE id=2", String.class));
    }
    @Test void directoryKeepsOrganizationKeysButDoesNotExposeUnusedSettingsAsPersonnelData() {
        var employee = personnel.directory().getData().stream()
                .filter(person -> Objects.equals(person.get("id"), 5L)).findFirst().orElseThrow();
        assertEquals(10L, employee.get("department_id"));
        assertEquals(11L, employee.get("team_id"));
        for (String field : List.of("last_birthday_change", "theme_mode", "manual_theme", "show_completed_tasks", "show_yesterday_log", "password_hash"))
            assertFalse(employee.containsKey(field), field);
        assertTrue(employee.containsKey("birthday"));
        assertTrue(employee.containsKey("id_card"));
    }
}

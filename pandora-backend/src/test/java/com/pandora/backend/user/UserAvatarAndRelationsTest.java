package com.pandora.backend.user;

import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.ErrorCode;
import com.pandora.backend.common.FileStorageService;
import com.pandora.backend.common.WebMvcConfig;
import com.pandora.backend.hierarchy.HierarchyService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserAvatarAndRelationsTest {
    @Test void signatureAllowsFourLinesAndNormalizesLineEndings() {
        controller.updateProfile(Map.of("signature", "一\r\n二\r三\u2028四"));
        assertEquals("一\n二\n三\n四", db.queryForObject("SELECT signature FROM users WHERE id=3", String.class));
    }
    @Test void signatureRejectsFifthLineIncludingTrailingBlankLinesBeforeAnyProfileWrite() {
        controller.updateProfile(Map.of("signature", "原签名"));
        for (String invalid : List.of("一\n二\n三\n四\n五", "一\n\n\n\n", "一\r\n二\r\n三\r\n四\r\n五", "一\u2029二\u0085三\r四\n五")) {
            BusinessException error = assertThrows(BusinessException.class,
                    () -> controller.updateProfile(Map.of("signature", invalid, "avatar", "/must-not-be-written.jpg")));
            assertEquals(ErrorCode.BAD_REQUEST, error.getErrorCode());
            assertTrue(error.getMessage().contains("4 行"));
            assertEquals("原签名", db.queryForObject("SELECT signature FROM users WHERE id=3", String.class));
            assertNull(db.queryForObject("SELECT avatar FROM users WHERE id=3", String.class));
        }
    }
    @Test void signatureAllowsTwentyCharactersRejectsTwentyOneAndCanBeCleared() {
        String twenty = "签".repeat(20);
        controller.updateProfile(Map.of("signature", twenty));
        assertEquals(twenty, db.queryForObject("SELECT signature FROM users WHERE id=3", String.class));
        BusinessException error = assertThrows(BusinessException.class,
                () -> controller.updateProfile(Map.of("signature", twenty + "名")));
        assertEquals(ErrorCode.BAD_REQUEST, error.getErrorCode());
        assertEquals(twenty, db.queryForObject("SELECT signature FROM users WHERE id=3", String.class));
        controller.updateProfile(Map.of("signature", ""));
        assertEquals("", db.queryForObject("SELECT signature FROM users WHERE id=3", String.class));
    }
    @Test void shorterLimitDoesNotRewriteExistingLongSignaturesWithoutASave() {
        String legacy = "旧签名".repeat(10);
        db.update("UPDATE users SET signature=? WHERE id=3", legacy);
        assertThrows(BusinessException.class, () -> controller.updateProfile(Map.of("signature", legacy)));
        assertEquals(legacy, db.queryForObject("SELECT signature FROM users WHERE id=3", String.class));
    }
    @TempDir Path storage;
    private JdbcTemplate db;
    private UserController controller;
    private FileStorageService files;
    @BeforeEach void setup() {
        var source = new DriverManagerDataSource("jdbc:h2:mem:avatar-relations-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("db/h2_schema.sql")).execute(source);
        db = new JdbcTemplate(source);
        db.update("DELETE FROM users");
        db.update("INSERT INTO departments (id,name) VALUES (10,'研发'),(20,'产品')");
        db.update("INSERT INTO teams (id,name,department_id) VALUES (11,'客户端',10)");
        for (int id = 1; id <= 4; id++) db.update("INSERT INTO users (id,username,password_hash,real_name,role,approval_status,department_id,team_id) " +
                "VALUES (?,?,'hash',?,?,'APPROVED',?,?)", id, "account" + id, "姓名" + id,
                id == 1 ? "DEPT_HEAD" : id == 2 ? "TEAM_LEADER" : "EMPLOYEE", id == 4 ? 20 : 10, id == 2 ? 11 : null);
        db.update("INSERT INTO user_hierarchy (superior_id,subordinate_id) VALUES (1,2),(2,3)");
        var hierarchy = new HierarchyService();
        ReflectionTestUtils.setField(hierarchy, "jdbcTemplate", db);
        ReflectionTestUtils.setField(hierarchy, "maxDepth", 10);
        files = new FileStorageService(); ReflectionTestUtils.setField(files, "uploadDir", storage.toString());
        controller = new UserController();
        ReflectionTestUtils.setField(controller, "jdbcTemplate", db);
        ReflectionTestUtils.setField(controller, "hierarchyService", hierarchy);
        ReflectionTestUtils.setField(controller, "fileStorageService", files);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(3L, null, List.of()));
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    @Test void acceptsExactFiveMegabytesAndServesFromConfiguredDirectory() throws Exception {
        byte[] bytes = new byte[5 * 1024 * 1024]; bytes[0] = 12; bytes[bytes.length - 1] = 42;
        controller.uploadAvatar(new MockMultipartFile("file", "avatar.jpg", "image/jpeg", bytes), "camera");
        String url = db.queryForObject("SELECT avatar FROM users WHERE id=3", String.class);
        assertNotNull(url);
        assertArrayEquals(bytes, Files.readAllBytes(storage.resolve(url.substring("/uploads/".length()))));
        try (var context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test-storage", Map.of("pandora.upload-dir", storage.toString())));
            context.register(Mvc.class, WebMvcConfig.class); context.refresh();
            var roleFilter = new com.pandora.backend.auth.RoleAuthFilter();
            var keyFilter = new com.pandora.backend.auth.ApiKeyFilter();
            ReflectionTestUtils.setField(keyFilter, "mode", "enforce");
            ReflectionTestUtils.setField(keyFilter, "apiKey", "test-only-key");
            var mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(keyFilter, roleFilter).build();
            // Exercise both real authentication filters, unlike the previous MVC-only regression.
            mvc.perform(get("/api" + url).contextPath("/api"))
                    .andExpect(status().isOk()).andExpect(content().bytes(bytes));
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head("/api" + url).contextPath("/api"))
                    .andExpect(status().isOk());
            mvc.perform(get("/api/user/me").contextPath("/api"))
                    .andExpect(status().isUnauthorized());
            mvc.perform(get("/api/user/me").contextPath("/api").header("X-Api-Key", "test-only-key"))
                    .andExpect(status().isUnauthorized());
            mvc.perform(get("/api/uploads/private.txt").contextPath("/api"))
                    .andExpect(status().isUnauthorized());
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api" + url).contextPath("/api"))
                    .andExpect(status().isUnauthorized());
        }
    }
    @Test void rejectsOversizedImageWithoutChangingAvatar() {
        db.update("UPDATE users SET avatar='/uploads/old.jpg' WHERE id=3");
        var error = assertThrows(BusinessException.class, () -> controller.uploadAvatar(
                new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[5 * 1024 * 1024 + 1]), "album"));
        assertEquals(ErrorCode.FILE_TOO_LARGE, error.getErrorCode());
        assertEquals("/uploads/old.jpg", db.queryForObject("SELECT avatar FROM users WHERE id=3", String.class));
    }
    @Test void rejectsEmptyAndUnsupportedImages() {
        assertThrows(BusinessException.class, () -> controller.uploadAvatar(new MockMultipartFile("file", new byte[0]), "album"));
        assertEquals(ErrorCode.UNSUPPORTED_IMAGE_FORMAT, assertThrows(BusinessException.class, () -> controller.uploadAvatar(
                new MockMultipartFile("file", "a.txt", "text/plain", new byte[]{1}), "album")).getErrorCode());
    }
    @Test void repeatedUploadsDoNotOverwriteFilesOrReuseUrls() throws Exception {
        var file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1, 2, 3});
        String first = files.storeAvatar(file, 3L), second = files.storeAvatar(file, 3L);
        assertNotEquals(first, second); assertTrue(first.endsWith(".png"));
        assertTrue(Files.exists(storage.resolve(first.substring("/uploads/".length()))));
        assertTrue(Files.exists(storage.resolve(second.substring("/uploads/".length()))));
    }
    @SuppressWarnings("unchecked") @Test void relationsIncludeDirectoryMetadataWithoutAddingUnrelatedUsers() {
        Map<String, Object> payload = controller.getUserRelations().getBody().getData();
        var superiors = (List<Map<String, Object>>) payload.get("superiors");
        assertEquals(2, superiors.size());
        var leader = superiors.stream().filter(row -> Objects.equals(2L, row.get("id"))).findFirst().orElseThrow();
        assertEquals("account2", leader.get("username")); assertEquals(10L, leader.get("departmentId"));
        assertEquals("研发", leader.get("departmentName")); assertEquals(11L, leader.get("teamId"));
        assertEquals("客户端", leader.get("teamName")); assertEquals(1, leader.get("depth"));
        assertTrue(((List<?>) payload.get("subordinates")).isEmpty());
        assertTrue(superiors.stream().noneMatch(row -> Objects.equals(4L, row.get("id"))));
    }
    @SuppressWarnings("unchecked") @Test void deletedDepartmentsDoNotCreateUnassignedDirectoryEntries() {
        db.update("UPDATE departments SET deleted=1 WHERE id=10");
        var rows = (List<Map<String, Object>>) controller.getUserRelations().getBody().getData().get("superiors");
        assertTrue(rows.isEmpty());
    }
    @SuppressWarnings("unchecked") @Test void companyLevelFoundersRemainInRelationsAndLegacyHeadTeamsAreHidden() {
        db.update("INSERT INTO users(id,username,password_hash,role,approval_status) VALUES(6,'founder','unchanged','FOUNDER','APPROVED')");
        db.update("INSERT INTO user_hierarchy(superior_id,subordinate_id) VALUES(6,1)");
        db.update("UPDATE users SET team_id=11 WHERE id=1");
        var rows = (List<Map<String,Object>>) controller.getUserRelations().getBody().getData().get("superiors");
        assertEquals(3, rows.size());
        var founder = rows.stream().filter(row -> Objects.equals(6L, row.get("id"))).findFirst().orElseThrow();
        assertNull(founder.get("departmentId")); assertNull(founder.get("teamId"));
        var head = rows.stream().filter(row -> Objects.equals(1L, row.get("id"))).findFirst().orElseThrow();
        assertEquals(10L, head.get("departmentId")); assertNull(head.get("teamId"));
    }
    @SuppressWarnings("unchecked") @Test void relationsIncludeRealAvatarUrlsForBothDirectionsAndPreserveMissingAvatars() {
        db.update("UPDATE users SET avatar='/uploads/head.jpg' WHERE id=1");
        db.update("UPDATE users SET avatar='/uploads/employee.jpg' WHERE id=3");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(2L, null, List.of()));
        var result = controller.getUserRelations().getBody().getData();
        var superiors = (List<Map<String,Object>>) result.get("superiors");
        var subordinates = (List<Map<String,Object>>) result.get("subordinates");
        assertEquals("/uploads/head.jpg", superiors.get(0).get("avatar"));
        assertEquals("/uploads/employee.jpg", subordinates.get(0).get("avatar"));
        db.update("UPDATE users SET avatar=NULL WHERE id=1");
        superiors = (List<Map<String,Object>>) controller.getUserRelations().getBody().getData().get("superiors");
        assertTrue(superiors.get(0).containsKey("avatar")); assertNull(superiors.get(0).get("avatar"));
    }
    @Test void employmentAndTransfersKeepRoleStructureAndAccounts() {
        db.update("UPDATE users SET role='ADMIN' WHERE id=4");
        var auth = new UsernamePasswordAuthenticationToken(4L, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
        var hierarchy = (HierarchyService) ReflectionTestUtils.getField(controller, "hierarchyService");
        var personnel = new PersonnelController(db);
        personnel.employment(auth, 1L, Map.of("role", "FOUNDER", "departmentId", 999, "teamId", 999));
        assertNull(db.queryForObject("SELECT department_id FROM users WHERE id=1", Long.class));
        controller.updateUserOrg(1L, Map.of("departmentId", 10, "teamId", 11));
        assertNull(db.queryForObject("SELECT department_id FROM users WHERE id=1", Long.class));
        personnel.employment(auth, 1L, Map.of("role", "DEPT_HEAD", "departmentId", 10, "teamId", 999));
        assertEquals(10L, db.queryForObject("SELECT department_id FROM users WHERE id=1", Long.class));
        assertNull(db.queryForObject("SELECT team_id FROM users WHERE id=1", Long.class));
        assertEquals("hash", db.queryForObject("SELECT password_hash FROM users WHERE id=1", String.class));
        assertEquals("account1", db.queryForObject("SELECT username FROM users WHERE id=1", String.class));
        assertEquals(2, db.queryForObject("SELECT COUNT(*) FROM user_hierarchy", Integer.class));
    }
    @Test void legacyOrganizationTransferEndpointAlsoRebindsTheRealPermissionHierarchy() {
        db.update("INSERT INTO users(id,username,password_hash,role,approval_status) VALUES(6,'admin','unchanged','ADMIN','APPROVED')");
        db.update("INSERT INTO teams(id,name,department_id) VALUES(21,'设计',20)");
        db.update("UPDATE users SET role='TEAM_LEADER',team_id=21 WHERE id=4");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(6L, null, List.of()));
        controller.updateUserOrg(3L, Map.of("departmentId", 20, "teamId", 21));
        assertEquals(List.of(4L), db.queryForList("SELECT superior_id FROM user_hierarchy WHERE subordinate_id=3", Long.class));
        var hierarchy = (HierarchyService) ReflectionTestUtils.getField(controller, "hierarchyService");
        assertFalse(hierarchy.isSubordinate(2L, 3L)); assertTrue(hierarchy.isSubordinate(4L, 3L));
        assertEquals("hash", db.queryForObject("SELECT password_hash FROM users WHERE id=3", String.class));
    }
    @Test void roleCleanupMigrationIsIdempotentAndOnlyClearsObsoleteAffiliations() {
        db.update("UPDATE users SET role='FOUNDER',department_id=10,team_id=11 WHERE id=1");
        db.update("UPDATE users SET role='DEPT_HEAD',team_id=11 WHERE id=2");
        db.update("INSERT INTO employee_records(id,real_name,id_card,role,department_id,team_id) VALUES(100,'档案创始人','test-identity','FOUNDER',10,11)");
        var source = db.getDataSource();
        var migration = new ResourceDatabasePopulator(new ClassPathResource("db/migrations/20261113_role_organization.sql"));
        migration.execute(source); migration.execute(source);
        assertNull(db.queryForObject("SELECT department_id FROM users WHERE id=1", Long.class));
        assertNull(db.queryForObject("SELECT team_id FROM users WHERE id=2", Long.class));
        assertEquals(10L, db.queryForObject("SELECT department_id FROM users WHERE id=2", Long.class));
        assertEquals(10L, db.queryForObject("SELECT department_id FROM users WHERE id=3", Long.class));
        assertEquals("hash", db.queryForObject("SELECT password_hash FROM users WHERE id=1", String.class));
        assertNull(db.queryForObject("SELECT department_id FROM employee_records WHERE id=100", Long.class));
        assertEquals("test-identity", db.queryForObject("SELECT id_card FROM employee_records WHERE id=100", String.class));
        assertEquals(2, db.queryForObject("SELECT COUNT(*) FROM user_hierarchy", Integer.class));
    }
    @Test void pendingApprovalUsesTheApprovedRolesAffiliationRules() {
        db.update("UPDATE users SET approval_status='PENDING',team_id=11 WHERE id=1");
        var approvals = new com.pandora.backend.permission.ApprovalController(); ReflectionTestUtils.setField(approvals, "jdbcTemplate", db);
        approvals.approve(1L, Map.of("role", "FOUNDER"));
        assertNull(db.queryForObject("SELECT department_id FROM users WHERE id=1", Long.class));
        assertNull(db.queryForObject("SELECT team_id FROM users WHERE id=1", Long.class));
        assertEquals("hash", db.queryForObject("SELECT password_hash FROM users WHERE id=1", String.class));
    }
    @Test void employeeRecordsFollowTheSameRoleRulesWhenCreatedAndEdited() {
        var records = new com.pandora.backend.permission.EmployeeRecordController(); ReflectionTestUtils.setField(records, "jdbcTemplate", db);
        var created = records.create(Map.of("realName", "创始人档案", "idCard", "test-founder", "role", "FOUNDER", "departmentId", 10, "teamId", 11)).getBody().getData();
        long id = ((Number) created.get("id")).longValue();
        assertNull(db.queryForObject("SELECT department_id FROM employee_records WHERE id=?", Long.class, id));
        records.update(id, Map.of("role", "DEPT_HEAD", "departmentId", 10, "teamId", 11));
        assertEquals(10L, db.queryForObject("SELECT department_id FROM employee_records WHERE id=?", Long.class, id));
        assertNull(db.queryForObject("SELECT team_id FROM employee_records WHERE id=?", Long.class, id));
        records.update(id, Map.of("role", "FOUNDER"));
        assertNull(db.queryForObject("SELECT department_id FROM employee_records WHERE id=?", Long.class, id));
    }
    @SuppressWarnings("unchecked") @Test void adminSuperiorsAndSubordinatesAreNotReturnedAsMobilePeople() {
        db.update("INSERT INTO users (id,username,password_hash,role,approval_status) VALUES (5,'hidden-admin','hash','ADMIN','APPROVED')");
        db.update("INSERT INTO user_hierarchy (superior_id,subordinate_id) VALUES (5,1),(3,5)");
        var data = controller.getUserRelations().getBody().getData();
        assertTrue(((List<Map<String, Object>>) data.get("superiors")).stream().noneMatch(row -> "ADMIN".equals(row.get("role"))));
        assertTrue(((List<Map<String, Object>>) data.get("subordinates")).stream().noneMatch(row -> "ADMIN".equals(row.get("role"))));
        db.update("UPDATE users SET department_id=10 WHERE id=5");
        data = controller.getUserRelations().getBody().getData();
        assertTrue(((List<Map<String, Object>>) data.get("superiors")).stream().noneMatch(row -> "ADMIN".equals(row.get("role"))));
        assertTrue(((List<Map<String, Object>>) data.get("subordinates")).stream().noneMatch(row -> "ADMIN".equals(row.get("role"))));
    }
    @Test void userListHidesAdminsForAppUsersButPreservesAdminWebManagement() {
        db.update("INSERT INTO users (id,username,password_hash,role,approval_status) VALUES (5,'hidden-admin','hash','ADMIN','APPROVED')");
        assertTrue(controller.listUsers(null, null, null).getBody().getData().stream().noneMatch(row -> "ADMIN".equals(row.get("role"))));
        assertTrue(controller.listUsers("ADMIN", null, null).getBody().getData().isEmpty());
        assertThrows(BusinessException.class, () -> controller.getUserPanel(5L));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(5L, null, List.of()));
        assertEquals(1, controller.listUsers("ADMIN", null, null).getBody().getData().size());
    }
    @SuppressWarnings("unchecked") @Test void hierarchyReadEndpointsHideAdminNodesButRetainTheirEmployeeChildren() {
        db.update("INSERT INTO users (id,username,password_hash,role,approval_status) VALUES (5,'hidden-admin','hash','ADMIN','APPROVED')");
        db.update("INSERT INTO user_hierarchy (superior_id,subordinate_id) VALUES (5,1)");
        var hierarchyController = new com.pandora.backend.hierarchy.HierarchyController();
        ReflectionTestUtils.setField(hierarchyController, "jdbcTemplate", db);
        ReflectionTestUtils.setField(hierarchyController, "hierarchyService", ReflectionTestUtils.getField(controller, "hierarchyService"));
        assertTrue(hierarchyController.getSuperiors(3L).getBody().getData().stream().noneMatch(row -> "ADMIN".equals(row.get("role"))));
        assertTrue(hierarchyController.getRelations().getBody().getData().stream().noneMatch(row -> "ADMIN".equals(row.get("sup_role"))));
        var roots = (List<Map<String, Object>>) hierarchyController.getTree().getBody().getData().get("roots");
        assertTrue(roots.stream().noneMatch(row -> "ADMIN".equals(row.get("role"))));
        assertTrue(roots.stream().anyMatch(row -> Objects.equals(1L, row.get("id"))));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(5L, null, List.of()));
        assertTrue(hierarchyController.getSuperiors(3L).getBody().getData().stream().anyMatch(row -> "ADMIN".equals(row.get("role"))));
    }
    @Test void transfersCannotClearOrSelectInvalidDepartment() {
        db.update("UPDATE users SET role='ADMIN' WHERE id=4");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(4L, null, List.of()));
        assertThrows(BusinessException.class, () -> controller.updateUserOrg(3L, Map.of()));
        assertThrows(BusinessException.class, () -> controller.updateUserOrg(3L, Map.of("departmentId", 999)));
        assertEquals(10L, db.queryForObject("SELECT department_id FROM users WHERE id=3", Long.class));
        db.update("INSERT INTO teams(id,name,department_id) VALUES(21,'产品团队',20)");
        controller.updateUserOrg(3L, Map.of("departmentId", 20, "teamId", 21));
        assertEquals(20L, db.queryForObject("SELECT department_id FROM users WHERE id=3", Long.class));
    }
    @Test void employmentChangesRequireDepartmentAndNonAdminDirectoryDoesNotIncludeAdmins() {
        db.update("UPDATE users SET role='ADMIN' WHERE id=4");
        var hierarchy = (HierarchyService) ReflectionTestUtils.getField(controller, "hierarchyService");
        var personnel = new PersonnelController(db);
        var auth = new UsernamePasswordAuthenticationToken(4L, null, List.of());
        assertThrows(BusinessException.class, () -> personnel.employment(auth, 3L, Map.of("role", "EMPLOYEE")));
        assertEquals(10L, db.queryForObject("SELECT department_id FROM users WHERE id=3", Long.class));
        assertTrue(personnel.directory().getData().stream().noneMatch(row -> "ADMIN".equals(row.get("role"))));
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertTrue(personnel.directory().getData().stream().anyMatch(row -> "ADMIN".equals(row.get("role"))));
    }
    @Test void taskListAndDetailKeepHistoricalTasksWithoutAdminNames() {
        db.update("UPDATE users SET role='ADMIN',real_name='后台管理员' WHERE id=4");
        db.update("INSERT INTO tasks(id,name,content,assignee_id,dispatcher_id,status) VALUES (100,'历史任务','正文',4,4,'IN_PROGRESS')");
        var tasks = new com.pandora.backend.task.TaskController();
        ReflectionTestUtils.setField(tasks, "jdbcTemplate", db);
        ReflectionTestUtils.setField(tasks, "taskTags", new com.pandora.backend.task.TaskTagService(db));
        var detail = tasks.getTask(100L).getBody().getData();
        assertEquals("历史任务", detail.get("name"));
        for (String field : List.of("assignee_name", "assignee_real_name", "dispatcher_name", "dispatcher_real_name")) {
            assertEquals("系统", detail.get(field));
            assertEquals("系统", tasks.listTasks(null, null, null, true).getBody().getData().get(0).get(field));
        }
    }
    @Test void multipartConfigurationAllowsFiveMegabytesPlusRequestOverhead() {
        var yaml = new YamlPropertiesFactoryBean(); yaml.setResources(new ClassPathResource("application.yml"));
        var properties = yaml.getObject();
        assertEquals("5MB", properties.getProperty("spring.servlet.multipart.max-file-size"));
        assertEquals("6MB", properties.getProperty("spring.servlet.multipart.max-request-size"));
    }
    @Configuration @EnableWebMvc static class Mvc {}
}

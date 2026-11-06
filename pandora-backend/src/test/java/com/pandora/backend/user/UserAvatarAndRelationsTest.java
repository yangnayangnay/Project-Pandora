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
        controller.updateUserOrg(3L, Map.of("departmentId", 20));
        assertEquals(20L, db.queryForObject("SELECT department_id FROM users WHERE id=3", Long.class));
    }
    @Test void employmentChangesRequireDepartmentAndNonAdminDirectoryDoesNotIncludeAdmins() {
        db.update("UPDATE users SET role='ADMIN' WHERE id=4");
        var hierarchy = (HierarchyService) ReflectionTestUtils.getField(controller, "hierarchyService");
        var personnel = new PersonnelController(db, hierarchy);
        var auth = new UsernamePasswordAuthenticationToken(4L, null, List.of());
        assertThrows(BusinessException.class, () -> personnel.employment(auth, 3L, Map.of("role", "EMPLOYEE")));
        assertEquals(10L, db.queryForObject("SELECT department_id FROM users WHERE id=3", Long.class));
        assertTrue(personnel.directory().getData().stream().noneMatch(row -> "ADMIN".equals(row.get("role"))));
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertTrue(personnel.directory().getData().stream().anyMatch(row -> "ADMIN".equals(row.get("role"))));
    }
    @Test void taskListAndDetailKeepHistoricalTasksWithoutAdminNames() {
        db.update("UPDATE users SET role='ADMIN',real_name='后台管理员' WHERE id=4");
        db.update("INSERT INTO tasks(id,name,content,assignee_id,dispatcher_id,status) VALUES (100,'历史任务','正文',4,4,'PENDING')");
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

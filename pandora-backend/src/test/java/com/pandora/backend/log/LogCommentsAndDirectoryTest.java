package com.pandora.backend.log;

import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.ErrorCode;
import com.pandora.backend.hierarchy.HierarchyService;
import org.junit.jupiter.api.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class LogCommentsAndDirectoryTest {
    private JdbcTemplate db;
    private HierarchyService hierarchy;
    private LogCommentService comments;
    private LogDirectoryService directory;

    @BeforeEach void setup() {
        var source = new DriverManagerDataSource("jdbc:h2:mem:log-features-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("db/h2_schema.sql")).execute(source);
        db = new JdbcTemplate(source);
        // The idempotent schema seeds an administrator; this is a fresh, private in-memory DB.
        db.update("DELETE FROM users");
        db.update("INSERT INTO departments (id,name) VALUES (10,'研发'),(20,'产品')");
        db.update("INSERT INTO teams (id,name,department_id) VALUES (11,'客户端',10)");
        String[] roles = {"DEPT_HEAD", "TEAM_LEADER", "EMPLOYEE", "EMPLOYEE", "ADMIN", "FOUNDER", "DEPT_HEAD", "TEAM_LEADER"};
        for (int index = 0; index < roles.length; index++) {
            long id = index + 1;
            db.update("INSERT INTO users (id,username,password_hash,real_name,role,approval_status,department_id,team_id) " +
                    "VALUES (?,?,'hash',?,?,'APPROVED',?,?)", id, "user" + id, "姓名" + id, roles[index], id == 4 || id == 7 ? 20 : 10, id == 3 ? 11 : null);
        }
        db.update("INSERT INTO user_hierarchy (superior_id,subordinate_id) VALUES (1,2),(2,3),(7,4)");
        db.update("INSERT INTO work_logs (id,user_id,work_item,log_date,deleted) VALUES " +
                "(100,3,'下级日志','2026-11-01',0),(101,4,'其他部门','2026-11-01',0)," +
                "(102,3,'已删除','2026-10-31',1),(103,1,'上级日志','2026-11-01',0)");
        hierarchy = new HierarchyService();
        ReflectionTestUtils.setField(hierarchy, "jdbcTemplate", db); ReflectionTestUtils.setField(hierarchy, "maxDepth", 10);
        comments = new LogCommentService(db, hierarchy); directory = new LogDirectoryService(db, hierarchy);
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }

    @Test void directAndIndirectSuperiorsCanCommentAndAuthorCanRead() {
        var first = comments.add(100, 2, "  请补充结果  "); var second = comments.add(100, 1, "收到");
        assertNotEquals(first.get("id"), second.get("id"));
        var payload = comments.list(100, 3); assertEquals(false, payload.get("canComment"));
        var rows = rows(payload); assertEquals(2, rows.size());
        assertEquals("请补充结果", rows.get(0).get("content")); assertEquals("姓名2", rows.get(0).get("author_name"));
        assertNotNull(rows.get(0).get("created_at")); assertEquals(true, comments.list(100, 2).get("canComment"));
    }
    @Test void rejectsSelfUnrelatedEmployeeAndPeerComments() {
        forbidden(() -> comments.add(100, 3, "自评"));
        forbidden(() -> comments.add(100, 4, "员工越权"));
        forbidden(() -> comments.add(100, 7, "其他部门上级"));
        forbidden(() -> comments.add(100, 8, "同部门无上下级关系"));
        forbidden(() -> comments.list(103, 3));
        assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM log_comments", Integer.class));
    }
    @Test void revokingRelationshipImmediatelyRemovesReadAndWriteAccess() {
        comments.add(100, 2, "之前的评论");
        db.update("DELETE FROM user_hierarchy WHERE superior_id = 2 AND subordinate_id = 3");
        forbidden(() -> comments.add(100, 2, "已调动")); forbidden(() -> comments.list(100, 2));
        assertEquals(1, rows(comments.list(100, 3)).size());
    }
    @Test void demotedOrDisabledUserCannotKeepCommenting() {
        db.update("UPDATE users SET role='EMPLOYEE' WHERE id=2");
        forbidden(() -> comments.add(100, 2, "已降级"));
        db.update("UPDATE users SET deleted=1 WHERE id=1"); forbidden(() -> comments.list(100, 1));
    }
    @Test void administratorsAndFoundersUseExistingGlobalScope() {
        comments.add(100, 5, "管理员评论"); comments.add(101, 6, "创始人评论");
        assertEquals(1, rows(comments.list(101, 4)).size());
        assertEquals(true, comments.list(100, 6).get("canComment"));
    }
    @Test void mobileCommentListsHideAdministratorIdentityAndComments() {
        comments.add(100, 5, "后台评论");
        comments.add(100, 2, "直接上级评论");
        var visible = rows(comments.list(100, 3));
        assertEquals(1, visible.size());
        assertEquals("直接上级评论", visible.get(0).get("content"));
        assertEquals(2, rows(comments.list(100, 5)).size());
    }
    @Test void mobileFounderCannotBrowseAdministratorLogsByListOrDirectId() {
        db.update("INSERT INTO work_logs(id,user_id,work_item,log_date) VALUES (105,5,'后台日志','2026-11-01')");
        var app = controller(6);
        assertTrue(app.listAllLogs(5L, null, null, null).getBody().getData().isEmpty());
        assertEquals(ErrorCode.NOT_FOUND, assertThrows(BusinessException.class, () -> app.getLogDetail(105L)).getErrorCode());
        assertEquals(1, controller(5).listAllLogs(5L, null, null, null).getBody().getData().size());
    }
    @Test void validatesCommentContentAndLength() {
        for (Object value : new Object[]{null, 1, "", " \n\t", "　"})
            assertThrows(BusinessException.class, () -> comments.add(100, 2, value));
        assertThrows(BusinessException.class, () -> comments.add(100, 2, "a".repeat(2001)));
        assertEquals("a".repeat(2000), comments.add(100, 2, "a".repeat(2000)).get("content"));
    }
    @Test void globalVisibilityDoesNotPermitCommentingOnPeers() {
        db.update("INSERT INTO work_logs (id,user_id,work_item,log_date) VALUES (104,8,'同级日志','2026-11-01')");
        db.update("UPDATE users SET role='ADMIN' WHERE id=8");
        assertEquals(false, comments.list(104, 5).get("canComment")); forbidden(() -> comments.add(104, 5, "同级"));
        db.update("UPDATE users SET role='FOUNDER' WHERE id=8");
        assertEquals(false, comments.list(104, 6).get("canComment")); forbidden(() -> comments.add(104, 6, "同级"));
    }
    @Test void missingOrDeletedLogsRejectCommentsAndReads() {
        for (long id : new long[]{102, 999}) {
            assertEquals(ErrorCode.NOT_FOUND, assertThrows(BusinessException.class, () -> comments.add(id, 2, "评论")).getErrorCode());
            assertEquals(ErrorCode.NOT_FOUND, assertThrows(BusinessException.class, () -> comments.list(id, 3)).getErrorCode());
        }
    }
    @Test void oldReadOnlyLogsStillAllowSuperiorComments() {
        db.update("UPDATE work_logs SET log_date='2025-01-01' WHERE id=100");
        comments.add(100, 2, "历史日志反馈"); assertEquals(1, rows(comments.list(100, 3)).size());
    }
    @Test void directoryContainsOnlyAuthorizedUsersAndOrganizationLabels() {
        var rows = directory.list(1); assertEquals(Set.of(2L, 3L), ids(rows));
        Map<String, Object> person = rows.stream().filter(row -> row.get("id").equals(3L)).findFirst().orElseThrow();
        assertEquals("研发", person.get("department_name")); assertEquals("客户端", person.get("team_name"));
        assertFalse(person.containsKey("password_hash"));
        db.update("UPDATE users SET approval_status='PENDING' WHERE id=3"); assertEquals(Set.of(2L), ids(directory.list(1)));
    }
    @Test void unassignedAndDeletedDepartmentsDoNotCreateDirectoryEntries() {
        db.update("UPDATE departments SET deleted=1 WHERE id=10");
        assertTrue(directory.list(1).isEmpty());
    }
    @Test void logPeopleDirectoryNeverIncludesAdministratorsEvenThroughLegacyHierarchyLinks() {
        db.update("INSERT INTO user_hierarchy (superior_id,subordinate_id) VALUES (1,5)");
        assertEquals(Set.of(2L, 3L), ids(directory.list(1)));
        assertFalse(ids(directory.list(5)).contains(5L));
    }
    @Test void departmentFiltersNeverExpandHierarchyScope() {
        var controller = controller(1);
        assertEquals(1, controller.getSubordinateLogs(null, 10L, null, null, null).getBody().getData().size());
        assertTrue(controller.getSubordinateLogs(null, 20L, null, null, null).getBody().getData().isEmpty());
        assertEquals(403, controller.getSubordinateLogs(4L, 20L, null, null, null).getStatusCode().value());
        assertEquals(1, controller.getSubordinateLogs(3L, 10L, "2026-11-01", "2026-11-01", "下级").getBody().getData().size());
    }
    @Test void unassignedDepartmentFilterKeepsAuthorizedSubset() {
        db.update("UPDATE users SET department_id=NULL WHERE id IN (3,4)");
        var rows = controller(1).getSubordinateLogs(null, -1L, null, null, null).getBody().getData();
        assertEquals(1, rows.size()); assertEquals(3L, rows.get(0).get("user_id"));
    }
    @Test void httpEndpointsExposeCommentsAndEnforceServerPermissions() throws Exception {
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(new LogCommentController(comments))
                .setControllerAdvice(new com.pandora.backend.common.GlobalExceptionHandler()).build();
        controller(2);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/log/100/comments")
                .contentType("application/json").content("{\"content\":\"HTTP comment\"}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.id").isNumber());
        controller(3);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/log/100/comments"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.canComment").value(false))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.comments[0].content").value("HTTP comment"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/log/100/comments")
                .contentType("application/json").content("{\"content\":\"self\"}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
    }
    private LogController controller(long caller) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(caller, null, Collections.emptyList()));
        var controller = new LogController(); ReflectionTestUtils.setField(controller, "jdbcTemplate", db);
        ReflectionTestUtils.setField(controller, "hierarchyService", hierarchy); ReflectionTestUtils.setField(controller, "logDirectoryService", directory);
        return controller;
    }
    @SuppressWarnings("unchecked") private List<Map<String, Object>> rows(Map<String, Object> payload) { return (List<Map<String, Object>>) payload.get("comments"); }
    private Set<Long> ids(List<Map<String, Object>> rows) {
        Set<Long> result = new HashSet<>(); for (var row : rows) result.add(((Number) row.get("id")).longValue()); return result;
    }
    private void forbidden(org.junit.jupiter.api.function.Executable action) {
        assertEquals(ErrorCode.FORBIDDEN, assertThrows(BusinessException.class, action).getErrorCode());
    }
}

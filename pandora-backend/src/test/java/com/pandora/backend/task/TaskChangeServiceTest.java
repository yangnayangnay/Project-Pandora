package com.pandora.backend.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.ErrorCode;
import com.pandora.backend.hierarchy.HierarchyService;
import com.pandora.backend.notification.NotificationService;
import com.pandora.backend.user.PersonnelController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import javax.sql.DataSource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;


@org.springframework.test.context.TestExecutionListeners({
    org.springframework.test.context.support.DependencyInjectionTestExecutionListener.class,
    org.springframework.test.context.transaction.TransactionalTestExecutionListener.class
})
@SpringJUnitConfig(TaskChangeServiceTest.Config.class)
@Transactional
class TaskChangeServiceTest {
    @Configuration @EnableTransactionManagement @EnableMethodSecurity
    static class Config {
        @Bean DataSource source() {
            DriverManagerDataSource ds = new DriverManagerDataSource("jdbc:h2:mem:task-policy;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
            new ResourceDatabasePopulator(new ClassPathResource("db/h2_schema.sql")).execute(ds);
            return ds;
        }
        @Bean JdbcTemplate db(DataSource ds) { return new JdbcTemplate(ds); }
        @Bean DataSourceTransactionManager transactionManager(DataSource ds) { return new DataSourceTransactionManager(ds); }
        @Bean HierarchyService hierarchy(JdbcTemplate db) {
            HierarchyService h = new HierarchyService();
            ReflectionTestUtils.setField(h, "jdbcTemplate", db);
            ReflectionTestUtils.setField(h, "maxDepth", 10);
            ReflectionTestUtils.setField(h, "cycleDetect", true); return h;
        }
        @Bean TaskTagService tags(JdbcTemplate db) { return new TaskTagService(db); }
        @Bean TaskChangeService changes(JdbcTemplate db, HierarchyService h, TaskTagService tags) { return new TaskChangeService(db, new ObjectMapper(), h, new NotificationService() { @Override public void detectNewTask(Long taskId, Long assigneeId, Long dispatcherId) {} }, tags); }
        @Bean PersonnelController personnel(JdbcTemplate db, HierarchyService h) { return new PersonnelController(db, h); }
    }
    @Autowired JdbcTemplate db;
    @Autowired TaskChangeService service;
    @Autowired PersonnelController personnel;
    @BeforeEach void seed() {
        SecurityContextHolder.clearContext();
        db.update("DELETE FROM user_hierarchy");
        db.update("DELETE FROM users");
        String[] roles = {"ADMIN", "FOUNDER", "DEPT_HEAD", "TEAM_LEADER", "EMPLOYEE", "EMPLOYEE", "TEAM_LEADER"};
        for (int i = 0; i < roles.length; i++) db.update("INSERT INTO users (id, username, password_hash, role, approval_status) VALUES (?, ?, 'hash', ?, 'APPROVED')", i + 1, "user" + (i + 1), roles[i]);
        db.update("INSERT INTO user_hierarchy (superior_id, subordinate_id) VALUES (2,3),(3,4),(4,5),(4,6),(3,7)");
    }
    private Map<String, Object> creation() { return new HashMap<>(Map.of("name", "跨级派发任务", "content", "完整任务说明", "assigneeId", 2L, "endTime", "2026-12-30")); }
    private long number(Map<String, Object> row, String key) { return ((Number) row.get(key)).longValue(); }
    private long countTasks() { return db.queryForObject("SELECT COUNT(*) FROM tasks WHERE deleted = 0", Long.class); }
    private long directTask() { return number(service.submit(1, "CREATE", null, creation()), "id"); }

    @ParameterizedTest @ValueSource(strings = {"2026-12-29 09:00:00", "2026-12-29 08:59:59"})
    void creationRejectsEqualOrReversedTimesBeforeWriting(String end) {
        Map<String, Object> data = creation();
        data.put("startTime", "2026-12-29 09:00:00"); data.put("endTime", end);
        assertThrows(BusinessException.class, () -> service.submit(1, "CREATE", null, data));
        assertEquals(0, countTasks());
        assertEquals(0L, db.queryForObject("SELECT COUNT(*) FROM task_change_requests", Long.class));
    }
    @Test void partialDateEditsCompareAgainstTheUnchangedOtherEndpoint() {
        Map<String, Object> data = creation();
        data.put("startTime", "2026-12-29 09:00:00"); data.put("endTime", "2026-12-29 10:00:00");
        long id = number(service.submit(1, "CREATE", null, data), "id");
        assertThrows(BusinessException.class, () -> service.submit(1, "UPDATE", id, Map.of("endTime", "2026-12-29 09:00:00")));
        assertThrows(BusinessException.class, () -> service.submit(1, "UPDATE", id, Map.of("startTime", "2026-12-29 10:00:00")));
        service.submit(1, "UPDATE", id, Map.of("endTime", "2026-12-29 09:00:01"));
        assertEquals(Boolean.TRUE, db.queryForObject("SELECT end_time > start_time FROM tasks WHERE id=?", Boolean.class, id));
    }
    @Test void approvalRechecksDatesInOlderPendingRequests() throws Exception {
        Map<String, Object> data = creation();
        data.put("startTime", "2026-12-29 09:00:00"); data.put("endTime", "2026-12-29 10:00:00");
        long requestId = number(service.submit(5, "CREATE", null, data), "requestId");
        data.put("endTime", "2026-12-29 09:00:00");
        db.update("UPDATE task_change_requests SET payload=? WHERE id=?", new ObjectMapper().writeValueAsString(data), requestId);
        assertThrows(BusinessException.class, () -> service.decide(4, requestId, true, ""));
        assertEquals(0, countTasks());
        assertEquals("PENDING", db.queryForObject("SELECT status FROM task_change_requests WHERE id=?", String.class, requestId));
    }

    @ParameterizedTest @CsvSource({"5,4", "4,3", "3,2"})
    void ordinaryRolesStageCreationUntilTheirSuperiorApproves(long actor, long reviewer) {
        Map<String, Object> result = service.submit(actor, "CREATE", null, creation());
        assertEquals("PENDING", result.get("approvalStatus")); assertEquals(0, countTasks());
        assertEquals(reviewer, service.approver(actor));
        List<Map<String, Object>> inbox = service.requests(reviewer, false);
        assertEquals(1, inbox.size()); assertInstanceOf(Map.class, inbox.get(0).get("payload"));
        assertEquals(1, service.requests(actor, true).size());
        service.decide(reviewer, number(result, "requestId"), true, "同意");
        assertEquals(1, countTasks());
        assertEquals(2L, db.queryForObject("SELECT assignee_id FROM tasks", Long.class));
    }
    @ParameterizedTest @CsvSource({"1", "2"})
    void founderAndAdminCreateEditDeleteWithoutReview(long actor) {
        Map<String, Object> created = service.submit(actor, "CREATE", null, creation());
        assertEquals("APPROVED", created.get("approvalStatus"));
        long id = number(created, "id");
        service.submit(actor, "UPDATE", id, Map.of("name", "已修改"));
        assertEquals("已修改", db.queryForObject("SELECT name FROM tasks WHERE id = ?", String.class, id));
        service.submit(actor, "DELETE", id, Map.of());
        assertEquals(0, countTasks());
        assertEquals(0L, db.queryForObject("SELECT COUNT(*) FROM task_change_requests", Long.class));
    }
    @Test void editAndDeleteDoNotMutateBeforeApproval() {
        Map<String, Object> data = creation(); data.put("assigneeId", 5L);
        long id = number(service.submit(1, "CREATE", null, data), "id");
        long edit = number(service.submit(5, "UPDATE", id, Map.of("name", "新名称")), "requestId");
        assertEquals("跨级派发任务", db.queryForObject("SELECT name FROM tasks WHERE id = ?", String.class, id));
        assertThrows(BusinessException.class, () -> service.decide(7, edit, true, ""));
        service.decide(4, edit, true, "");
        assertEquals("新名称", db.queryForObject("SELECT name FROM tasks WHERE id = ?", String.class, id));
        db.update("UPDATE tasks SET dispatcher_id = 5 WHERE id = ?", id);
        long delete = number(service.submit(5, "DELETE", id, Map.of()), "requestId");
        assertEquals(1, countTasks()); service.decide(4, delete, true, ""); assertEquals(0, countTasks());
    }
    @Test void rejectionKeepsOriginalAndRequiresReason() {
        long request = number(service.submit(5, "CREATE", null, creation()), "requestId");
        assertThrows(BusinessException.class, () -> service.decide(4, request, false, " "));
        service.decide(4, request, false, "请补充范围");
        assertEquals(0, countTasks()); assertEquals("REJECTED", service.requests(5, true).get(0).get("status"));
        assertThrows(BusinessException.class, () -> service.decide(4, request, true, ""));
    }
    @Test void currentHierarchyControlsPendingReview() {
        long request = number(service.submit(5, "CREATE", null, creation()), "requestId");
        db.update("UPDATE user_hierarchy SET superior_id = 7 WHERE subordinate_id = 5");
        assertTrue(service.requests(4, false).isEmpty()); assertEquals(1, service.requests(7, false).size());
        assertThrows(BusinessException.class, () -> service.decide(4, request, true, ""));
        service.decide(7, request, true, ""); assertEquals(1, countTasks());
    }
    @Test void staleRevisionCannotOverwriteNewExecutionState() {
        Map<String, Object> data = creation(); data.put("assigneeId", 5L);
        long id = number(service.submit(1, "CREATE", null, data), "id");
        long request = number(service.submit(5, "UPDATE", id, Map.of("name", "旧申请")), "requestId");
        service.updateExecution(5, id, "COMPLETED", "已完成");
        assertThrows(BusinessException.class, () -> service.decide(4, request, true, ""));
        assertEquals("COMPLETED", db.queryForObject("SELECT status FROM tasks WHERE id = ?", String.class, id));
    }
    @Test void unauthorizedEditingAndExecutionAreRejected() {
        long id = directTask();
        assertThrows(BusinessException.class, () -> service.submit(6, "UPDATE", id, Map.of("name", "越权")));
        assertThrows(BusinessException.class, () -> service.submit(6, "DELETE", id, Map.of()));
        assertThrows(BusinessException.class, () -> service.updateExecution(6, id, "IN_PROGRESS", "越权"));
        assertNull(db.queryForObject("SELECT progress_note FROM tasks WHERE id = ?", String.class, id));
    }

    @Test void multipleAssigneesUseRelationRowsAndEachAssigneeCanUpdateProgress() {
        Map<String, Object> data = creation(); data.remove("assigneeId"); data.put("assigneeIds", List.of(5L, 6L));
        long id = number(service.submit(1, "CREATE", null, data), "id");
        assertEquals(List.of(5L, 6L), db.queryForList(
                "SELECT user_id FROM task_assignees WHERE task_id=? ORDER BY user_id", Long.class, id));
        service.updateExecution(6, id, null, "协同处理中");
        assertEquals("协同处理中", db.queryForObject("SELECT progress_note FROM tasks WHERE id=?", String.class, id));
        assertThrows(BusinessException.class, () -> service.updateExecution(7, id, null, "越权"));
        service.submit(1, "UPDATE", id, Map.of("assigneeIds", List.of(4L, 5L)));
        assertEquals(List.of(4L, 5L), db.queryForList(
                "SELECT user_id FROM task_assignees WHERE task_id=? ORDER BY user_id", Long.class, id));
    }
    @Test void directoryAndEmploymentRequireAdminAndExcludePassword() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(5L, null, List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))));
        assertThrows(AccessDeniedException.class, () -> personnel.directory());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        var directory = personnel.directory().getData();
        assertEquals(7, directory.size()); assertFalse(directory.get(0).containsKey("password_hash"));
        db.update("INSERT INTO departments (id,name) VALUES (100,'测试部门')");
        personnel.employment(SecurityContextHolder.getContext().getAuthentication(), 6,
                Map.of("role", "TEAM_LEADER", "superiorId", 3, "departmentId", 100));
        assertEquals("TEAM_LEADER", db.queryForObject("SELECT role FROM users WHERE id=6", String.class));
        assertEquals(100L, db.queryForObject("SELECT department_id FROM users WHERE id=6", Long.class));
        assertEquals(3L, service.approver(6));
    }

    @Test void administratorCanCreateRenameMoveAndDeleteEmptyTeamsButCannotDeleteTeamsWithMembers() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                1L, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        db.update("INSERT INTO departments (id,name) VALUES (901,'团队接口部门一'),(902,'团队接口部门二')");

        Map<String, Object> created = personnel.createTeam(Map.of(
                "name", "接口测试团队", "departmentId", 901L)).getData();
        long teamId = ((Number) created.get("id")).longValue();
        assertEquals(901L, db.queryForObject("SELECT department_id FROM teams WHERE id=?", Long.class, teamId));

        personnel.updateTeam(teamId, Map.of("name", "已改名团队", "departmentId", 902L));
        assertEquals("已改名团队", db.queryForObject("SELECT name FROM teams WHERE id=?", String.class, teamId));
        assertEquals(902L, db.queryForObject("SELECT department_id FROM teams WHERE id=?", Long.class, teamId));

        db.update("UPDATE users SET department_id=902,team_id=? WHERE id=5", teamId);
        assertThrows(BusinessException.class, () -> personnel.deleteTeam(teamId));
        assertThrows(BusinessException.class, () -> personnel.updateTeam(teamId,
                Map.of("name", "仍有成员", "departmentId", 901L)));

        db.update("UPDATE users SET team_id=NULL WHERE id=5");
        personnel.deleteTeam(teamId);
        assertEquals(1, db.queryForObject("SELECT deleted FROM teams WHERE id=?", Integer.class, teamId));
    }

    private void login(long id) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(id, null, List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))));
    }
    @Test void selfProfileRejectsContactIdentityAndRank() {
        login(5);
        var controller = new com.pandora.backend.user.UserController();
        ReflectionTestUtils.setField(controller, "jdbcTemplate", db);
        assertThrows(BusinessException.class, () -> controller.updateProfile(Map.of("username", "replacement")));
        assertThrows(BusinessException.class, () -> controller.updateProfile(Map.of("realName", "替代姓名")));
        assertThrows(BusinessException.class, () -> controller.updateProfile(Map.of("role", "ADMIN")));
        assertEquals(ErrorCode.FORBIDDEN, assertThrows(BusinessException.class,
                () -> controller.updateProfile(Map.of("email", "member@example.com", "phone", "13812345678"))).getErrorCode());
        assertEquals("user5", db.queryForObject("SELECT username FROM users WHERE id=5", String.class));
        assertNull(db.queryForObject("SELECT email FROM users WHERE id=5", String.class));
    }
    @Test void employeeCanEditOnlyOwnBoundedSignature() {
        login(5);
        var controller = new com.pandora.backend.user.UserController();
        ReflectionTestUtils.setField(controller, "jdbcTemplate", db);
        controller.updateProfile(Map.of("signature", "专注协作"));
        assertEquals("专注协作", db.queryForObject("SELECT signature FROM users WHERE id=5", String.class));
        assertThrows(BusinessException.class, () -> controller.updateProfile(Map.of("signature", "签".repeat(161))));
    }
    @Test void adminSelfPasswordChecksOldPasswordAndStoresOnlyHash() {
        login(1);
        var controller = new com.pandora.backend.user.UserController();
        var encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        ReflectionTestUtils.setField(controller, "jdbcTemplate", db);
        ReflectionTestUtils.setField(controller, "passwordEncoder", encoder);
        db.update("UPDATE users SET password_hash=? WHERE id=1", encoder.encode("old-test-value"));
        assertThrows(BusinessException.class, () -> controller.changePassword(Map.of("oldPassword", "wrong", "newPassword", "new-test-value")));
        assertThrows(BusinessException.class, () -> controller.changePassword(Map.of("oldPassword", "old-test-value", "newPassword", "短")));
        assertThrows(BusinessException.class, () -> controller.changePassword(Map.of("oldPassword", "old-test-value", "newPassword", "密".repeat(25))));
        controller.changePassword(Map.of("oldPassword", "old-test-value", "newPassword", "new-test-value"));
        String stored = db.queryForObject("SELECT password_hash FROM users WHERE id=1", String.class);
        assertNotEquals("new-test-value", stored); assertTrue(encoder.matches("new-test-value", stored));
    }
    @Test void logEditingPreservesDateAndCompletionAndRejectsOldDates() {
        login(5);
        var controller = new com.pandora.backend.log.LogController();
        ReflectionTestUtils.setField(controller, "jdbcTemplate", db);
        String today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")).toString();
        var created = controller.createLog(Map.of("workItem", "今日事项", "workContent", "正文", "logDate", today, "completionStatus", "COMPLETED"), null).getBody().getData();
        long id = number(created, "id");
        controller.editLog(id, Map.of("workItem", "更新事项", "workContent", "更新正文", "logDate", today));
        assertEquals("COMPLETED", db.queryForObject("SELECT completion_status FROM work_logs WHERE id=?", String.class, id));
        assertThrows(BusinessException.class, () -> controller.editLog(id, Map.of("logDate", java.time.LocalDate.parse(today).minusDays(1).toString())));
        assertThrows(BusinessException.class, () -> controller.createLog(Map.of("workItem", "重复事项", "logDate", today), null));
        db.update("UPDATE work_logs SET log_date=? WHERE id=?", java.time.LocalDate.parse(today).minusDays(3), id);
        assertThrows(BusinessException.class, () -> controller.editLog(id, Map.of("workItem", "过期编辑")));
        login(6); assertThrows(BusinessException.class, () -> controller.editLog(id, Map.of("workItem", "他人编辑")));
    }
    @Test @SuppressWarnings("unchecked") void relayCannotSilentlyOverwriteAnExistingDailyLog() {
        login(5);
        var controller = new com.pandora.backend.sync.SyncController();
        ReflectionTestUtils.setField(controller, "jdbcTemplate", db);
        ReflectionTestUtils.setField(controller, "taskChanges", service);
        String today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")).toString();
        db.update("INSERT INTO work_logs (user_id,work_item,log_date,completion_status) VALUES (5,'原始事项',?,'COMPLETED')", today);
        Map<String, Object> content = Map.of("operation", "CREATE", "workItem", "新标题", "workContent", "新内容", "logDate", today);
        Map<String, Object> item = Map.of("clientKey", "test-create", "type", "log", "content", content);
        var result = controller.sync(Map.of("items", List.of(item))).getBody().getData();
        var row = ((List<Map<String, Object>>) result.get("synced")).get(0);
        assertEquals("NO_MAPPING", row.get("status"));
        assertEquals("原始事项", db.queryForObject("SELECT work_item FROM work_logs WHERE user_id=5", String.class));
        item = Map.of("clientKey", "test-edit", "type", "log", "content", Map.of("operation", "EDIT", "workItem", "编辑事项", "workContent", "编辑内容", "logDate", today));
        result = controller.sync(Map.of("items", List.of(item))).getBody().getData();
        assertEquals("SYNCED", ((List<Map<String, Object>>) result.get("synced")).get(0).get("status"));
        assertEquals("COMPLETED", db.queryForObject("SELECT completion_status FROM work_logs WHERE user_id=5", String.class));
    }

    private long countRequests() { return db.queryForObject("SELECT COUNT(*) FROM task_change_requests", Long.class); }

    @Test void assigneesExcludeAdminPendingAndDeletedUsers() {
        db.update("INSERT INTO users (id, username, password_hash, role, approval_status) VALUES (8, 'pending8', 'hash', 'EMPLOYEE', 'PENDING')");
        db.update("INSERT INTO users (id, username, password_hash, role, approval_status, deleted) VALUES (9, 'deleted9', 'hash', 'EMPLOYEE', 'APPROVED', 1)");
        TaskController tasks = new TaskController();
        ReflectionTestUtils.setField(tasks, "jdbcTemplate", db);
        ReflectionTestUtils.setField(tasks, "hierarchyService", hierarchy());
        login(2);

        List<Map<String, Object>> assignees = tasks.assignees().getBody().getData();
        Set<Long> ids = new HashSet<>();
        for (Map<String, Object> assignee : assignees) {
            ids.add(number(assignee, "id"));
            assertNotEquals("ADMIN", assignee.get("role"));
        }
        assertEquals(6, assignees.size());
        assertEquals(Set.of(2L, 3L, 4L, 5L, 6L, 7L), ids);
        assertFalse(ids.contains(8L));
        assertFalse(ids.contains(9L));
    }

    @Test void assigneesRespectHierarchyForManagersAndEmployees() {
        TaskController tasks = new TaskController();
        ReflectionTestUtils.setField(tasks, "jdbcTemplate", db);
        ReflectionTestUtils.setField(tasks, "hierarchyService", hierarchy());
        login(3);
        Set<Long> managerIds = new HashSet<>();
        for (Map<String, Object> row : tasks.assignees().getBody().getData()) managerIds.add(number(row, "id"));
        assertEquals(Set.of(3L, 4L, 5L, 6L, 7L), managerIds);

        login(5);
        List<Map<String, Object>> employeeRows = tasks.assignees().getBody().getData();
        assertEquals(1, employeeRows.size());
        assertEquals(5L, number(employeeRows.get(0), "id"));
    }

    private HierarchyService hierarchy() {
        HierarchyService hierarchy = new HierarchyService();
        ReflectionTestUtils.setField(hierarchy, "jdbcTemplate", db);
        ReflectionTestUtils.setField(hierarchy, "maxDepth", 10);
        ReflectionTestUtils.setField(hierarchy, "cycleDetect", true);
        return hierarchy;
    }

    @ParameterizedTest @ValueSource(longs = {1L, 2L, 5L})
    void createTargetingAdminAssigneeIsRejectedWithoutAnySideEffect(long actor) {
        Map<String, Object> data = creation();
        data.put("assigneeId", 1L);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.submit(actor, "CREATE", null, data));

        assertEquals(ErrorCode.BAD_REQUEST, error.getErrorCode());
        assertTrue(error.getMessage().contains("管理员"));
        assertEquals(0, countTasks());
        assertEquals(0L, countRequests());
    }

    @Test void updateTargetingAdminAssigneeIsRejectedAndOriginalAssigneeStays() {
        long id = directTask();

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.submit(1, "UPDATE", id, Map.of("assigneeId", 1L)));

        assertEquals(ErrorCode.BAD_REQUEST, error.getErrorCode());
        assertTrue(error.getMessage().contains("管理员"));
        assertEquals(2L, db.queryForObject("SELECT assignee_id FROM tasks WHERE id = ?", Long.class, id).longValue());
        assertEquals(0L, countRequests());
    }

    @Test void pendingCreateRejectedWhenItsAssigneeBecomesAdmin() {
        Map<String, Object> data = creation();
        data.put("assigneeId", 6L);
        long request = number(service.submit(5, "CREATE", null, data), "requestId");
        db.update("UPDATE users SET role = 'ADMIN' WHERE id = 6");

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.decide(4, request, true, ""));

        assertEquals(ErrorCode.BAD_REQUEST, error.getErrorCode());
        assertTrue(error.getMessage().contains("管理员"));
        assertEquals(0, countTasks());
        assertEquals("PENDING", db.queryForObject(
                "SELECT status FROM task_change_requests WHERE id = ?", String.class, request));
    }

    @Test void pendingUpdateRejectedWhenItsNewAssigneeBecomesAdmin() {
        Map<String, Object> create = creation();
        create.put("assigneeId", 5L);
        long id = number(service.submit(1, "CREATE", null, create), "id");
        long request = number(service.submit(5, "UPDATE", id, Map.of("assigneeId", 6L)), "requestId");
        db.update("UPDATE users SET role = 'ADMIN' WHERE id = 6");

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.decide(4, request, true, ""));

        assertEquals(ErrorCode.BAD_REQUEST, error.getErrorCode());
        assertTrue(error.getMessage().contains("管理员"));
        assertEquals(5L, db.queryForObject("SELECT assignee_id FROM tasks WHERE id = ?", Long.class, id).longValue());
        assertEquals("PENDING", db.queryForObject(
                "SELECT status FROM task_change_requests WHERE id = ?", String.class, request));
    }
}

package com.pandora.backend.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandora.backend.common.BusinessException;
import com.pandora.backend.hierarchy.HierarchyService;
import com.pandora.backend.notification.NotificationService;
import com.pandora.backend.user.PersonnelController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
        @Bean TaskChangeService changes(JdbcTemplate db, HierarchyService h) { return new TaskChangeService(db, new ObjectMapper(), h, new NotificationService() { @Override public void detectNewTask(Long taskId, Long assigneeId, Long dispatcherId) {} }); }
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
        service.updateExecution(5, id, "IN_PROGRESS", "已开始");
        assertThrows(BusinessException.class, () -> service.decide(4, request, true, ""));
        assertEquals("IN_PROGRESS", db.queryForObject("SELECT status FROM tasks WHERE id = ?", String.class, id));
    }
    @Test void unauthorizedEditingAndExecutionAreRejected() {
        long id = directTask();
        assertThrows(BusinessException.class, () -> service.submit(6, "UPDATE", id, Map.of("name", "越权")));
        assertThrows(BusinessException.class, () -> service.submit(6, "DELETE", id, Map.of()));
        assertThrows(BusinessException.class, () -> service.updateExecution(6, id, "IN_PROGRESS", "越权"));
        assertNull(db.queryForObject("SELECT progress_note FROM tasks WHERE id = ?", String.class, id));
    }
    @Test void directoryAndEmploymentRequireAdminAndExcludePassword() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(5L, null, List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))));
        assertThrows(AccessDeniedException.class, () -> personnel.directory());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        var directory = personnel.directory().getData();
        assertEquals(7, directory.size()); assertFalse(directory.get(0).containsKey("password_hash"));
        personnel.employment(SecurityContextHolder.getContext().getAuthentication(), 6, Map.of("role", "TEAM_LEADER", "superiorId", 3));
        assertEquals("TEAM_LEADER", db.queryForObject("SELECT role FROM users WHERE id=6", String.class));
        assertEquals(3L, service.approver(6));
    }

    private void login(long id) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(id, null, List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))));
    }
    @Test void selfProfileAllowsContactButRejectsIdentityAndRank() {
        login(5);
        var controller = new com.pandora.backend.user.UserController();
        ReflectionTestUtils.setField(controller, "jdbcTemplate", db);
        assertThrows(BusinessException.class, () -> controller.updateProfile(Map.of("username", "replacement")));
        assertThrows(BusinessException.class, () -> controller.updateProfile(Map.of("realName", "替代姓名")));
        assertThrows(BusinessException.class, () -> controller.updateProfile(Map.of("role", "ADMIN")));
        controller.updateProfile(Map.of("email", "member@example.com", "phone", "13812345678"));
        assertEquals("user5", db.queryForObject("SELECT username FROM users WHERE id=5", String.class));
        assertEquals("member@example.com", db.queryForObject("SELECT email FROM users WHERE id=5", String.class));
    }
    @Test void selfPasswordChecksOldPasswordAndStoresOnlyHash() {
        login(5);
        var controller = new com.pandora.backend.user.UserController();
        var encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        ReflectionTestUtils.setField(controller, "jdbcTemplate", db);
        ReflectionTestUtils.setField(controller, "passwordEncoder", encoder);
        db.update("UPDATE users SET password_hash=? WHERE id=5", encoder.encode("old-test-value"));
        assertThrows(BusinessException.class, () -> controller.changePassword(Map.of("oldPassword", "wrong", "newPassword", "new-test-value")));
        assertThrows(BusinessException.class, () -> controller.changePassword(Map.of("oldPassword", "old-test-value", "newPassword", "短")));
        assertThrows(BusinessException.class, () -> controller.changePassword(Map.of("oldPassword", "old-test-value", "newPassword", "密".repeat(25))));
        controller.changePassword(Map.of("oldPassword", "old-test-value", "newPassword", "new-test-value"));
        String stored = db.queryForObject("SELECT password_hash FROM users WHERE id=5", String.class);
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
}

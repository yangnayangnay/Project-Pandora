package com.pandora.backend.view;

import com.pandora.backend.task.TaskTagService;
import org.junit.jupiter.api.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class HomeServiceTest {
    private JdbcTemplate db;
    private HomeService home;
    @BeforeEach void setup() {
        var source = new DriverManagerDataSource("jdbc:h2:mem:home-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("db/h2_schema.sql")).execute(source);
        db = new JdbcTemplate(source); home = new HomeService(db, new TaskTagService(db));
        db.update("DELETE FROM users");
        db.update("INSERT INTO users(id,username,password_hash,role,approval_status) VALUES (1,'first','hash','EMPLOYEE','APPROVED'),(2,'second','hash','EMPLOYEE','APPROVED')");
    }
    private void task(int id, long assignee, long dispatcher, String status) { db.update("INSERT INTO tasks(id,name,content,assignee_id,dispatcher_id,status) VALUES (?,?,'正文',?,?,?)", id, "任务" + id, assignee, dispatcher, status); }
    private List<Map<String, Object>> tasks(long actor, boolean completed) { return (List<Map<String, Object>>) home.overview(actor, completed).get("tasks"); }
    @Test void everyAssigneeSeesAllTheirDispatchedTasksBeyondTen() {
        for (int id = 1; id <= 15; id++) task(id, 1, 2, "PENDING");
        task(16, 2, 1, "IN_PROGRESS"); task(17, 1, 1, "IN_PROGRESS");
        assertEquals(16, tasks(1, false).size()); assertEquals(1, tasks(2, false).size());
        assertTrue(tasks(1, false).stream().allMatch(row -> ((Number) row.get("assignee_id")).longValue() == 1));
        assertTrue(tasks(1, false).stream().anyMatch(row -> ((Number) row.get("id")).intValue() == 17));
    }
    @Test void statusSwitchSeparatesFinishedAndActiveAndOmitsDeletedTasks() {
        int id = 1; for (String state : List.of("PENDING", "IN_PROGRESS", "REJECTED", "COMPLETED", "CONFIRMED")) task(id++, 1, 2, state);
        task(6, 1, 2, "PENDING"); db.update("UPDATE tasks SET deleted = 1 WHERE id = 6");
        assertEquals(3, tasks(1, false).size()); assertEquals(2, tasks(1, true).size());
        assertTrue(tasks(1, true).stream().allMatch(row -> List.of("COMPLETED", "CONFIRMED").contains(row.get("status"))));
    }
    @Test void historicalAdminDispatchedTasksKeepContentWithoutExposingAdministratorIdentity() {
        db.update("UPDATE users SET role='ADMIN',real_name='后台管理员' WHERE id=2");
        task(1, 1, 2, "PENDING");
        var row = tasks(1, false).get(0);
        assertEquals("系统", row.get("dispatcher_name"));
        assertEquals("任务1", row.get("name"));
        assertEquals("first", row.get("assignee_name"));
    }
    @Test void eventsRetainTheirExistingOrderAndPersonalPanelsAreNotFetched() {
        db.update("INSERT INTO top10_works(user_id,work_type,rank_order,content) VALUES (2,'COMPANY_IMPORTANT',2,'第二项'),(1,'COMPANY_IMPORTANT',1,'第一项'),(1,'PERSONAL_IMPORTANT',1,'个人事项')");
        Map<String, Object> result = home.overview(1, false);
        assertEquals(Set.of("events", "tasks"), result.keySet());
        List<Map<String, Object>> events = (List<Map<String, Object>>) result.get("events");
        assertEquals(2, events.size()); assertEquals("第一项", events.get(0).get("content")); assertEquals("第二项", events.get(1).get("content"));
    }
    @Test void pendingCreationRequestsAreNotShownAndTagsAreIncluded() {
        db.update("INSERT INTO task_change_requests(operation,requester_id,approver_id,payload,base_revision,status) VALUES ('CREATE',2,1,'{}',0,'PENDING')");
        assertTrue(tasks(1, false).isEmpty());
        task(1, 1, 2, "PENDING");
        TaskTagService tags = new TaskTagService(db);
        db.update("UPDATE users SET role = 'FOUNDER' WHERE id = 2");
        long tag = ((Number) tags.save(2, null, Map.of("name", "研发", "colorIndex", 3)).get("id")).longValue(); tags.replace(1, List.of(tag));
        assertEquals("first", tasks(1, false).get(0).get("assignee_name"));
        assertEquals(1, ((List<?>) tasks(1, false).get(0).get("tags")).size());
    }
}

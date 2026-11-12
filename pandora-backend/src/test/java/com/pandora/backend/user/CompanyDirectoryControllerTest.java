package com.pandora.backend.user;

import com.pandora.backend.common.BusinessException;
import com.pandora.backend.hierarchy.HierarchyService;
import com.pandora.backend.task.TaskTagService;
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

class CompanyDirectoryControllerTest {
    private JdbcTemplate db;
    private CompanyDirectoryController directory;

    @BeforeEach void setup() {
        var source = new DriverManagerDataSource("jdbc:h2:mem:company-directory-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("db/h2_schema.sql")).execute(source);
        db = new JdbcTemplate(source); db.update("DELETE FROM user_hierarchy"); db.update("DELETE FROM users");
        db.update("INSERT INTO users(id,username,password_hash,role,approval_status,real_name) VALUES " +
                "(1,'founder','hash','FOUNDER','APPROVED','创始人')," +
                "(2,'head','hash','DEPT_HEAD','APPROVED','主管')," +
                "(3,'member','hash','EMPLOYEE','APPROVED','员工')," +
                "(4,'outside','hash','EMPLOYEE','APPROVED','其他员工')");
        db.update("INSERT INTO user_hierarchy(superior_id,subordinate_id) VALUES(1,2),(2,3)");
        HierarchyService hierarchy = new HierarchyService();
        ReflectionTestUtils.setField(hierarchy, "jdbcTemplate", db);
        ReflectionTestUtils.setField(hierarchy, "maxDepth", 10);
        ReflectionTestUtils.setField(hierarchy, "cycleDetect", true);
        directory = new CompanyDirectoryController(db, hierarchy, new TaskTagService(db));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void founderSeesCompanyManagerSeesSubtreeAndEmployeeCannotEscapeScope() {
        login(1); assertEquals(Set.of(1L,2L,3L,4L), ids(directory.list().getData()));
        login(2); assertEquals(Set.of(2L,3L), ids(directory.list().getData()));
        login(3); assertEquals(Set.of(3L), ids(directory.list().getData()));
        assertThrows(BusinessException.class, () -> directory.detail(4, null, null, null));
        assertEquals(3L, number(directory.detail(3, null, null, null).getData().get("person")));
    }

    @Test void personTaskCardsIncludeThePrimaryAssigneeName() {
        db.update("INSERT INTO tasks(id,name,status,assignee_id,dispatcher_id,is_urgent) VALUES(91,'通讯录任务','IN_PROGRESS',3,2,0)");
        db.update("INSERT INTO task_assignees(task_id,user_id) VALUES(91,3)");
        login(2);
        @SuppressWarnings("unchecked")
        List<Map<String,Object>> tasks = (List<Map<String,Object>>) directory.detail(3, null, null, null).getData().get("tasks");
        assertEquals(1, tasks.size());
        Object assignee = tasks.get(0).get("assignee_real_name");
        if (assignee == null) assignee = tasks.get(0).get("ASSIGNEE_REAL_NAME");
        assertEquals("员工", assignee);
    }

    private void login(long id) { SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(id, "", List.of())); }
    private Set<Long> ids(List<Map<String, Object>> rows) { Set<Long> ids = new HashSet<>(); for (Map<String, Object> row : rows) ids.add(number(row)); return ids; }
    @SuppressWarnings("unchecked") private long number(Object value) {
        Map<String,Object> row = (Map<String,Object>) value;
        Object id = row.get("id"); if (id == null) id = row.get("ID"); return ((Number) id).longValue();
    }
}

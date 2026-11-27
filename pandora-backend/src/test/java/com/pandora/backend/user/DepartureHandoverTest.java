package com.pandora.backend.user;

import com.pandora.backend.auth.SessionStore;
import com.pandora.backend.common.BusinessException;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DepartureHandoverTest {
    JdbcTemplate db; AccountDepartureService departure; SessionStore sessions; TransactionTemplate tx;
    @BeforeEach void setup() {
        var ds=new DriverManagerDataSource("jdbc:h2:mem:handover-"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa","");
        new ResourceDatabasePopulator(new FileSystemResource("src/main/resources/db/h2_schema.sql")).execute(ds);
        db=new JdbcTemplate(ds); db.update("DELETE FROM users");
        db.update("INSERT INTO departments(id,name) VALUES(10,'产品部'),(20,'运营部')");
        db.update("INSERT INTO teams(id,name,department_id) VALUES(11,'设计组',10),(21,'运营组',20)");
        db.update("INSERT INTO users(id,username,real_name,password_hash,role,approval_status,department_id,team_id) VALUES "
                +"(1,'admin','管理员','unchanged','ADMIN','APPROVED',NULL,NULL),"
                +"(2,'founder','创始人','unchanged','FOUNDER','APPROVED',NULL,NULL),"
                +"(3,'head','部门负责人','unchanged','DEPT_HEAD','APPROVED',10,NULL),"
                +"(4,'leader','团队负责人','unchanged','TEAM_LEADER','APPROVED',10,11),"
                +"(5,'member','员工甲','unchanged','EMPLOYEE','APPROVED',10,11),"
                +"(6,'receiver','员工乙','unchanged','EMPLOYEE','APPROVED',10,11),"
                +"(7,'outside','其他部门','unchanged','EMPLOYEE','APPROVED',20,21)");
        sessions=new SessionStore(); sessions.put(4L,"session");
        departure=new AccountDepartureService(db,sessions); tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        new OrganizationHierarchy(db).synchronize(List.of(),4);
        db.update("INSERT INTO tasks(id,name,status,assignee_id,dispatcher_id,revision) VALUES(10,'主执行','IN_PROGRESS',4,3,1),"
                +"(11,'共同执行','IN_PROGRESS',5,3,0),(12,'派发职责','IN_PROGRESS',5,4,2),(13,'已完成','COMPLETED',4,4,0)");
        db.update("INSERT INTO task_assignees(task_id,user_id) VALUES(10,4),(10,6),(11,4),(11,5),(12,5),(13,4)");
        db.update("INSERT INTO work_logs(user_id,work_item,log_date) VALUES(4,'原日志','2026-11-23')");
    }
    @Test void previewIncludesExecutionCollaborationAndDispatchButExcludesCompleted() {
        var preview=departure.preview(1,4);
        assertEquals(true,preview.get("positionRequired"));
        @SuppressWarnings("unchecked") var tasks=(List<Map<String,Object>>)preview.get("tasks");
        assertEquals(List.of(10L,11L,12L),tasks.stream().map(t->((Number)t.get("id")).longValue()).toList());
        @SuppressWarnings("unchecked") var candidates=(List<Map<String,Object>>)preview.get("positionCandidates");
        assertEquals(Set.of(5L,6L),new HashSet<>(candidates.stream().map(t->((Number)t.get("id")).longValue()).toList()));
    }
    @Test void missingSelectionsCannotBypassHandoverAndDoNotRevokeSession() {
        for(var body:List.of(Map.<String,Object>of(),Map.<String,Object>of("positionSuccessorId",6)))
            assertThrows(BusinessException.class,()->tx.execute(s->{departure.depart(1,4,body);return null;}));
        assertEquals(0,db.queryForObject("SELECT deleted FROM users WHERE id=4",Integer.class));
        assertEquals("session",sessions.get(4L));
        assertEquals(4L,db.queryForObject("SELECT assignee_id FROM tasks WHERE id=10",Long.class));
    }
    @Test void succeedsAtomicallyPreservesHistoryAndOtherCollaboratorsAndAddsReceiverNotification() {
        tx.executeWithoutResult(s->departure.depart(1,4,Map.of("positionSuccessorId",6,"taskSuccessorId",6)));
        assertNull(sessions.get(4L)); assertEquals(1,db.queryForObject("SELECT deleted FROM users WHERE id=4",Integer.class));
        assertEquals("TEAM_LEADER",db.queryForObject("SELECT role FROM users WHERE id=6",String.class));
        assertEquals(11L,db.queryForObject("SELECT team_id FROM users WHERE id=6",Long.class));
        assertEquals(6L,db.queryForObject("SELECT leader_user_id FROM teams WHERE id=11",Long.class));
        assertEquals(6L,db.queryForObject("SELECT assignee_id FROM tasks WHERE id=10",Long.class));
        assertEquals(List.of(5L,6L),db.queryForList("SELECT user_id FROM task_assignees WHERE task_id=11 ORDER BY user_id",Long.class));
        assertEquals(6L,db.queryForObject("SELECT dispatcher_id FROM tasks WHERE id=12",Long.class));
        assertEquals(4L,db.queryForObject("SELECT assignee_id FROM tasks WHERE id=13",Long.class));
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM work_logs WHERE user_id=4",Integer.class));
        assertEquals("unchanged",db.queryForObject("SELECT password_hash FROM users WHERE id=6",String.class));
        assertEquals(List.of(6L),db.queryForList("SELECT superior_id FROM user_hierarchy WHERE subordinate_id=5",Long.class));
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM notifications WHERE target_user_id=6 AND type='NEW_TASK'",Integer.class));
        assertEquals(2L,db.queryForObject("SELECT revision FROM tasks WHERE id=10",Long.class));
    }
    @Test void positionAndTaskMayBeHandedToDifferentPeople() {
        tx.executeWithoutResult(s->departure.depart(1,4,Map.of("positionSuccessorId",6,"taskSuccessorId",7)));
        assertEquals("TEAM_LEADER",db.queryForObject("SELECT role FROM users WHERE id=6",String.class));
        assertEquals(7L,db.queryForObject("SELECT assignee_id FROM tasks WHERE id=10",Long.class));
    }
    @Test void invalidOrDepartedRecipientsAndStalePostRejectTheWholeOperation() {
        for(long invalid:List.of(1L,4L,7L,99L))
            assertThrows(BusinessException.class,()->tx.execute(s->{departure.depart(1,4,Map.of("positionSuccessorId",invalid,"taskSuccessorId",6));return null;}));
        db.update("UPDATE users SET deleted=1 WHERE id=6");
        assertThrows(BusinessException.class,()->tx.execute(s->{departure.depart(1,4,Map.of("positionSuccessorId",6,"taskSuccessorId",5));return null;}));
        assertThrows(BusinessException.class,()->tx.execute(s->{departure.depart(1,4,Map.of("expectedRole","DEPT_HEAD","expectedDepartmentId",10,"positionSuccessorId",5,"taskSuccessorId",5));return null;}));
        assertEquals(0,db.queryForObject("SELECT deleted FROM users WHERE id=4",Integer.class));
    }
    @Test void rollbackRestoresPostWorkRelationsAndSession() {
        tx.executeWithoutResult(s->{departure.depart(1,4,Map.of("positionSuccessorId",6,"taskSuccessorId",6));s.setRollbackOnly();});
        assertEquals("EMPLOYEE",db.queryForObject("SELECT role FROM users WHERE id=6",String.class));
        assertEquals(4L,db.queryForObject("SELECT assignee_id FROM tasks WHERE id=10",Long.class));
        assertEquals("session",sessions.get(4L));
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM notifications",Integer.class));
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM audit_logs",Integer.class));
    }
    @Test void successfulDepartureRecordsWhoReceivedPostAndWorkExactlyOnce() {
        tx.executeWithoutResult(s->departure.depart(1,4,Map.of("positionSuccessorId",6,"taskSuccessorId",5)));
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM audit_logs WHERE operation_type='USER_DEPARTURE'",Integer.class));
        String content=db.queryForObject("SELECT operation_content FROM audit_logs WHERE operation_type='USER_HANDOVER'",String.class);
        assertTrue(content.contains("员工乙")); assertTrue(content.contains("员工甲")); assertTrue(content.contains("3项"));
        assertFalse(content.contains("unchanged"));
    }
    @Test void employeeWithWorkNeedsOnlyTaskRecipientAndNoWorkEmployeeCanLeave() {
        assertThrows(BusinessException.class,()->tx.execute(s->{departure.depart(1,5);return null;}));
        tx.executeWithoutResult(s->departure.depart(1,5,Map.of("taskSuccessorId",6)));
        assertEquals("EMPLOYEE",db.queryForObject("SELECT role FROM users WHERE id=6",String.class));
        tx.executeWithoutResult(s->departure.depart(1,7));
        assertEquals(1,db.queryForObject("SELECT deleted FROM users WHERE id=7",Integer.class));
    }
}

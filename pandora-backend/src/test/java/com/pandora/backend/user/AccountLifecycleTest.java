package com.pandora.backend.user;

import com.pandora.backend.auth.SessionStore;
import com.pandora.backend.common.BusinessException;
import org.junit.jupiter.api.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AccountLifecycleTest {
    JdbcTemplate db; AccountCreationService accounts; AccountDepartureService departures;
    SessionStore sessions; TransactionTemplate tx;
    @BeforeEach void setup() {
        var source = new DriverManagerDataSource("jdbc:h2:mem:lifecycle-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new org.springframework.core.io.FileSystemResource("src/main/resources/db/h2_schema.sql")).execute(source);
        db = new JdbcTemplate(source); db.update("DELETE FROM users");
        db.update("INSERT INTO users(id,username,password_hash,role,approval_status) VALUES(1,'admin0','unchanged','ADMIN','APPROVED'),(2,'000001','old','FOUNDER','APPROVED'),(3,'000003','old','FOUNDER','APPROVED')");
        accounts = new AccountCreationService(db, new BCryptPasswordEncoder(4)); sessions = new SessionStore();
        departures = new AccountDepartureService(db, sessions); tx = new TransactionTemplate(new DataSourceTransactionManager(source));
    }
    Map<String, Object> form(String key) { return new HashMap<>(Map.of("password", "secret123", "role", "FOUNDER", "realName", "测试", "email", key+"@example.test", "phone", "13800000000", "idCard", AccountInputRulesTest.identity("110101199001010"+key), "birthday", "1990-01-01")); }
    @Test void accountCreationAuditKeepsNewAccountIdAndNeverCredentialsAndRollsBackTogether() {
        var security=org.springframework.security.core.context.SecurityContextHolder.getContext();
        security.setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(1L,null,List.of()));
        try {
            var created=tx.execute(s->accounts.create(form("21")));
            var record=db.queryForMap("SELECT target_id,operation_content,detail FROM audit_logs WHERE operation_type='USER_CREATE'");
            assertEquals(((Number)created.get("id")).longValue(),((Number)record.get("target_id")).longValue());
            assertTrue(record.get("operation_content").toString().contains(created.get("employeeNo").toString()));
            assertFalse(record.toString().contains("secret123"));
            tx.executeWithoutResult(s->{accounts.create(form("22"));s.setRollbackOnly();});
            assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM audit_logs WHERE operation_type='USER_CREATE'",Integer.class));
        } finally { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
    }
    @Test void generatedNumberUsesFirstGapAndNeverReusesDepartedIdentity() {
        assertEquals("000002", accounts.nextEmployeeNumber());
        var created = tx.execute(status -> accounts.create(form("11")));
        assertEquals("000002", created.get("employeeNo")); assertEquals("000004", accounts.nextEmployeeNumber());
        tx.executeWithoutResult(status -> departures.depart(1, ((Number) created.get("id")).longValue(),Map.of("positionSuccessorId",3)));
        assertEquals("000004", accounts.nextEmployeeNumber());
        assertEquals("unchanged", db.queryForObject("SELECT password_hash FROM users WHERE id=1", String.class));
    }
    @Test void concurrentCreatorsGetDistinctSmallestNumbers() throws Exception {
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var gate = new java.util.concurrent.CountDownLatch(1);
            var first = pool.submit(() -> { gate.await(); return tx.execute(status -> accounts.create(form("11"))); });
            var second = pool.submit(() -> { gate.await(); return tx.execute(status -> accounts.create(form("12"))); });
            gate.countDown();
            assertEquals(Set.of("000002", "000004"), Set.of(first.get().get("employeeNo"), second.get().get("employeeNo")));
        } finally { pool.shutdownNow(); }
    }
    @Test void departureKeepsHistoricalWorkRevokesSessionAndPreservesAdministrator() {
        db.update("INSERT INTO user_hierarchy(superior_id,subordinate_id) VALUES(2,3)");
        db.update("INSERT INTO work_logs(user_id,log_date,work_item) VALUES(2,'2026-11-23','历史日志')");
        sessions.put(2L, "token");
        tx.executeWithoutResult(status -> departures.depart(1, 2,Map.of("positionSuccessorId",3)));
        assertNull(sessions.get(2L)); assertEquals(1, db.queryForObject("SELECT deleted FROM users WHERE id=2", Integer.class));
        assertEquals(1, db.queryForObject("SELECT COUNT(*) FROM work_logs WHERE user_id=2", Integer.class));
        assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM user_hierarchy WHERE superior_id=2 OR subordinate_id=2", Integer.class));
        assertThrows(BusinessException.class, () -> tx.executeWithoutResult(status -> departures.depart(1, 1)));
        assertThrows(BusinessException.class, () -> tx.executeWithoutResult(status -> departures.depart(3, 1)));
        assertThrows(BusinessException.class, () -> tx.executeWithoutResult(status -> departures.depart(1, 2)));
    }
    @Test void rollbackPreservesAccountRelationsAndSession() {
        sessions.put(2L, "token");
        tx.executeWithoutResult(status -> { departures.depart(1, 2,Map.of("positionSuccessorId",3)); status.setRollbackOnly(); });
        assertEquals("token", sessions.get(2L)); assertEquals(0, db.queryForObject("SELECT deleted FROM users WHERE id=2", Integer.class));
    }
    @Test void sameNameRecreationNeedsExplicitAcknowledgementAndNeverInheritsOldWork() {
        db.update("UPDATE users SET real_name='测试' WHERE id=2");
        db.update("INSERT INTO work_logs(user_id,log_date,work_item) VALUES(2,'2026-11-23','旧记录')");
        tx.executeWithoutResult(s -> departures.depart(1,2,Map.of("positionSuccessorId",3)));
        assertEquals(com.pandora.backend.common.ErrorCode.CONFLICT,
                assertThrows(BusinessException.class,()->tx.execute(s->accounts.create(form("11")))).getErrorCode());
        var request=form("11"); request.put("acknowledgeArchivedName",true);
        var result=tx.execute(s->accounts.create(request));
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM work_logs WHERE user_id=?",Integer.class,result.get("id")));
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM work_logs WHERE user_id=2",Integer.class));
    }
    @Test void restoreKeepsOriginalIdentityPasswordAndWorkAndRebuildsRelations() {
        db.update("INSERT INTO departments(id,name) VALUES(10,'产品部')");
        db.update("UPDATE users SET role='DEPT_HEAD',department_id=10 WHERE id=2");
        db.update("INSERT INTO users(id,username,password_hash,role,approval_status,department_id) VALUES(4,'000004','old','EMPLOYEE','APPROVED',10)");
        db.update("INSERT INTO work_logs(user_id,log_date,work_item) VALUES(2,'2026-11-23','保留记录')");
        tx.executeWithoutResult(s->departures.depart(1,2,Map.of("positionSuccessorId",4)));
        assertEquals(10L,db.queryForObject("SELECT department_id FROM users WHERE id=2",Long.class));
        var personnel=new PersonnelController(db);
        var admin=new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(1L,null,List.of());
        tx.executeWithoutResult(s->personnel.restore(admin,2,Map.of("role","DEPT_HEAD","departmentId",10)));
        assertEquals("old",db.queryForObject("SELECT password_hash FROM users WHERE id=2",String.class));
        assertEquals("000001",db.queryForObject("SELECT username FROM users WHERE id=2",String.class));
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM work_logs WHERE user_id=2",Integer.class));
        assertEquals(List.of(3L),db.queryForList("SELECT superior_id FROM user_hierarchy WHERE subordinate_id=2",Long.class));
        tx.executeWithoutResult(s->departures.depart(1,2,Map.of("positionSuccessorId",4)));
        assertThrows(BusinessException.class,()->tx.execute(s->personnel.restore(admin,2,Map.of("role","DEPT_HEAD","departmentId",99))));
        assertEquals(1,db.queryForObject("SELECT deleted FROM users WHERE id=2",Integer.class));
    }
}

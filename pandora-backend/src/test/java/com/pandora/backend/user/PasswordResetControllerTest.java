package com.pandora.backend.user;

import com.pandora.backend.audit.AuditService;
import com.pandora.backend.auth.SessionStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordResetControllerTest {
    private JdbcTemplate db;
    private PasswordResetController controller;
    private BCryptPasswordEncoder passwords;
    private SessionStore sessions;

    @BeforeEach void setup() {
        var source = new DriverManagerDataSource(
                "jdbc:h2:mem:password-reset-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("db/h2_schema.sql")).execute(source);
        db = new JdbcTemplate(source);
        db.update("DELETE FROM password_reset_requests");
        db.update("DELETE FROM users");
        db.update("INSERT INTO users(id,username,password_hash,role,approval_status,real_name) VALUES " +
                "(1,'admin','old','ADMIN','APPROVED','管理员'),(2,'employee','old','EMPLOYEE','APPROVED','员工')");

        controller = new PasswordResetController();
        passwords = new BCryptPasswordEncoder();
        sessions = new SessionStore();
        AuditService audit = new AuditService();
        ReflectionTestUtils.setField(audit, "jdbcTemplate", db);
        ReflectionTestUtils.setField(controller, "jdbcTemplate", db);
        ReflectionTestUtils.setField(controller, "passwordEncoder", passwords);
        ReflectionTestUtils.setField(controller, "sessionStore", sessions);
        ReflectionTestUtils.setField(controller, "auditService", audit);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                1L, "", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void administratorApprovesRequestAndInvalidatesExistingSession() {
        db.update("INSERT INTO password_reset_requests(id,user_id,username,status,request_reason) VALUES(11,2,'employee','PENDING','忘记密码')");
        sessions.put(2L, "existing-session");

        Map<String, Object> result = controller.approveResetRequest(11L, Map.of("newPassword", "newpass123")).getBody().getData();

        assertEquals("newpass123", result.get("newPassword"));
        assertTrue(passwords.matches("newpass123", db.queryForObject(
                "SELECT password_hash FROM users WHERE id=2", String.class)));
        assertEquals("APPROVED", db.queryForObject(
                "SELECT status FROM password_reset_requests WHERE id=11", String.class));
        assertEquals(null, sessions.get(2L));
    }

    @Test void administratorCanRejectPendingRequestWithReason() {
        db.update("INSERT INTO password_reset_requests(id,user_id,username,status) VALUES(12,2,'employee','PENDING')");

        controller.rejectResetRequest(12L, Map.of("reason", "请联系部门负责人核验身份"));

        assertEquals("REJECTED", db.queryForObject(
                "SELECT status FROM password_reset_requests WHERE id=12", String.class));
        assertEquals("请联系部门负责人核验身份", db.queryForObject(
                "SELECT reject_reason FROM password_reset_requests WHERE id=12", String.class));
    }
}

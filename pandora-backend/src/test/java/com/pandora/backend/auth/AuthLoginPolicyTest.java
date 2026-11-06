package com.pandora.backend.auth;

import com.pandora.backend.common.ApiResponse;
import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AuthLoginPolicyTest {
    private static final String PASSWORD = "test-login-password";
    private static final String SECRET =
            "test-only-dummy-signing-secret-0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final AtomicInteger DB_SEQUENCE = new AtomicInteger();

    private JdbcTemplate db;
    private AuthController auth;
    private SessionStore sessions;
    private JwtUtil jwt;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:auth-login-" + DB_SEQUENCE.incrementAndGet() + ";MODE=MySQL;DB_CLOSE_DELAY=-1",
                "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("db/h2_schema.sql")).execute(dataSource);
        db = new JdbcTemplate(dataSource);
        db.update("DELETE FROM users");

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String passwordHash = encoder.encode(PASSWORD);
        String[] roles = {"ADMIN", "FOUNDER", "DEPT_HEAD", "TEAM_LEADER", "EMPLOYEE"};
        for (int i = 0; i < roles.length; i++) {
            db.update("INSERT INTO users (id, username, password_hash, role, approval_status, deleted) " +
                            "VALUES (?, ?, ?, ?, 'APPROVED', 0)",
                    i + 1, "login" + (i + 1), passwordHash, roles[i]);
        }

        sessions = new SessionStore();
        jwt = new JwtUtil();
        ReflectionTestUtils.setField(jwt, "secret", SECRET);
        ReflectionTestUtils.setField(jwt, "expiration", 60_000L);
        ReflectionTestUtils.setField(jwt, "refreshExpiration", 120_000L);

        auth = new AuthController();
        ReflectionTestUtils.setField(auth, "jdbcTemplate", db);
        ReflectionTestUtils.setField(auth, "passwordEncoder", encoder);
        ReflectionTestUtils.setField(auth, "sessionStore", sessions);
        ReflectionTestUtils.setField(auth, "jwtUtil", jwt);
    }

    @Test
    void adminAppLoginIsForbiddenAndExistingWebSessionIsKept() {
        sessions.put(1L, "existing-web-token");

        BusinessException error = rejected("login1", "APP", ErrorCode.FORBIDDEN);

        assertTrue(error.getMessage().contains("Web"));
        assertEquals("existing-web-token", sessions.get(1L));
    }

    @Test
    void adminWebLoginSucceeds() {
        Map<String, Object> data = successData("login1", "WEB");
        String token = (String) data.get("token");

        assertEquals(token, sessions.get(1L));
        assertTrue(jwt.validateToken(token));
        assertEquals(1L, jwt.getUserIdFromToken(token).longValue());
        assertEquals("ADMIN", userRole(data));
    }

    @Test
    void adminLoginWithoutClientTypeDefaultsToWeb() {
        Map<String, Object> data = successData("login1", null);

        assertEquals("ADMIN", userRole(data));
        assertEquals(data.get("token"), sessions.get(1L));
    }

    @ParameterizedTest
    @CsvSource({"FOUNDER,2", "DEPT_HEAD,3", "TEAM_LEADER,4", "EMPLOYEE,5"})
    void ordinaryRolesMayLoginFromTheApp(String role, long userId) {
        Map<String, Object> data = successData("login" + userId, "APP");

        assertEquals(role, userRole(data));
        assertEquals(data.get("token"), sessions.get(userId));
        assertTrue(jwt.validateToken((String) data.get("token")));
    }

    @ParameterizedTest
    @CsvSource({"FOUNDER,2", "DEPT_HEAD,3", "TEAM_LEADER,4", "EMPLOYEE,5"})
    void ordinaryRolesMayNotLoginFromWebAndNoSessionIsWritten(String role, long userId) {
        assertNull(sessions.get(userId));

        BusinessException error = rejected("login" + userId, "WEB", ErrorCode.FORBIDDEN);

        assertTrue(error.getMessage().contains("管理端"));
        assertNull(sessions.get(userId));
    }

    @Test
    void unknownClientTypeIsRejectedWithoutSession() {
        BusinessException error = rejected("login5", "MOBILE", ErrorCode.BAD_REQUEST);

        assertTrue(error.getMessage().contains("登录端"));
        assertNull(sessions.get(5L));
    }

    @Test void appUsesEmployeeNumberAndDoesNotFallBackToEmail() {
        db.update("UPDATE users SET username='000005',email='employee@example.test' WHERE id=5");
        var request = credentials("000005", "APP"); request.put("employeeNo", " 000005 ");
        var payload = auth.login(request).getBody().getData();
        assertEquals("EMPLOYEE", userRole(payload));
        assertEquals(ErrorCode.INVALID_CREDENTIALS, assertThrows(BusinessException.class,
                () -> auth.login(credentials("employee@example.test", "APP"))).getErrorCode());
        request.put("employeeNo", "different");
        assertEquals(ErrorCode.BAD_REQUEST, assertThrows(BusinessException.class, () -> auth.login(request)).getErrorCode());
    }
    @Test void administratorUsernameAndEmailLoginKeepExistingBehavior() {
        db.update("UPDATE users SET email='admin@example.test' WHERE id=1");
        assertEquals("ADMIN", userRole(successData("login1", "WEB")));
        assertEquals("ADMIN", userRole(successData("admin@example.test", "WEB")));
        Map<String, String> emailOnly = new HashMap<>(); emailOnly.put("email", "admin@example.test"); emailOnly.put("password", PASSWORD);
        assertEquals("ADMIN", userRole(auth.login(emailOnly).getBody().getData()));
        assertEquals("用户名或密码错误", rejected("does-not-exist", "WEB", ErrorCode.INVALID_CREDENTIALS).getMessage());
    }

    private Map<String, String> credentials(String username, String clientType) {
        Map<String, String> request = new HashMap<>();
        request.put("username", username);
        request.put("password", PASSWORD);
        if (clientType != null) request.put("clientType", clientType);
        return request;
    }

    private Map<String, Object> successData(String username, String clientType) {
        ResponseEntity<ApiResponse<Map<String, Object>>> response = auth.login(credentials(username, clientType));
        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().getCode());
        Map<String, Object> data = response.getBody().getData();
        assertNotNull(data);
        assertNotNull(data.get("token"));
        assertFalse(((String) data.get("token")).isBlank());
        assertNotNull(data.get("refreshToken"));
        assertNotNull(data.get("user"));
        return data;
    }

    private BusinessException rejected(String username, String clientType, ErrorCode expected) {
        BusinessException error = assertThrows(BusinessException.class,
                () -> auth.login(credentials(username, clientType)));
        assertEquals(expected, error.getErrorCode());
        return error;
    }

    @SuppressWarnings("unchecked")
    private String userRole(Map<String, Object> data) {
        return (String) ((Map<String, Object>) data.get("user")).get("role");
    }
}

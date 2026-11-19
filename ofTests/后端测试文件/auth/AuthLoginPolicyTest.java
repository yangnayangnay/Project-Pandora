package com.pandora.backend.auth;

/**
 * AuthLoginPolicy 单元测试 - 认证登录策略模块
 * 
 * 测试目标：验证不同角色在不同客户端类型的登录策略
 * 
 * 测试范围：
 * - 正常场景：管理员Web登录、普通角色App登录、凭证规则
 * - 边界场景：缺省客户端类型
 * - 异常场景：管理员App登录被拒、未知客户端类型、普通角色Web越权
 * 
 * 前置条件：使用 H2 内存数据库初始化测试数据
 * 测试用例：8个
 */


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
        // 按依赖倒序删除：先删子表，再删父表
        db.update("DELETE FROM user_hierarchy");
        db.update("DELETE FROM task_tag_links");
        db.update("DELETE FROM task_tags");
        db.update("DELETE FROM task_assignees");
        db.update("DELETE FROM task_change_requests");
        db.update("DELETE FROM tasks");
        db.update("DELETE FROM log_comments");
        db.update("DELETE FROM work_logs");
        db.update("DELETE FROM daily_items");
        db.update("DELETE FROM ai_analysis_records");
        db.update("DELETE FROM divination_records");
        db.update("DELETE FROM mbti_reports");
        db.update("DELETE FROM calendar_events");
        db.update("DELETE FROM teams");
        db.update("DELETE FROM departments");
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

    /**
     * 测试用例：管理员App登录禁止
     * 输入类别：异类-越权（越权输入）
     * 测试步骤：1. 设置现有Web会话 2. 尝试从App登录 3. 检查会话是否保留
     * 预期结果：管理员从App登录被拒绝，Web会话保留
     */
    void adminAppLoginIsForbiddenAndExistingWebSessionIsKept() {
        sessions.put(1L, "existing-web-token");

        BusinessException error = rejected("login1", "APP", ErrorCode.FORBIDDEN);

        assertTrue(error.getMessage().contains("Web"));
        assertEquals("existing-web-token", sessions.get(1L));
    }

    /**
     * 测试用例：管理员Web登录成功
     * 输入类别：同类（有效等价类）
     * 测试步骤：使用管理员账号从Web端登录
     * 预期结果：登录成功，返回有效令牌
     */
    void adminWebLoginSucceeds() {
        Map<String, Object> data = successData("login1", "WEB");
        String token = (String) data.get("token");

        assertEquals(token, sessions.get(1L));
        assertTrue(jwt.validateToken(token));
        assertEquals(1L, jwt.getUserIdFromToken(token).longValue());
        assertEquals("ADMIN", userRole(data));
    }

    /**
     * 测试用例：缺省客户端类型
     * 输入类别：边界（边界值分析）
     * 测试步骤：不指定客户端类型进行登录
     * 预期结果：缺省为Web端登录
     */
    void adminLoginWithoutClientTypeDefaultsToWeb() {
        Map<String, Object> data = successData("login1", null);

        assertEquals("ADMIN", userRole(data));
        assertEquals(data.get("token"), sessions.get(1L));
    }

    @ParameterizedTest
    /**
     * 测试用例：普通角色App登录
     * 输入类别：同类（有效等价类）
     * 测试步骤：FOUNDER/DEPT_HEAD/TEAM_LEADER/EMPLOYEE 从App端登录
     * 预期结果：4个普通角色从App端登录成功
     */
    void ordinaryRolesMayLoginFromTheApp(String role, long userId) {
        Map<String, Object> data = successData("login" + userId, "APP");

        assertEquals(role, userRole(data));
        assertEquals(data.get("token"), sessions.get(userId));
        assertTrue(jwt.validateToken((String) data.get("token")));
    }

    @ParameterizedTest
    /**
     * 测试用例：普通角色Web越权
     * 输入类别：异类-越权（越权输入）
     * 测试步骤：普通角色尝试从Web端登录
     * 预期结果：4个普通角色从Web端登录被拒绝
     */
    void ordinaryRolesMayNotLoginFromWebAndNoSessionIsWritten(String role, long userId) {
        assertNull(sessions.get(userId));

        BusinessException error = rejected("login" + userId, "WEB", ErrorCode.FORBIDDEN);

        assertTrue(error.getMessage().contains("管理端"));
        assertNull(sessions.get(userId));
    }

    /**
     * 测试用例：未知客户端类型
     * 输入类别：异类-异常（无效等价类）
     * 测试步骤：使用未知客户端类型登录
     * 预期结果：被拒绝且不写入会话
     */
    void unknownClientTypeIsRejectedWithoutSession() {
        BusinessException error = rejected("login5", "MOBILE", ErrorCode.BAD_REQUEST);

        assertTrue(error.getMessage().contains("登录端"));
        assertNull(sessions.get(5L));
    }
    /**
     * 测试用例：凭证规则
     * 输入类别：正+边（正常+边界）
     * 测试步骤：使用工号和邮箱分别登录App端
     * 预期结果：App使用工号且不通配邮箱
     */
    @Test void appUsesEmployeeNumberAndDoesNotFallBackToEmail() {
        db.update("UPDATE users SET username='000005',email='employee@example.test' WHERE id=5");
        var request = credentials("000005", "APP"); request.put("employeeNo", " 000005 ");
        var payload = auth.login(request).getBody().getData();
        assertEquals("EMPLOYEE", userRole(payload));
        assertEquals(ErrorCode.INVALID_CREDENTIALS, assertThrows(BusinessException.class,
                () -> auth.login(credentials("employee@example.test", "APP"))).getErrorCode());
        request.put("employeeNo", "different");
        assertEquals(ErrorCode.BAD_REQUEST, assertThrows(BusinessException.class, () -> auth.login(request)).getErrorCode());
    /**
     * 测试用例：兼容旧行为
     * 输入类别：同类（有效等价类）
     * 测试步骤：使用用户名和邮箱分别登录
     * 预期结果：管理员用户名和邮箱登录保持兼容
     */
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

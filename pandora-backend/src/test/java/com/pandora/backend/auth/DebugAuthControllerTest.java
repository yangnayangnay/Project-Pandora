package com.pandora.backend.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DebugAuthControllerTest {
    private JdbcTemplate db;
    private AuthController auth;
    private SessionStore sessions;
    private JwtUtil jwt;
    
    @BeforeEach
    void setUp() {
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:debug-auth;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "sa", "");
        
        // 加载 schema
        new ResourceDatabasePopulator(new ClassPathResource("db/h2_schema.sql")).execute(dataSource);
        db = new JdbcTemplate(dataSource);
        
        // 清空所有表（按依赖倒序）
        db.execute("DELETE FROM user_hierarchy");
        db.execute("DELETE FROM task_tag_links");
        db.execute("DELETE FROM task_tags");
        db.execute("DELETE FROM task_assignees");
        db.execute("DELETE FROM task_change_requests");
        db.execute("DELETE FROM tasks");
        db.execute("DELETE FROM log_comments");
        db.execute("DELETE FROM work_logs");
        db.execute("DELETE FROM daily_items");
        db.execute("DELETE FROM ai_analysis_records");
        db.execute("DELETE FROM divination_records");
        db.execute("DELETE FROM mbti_reports");
        db.execute("DELETE FROM calendar_events");
        db.execute("DELETE FROM teams");
        db.execute("DELETE FROM departments");
        db.execute("DELETE FROM users");
        
        // 插入测试用户（与 AuthLoginPolicyTest 相同）
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String passwordHash = encoder.encode("test-login-password");
        String[] roles = {"ADMIN", "FOUNDER", "DEPT_HEAD", "TEAM_LEADER", "EMPLOYEE"};
        for (int i = 0; i < roles.length; i++) {
            db.update("INSERT INTO users (id, username, password_hash, role, approval_status, deleted) VALUES (?, ?, ?, ?, 'APPROVED', 0)",
                    i + 1, "login" + (i + 1), passwordHash, roles[i]);
        }
        
        // 初始化控制器
        sessions = new SessionStore();
        jwt = new JwtUtil();
        ReflectionTestUtils.setField(jwt, "secret", "test-only-dummy-signing-secret-0123456789abcdef0123456789abcdef0123456789abcdef");
        ReflectionTestUtils.setField(jwt, "expiration", 60_000L);
        ReflectionTestUtils.setField(jwt, "refreshExpiration", 120_000L);
        
        auth = new AuthController();
        ReflectionTestUtils.setField(auth, "jdbcTemplate", db);
        ReflectionTestUtils.setField(auth, "passwordEncoder", encoder);
        ReflectionTestUtils.setField(auth, "sessionStore", sessions);
        ReflectionTestUtils.setField(auth, "jwtUtil", jwt);
    }
    
    @Test
    void testAdminWebLogin() {
        // 先检查用户是否存在
        try {
            Map<String, Object> user = db.queryForMap("SELECT * FROM users WHERE username = 'login1'");
            System.out.println("Found user in DB: " + user);
            
            String hash = (String) user.get("password_hash");
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            System.out.println("Password matches: " + encoder.matches("test-login-password", hash));
        } catch (Exception e) {
            System.out.println("Error finding user: " + e.getMessage());
        }
        
        // 检查passwordEncoder是否注入
        BCryptPasswordEncoder injectedEncoder = (BCryptPasswordEncoder) ReflectionTestUtils.getField(auth, "passwordEncoder");
        System.out.println("Injected encoder: " + injectedEncoder);
        
        // 检查jdbcTemplate是否注入
        JdbcTemplate injectedDb = (JdbcTemplate) ReflectionTestUtils.getField(auth, "jdbcTemplate");
        System.out.println("Injected jdbcTemplate: " + injectedDb);
        
        // 尝试用AuthController的jdbcTemplate查询用户
        if (injectedDb != null) {
            try {
                Map<String, Object> user = injectedDb.queryForMap("SELECT * FROM users WHERE username = 'login1'");
                System.out.println("Found user via injected jdbcTemplate: " + user);
            } catch (Exception e) {
                System.out.println("Error finding user via injected jdbcTemplate: " + e.getMessage());
            }
        }
        
        // 检查auditService是否注入
        Object auditService = ReflectionTestUtils.getField(auth, "auditService");
        System.out.println("Injected auditService: " + auditService);
        
        // 直接调用 findUserByUsername 检查
        try {
            var method = AuthController.class.getDeclaredMethod("findUserByUsername", String.class);
            method.setAccessible(true);
            Object result = method.invoke(auth, "login1");
            System.out.println("Found user via reflection: " + result);
            System.out.println("Result type: " + (result != null ? result.getClass() : "null"));
        } catch (Exception e) {
            System.out.println("Error calling findUserByUsername: " + e.getMessage());
            e.printStackTrace();
        }
        
        // 直接用 jdbcTemplate 执行相同的查询
        try {
            Map<String, Object> user = injectedDb.queryForMap(
                "SELECT id, username, password_hash, role, email, real_name, id_card, approval_status, " +
                "department_id, team_id, deleted FROM users WHERE username = ?",
                "login1"
            );
            System.out.println("Found user via direct query: " + user);
        } catch (Exception e) {
            System.out.println("Error in direct query: " + e.getMessage());
            e.printStackTrace();
        }
        
        // 模拟登录请求
        var request = Map.of(
            "username", "login1",
            "password", "test-login-password",
            "clientType", "WEB"
        );
        
        var response = auth.login(request);
        System.out.println("Response: " + response.getBody());
        
        assertEquals(200, response.getBody().getCode());
        assertNotNull(response.getBody().getData());
        assertNotNull(response.getBody().getData().get("token"));
    }
    
    @Test
    void testAppLogin() {
        // 模拟 APP 登录请求
        var request = Map.of(
            "username", "login2",
            "password", "test-login-password",
            "clientType", "APP"
        );
        
        var response = auth.login(request);
        System.out.println("Response: " + response.getBody());
        
        assertEquals(200, response.getBody().getCode());
        assertNotNull(response.getBody().getData());
        assertNotNull(response.getBody().getData().get("token"));
    }
}
package com.pandora.backend.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.sql.DataSource;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DebugSchemaTest {
    private JdbcTemplate db;
    
    @BeforeEach
    void setUp() {
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:debug-schema;MODE=MySQL;DB_CLOSE_DELAY=-1",
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
        
        // 插入测试用户
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String passwordHash = encoder.encode("test-login-password");
        String[] roles = {"ADMIN", "FOUNDER", "DEPT_HEAD", "TEAM_LEADER", "EMPLOYEE"};
        for (int i = 0; i < roles.length; i++) {
            db.update("INSERT INTO users (id, username, password_hash, role, approval_status, deleted) VALUES (?, ?, ?, ?, 'APPROVED', 0)",
                    i + 1, "login" + (i + 1), passwordHash, roles[i]);
        }
    }
    
    @Test
    void testAdminLogin() {
        // 验证用户存在
        Map<String, Object> user = db.queryForMap("SELECT * FROM users WHERE username = 'login1'");
        assertNotNull(user);
        System.out.println("Admin user: " + user);
        
        // 验证密码
        String hash = (String) user.get("password_hash");
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        boolean matches = encoder.matches("test-login-password", hash);
        System.out.println("Password matches: " + matches);
        assertTrue(matches);
    }
    
    @Test
    void testAppLogin() {
        // 验证 APP 用户存在
        Map<String, Object> user = db.queryForMap("SELECT * FROM users WHERE username = 'login2'");
        assertNotNull(user);
        System.out.println("Founder user: " + user);
        
        // 验证密码
        String hash = (String) user.get("password_hash");
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        boolean matches = encoder.matches("test-login-password", hash);
        System.out.println("Password matches: " + matches);
        assertTrue(matches);
    }
}
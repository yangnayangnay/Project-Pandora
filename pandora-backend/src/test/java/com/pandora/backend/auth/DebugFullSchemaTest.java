package com.pandora.backend.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DebugFullSchemaTest {
    private JdbcTemplate db;
    
    @BeforeEach
    void setUp() {
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:debug-full-schema;MODE=MySQL;DB_CLOSE_DELAY=-1",
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
        db.execute("DELETE FROM divination_records");
        db.execute("DELETE FROM mbti_reports");
        db.execute("DELETE FROM teams");
        db.execute("DELETE FROM departments");
        db.execute("DELETE FROM users");
        
        // 插入测试用户
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String passwordHash = encoder.encode("test-login-password");
        System.out.println("Generated hash: " + passwordHash);
        
        String[] roles = {"ADMIN", "FOUNDER", "DEPT_HEAD", "TEAM_LEADER", "EMPLOYEE"};
        for (int i = 0; i < roles.length; i++) {
            db.update("INSERT INTO users (id, username, password_hash, role, approval_status, deleted) VALUES (?, ?, ?, ?, 'APPROVED', 0)",
                    i + 1, "login" + (i + 1), passwordHash, roles[i]);
        }
    }
    
    @Test
    void testAllUsers() {
        // 查询所有用户
        List<Map<String, Object>> users = db.queryForList("SELECT id, username, password_hash, role FROM users ORDER BY id");
        System.out.println("Total users: " + users.size());
        
        for (Map<String, Object> user : users) {
            System.out.println("User: " + user.get("username") + ", Role: " + user.get("role") + ", Hash: " + user.get("password_hash"));
        }
        
        // 验证第一个用户
        Map<String, Object> admin = users.get(0);
        String hash = (String) admin.get("password_hash");
        
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        boolean matches = encoder.matches("test-login-password", hash);
        System.out.println("Password matches: " + matches);
        
        assertTrue(matches);
    }
    
    @Test
    void testLoginQuery() {
        // 模拟登录查询
        String username = "login1";
        try {
            Map<String, Object> user = db.queryForMap(
                "SELECT id, username, password_hash, role, approval_status, deleted FROM users WHERE username = ?",
                username
            );
            System.out.println("Found user: " + user);
            
            String hash = (String) user.get("password_hash");
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            boolean matches = encoder.matches("test-login-password", hash);
            System.out.println("Password matches: " + matches);
            
            assertTrue(matches);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            fail("User not found");
        }
    }
}

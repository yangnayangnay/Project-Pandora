package com.pandora.backend.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DebugUserTest {
    private JdbcTemplate db;
    
    @BeforeEach
    void setUp() {
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:debug-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "sa", "");
        db = new JdbcTemplate(dataSource);
        
        // 创建表
        db.execute("CREATE TABLE users (id BIGINT PRIMARY KEY AUTO_INCREMENT, username VARCHAR(50), password_hash VARCHAR(255), role VARCHAR(50), approval_status VARCHAR(50), deleted TINYINT)");
        
        // 插入测试数据
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String passwordHash = encoder.encode("test-login-password");
        db.update("INSERT INTO users (username, password_hash, role, approval_status, deleted) VALUES (?, ?, ?, 'APPROVED', 0)", 
                "login1", passwordHash, "ADMIN");
    }
    
    @Test
    void testUserExists() {
        Map<String, Object> user = db.queryForMap("SELECT * FROM users WHERE username = 'login1'");
        System.out.println("User: " + user);
        assertNotNull(user);
        
        String hash = (String) user.get("password_hash");
        System.out.println("Password hash: " + hash);
        
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        boolean matches = encoder.matches("test-login-password", hash);
        System.out.println("Password matches: " + matches);
        assertTrue(matches);
    }
}
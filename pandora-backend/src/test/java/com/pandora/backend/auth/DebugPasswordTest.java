package com.pandora.backend.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

public class DebugPasswordTest {
    @Test
    void testBcryptEncoding() {
        String password = "test-login-password";
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // 生成哈希
        String hash = encoder.encode(password);
        System.out.println("Generated hash: " + hash);
        
        // 验证
        boolean matches = encoder.matches(password, hash);
        System.out.println("Matches: " + matches);
        
        assertTrue(matches);
    }
    
    @Test
    void testPythonHash() {
        String password = "test-login-password";
        String pythonHash = "$2b$12$vT94hqd.aKcyZhoGcHZ3weNv0QdeoNy20kOjk0XfxeGs4sKjlqnja";
        
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        boolean matches = encoder.matches(password, pythonHash);
        System.out.println("Python hash matches: " + matches);
        
        assertTrue(matches);
    }
}
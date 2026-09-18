package com.pandora.backend.auth;

import com.pandora.backend.common.ApiResponse;
import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SessionStore sessionStore;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        String role = request.get("role");

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD);
        }

        if (password.length() < 6) {
            throw new BusinessException(ErrorCode.PASSWORD_TOO_SHORT);
        }

        if (role == null || role.trim().isEmpty()) {
            role = "EMPLOYEE";
        }

        if (!isValidRole(role)) {
            throw new BusinessException(ErrorCode.INVALID_ROLE);
        }

        if (findUserByUsername(username) != null) {
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        String passwordHash = passwordEncoder.encode(password);
        jdbcTemplate.update(
                "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)",
                username, passwordHash, role
        );

        Map<String, Object> user = findUserByUsername(username);
        Long userId = ((Number) user.get("id")).longValue();
        Long departmentId = user.get("department_id") != null ? ((Number) user.get("department_id")).longValue() : null;
        Long teamId = user.get("team_id") != null ? ((Number) user.get("team_id")).longValue() : null;

        String token = jwtUtil.generateToken(userId, username, role, departmentId, teamId);
        String refreshToken = jwtUtil.generateRefreshToken(userId, username);

        sessionStore.put(userId, token);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("refreshToken", refreshToken);
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", userId);
        userInfo.put("username", username);
        userInfo.put("role", role);
        userInfo.put("departmentId", departmentId);
        userInfo.put("teamId", teamId);
        result.put("user", userInfo);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        if (username == null || password == null) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD);
        }

        Map<String, Object> user = findUserByUsername(username);
        if (user == null) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        String passwordHash = (String) user.get("password_hash");
        if (!passwordEncoder.matches(password, passwordHash)) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        if ((Integer) user.get("deleted") == 1) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        Long userId = ((Number) user.get("id")).longValue();
        String role = (String) user.get("role");
        Long departmentId = user.get("department_id") != null ? ((Number) user.get("department_id")).longValue() : null;
        Long teamId = user.get("team_id") != null ? ((Number) user.get("team_id")).longValue() : null;

        String token = jwtUtil.generateToken(userId, username, role, departmentId, teamId);
        String refreshToken = jwtUtil.generateRefreshToken(userId, username);

        sessionStore.put(userId, token);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("refreshToken", refreshToken);
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", userId);
        userInfo.put("username", username);
        userInfo.put("role", role);
        userInfo.put("departmentId", departmentId);
        userInfo.put("teamId", teamId);
        result.put("user", userInfo);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(token);
        sessionStore.remove(userId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, String>>> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null || !jwtUtil.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        String username = jwtUtil.getUsernameFromToken(refreshToken);

        Map<String, Object> user = findUserById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        String role = (String) user.get("role");
        Long departmentId = user.get("department_id") != null ? ((Number) user.get("department_id")).longValue() : null;
        Long teamId = user.get("team_id") != null ? ((Number) user.get("team_id")).longValue() : null;

        String newToken = jwtUtil.generateToken(userId, username, role, departmentId, teamId);

        Map<String, String> result = new HashMap<>();
        result.put("token", newToken);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private boolean isValidRole(String role) {
        return role.equals("ADMIN") || role.equals("FOUNDER") || role.equals("DEPT_HEAD")
                || role.equals("TEAM_LEADER") || role.equals("EMPLOYEE");
    }

    private Map<String, Object> findUserByUsername(String username) {
        try {
            return jdbcTemplate.queryForMap(
                    "SELECT id, username, password_hash, role, department_id, team_id, deleted FROM users WHERE username = ?",
                    username
            );
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> findUserById(Long userId) {
        try {
            return jdbcTemplate.queryForMap(
                    "SELECT id, username, role, department_id, team_id FROM users WHERE id = ? AND deleted = 0",
                    userId
            );
        } catch (Exception e) {
            return null;
        }
    }
}

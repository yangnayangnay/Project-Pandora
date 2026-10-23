package com.pandora.backend.auth;

import com.pandora.backend.common.ApiResponse;
import com.pandora.backend.common.BusinessException;

import com.pandora.backend.common.ErrorCode;
import com.pandora.backend.audit.Auditable;
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

    @Autowired
    private com.pandora.backend.audit.AuditService auditService;


    @PostMapping("/register")
    @Auditable(operationType = "USER_REGISTER", targetType = "USER")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(@RequestBody Map<String, Object> request) {
        String username = (String) request.get("username");
        String password = (String) request.get("password");
        String role = (String) request.get("role");
        String email = (String) request.get("email");
        String realName = (String) request.get("realName");
        String idCard = (String) request.get("idCard");
        String phone = (String) request.get("phone");

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

        if (email != null && !email.trim().isEmpty()) {
            if (!isValidEmail(email)) {
                throw new BusinessException(ErrorCode.INVALID_EMAIL_FORMAT);
            }
            if (findUserByEmail(email) != null) {
                throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }
        }

        if (idCard != null && !idCard.trim().isEmpty()) {
            if (!isValidIdCard(idCard)) {
                throw new BusinessException(ErrorCode.INVALID_ID_CARD);
            }
            try {
                jdbcTemplate.queryForMap("SELECT id FROM users WHERE id_card = ? AND deleted = 0", idCard);
                throw new BusinessException(ErrorCode.ID_CARD_ALREADY_EXISTS);
            } catch (BusinessException e) {
                throw e;
            } catch (Exception ignored) {
            }
        }

        String approvalStatus = "PENDING";
        String matchedRole = role;
        Long matchedDeptId = null;
        Long matchedTeamId = null;
        Long matchedEmpRecordId = null;

        if (realName != null && idCard != null) {
            Map<String, Object> empRecord = findEmployeeRecord(realName, idCard);
            if (empRecord != null) {
                String empStatus = (String) empRecord.get("status");
                if ("PENDING".equals(empStatus)) {
                    approvalStatus = "APPROVED";
                    matchedRole = (String) empRecord.get("role");
                    matchedEmpRecordId = ((Number) empRecord.get("id")).longValue();
                    if (empRecord.get("department_id") != null) {
                        matchedDeptId = ((Number) empRecord.get("department_id")).longValue();
                    }
                    if (empRecord.get("team_id") != null) {
                        matchedTeamId = ((Number) empRecord.get("team_id")).longValue();
                    }
                    if (email == null && empRecord.get("email") != null) {
                        email = (String) empRecord.get("email");
                    }
                    if (phone == null && empRecord.get("phone") != null) {
                        phone = (String) empRecord.get("phone");
                    }
                } else if ("REGISTERED".equals(empStatus)) {
                    throw new BusinessException(ErrorCode.EMPLOYEE_RECORD_ALREADY_REGISTERED);
                }
            }
        }

        String passwordHash = passwordEncoder.encode(password);
        jdbcTemplate.update(
                "INSERT INTO users (username, password_hash, role, email, real_name, id_card, phone, " +
                        "approval_status, department_id, team_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                username, passwordHash, matchedRole, email, realName, idCard, phone,
                approvalStatus, matchedDeptId, matchedTeamId
        );

        Map<String, Object> user = findUserByUsername(username);
        Long userId = ((Number) user.get("id")).longValue();
        Long departmentId = user.get("department_id") != null ? ((Number) user.get("department_id")).longValue() : null;
        Long teamId = user.get("team_id") != null ? ((Number) user.get("team_id")).longValue() : null;

        if (matchedEmpRecordId != null) {
            jdbcTemplate.update(
                    "UPDATE employee_records SET status = 'REGISTERED', registered_user_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                    userId, matchedEmpRecordId);
        }


        Map<String, Object> result = new HashMap<>();
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", userId);
        userInfo.put("username", username);
        userInfo.put("role", matchedRole);
        userInfo.put("departmentId", departmentId);
        userInfo.put("teamId", teamId);
        userInfo.put("approvalStatus", approvalStatus);
        userInfo.put("email", email);
        userInfo.put("realName", realName);
        result.put("user", userInfo);

        if ("APPROVED".equals(approvalStatus)) {
            String token = jwtUtil.generateToken(userId, username, matchedRole, departmentId, teamId);
            String refreshToken = jwtUtil.generateRefreshToken(userId, username);
            sessionStore.put(userId, token);
            result.put("token", token);
            result.put("refreshToken", refreshToken);
        }

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@RequestBody Map<String, String> loginRequest) {
        String usernameOrEmail = loginRequest.get("username");
        if (usernameOrEmail == null) {
            usernameOrEmail = loginRequest.get("email");
        }
        String password = loginRequest.get("password");

        if (usernameOrEmail == null || password == null) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD);
        }

        String clientType = loginRequest.get("clientType");

        Map<String, Object> user = findUserByUsername(usernameOrEmail);
        if (user == null && usernameOrEmail.contains("@")) {
            user = findUserByEmail(usernameOrEmail);
        }
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

        String userRole = (String) user.get("role");
        // Round25 决策A: 放开 ADMIN 的 App 端登录（原 APP+ADMIN 互斥 403 分支已删除），
        // Web 端分流（非 APP 端仅 ADMIN 可登录）保持不变
        if (!"APP".equals(clientType) && !"ADMIN".equals(userRole)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        String approvalStatus = user.get("approval_status") != null ? (String) user.get("approval_status") : "APPROVED";
        if ("PENDING".equals(approvalStatus)) {
            throw new BusinessException(ErrorCode.ACCOUNT_PENDING_APPROVAL);
        }
        if ("REJECTED".equals(approvalStatus)) {
            throw new BusinessException(ErrorCode.ACCOUNT_REJECTED);
        }

        Long userId = ((Number) user.get("id")).longValue();
        String role = (String) user.get("role");
        String username = (String) user.get("username");
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
        try {
            auditService.record(userId, "logout", "USER", userId, "用户登出", "LOGOUT", "用户登出");
        } catch (Exception ignored) {}
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/refresh")
    @Auditable(operationType = "AUTH_TOKEN_REFRESH", targetType = "TOKEN")
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

    @PostMapping("/forgot-password")
    @Auditable(operationType = "PASSWORD_RESET_REQUEST", targetType = "USER")
    public ResponseEntity<ApiResponse<Map<String, Object>>> forgotPassword(@RequestBody Map<String, String> request) {
        String usernameOrEmail = request.get("username");
        if (usernameOrEmail == null || usernameOrEmail.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD);
        }
        String reason = request.get("reason");
        usernameOrEmail = usernameOrEmail.trim();

        Map<String, Object> user = findUserByUsername(usernameOrEmail);
        if (user == null && usernameOrEmail.contains("@")) {
            user = findUserByEmail(usernameOrEmail);
        }

        Long userId = null;
        String usernameForRecord = usernameOrEmail;
        if (user != null) {
            userId = ((Number) user.get("id")).longValue();
            usernameForRecord = (String) user.get("username");
            Integer pendingCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM password_reset_requests WHERE user_id = ? AND status = 'PENDING'",
                    Integer.class, userId);
            if (pendingCount != null && pendingCount > 0) {
                throw new BusinessException(ErrorCode.RESET_REQUEST_DUPLICATE);
            }
        }

        jdbcTemplate.update(
                "INSERT INTO password_reset_requests (user_id, username, request_reason, status) VALUES (?, ?, ?, 'PENDING')",
                userId, usernameForRecord, reason);

        Map<String, Object> data = new HashMap<>();
        data.put("matched", user != null);
        data.put("message", user != null
                ? "重置申请已提交，请等待管理员审批"
                : "申请已提交，若账号存在管理员将处理");
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    private boolean isValidRole(String role) {
        return role.equals("ADMIN") || role.equals("FOUNDER") || role.equals("DEPT_HEAD")
                || role.equals("TEAM_LEADER") || role.equals("EMPLOYEE");
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private boolean isValidIdCard(String idCard) {
        return idCard != null && idCard.matches("^\\d{17}[\\dXx]$");
    }

    private Map<String, Object> findUserByUsername(String username) {
        try {
            return jdbcTemplate.queryForMap(
                    "SELECT id, username, password_hash, role, email, real_name, id_card, approval_status, " +
                            "department_id, team_id, deleted FROM users WHERE username = ?",
                    username
            );
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> findUserByEmail(String email) {
        try {
            return jdbcTemplate.queryForMap(
                    "SELECT id, username, password_hash, role, email, real_name, id_card, approval_status, " +
                            "department_id, team_id, deleted FROM users WHERE email = ?",
                    email
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

    private Map<String, Object> findEmployeeRecord(String realName, String idCard) {
        try {
            return jdbcTemplate.queryForMap(
                    "SELECT id, real_name, id_card, email, phone, department_id, team_id, role, status " +
                            "FROM employee_records WHERE real_name = ? AND id_card = ? AND deleted = 0",
                    realName, idCard
            );
        } catch (Exception e) {
            return null;
        }
    }
}

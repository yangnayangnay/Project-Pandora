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

/**
 * 认证控制器——处理用户注册、登录、登出、令牌刷新及密码重置等身份管理接口。
 * 该控制器是系统安全入口，所有与身份认证相关的 HTTP 请求均由此类处理。
 * 被前端（Web/App）调用，依赖 JwtUtil 进行令牌操作、JdbcTemplate 访问数据库。
 */
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


    /**
     * 用户注册接口。
     * 支持自助注册和员工记录匹配两种模式。若匹配到待审批的员工记录，则自动激活账号。
     * 业务规则：
     * - 密码最小长度为 6 位（系统安全策略）
     * - 默认角色为 EMPLOYEE，仅允许业务角色
     * - 用户名、邮箱、身份证号需全局唯一
     *
     * @param request 包含 username、password、role、email、realName、idCard、phone 的注册表单
     * @return 包含用户信息和令牌（若审批通过）的响应
     */
    @PostMapping("/register")
    @org.springframework.transaction.annotation.Transactional
    @Auditable(operationType = "USER_REGISTER", targetType = "USER")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(@RequestBody Map<String, Object> request) {
        String username = (String) request.get("username");
        String password = (String) request.get("password");
        String role = (String) request.get("role");
        String email = (String) request.get("email");
        String realName = (String) request.get("realName");
        String idCard = (String) request.get("idCard");
        String phone = (String) request.get("phone");

        // 拦截必填字段缺失的非法请求
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD);
        }

        // 密码长度校验：系统安全策略要求至少 6 位
        if (password.length() < 6) {
            throw new BusinessException(ErrorCode.PASSWORD_TOO_SHORT);
        }

        // 角色为空时默认为普通员工
        if (role == null || role.trim().isEmpty()) {
            role = "EMPLOYEE";
        }

        // 拦截非业务角色的注册请求（如 ADMIN 不能直接注册）
        if (!isValidRole(role)) {
            throw new BusinessException(ErrorCode.INVALID_ROLE);
        }

        // 拦截用户名重复的注册请求
        if (findUserByUsername(username) != null) {
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        // 邮箱格式校验与唯一性检查
        if (email != null && !email.trim().isEmpty()) {
            if (!isValidEmail(email)) {
                throw new BusinessException(ErrorCode.INVALID_EMAIL_FORMAT);
            }
            if (findUserByEmail(email) != null) {
                throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }
        }

        // 身份证号格式校验与唯一性检查（中国身份证：18位，末位可为X）
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

        // 默认审批状态为待审批，若匹配到待激活员工记录则自动审批
        String approvalStatus = "PENDING";
        String matchedRole = role;
        Long matchedDeptId = null;
        Long matchedTeamId = null;
        Long matchedEmpRecordId = null;

        // 尝试匹配员工记录：实名+身份证匹配则自动激活
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

        // 学习备注: 此处逻辑待确认——matchedRole 来自员工记录，可能不是业务角色，但后续再次校验
        if (!isValidRole(matchedRole)) throw new BusinessException(ErrorCode.INVALID_ROLE);
        String passwordHash = passwordEncoder.encode(password);
        // 非业务角色不分配部门/团队
        if (!com.pandora.backend.user.OrganizationRules.hasDepartment(matchedRole)) matchedDeptId = null;
        if (!com.pandora.backend.user.OrganizationRules.hasTeam(matchedRole)) matchedTeamId = null;
        var relationships = new com.pandora.backend.user.OrganizationHierarchy(jdbcTemplate);
        var before = relationships.snapshot();
        // 写入用户表：users 表
        jdbcTemplate.update(
                "INSERT INTO users (username, password_hash, role, email, real_name, id_card, phone, " +
                        "approval_status, department_id, team_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                username, passwordHash, matchedRole, email, realName, idCard, phone,
                approvalStatus, matchedDeptId, matchedTeamId
        );

        Map<String, Object> user = findUserByUsername(username);
        com.pandora.backend.user.OrganizationRules.normalize(user);
        Long userId = ((Number) user.get("id")).longValue();
        // 已审批的业务角色：同步组织层级关系
        if ("APPROVED".equals(approvalStatus) && com.pandora.backend.user.OrganizationRules.isBusinessRole(matchedRole))
            relationships.synchronize(before, userId);
        Long departmentId = user.get("department_id") != null ? ((Number) user.get("department_id")).longValue() : null;
        Long teamId = user.get("team_id") != null ? ((Number) user.get("team_id")).longValue() : null;

        // 更新员工记录状态为已注册
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

        // 审批通过后自动签发令牌，用户无需再次登录
        if ("APPROVED".equals(approvalStatus)) {
            String token = jwtUtil.generateToken(userId, username, matchedRole, departmentId, teamId);
            String refreshToken = jwtUtil.generateRefreshToken(userId, username);
            sessionStore.put(userId, token);
            result.put("token", token);
            result.put("refreshToken", refreshToken);
        }

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 用户登录接口。
     * 支持 Web 端（用户名/邮箱）和 App 端（工号）两种登录方式。
     * 业务规则：
     * - 管理员仅可登录 Web 端
     * - 普通员工仅可登录 App 端
     * - 待审批/已拒绝账号禁止登录
     *
     * @param loginRequest 包含 username/email/employeeNo、password、clientType 的登录表单
     * @return 包含用户信息和令牌的响应
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@RequestBody Map<String, String> loginRequest) {
        String clientType = loginRequest.getOrDefault("clientType", "WEB");
        String usernameOrEmail = loginRequest.get("username");
        // App 端兼容字段：工号优先
        if ("APP".equals(clientType)) {
            String employeeNo = loginRequest.get("employeeNo");
            if (employeeNo != null) {
                if (usernameOrEmail != null && !employeeNo.trim().equals(usernameOrEmail.trim()))
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "工号与兼容账号字段不一致");
                usernameOrEmail = employeeNo;
            }
            if (usernameOrEmail != null) usernameOrEmail = usernameOrEmail.trim();
        } else if (usernameOrEmail == null) {
            usernameOrEmail = loginRequest.get("email");
        }
        String password = loginRequest.get("password");

        // 拦截必填字段缺失的登录请求
        if (usernameOrEmail == null || password == null || "APP".equals(clientType) && usernameOrEmail.isEmpty()) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD);
        }

        // 先按用户名查询，Web 端支持邮箱登录
        Map<String, Object> user = findUserByUsername(usernameOrEmail);
        if (user == null && !"APP".equals(clientType) && usernameOrEmail.contains("@")) {
            user = findUserByEmail(usernameOrEmail);
        }
        if (user == null) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS,
                    "APP".equals(clientType) ? "工号或密码错误" : ErrorCode.INVALID_CREDENTIALS.getMessage());
        }

        // 密码校验：BCrypt 匹配
        String passwordHash = (String) user.get("password_hash");
        if (!passwordEncoder.matches(password, passwordHash)) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS,
                    "APP".equals(clientType) ? "工号或密码错误" : ErrorCode.INVALID_CREDENTIALS.getMessage());
        }

        // 拦截已删除账号（deleted = 1 表示软删除）
        if (((Number) user.get("deleted")).intValue() == 1) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        String userRole = (String) user.get("role");
        // 客户端类型校验：仅允许 WEB 或 APP
        if (!"APP".equals(clientType) && !"WEB".equals(clientType)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "登录端类型无效");
        }
        // 角色与客户端匹配：管理员仅可登录 Web 端
        if ("APP".equals(clientType) && "ADMIN".equals(userRole)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "管理员仅可登录管理员 Web 端");
        }
        // 普通员工不可登录 Web 管理端
        if ("WEB".equals(clientType) && !"ADMIN".equals(userRole)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "管理端仅允许管理员登录");
        }

        com.pandora.backend.user.OrganizationRules.normalize(user);
        String approvalStatus = user.get("approval_status") != null ? (String) user.get("approval_status") : "APPROVED";
        // 拦截待审批账号
        if ("PENDING".equals(approvalStatus)) {
            throw new BusinessException(ErrorCode.ACCOUNT_PENDING_APPROVAL);
        }
        // 拦截已拒绝账号
        if ("REJECTED".equals(approvalStatus)) {
            throw new BusinessException(ErrorCode.ACCOUNT_REJECTED);
        }

        Long userId = ((Number) user.get("id")).longValue();
        String role = (String) user.get("role");
        String username = (String) user.get("username");
        Long departmentId = user.get("department_id") != null ? ((Number) user.get("department_id")).longValue() : null;
        Long teamId = user.get("team_id") != null ? ((Number) user.get("team_id")).longValue() : null;

        // 生成访问令牌和刷新令牌
        String token = jwtUtil.generateToken(userId, username, role, departmentId, teamId);
        String refreshToken = jwtUtil.generateRefreshToken(userId, username);

        // 会话存储：用于单点登录控制
        sessionStore.put(userId, token);


        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("refreshToken", refreshToken);
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", userId);
        userInfo.put("username", username);
        userInfo.put("role", role);
        if ("APP".equals(clientType)) userInfo.put("employeeNo", username);
        userInfo.put("departmentId", departmentId);
        userInfo.put("teamId", teamId);
        result.put("user", userInfo);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 用户登出接口。
     * 从会话存储中移除令牌，并记录审计日志。
     *
     * @param authHeader Authorization 请求头，格式为 "Bearer {token}"
     * @return 成功响应
     */
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

    /**
     * 令牌刷新接口。
     * 使用有效的刷新令牌获取新的访问令牌。
     *
     * @param request 包含 refreshToken 的请求体
     * @return 新的访问令牌
     */
    @PostMapping("/refresh")
    @Auditable(operationType = "AUTH_TOKEN_REFRESH", targetType = "TOKEN")
    public ResponseEntity<ApiResponse<Map<String, String>>> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        // 拦截无效的刷新令牌
        if (refreshToken == null || !jwtUtil.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        String username = jwtUtil.getUsernameFromToken(refreshToken);

        Map<String, Object> user = findUserById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        com.pandora.backend.user.OrganizationRules.normalize(user);
        String role = (String) user.get("role");
        Long departmentId = user.get("department_id") != null ? ((Number) user.get("department_id")).longValue() : null;
        Long teamId = user.get("team_id") != null ? ((Number) user.get("team_id")).longValue() : null;

        String newToken = jwtUtil.generateToken(userId, username, role, departmentId, teamId);

        Map<String, String> result = new HashMap<>();
        result.put("token", newToken);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 忘记密码接口。
     * 提交重置申请，管理员审批后生效。
     * 防重逻辑：同一用户不能有多个待处理的重置请求。
     *
     * @param request 包含 username 和可选 reason 的请求体
     * @return 包含处理结果的响应
     */
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
            // 检查是否存在待处理的重置请求，防止重复提交
            Integer pendingCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM password_reset_requests WHERE user_id = ? AND status = 'PENDING'",
                    Integer.class, userId);
            if (pendingCount != null && pendingCount > 0) {
                throw new BusinessException(ErrorCode.RESET_REQUEST_DUPLICATE);
            }
        }

        // 写入密码重置请求表（无论用户是否存在，防止枚举攻击）
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

    /**
     * 校验角色是否为业务角色。
     * 委托给 OrganizationRules 进行校验。
     */
    private boolean isValidRole(String role) {
        return role != null && com.pandora.backend.user.OrganizationRules.isBusinessRole(role);
    }

    /**
     * 校验邮箱格式。
     * 使用正则表达式验证标准邮箱格式。
     */
    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    /**
     * 校验身份证号格式。
     * 中国身份证：18位，末位可为 X/x。
     */
    private boolean isValidIdCard(String idCard) {
        return idCard != null && idCard.matches("^\\d{17}[\\dXx]$");
    }

    /**
     * 按用户名查询用户。
     * 查询 users 表，返回完整用户信息。
     */
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

    /**
     * 按邮箱查询用户。
     * 用于邮箱登录和唯一性校验。
     */
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

    /**
     * 按ID查询用户。
     * 仅返回基本信息，用于令牌刷新等场景。
     */
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

    /**
     * 查询员工记录。
     * 用于注册时匹配待激活的员工档案。
     */
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
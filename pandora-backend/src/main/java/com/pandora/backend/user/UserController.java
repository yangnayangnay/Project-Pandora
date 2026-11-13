package com.pandora.backend.user;

import com.pandora.backend.common.ApiResponse;
import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.ErrorCode;
import com.pandora.backend.common.FileStorageService;
import com.pandora.backend.common.RoleChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.pandora.backend.audit.Auditable;

import java.util.*;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private AccountCreationService accountCreationService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private com.pandora.backend.hierarchy.HierarchyService hierarchyService;

    // Round28 REQ-28-12: 个人信息变更同步失效个人十大 AI 点评缓存
    @Autowired
    private com.pandora.backend.view.PersonalTop10Generator personalTop10Generator;

    private static final Pattern EMAIL_REGEX = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_REGEX = Pattern.compile("^1[3-9]\\d{9}$");

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser() {
        Long userId = getCurrentUserId();
        try {
            // Round26 观察F: LEFT JOIN 补部门/团队名称（悬空/无归属返回 NULL，App 端显示占位）
            Map<String, Object> user = jdbcTemplate.queryForMap(
                    "SELECT u.id AS \"id\", u.username AS \"username\", u.role AS \"role\", u.email AS \"email\", " +
                    "u.real_name AS \"realName\", u.phone AS \"phone\", u.department_id AS \"departmentId\", " +
                    "u.team_id AS \"teamId\", d.name AS \"departmentName\", t.name AS \"teamName\", " +
                    "u.avatar AS \"avatar\", u.signature AS \"signature\", u.theme_mode AS \"themeMode\", u.manual_theme AS \"manualTheme\" " +
                    "FROM users u " +
                    "LEFT JOIN departments d ON u.department_id = d.id AND d.deleted = 0 " +
                    "LEFT JOIN teams t ON u.team_id = t.id AND t.deleted = 0 " +
                    "WHERE u.id = ? AND u.deleted = 0", userId);
            return ResponseEntity.ok(ApiResponse.success(OrganizationRules.normalize(user)));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long teamId) {
        StringBuilder sql = new StringBuilder("SELECT id, username, role, real_name, email, phone, department_id, team_id, avatar, approval_status FROM users WHERE deleted = 0");
        List<Object> params = new ArrayList<>();
        if (!"ADMIN".equals(getCurrentUserRole())) sql.append(" AND role <> 'ADMIN'");

        if (role != null) {
            sql.append(" AND role = ?");
            params.add(role);
        }
        if (departmentId != null) {
            sql.append(" AND role <> 'FOUNDER' AND department_id = ?");
            params.add(departmentId);
        }
        if (teamId != null) {
            sql.append(" AND role NOT IN ('FOUNDER','DEPT_HEAD') AND team_id = ?");
            params.add(teamId);
        }
        sql.append(" ORDER BY username");

        List<Map<String, Object>> users = jdbcTemplate.queryForList(sql.toString(), params.toArray());
        return ResponseEntity.ok(ApiResponse.success(OrganizationRules.normalize(users)));
    }

    @GetMapping("/{userId}/panel")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserPanel(@PathVariable Long userId) {
        try {
            Map<String, Object> user = jdbcTemplate.queryForMap(
                    "SELECT id, username, role, department_id, team_id, avatar FROM users WHERE id = ? AND deleted = 0", userId);
            if ("ADMIN".equals(user.get("role")) && !"ADMIN".equals(getCurrentUserRole())) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND);
            }

            List<Map<String, Object>> tasks = jdbcTemplate.queryForList(
                    "SELECT * FROM tasks WHERE assignee_id = ? AND deleted = 0 ORDER BY created_at DESC LIMIT 20", userId);

            List<Map<String, Object>> logs = jdbcTemplate.queryForList(
                    "SELECT * FROM work_logs WHERE user_id = ? AND deleted = 0 ORDER BY log_date DESC LIMIT 20", userId);

            List<Map<String, Object>> top10 = jdbcTemplate.queryForList(
                    "SELECT * FROM top10_works WHERE user_id = ? AND deleted = 0 ORDER BY rank_order", userId);

            Map<String, Object> panel = new HashMap<>();
            panel.put("user", OrganizationRules.normalize(user));
            panel.put("tasks", tasks);
            panel.put("logs", logs);
            panel.put("top10Works", top10);

            return ResponseEntity.ok(ApiResponse.success(panel));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    @PutMapping("/me")
    @Transactional
    @Auditable(operationType = "USER_PROFILE_UPDATE", targetType = "USER")
    public ResponseEntity<ApiResponse<Void>> updateProfile(@RequestBody Map<String, Object> request) {
        Long userId = getCurrentUserId();
        // APP accounts are maintained by administrators. Reject the entire request before any write.
        if (!RoleChecker.isAdminOnly(getCurrentUserRole())
                && request.keySet().stream().anyMatch(key -> !Set.of("avatar", "signature").contains(key))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "真实姓名、邮箱、工号、手机号及密码由管理员统一维护，不能自行修改");
        }
        String username = (String) request.get("username");
        String avatar = (String) request.get("avatar");
        String email = (String) request.get("email");
        String phone = (String) request.get("phone");
        String realName = (String) request.get("realName");
        Object signatureRaw = request.get("signature");
        if (signatureRaw != null && !(signatureRaw instanceof String)) throw new BusinessException(ErrorCode.BAD_REQUEST, "个人签名格式无效");
        String signature = signatureRaw == null ? null : signatureRaw.toString().replace("\r\n", "\n").replace('\r', '\n')
                .replace('\u0085', '\n').replace('\u2028', '\n').replace('\u2029', '\n');
        if (signature != null && signature.chars().filter(ch -> ch == '\n').count() > 3)
            throw new BusinessException(ErrorCode.BAD_REQUEST, "个人签名最多 4 行");
        if (signature != null) signature = signature.trim();
        if (signature != null && signature.length() > 20) throw new BusinessException(ErrorCode.BAD_REQUEST, "个人签名最多 20 字");
        Map<String, Object> identity = jdbcTemplate.queryForMap(
                "SELECT username, real_name FROM users WHERE id = ? AND deleted = 0", userId);
        if ((username != null && !username.equals(identity.get("username")))
                || (realName != null && !realName.equals(identity.get("real_name")))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "用户名和真实姓名由组织统一维护，不能自行修改");
        }
        for (String key : request.keySet()) {
            if (!Set.of("username", "realName", "email", "phone", "avatar", "signature").contains(key)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "不能自行修改身份、职级或组织字段");
            }
        }

        if (realName != null && realName.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD, "真实姓名不能为空");
        }
        if (email != null && !email.isEmpty() && !EMAIL_REGEX.matcher(email).matches()) {
            throw new BusinessException(ErrorCode.INVALID_EMAIL_FORMAT);
        }
        if (phone != null && !phone.isEmpty() && !PHONE_REGEX.matcher(phone).matches()) {
            throw new BusinessException(ErrorCode.INVALID_PHONE_FORMAT);
        }

        if (avatar != null) {
            jdbcTemplate.update("UPDATE users SET avatar = ? WHERE id = ?", avatar, userId);
        }
        if (email != null) {
            jdbcTemplate.update("UPDATE users SET email = ? WHERE id = ?", email, userId);
        }
        if (phone != null) {
            jdbcTemplate.update("UPDATE users SET phone = ? WHERE id = ?", phone, userId);
        }
        if (signature != null) {
            jdbcTemplate.update("UPDATE users SET signature = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?", signature, userId);
        }

        // Round28 REQ-28-12: 基本资料变更后失效个人十大（下次查看按当日+新信息重新生成）
        try {
            personalTop10Generator.invalidateUser(userId);
        } catch (Exception ignored) {}

        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/me/avatar")
    @Auditable(operationType = "USER_AVATAR_UPDATE", targetType = "USER")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "source", required = false) String source) {
        Long userId = getCurrentUserId();

        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD, "请选择图片文件");
        }
        if (file.getSize() > 5L * 1024 * 1024) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("image/jpeg") && !contentType.equals("image/png") && !contentType.equals("image/webp")) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_IMAGE_FORMAT);
        }

        try {
            String avatarUrl = fileStorageService.storeAvatar(file, userId);
            jdbcTemplate.update("UPDATE users SET avatar = ? WHERE id = ?", avatarUrl, userId);
            Map<String, Object> result = new HashMap<>();
            result.put("avatarUrl", avatarUrl);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "头像上传失败: " + e.getMessage());
        }
    }

    @PutMapping("/me/password")
    @Auditable(operationType = "USER_PASSWORD_CHANGE", targetType = "USER")
    public ResponseEntity<ApiResponse<Void>> changePassword(@RequestBody Map<String, String> request) {
        Long userId = getCurrentUserId();
        if (!RoleChecker.isAdminOnly(getCurrentUserRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "密码由管理员统一维护，如需重置请提交申请或联系管理员");
        }
        String oldPassword = request.get("oldPassword");
        String newPassword = request.get("newPassword");

        if (oldPassword == null || newPassword == null) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD);
        }
        if (newPassword.length() < 6 || newPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "新密码至少6位，UTF-8长度不能超过72字节");
        }
        if (newPassword.equals(oldPassword)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "新密码不能与原密码相同");
        }

        Map<String, Object> user;
        try {
            user = jdbcTemplate.queryForMap("SELECT password_hash FROM users WHERE id = ? AND deleted = 0", userId);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        String passwordHash = (String) user.get("password_hash");
        if (!passwordEncoder.matches(oldPassword, passwordHash)) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        String newHash = passwordEncoder.encode(newPassword);
        jdbcTemplate.update("UPDATE users SET password_hash = ? WHERE id = ?", newHash, userId);

        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSettings() {
        Long userId = getCurrentUserId();
        try {
            Map<String, Object> settings = jdbcTemplate.queryForMap(
                    "SELECT show_completed_tasks AS \"showCompletedTasks\", " +
                    "show_yesterday_log AS \"showYesterdayLog\", " +
                    "birthday AS \"birthday\", last_birthday_change AS \"lastBirthdayChange\" " +
                    "FROM users WHERE id = ? AND deleted = 0", userId);
            return ResponseEntity.ok(ApiResponse.success(settings));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    @PutMapping("/settings")
    @Auditable(operationType = "USER_SETTINGS_UPDATE", targetType = "USER")
    public ResponseEntity<ApiResponse<Void>> updateSettings(@RequestBody Map<String, Object> request) {
        Long userId = getCurrentUserId();
        if (request.containsKey("showCompletedTasks")) {
            boolean showCompleted = Boolean.TRUE.equals(request.get("showCompletedTasks"));
            jdbcTemplate.update("UPDATE users SET show_completed_tasks = ? WHERE id = ?",
                    showCompleted ? 1 : 0, userId);
        }
        if (request.containsKey("showYesterdayLog")) {
            boolean showYesterday = Boolean.TRUE.equals(request.get("showYesterdayLog"));
            jdbcTemplate.update("UPDATE users SET show_yesterday_log = ? WHERE id = ?",
                    showYesterday ? 1 : 0, userId);
        }
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/relations")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserRelations() {
        Long userId = getCurrentUserId();

        List<Map<String, Object>> superiorMaps = hierarchyService.getSuperiors(userId);
        List<Map<String, Object>> subordinateMaps = hierarchyService.getSubordinates(userId);

        List<Map<String, Object>> superiors = new ArrayList<>();
        for (Map<String, Object> s : superiorMaps) {
            Map<String, Object> person = enrichRelationItem(s);
            if (isOrganizationPerson(person)) superiors.add(person);
        }
        List<Map<String, Object>> subordinates = new ArrayList<>();
        for (Map<String, Object> s : subordinateMaps) {
            Map<String, Object> person = enrichRelationItem(s);
            if (isOrganizationPerson(person)) subordinates.add(person);
        }

        superiors.sort((a, b) -> Integer.compare(permLevel((String) b.get("role")), permLevel((String) a.get("role"))));
        subordinates.sort((a, b) -> Integer.compare(permLevel((String) b.get("role")), permLevel((String) a.get("role"))));

        Map<String, Object> result = new HashMap<>();
        result.put("superiors", superiors);
        result.put("subordinates", subordinates);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private boolean isOrganizationPerson(Map<String, Object> person) {
        return !"ADMIN".equals(person.get("role")) && ("FOUNDER".equals(person.get("role"))
                || person.get("departmentId") != null && !Objects.toString(person.get("departmentName"), "").isBlank());
    }

    private Map<String, Object> enrichRelationItem(Map<String, Object> hierarchyEntry) {
        Long userId = toLong(hierarchyEntry.get("id"));
        Map<String, Object> item = new HashMap<>();
        item.put("id", userId);
        item.put("username", hierarchyEntry.get("username") != null ? hierarchyEntry.get("username") : hierarchyEntry.get("USERNAME"));
        item.put("realName", hierarchyEntry.get("real_name") != null ? hierarchyEntry.get("real_name") : hierarchyEntry.get("REAL_NAME"));
        item.put("role", hierarchyEntry.get("role") != null ? hierarchyEntry.get("role") : hierarchyEntry.get("ROLE"));
        item.put("permissionLevel", permLevel((String) item.get("role")));
        Object depthObj = hierarchyEntry.get("depth");
        if (depthObj != null) {
            item.put("depth", depthObj);
        } else {
            item.put("depth", 0);
        }

        if (userId != null) {
            try {
                Map<String, Object> user = jdbcTemplate.queryForMap(
                        "SELECT u.avatar, d.id AS department_id, t.id AS team_id FROM users u " +
                        "LEFT JOIN departments d ON d.id = u.department_id AND d.deleted = 0 " +
                        "LEFT JOIN teams t ON t.id = u.team_id AND t.deleted = 0 " +
                        "WHERE u.id = ? AND u.deleted = 0", userId);
                Long dId = toLong(user.get("department_id"));
                Long tId = toLong(user.get("team_id"));
                item.put("avatar", user.get("avatar"));
                item.put("departmentId", dId);
                item.put("teamId", tId);
                item.put("departmentName", dId != null ? getDeptName(dId) : "");
                item.put("teamName", tId != null ? getTeamName(tId) : "");
            } catch (Exception e) {
                item.put("departmentName", "");
                item.put("teamName", "");
            }
        } else {
            item.put("departmentName", "");
            item.put("teamName", "");
        }
        return OrganizationRules.normalize(item);
    }

    private String getDeptName(Long deptId) {
        try {
            Map<String, Object> dept = jdbcTemplate.queryForMap(
                    "SELECT name FROM departments WHERE id = ? AND deleted = 0", deptId);
            Object name = dept.get("name");
            return name != null ? name.toString() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private String getTeamName(Long teamId) {
        try {
            Map<String, Object> team = jdbcTemplate.queryForMap(
                    "SELECT name FROM teams WHERE id = ? AND deleted = 0", teamId);
            Object name = team.get("name");
            return name != null ? name.toString() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private int permLevel(String role) {
        if (role == null) return -1;
        switch (role) {
            case "FOUNDER": return 4;
            case "DEPT_HEAD": return 3;
            case "TEAM_LEADER": return 2;
            case "EMPLOYEE": return 1;
            case "ADMIN": return 0;
            default: return -1;
        }
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        try { return Long.parseLong(val.toString()); } catch (Exception e) { return null; }
    }


    /**
     * R40 REQ-40-C: 部门/团队归属选项（调动界面下拉数据源，ADMIN 独占）。
     */
    @GetMapping("/org-options")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrgOptions() {
        String role = getCurrentUserRole();
        if (!RoleChecker.isAdminOnly(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(403, "无权操作"));
        }
        List<Map<String, Object>> departments = jdbcTemplate.queryForList(
                "SELECT id, name FROM departments ORDER BY id");
        List<Map<String, Object>> teams = jdbcTemplate.queryForList(
                "SELECT id, name, department_id FROM teams ORDER BY id");
        Map<String, Object> result = new HashMap<>();
        result.put("departments", departments);
        result.put("teams", teams);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * R40 REQ-40-C: 人员归属调动（部门/团队，ADMIN 独占）。
     * 直接更新 users 表 department_id/team_id（user_org_affiliation 为零引用历史设计，不启用）。
     */
    @PutMapping("/{userId}/org")
    @Transactional
    @Auditable(operationType = "USER_ORG_TRANSFER", targetType = "USER")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateUserOrg(
            @PathVariable Long userId, @RequestBody Map<String, Object> request) {
        String role = getCurrentUserRole();
        if (!RoleChecker.isAdminOnly(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(403, "无权操作"));
        }
        List<String> targetRoles = jdbcTemplate.queryForList("SELECT role FROM users WHERE id=? AND deleted=0", String.class, userId);
        if (targetRoles.isEmpty()) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        String targetRole = targetRoles.get(0);
        if (!OrganizationRules.isBusinessRole(targetRole))
            throw new BusinessException(ErrorCode.BAD_REQUEST, "管理员账号不参与公司岗位调整");
        Long departmentId = OrganizationRules.hasDepartment(targetRole) && request.get("departmentId") != null
                ? Long.valueOf(request.get("departmentId").toString()) : null;
        Long teamId = OrganizationRules.hasTeam(targetRole) && request.get("teamId") != null
                ? Long.valueOf(request.get("teamId").toString()) : null;
        if (OrganizationRules.hasDepartment(targetRole) && (departmentId == null || departmentId <= 0 || jdbcTemplate.queryForList(
                "SELECT id FROM departments WHERE id=? AND deleted=0", Long.class, departmentId).isEmpty())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "必须选择有效部门，不能清空人员部门归属");
        }
        if (OrganizationRules.hasTeam(targetRole) && teamId == null)
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请为团队负责人或员工选择团队");
        // 团队负责人和员工需选择本部门团队；创始人和部门负责人不保留团队。
        if (teamId != null) {
            if (jdbcTemplate.queryForList("SELECT id FROM teams WHERE id=? AND department_id=? AND deleted=0",
                    Long.class, teamId, departmentId).isEmpty()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "团队与部门不匹配");
            }
        }
        var relationships = new OrganizationHierarchy(jdbcTemplate);
        var before = relationships.snapshot();
        jdbcTemplate.update("UPDATE users SET department_id = ?, team_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND deleted = 0",
                departmentId, teamId, userId);
        relationships.synchronize(before, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("departmentId", departmentId);
        result.put("teamId", teamId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Round19 REQ-19-06: 管理员独占的账号创建接口。
     * 仅 ADMIN 角色可调用，非 ADMIN 返回 403。
     * 密码经 BCryptPasswordEncoder 加密存储，approval_status='APPROVED'，deleted=0。
     */
    @PostMapping("/create")
    @Auditable(operationType = "USER_CREATE", targetType = "USER")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createUser(@RequestBody Map<String, Object> request) {
        // 权限校验：仅 ADMIN 独占
        String role = getCurrentUserRole();
        if (!RoleChecker.isAdminOnly(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(403, "无权操作"));
        }

        return ResponseEntity.ok(ApiResponse.success(accountCreationService.create(request)));
    }

    @PostMapping("/rename-batch")
    @Auditable(operationType = "USER_BATCH_RENAME", targetType = "USER")
    public ResponseEntity<ApiResponse<Map<String, Object>>> renameBatch() {
        String role = getCurrentUserRole();
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(403, "仅管理员可执行批量重命名"));
        }

        List<Map<String, Object>> users = jdbcTemplate.queryForList(
                "SELECT id, username, role FROM users WHERE deleted = 0 AND approval_status = 'APPROVED' ORDER BY id");

        Map<String, String> renameMap = new LinkedHashMap<>();
        int renamed = 0;
        int skipped = 0;

        for (Map<String, Object> user : users) {
            Long id = ((Number) user.get("id")).longValue();
            String username = (String) user.get("username");
            String userRole = (String) user.get("role");

            int suffix;
            switch (userRole) {
                case "ADMIN": suffix = 0; break;
                case "FOUNDER": suffix = 1; break;
                case "DEPT_HEAD": suffix = 2; break;
                case "TEAM_LEADER": suffix = 3; break;
                case "EMPLOYEE": suffix = 4; break;
                default: suffix = 9; break;
            }

            String newName = username + suffix;
            if (newName.equals(username)) continue;

            Integer existing = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users WHERE username = ? AND id != ?",
                    Integer.class, newName, id);
            if (existing != null && existing > 0) {
                skipped++;
                continue;
            }

            jdbcTemplate.update("UPDATE users SET username = ? WHERE id = ?", newName, id);
            renameMap.put(username, newName);
            renamed++;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("renamed", renamed);
        result.put("skipped", skipped);
        result.put("mapping", renameMap);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }

    private String getCurrentUserRole() {
        Long userId = getCurrentUserId();
        try {
            Map<String, Object> user = jdbcTemplate.queryForMap("SELECT role FROM users WHERE id = ?", userId);
            return (String) user.get("role");
        } catch (Exception e) {
            return null;
        }
    }
}

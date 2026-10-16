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
    private FileStorageService fileStorageService;

    @Autowired
    private com.pandora.backend.hierarchy.HierarchyService hierarchyService;

    private static final Pattern EMAIL_REGEX = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_REGEX = Pattern.compile("^1[3-9]\\d{9}$");

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser() {
        Long userId = getCurrentUserId();
        try {
            Map<String, Object> user = jdbcTemplate.queryForMap(
                    "SELECT id AS \"id\", username AS \"username\", role AS \"role\", email AS \"email\", " +
                    "real_name AS \"realName\", phone AS \"phone\", department_id AS \"departmentId\", " +
                    "team_id AS \"teamId\", avatar AS \"avatar\", theme_mode AS \"themeMode\", manual_theme AS \"manualTheme\" " +
                    "FROM users WHERE id = ? AND deleted = 0", userId);
            return ResponseEntity.ok(ApiResponse.success(user));
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

        if (role != null) {
            sql.append(" AND role = ?");
            params.add(role);
        }
        if (departmentId != null) {
            sql.append(" AND department_id = ?");
            params.add(departmentId);
        }
        if (teamId != null) {
            sql.append(" AND team_id = ?");
            params.add(teamId);
        }
        sql.append(" ORDER BY username");

        List<Map<String, Object>> users = jdbcTemplate.queryForList(sql.toString(), params.toArray());
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/{userId}/panel")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserPanel(@PathVariable Long userId) {
        try {
            Map<String, Object> user = jdbcTemplate.queryForMap(
                    "SELECT id, username, role, department_id, team_id, avatar FROM users WHERE id = ? AND deleted = 0", userId);

            List<Map<String, Object>> tasks = jdbcTemplate.queryForList(
                    "SELECT * FROM tasks WHERE assignee_id = ? AND deleted = 0 ORDER BY created_at DESC LIMIT 20", userId);

            List<Map<String, Object>> logs = jdbcTemplate.queryForList(
                    "SELECT * FROM work_logs WHERE user_id = ? AND deleted = 0 ORDER BY log_date DESC LIMIT 20", userId);

            List<Map<String, Object>> top10 = jdbcTemplate.queryForList(
                    "SELECT * FROM top10_works WHERE user_id = ? AND deleted = 0 ORDER BY rank_order", userId);

            Map<String, Object> panel = new HashMap<>();
            panel.put("user", user);
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
        String username = (String) request.get("username");
        String avatar = (String) request.get("avatar");
        String email = (String) request.get("email");
        String phone = (String) request.get("phone");
        String realName = (String) request.get("realName");

        if (realName != null && realName.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD, "真实姓名不能为空");
        }
        if (email != null && !email.isEmpty() && !EMAIL_REGEX.matcher(email).matches()) {
            throw new BusinessException(ErrorCode.INVALID_EMAIL_FORMAT);
        }
        if (phone != null && !phone.isEmpty() && !PHONE_REGEX.matcher(phone).matches()) {
            throw new BusinessException(ErrorCode.INVALID_PHONE_FORMAT);
        }

        if (username != null) {
            jdbcTemplate.update("UPDATE users SET username = ? WHERE id = ?", username, userId);
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
        if (realName != null) {
            jdbcTemplate.update("UPDATE users SET real_name = ? WHERE id = ?", realName, userId);
        }

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
        if (file.getSize() > 2 * 1024 * 1024) {
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
        String oldPassword = request.get("oldPassword");
        String newPassword = request.get("newPassword");

        if (oldPassword == null || newPassword == null) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD);
        }
        if (newPassword.length() < 6) {
            throw new BusinessException(ErrorCode.PASSWORD_TOO_SHORT);
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

    @PutMapping("/birthday")
    @Auditable(operationType = "USER_BIRTHDAY_UPDATE", targetType = "USER")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateBirthday(@RequestBody Map<String, Object> request) {
        Long userId = getCurrentUserId();
        String newBirthday = (String) request.get("birthday");

        Map<String, Object> user;
        try {
            user = jdbcTemplate.queryForMap(
                    "SELECT birthday, last_birthday_change FROM users WHERE id = ? AND deleted = 0", userId);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        Object currentBirthdayObj = user.get("birthday");
        Object lastChangeObj = user.get("last_birthday_change");

        String currentBirthday = currentBirthdayObj != null ? currentBirthdayObj.toString() : null;

        if (newBirthday != null && newBirthday.equals(currentBirthday)) {
            Map<String, Object> result = new HashMap<>();
            result.put("birthday", newBirthday);
            result.put("updated", false);
            result.put("message", "生日未变化");
            return ResponseEntity.ok(ApiResponse.success(result));
        }

        if (currentBirthday != null && lastChangeObj != null) {
            java.sql.Timestamp lastChange = (java.sql.Timestamp) lastChangeObj;
            long millisSinceLastChange = System.currentTimeMillis() - lastChange.getTime();
            long daysSinceLastChange = millisSinceLastChange / (1000L * 60 * 60 * 24);
            if (daysSinceLastChange < 365) {
                long remainingDays = 365 - daysSinceLastChange;
                throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                        "距上次修改不足365天，还需等待" + remainingDays + "天");
            }
        }

        jdbcTemplate.update("UPDATE users SET birthday = ?, last_birthday_change = CURRENT_TIMESTAMP WHERE id = ?",
                newBirthday, userId);

        Map<String, Object> result = new HashMap<>();
        result.put("birthday", newBirthday);
        result.put("updated", true);
        return ResponseEntity.ok(ApiResponse.success(result));
    }


    @GetMapping("/relations")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserRelations() {
        Long userId = getCurrentUserId();

        List<Map<String, Object>> superiorMaps = hierarchyService.getSuperiors(userId);
        List<Map<String, Object>> subordinateMaps = hierarchyService.getSubordinates(userId);

        List<Map<String, Object>> superiors = new ArrayList<>();
        for (Map<String, Object> s : superiorMaps) {
            superiors.add(enrichRelationItem(s));
        }
        List<Map<String, Object>> subordinates = new ArrayList<>();
        for (Map<String, Object> s : subordinateMaps) {
            subordinates.add(enrichRelationItem(s));
        }

        superiors.sort((a, b) -> Integer.compare(permLevel((String) b.get("role")), permLevel((String) a.get("role"))));
        subordinates.sort((a, b) -> Integer.compare(permLevel((String) b.get("role")), permLevel((String) a.get("role"))));

        Map<String, Object> result = new HashMap<>();
        result.put("superiors", superiors);
        result.put("subordinates", subordinates);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private Map<String, Object> enrichRelationItem(Map<String, Object> hierarchyEntry) {
        Long userId = toLong(hierarchyEntry.get("id"));
        Map<String, Object> item = new HashMap<>();
        item.put("id", userId);
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
                        "SELECT department_id, team_id FROM users WHERE id = ? AND deleted = 0", userId);
                Long dId = toLong(user.get("department_id"));
                Long tId = toLong(user.get("team_id"));
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
        return item;
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

        String username = (String) request.get("username");
        String password = (String) request.get("password");
        String newRole = (String) request.get("role");
        String realName = (String) request.get("realName");
        String email = (String) request.get("email");
        String phone = (String) request.get("phone");

        // 必填字段校验
        if (username == null || username.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD, "用户名不能为空");
        }
        if (password == null || password.length() < 6) {
            throw new BusinessException(ErrorCode.PASSWORD_TOO_SHORT);
        }
        if (newRole == null || newRole.isEmpty()) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD, "角色不能为空");
        }
        // 角色合法性校验
        if (!Arrays.asList("ADMIN", "FOUNDER", "DEPT_HEAD", "TEAM_LEADER", "EMPLOYEE").contains(newRole)) {
            throw new BusinessException(ErrorCode.INVALID_ROLE);
        }
        if (realName == null || realName.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD, "真实姓名不能为空");
        }
        // 邮箱格式校验（可选字段）
        if (email != null && !email.isEmpty() && !EMAIL_REGEX.matcher(email).matches()) {
            throw new BusinessException(ErrorCode.INVALID_EMAIL_FORMAT);
        }
        // 手机号格式校验（可选字段）
        if (phone != null && !phone.isEmpty() && !PHONE_REGEX.matcher(phone).matches()) {
            throw new BusinessException(ErrorCode.INVALID_PHONE_FORMAT);
        }

        // 用户名唯一性校验
        Integer existingCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username = ? AND deleted = 0",
                Integer.class, username.trim());
        if (existingCount != null && existingCount > 0) {
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        // 密码加密后入库
        String passwordHash = passwordEncoder.encode(password);
        jdbcTemplate.update(
                "INSERT INTO users (username, password_hash, role, real_name, email, phone, approval_status, deleted) " +
                        "VALUES (?, ?, ?, ?, ?, ?, 'APPROVED', 0)",
                username.trim(), passwordHash, newRole, realName.trim(),
                (email != null && !email.isEmpty()) ? email : null,
                (phone != null && !phone.isEmpty()) ? phone : null);

        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        return ResponseEntity.ok(ApiResponse.success(result));
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
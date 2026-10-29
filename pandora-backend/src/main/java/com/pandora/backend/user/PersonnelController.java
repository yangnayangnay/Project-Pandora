package com.pandora.backend.user;

import com.pandora.backend.audit.Auditable;
import com.pandora.backend.common.*;
import com.pandora.backend.hierarchy.HierarchyService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/user")
@PreAuthorize("hasAnyRole('ADMIN','FOUNDER')")
public class PersonnelController {
    private final JdbcTemplate db;
    private final HierarchyService hierarchy;
    public PersonnelController(JdbcTemplate db, HierarchyService hierarchy) { this.db = db; this.hierarchy = hierarchy; }

    @GetMapping("/directory/options")
    public ApiResponse<Map<String, Object>> options() {
        return ApiResponse.success(Map.of(
                "departments", db.queryForList("SELECT id, name FROM departments WHERE deleted = 0 ORDER BY name"),
                "teams", db.queryForList("SELECT id, name, department_id FROM teams WHERE deleted = 0 ORDER BY name")));
    }

    @GetMapping("/directory")
    public ApiResponse<List<Map<String, Object>>> directory() {
        List<Map<String, Object>> users = db.queryForList("SELECT u.id, u.username, u.real_name, u.role, u.email, u.phone, u.id_card, " +
                "u.birthday, u.last_birthday_change, u.department_id, u.team_id, u.avatar, u.approval_status, u.theme_mode, u.manual_theme, " +
                "u.show_completed_tasks, u.show_yesterday_log, u.created_at, u.updated_at, " +
                "d.name AS department_name, t.name AS team_name FROM users u LEFT JOIN departments d ON d.id = u.department_id " +
                "LEFT JOIN teams t ON t.id = u.team_id WHERE u.deleted = 0 ORDER BY u.id");
        List<Map<String, Object>> relations = db.queryForList("SELECT h.superior_id, h.subordinate_id, COALESCE(s.real_name,s.username) AS superior_name, " +
                "COALESCE(u.real_name,u.username) AS subordinate_name FROM user_hierarchy h JOIN users s ON s.id = h.superior_id " +
                "JOIN users u ON u.id = h.subordinate_id WHERE s.deleted = 0 AND u.deleted = 0");
        for (Map<String, Object> user : users) {
            Object id = user.get("id");
            user.put("superiors", relations.stream().filter(r -> Objects.equals(r.get("subordinate_id"), id)).toList());
            user.put("subordinates", relations.stream().filter(r -> Objects.equals(r.get("superior_id"), id)).toList());
        }
        return ApiResponse.success(users);
    }

    @PutMapping("/{id}/employment")
    @Transactional
    @Auditable(operationType = "USER_EMPLOYMENT_UPDATE", targetType = "USER")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> employment(Authentication auth, @PathVariable long id, @RequestBody Map<String, Object> body) {
        String role = Objects.toString(body.get("role"), "");
        if (!List.of("ADMIN", "FOUNDER", "DEPT_HEAD", "TEAM_LEADER", "EMPLOYEE").contains(role)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择有效职级");
        }
        if (!Set.of("role", "departmentId", "teamId", "superiorId").containsAll(body.keySet())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的人员调整字段");
        }
        List<Map<String, Object>> target = db.queryForList("SELECT role FROM users WHERE id = ? AND deleted = 0 FOR UPDATE", id);
        if (target.isEmpty()) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        if (Objects.equals(auth.getPrincipal(), id) && !"ADMIN".equals(role)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能降低自己的管理员职级");
        }
        Long department = id(body.get("departmentId"));
        Long team = id(body.get("teamId"));
        Long superior = id(body.get("superiorId"));
        if (department != null && db.queryForList("SELECT id FROM departments WHERE id = ? AND deleted = 0", Long.class, department).isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "部门不存在");
        }
        if (team != null && db.queryForList("SELECT id FROM teams WHERE id = ? AND department_id = ? AND deleted = 0", Long.class, team, department).isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "团队与部门不匹配");
        }
        if (superior != null) {
            List<String> parentRoles = db.queryForList("SELECT role FROM users WHERE id = ? AND deleted = 0 AND approval_status = 'APPROVED'", String.class, superior);
            if (parentRoles.isEmpty() || rank(parentRoles.get(0)) >= rank(role) || !hierarchy.checkNoCycle(superior, id)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "上级必须职级更高，且不能形成循环关系");
            }
        }
        for (String child : db.queryForList("SELECT u.role FROM users u JOIN user_hierarchy h ON h.subordinate_id = u.id WHERE h.superior_id = ? AND u.deleted = 0", String.class, id)) {
            if (rank(child) <= rank(role)) throw new BusinessException(ErrorCode.BAD_REQUEST, "请先调整下属关系，再修改为该职级");
        }
        db.update("UPDATE users SET role = ?, department_id = ?, team_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?", role, department, team, id);
        db.update("DELETE FROM user_hierarchy WHERE subordinate_id = ?", id);
        if (superior != null) db.update("INSERT INTO user_hierarchy (superior_id, subordinate_id) VALUES (?, ?)", superior, id);
        return ApiResponse.success();
    }

    private int rank(String role) { return List.of("ADMIN", "FOUNDER", "DEPT_HEAD", "TEAM_LEADER", "EMPLOYEE").indexOf(role); }
    private Long id(Object value) {
        if (value == null || value.toString().isBlank()) return null;
        try { return Long.valueOf(value.toString()); }
        catch (NumberFormatException e) { throw new BusinessException(ErrorCode.BAD_REQUEST, "人员或组织编号格式错误"); }
    }
}

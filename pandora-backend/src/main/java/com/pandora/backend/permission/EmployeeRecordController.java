package com.pandora.backend.permission;

import com.pandora.backend.common.ApiResponse;
import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import com.pandora.backend.audit.Auditable;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/employee-records")
public class EmployeeRecordController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> list(
            @RequestParam(required = false) String status) {
        String sql = "SELECT er.*, d.name as dept_name, t.name as team_name " +
                "FROM employee_records er " +
                "LEFT JOIN departments d ON er.department_id = d.id " +
                "LEFT JOIN teams t ON er.team_id = t.id " +
                "WHERE er.deleted = 0";
        if (status != null && !status.isEmpty()) {
            sql += " AND er.status = '" + status.replace("'", "") + "'";
        }
        sql += " ORDER BY er.id";
        List<Map<String, Object>> records = jdbcTemplate.queryForList(sql);
        return ResponseEntity.ok(ApiResponse.success(records));
    }

    @PostMapping
    @Auditable(operationType = "EMPLOYEE_RECORD_CREATE", targetType = "EMPLOYEE_RECORD")
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(@RequestBody Map<String, Object> request) {
        String realName = (String) request.get("realName");
        String idCard = (String) request.get("idCard");
        String email = (String) request.get("email");
        String phone = (String) request.get("phone");
        String role = (String) request.get("role");
        String position = (String) request.get("position");
        Object deptId = request.get("departmentId");
        Object teamId = request.get("teamId");

        if (realName == null || realName.trim().isEmpty() || idCard == null || idCard.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD);
        }

        if (role == null || role.trim().isEmpty()) {
            role = "EMPLOYEE";
        }

        try {
            jdbcTemplate.queryForMap("SELECT id FROM employee_records WHERE id_card = ? AND deleted = 0", idCard);
            throw new BusinessException(ErrorCode.ID_CARD_ALREADY_EXISTS);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception ignored) {
        }

        jdbcTemplate.update(
                "INSERT INTO employee_records (real_name, id_card, email, phone, role, position, department_id, team_id, status) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PENDING')",
                realName, idCard, email, phone, role, position,
                deptId != null ? ((Number) deptId).longValue() : null,
                teamId != null ? ((Number) teamId).longValue() : null
        );

        Map<String, Object> record = jdbcTemplate.queryForMap(
                "SELECT * FROM employee_records WHERE id_card = ? AND deleted = 0", idCard);
        return ResponseEntity.ok(ApiResponse.success(record));
    }

    @PutMapping("/{id}")
    @Auditable(operationType = "EMPLOYEE_RECORD_UPDATE", targetType = "EMPLOYEE_RECORD")
    public ResponseEntity<ApiResponse<Map<String, Object>>> update(
            @PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            jdbcTemplate.queryForMap("SELECT id FROM employee_records WHERE id = ? AND deleted = 0", id);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.EMPLOYEE_RECORD_NOT_FOUND);
        }

        StringBuilder sql = new StringBuilder("UPDATE employee_records SET updated_at = CURRENT_TIMESTAMP");
        java.util.List<Object> params = new java.util.ArrayList<>();
        String[] fields = {"real_name", "id_card", "email", "phone", "role", "position", "department_id", "team_id", "status"};
        String[] requestKeys = {"realName", "idCard", "email", "phone", "role", "position", "departmentId", "teamId", "status"};

        for (int i = 0; i < fields.length; i++) {
            if (request.containsKey(requestKeys[i])) {
                sql.append(", ").append(fields[i]).append(" = ?");
                Object val = request.get(requestKeys[i]);
                if ((fields[i].equals("department_id") || fields[i].equals("team_id")) && val != null) {
                    val = ((Number) val).longValue();
                }
                params.add(val);
            }
        }
        sql.append(" WHERE id = ?");
        params.add(id);

        jdbcTemplate.update(sql.toString(), params.toArray());
        Map<String, Object> record = jdbcTemplate.queryForMap("SELECT * FROM employee_records WHERE id = ?", id);
        return ResponseEntity.ok(ApiResponse.success(record));
    }

    @DeleteMapping("/{id}")
    @Auditable(operationType = "EMPLOYEE_RECORD_DELETE", targetType = "EMPLOYEE_RECORD")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        jdbcTemplate.update("UPDATE employee_records SET deleted = 1 WHERE id = ?", id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> get(@PathVariable Long id) {
        try {
            Map<String, Object> record = jdbcTemplate.queryForMap(
                    "SELECT er.*, d.name as dept_name, t.name as team_name " +
                            "FROM employee_records er " +
                            "LEFT JOIN departments d ON er.department_id = d.id " +
                            "LEFT JOIN teams t ON er.team_id = t.id " +
                            "WHERE er.id = ? AND er.deleted = 0", id);
            return ResponseEntity.ok(ApiResponse.success(record));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.EMPLOYEE_RECORD_NOT_FOUND);
        }
    }
}
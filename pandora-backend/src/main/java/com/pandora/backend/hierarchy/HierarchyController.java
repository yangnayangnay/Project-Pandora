package com.pandora.backend.hierarchy;

import com.pandora.backend.audit.Auditable;
import com.pandora.backend.common.ApiResponse;
import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/hierarchy")
public class HierarchyController {

    @Autowired
    private HierarchyService hierarchyService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping("/relation")
    @Auditable(operationType = "HIERARCHY_ADD", targetType = "USER_HIERARCHY")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addRelation(@RequestBody Map<String, Object> request) {
        Long superiorId = Long.valueOf(request.get("superiorId").toString());
        Long subordinateId = Long.valueOf(request.get("subordinateId").toString());
        boolean forceReplace = Boolean.TRUE.equals(request.get("forceReplace"));
        Map<String, Object> result = hierarchyService.addRelation(superiorId, subordinateId, forceReplace);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @DeleteMapping("/relation")
    @Auditable(operationType = "HIERARCHY_REMOVE", targetType = "USER_HIERARCHY")
    public ResponseEntity<ApiResponse<Map<String, Object>>> removeRelation(@RequestParam Long superiorId, @RequestParam Long subordinateId) {
        hierarchyService.removeRelation(superiorId, subordinateId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("success", true)));
    }

    @GetMapping("/relations")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRelations() {
        List<Map<String, Object>> rawRelations = hierarchyService.getHierarchyTree();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> rel : rawRelations) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("superior_id", rel.get("SUPERIOR_ID") != null ? rel.get("SUPERIOR_ID") : rel.get("superior_id"));
            entry.put("subordinate_id", rel.get("SUBORDINATE_ID") != null ? rel.get("SUBORDINATE_ID") : rel.get("subordinate_id"));
            entry.put("sup_name", rel.get("SUP_NAME") != null ? rel.get("SUP_NAME") : rel.get("sup_name"));
            entry.put("sup_role", rel.get("SUP_ROLE") != null ? rel.get("SUP_ROLE") : rel.get("sup_role"));
            entry.put("sub_name", rel.get("SUB_NAME") != null ? rel.get("SUB_NAME") : rel.get("sub_name"));
            entry.put("sub_role", rel.get("SUB_ROLE") != null ? rel.get("SUB_ROLE") : rel.get("sub_role"));
            result.add(entry);
        }
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/tree")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTree() {
        List<Map<String, Object>> relations = hierarchyService.getHierarchyTree();
        List<Map<String, Object>> roots = jdbcTemplate.queryForList(
                "SELECT id, username, real_name, role FROM users WHERE deleted = 0 AND approval_status = 'APPROVED' AND id NOT IN (SELECT subordinate_id FROM user_hierarchy) ORDER BY id");
        for (Map<String, Object> root : roots) {
            root.put("children", buildChildren((Long) root.get("ID") != null ? (Long) root.get("ID") : (Long) root.get("id"), relations));
        }
        return ResponseEntity.ok(ApiResponse.success(roots));
    }

    private List<Map<String, Object>> buildChildren(Long parentId, List<Map<String, Object>> relations) {
        List<Map<String, Object>> children = new ArrayList<>();
        for (Map<String, Object> rel : relations) {
            Long supId = (Long) (rel.get("SUPERIOR_ID") != null ? rel.get("SUPERIOR_ID") : rel.get("superior_id"));
            if (parentId.equals(supId)) {
                Long subId = (Long) (rel.get("SUBORDINATE_ID") != null ? rel.get("SUBORDINATE_ID") : rel.get("subordinate_id"));
                Map<String, Object> child = new HashMap<>();
                child.put("id", subId);
                child.put("realName", rel.get("SUB_NAME") != null ? rel.get("SUB_NAME") : rel.get("sub_name"));
                child.put("role", rel.get("SUB_ROLE") != null ? rel.get("SUB_ROLE") : rel.get("sub_role"));
                child.put("children", buildChildren(subId, relations));
                children.add(child);
            }
        }
        return children;
    }

    @GetMapping("/superiors/{userId}")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSuperiors(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(hierarchyService.getSuperiors(userId)));
    }

    @GetMapping("/subordinates/{userId}")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSubordinates(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(hierarchyService.getSubordinates(userId)));
    }

    @GetMapping("/is-subordinate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> isSubordinate(@RequestParam Long superiorId, @RequestParam Long subordinateId) {
        Map<String, Object> result = new HashMap<>();
        result.put("isSubordinate", hierarchyService.isSubordinate(superiorId, subordinateId));
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
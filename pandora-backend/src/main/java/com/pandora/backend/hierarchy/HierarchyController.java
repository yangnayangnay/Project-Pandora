package com.pandora.backend.hierarchy;

import com.pandora.backend.audit.Auditable;
import com.pandora.backend.common.ApiResponse;
import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/hierarchy")
public class HierarchyController {

    private static final Logger log = LoggerFactory.getLogger(HierarchyController.class);

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
            // B9：字段 null 兜底为空字符串
            Object supName = rel.get("SUP_NAME") != null ? rel.get("SUP_NAME") : rel.get("sup_name");
            entry.put("sup_name", supName != null ? supName : "");
            Object supRole = rel.get("SUP_ROLE") != null ? rel.get("SUP_ROLE") : rel.get("sup_role");
            entry.put("sup_role", supRole != null ? supRole : "");
            Object subName = rel.get("SUB_NAME") != null ? rel.get("SUB_NAME") : rel.get("sub_name");
            entry.put("sub_name", subName != null ? subName : "");
            Object subRole = rel.get("SUB_ROLE") != null ? rel.get("SUB_ROLE") : rel.get("sub_role");
            entry.put("sub_role", subRole != null ? subRole : "");
            result.add(entry);
        }
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/tree")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTree() {
        List<Map<String, Object>> relations = hierarchyService.getHierarchyTree();
        List<Map<String, Object>> roots = jdbcTemplate.queryForList(
                "SELECT id, username, real_name, role FROM users WHERE deleted = 0 AND approval_status = 'APPROVED' AND id NOT IN (SELECT subordinate_id FROM user_hierarchy) ORDER BY id");

        // B8：字段 null 兜底 + 构建 children（每个根节点独立 visited 集合防循环）
        for (Map<String, Object> root : roots) {
            // 根节点字段 null 兜底
            Object username = root.get("USERNAME") != null ? root.get("USERNAME") : root.get("username");
            root.put("username", username != null ? username : "");
            Object realName = root.get("REAL_NAME") != null ? root.get("REAL_NAME") : root.get("real_name");
            root.put("realName", realName != null ? realName : "");
            Object role = root.get("ROLE") != null ? root.get("ROLE") : root.get("role");
            root.put("role", role != null ? role : "");

            Long rootId = (Long) (root.get("ID") != null ? root.get("ID") : root.get("id"));
            // 每个根节点创建新的 visited 集合
            Set<Long> visited = new HashSet<>();
            root.put("children", buildChildren(rootId, relations, visited));
        }

        // B8、B10：孤儿节点查询并单独返回
        List<Map<String, Object>> orphanNodes = hierarchyService.getOrphanNodes();
        // 孤儿节点字段 null 兜底
        for (Map<String, Object> orphan : orphanNodes) {
            Object username = orphan.get("USERNAME") != null ? orphan.get("USERNAME") : orphan.get("username");
            orphan.put("username", username != null ? username : "");
            Object realName = orphan.get("REAL_NAME") != null ? orphan.get("REAL_NAME") : orphan.get("real_name");
            orphan.put("realName", realName != null ? realName : "");
            Object role = orphan.get("ROLE") != null ? orphan.get("ROLE") : orphan.get("role");
            orphan.put("role", role != null ? role : "");
        }

        Map<String, Object> resultBody = new HashMap<>();
        resultBody.put("roots", roots);
        resultBody.put("orphanNodes", orphanNodes);
        return ResponseEntity.ok(ApiResponse.success(resultBody));
    }

    /**
     * B8：递归构建 children，增加 Set<Long> visited 参数防循环引用
     */
    private List<Map<String, Object>> buildChildren(Long parentId, List<Map<String, Object>> relations, Set<Long> visited) {
        // 循环引用检测
        if (visited.contains(parentId)) {
            log.warn("检测到循环引用，parentId={}，已截断", parentId);
            return Collections.emptyList();
        }
        visited.add(parentId);

        List<Map<String, Object>> children = new ArrayList<>();
        for (Map<String, Object> rel : relations) {
            Long supId = (Long) (rel.get("SUPERIOR_ID") != null ? rel.get("SUPERIOR_ID") : rel.get("superior_id"));
            if (parentId.equals(supId)) {
                Long subId = (Long) (rel.get("SUBORDINATE_ID") != null ? rel.get("SUBORDINATE_ID") : rel.get("subordinate_id"));
                Map<String, Object> child = new HashMap<>();
                child.put("id", subId);
                // B8：字段 null 兜底
                Object username = rel.get("SUB_USERNAME") != null ? rel.get("SUB_USERNAME") : rel.get("sub_username");
                child.put("username", username != null ? username : "");
                Object realName = rel.get("SUB_REAL_NAME") != null ? rel.get("SUB_REAL_NAME") : rel.get("sub_real_name");
                child.put("realName", realName != null ? realName : "");
                Object displayName = rel.get("SUB_NAME") != null ? rel.get("SUB_NAME") : rel.get("sub_name");
                child.put("displayName", displayName != null ? displayName : "");
                Object role = rel.get("SUB_ROLE") != null ? rel.get("SUB_ROLE") : rel.get("sub_role");
                child.put("role", role != null ? role : "");
                child.put("children", buildChildren(subId, relations, visited));
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
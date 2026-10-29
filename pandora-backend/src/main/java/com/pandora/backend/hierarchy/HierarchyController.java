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

    // Round28 REQ-28-12: 归属关系变更后失效受影响用户的个人十大 AI 点评缓存
    @Autowired
    private com.pandora.backend.view.PersonalTop10Generator personalTop10Generator;

    @PostMapping("/relation")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @org.springframework.transaction.annotation.Transactional
    @Auditable(operationType = "HIERARCHY_ADD", targetType = "USER_HIERARCHY")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addRelation(@RequestBody Map<String, Object> request) {
        Long superiorId = Long.valueOf(request.get("superiorId").toString());
        Long subordinateId = Long.valueOf(request.get("subordinateId").toString());
        boolean forceReplace = Boolean.TRUE.equals(request.get("forceReplace"));
        Map<String, Object> result = hierarchyService.addRelation(superiorId, subordinateId, forceReplace);

        // Round28 REQ-28-12: 归属变更失效双方个人十大
        invalidateTop10Safely(superiorId, subordinateId);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @DeleteMapping("/relation")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @Auditable(operationType = "HIERARCHY_REMOVE", targetType = "USER_HIERARCHY")
    public ResponseEntity<ApiResponse<Map<String, Object>>> removeRelation(@RequestParam Long superiorId, @RequestParam Long subordinateId) {
        hierarchyService.removeRelation(superiorId, subordinateId);

        // Round28 REQ-28-12: 归属变更失效双方个人十大
        invalidateTop10Safely(superiorId, subordinateId);

        return ResponseEntity.ok(ApiResponse.success(Map.of("success", true)));
    }

    private void invalidateTop10Safely(Long superiorId, Long subordinateId) {
        try {
            personalTop10Generator.invalidateUser(superiorId);
        } catch (Exception ignored) {}
        try {
            personalTop10Generator.invalidateUser(subordinateId);
        } catch (Exception ignored) {}
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

            // TASK-R18-004: 追加 snake_case 副字段（real_name/display_name/username 已有小写对应）
            // 使 request.js lowercaseKeys 递归转换后 realname 与 real_name 双存在（向后兼容）
            root.put("real_name", root.get("realName"));
            root.put("display_name", root.get("realName"));

            Long rootId = (Long) (root.get("ID") != null ? root.get("ID") : root.get("id"));
            // 每个根节点创建新的 visited 集合
            Set<Long> visited = new HashSet<>();
            root.put("children", buildChildren(rootId, relations, visited));
        }

        // TASK-R18-004: roots 按 RoleRank 升序 + 用户 id 升序稳定排序
        sortNodesByRoleRank(roots);

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

            // TASK-R18-004: 孤儿节点同样追加 snake_case 副字段
            orphan.put("real_name", orphan.get("realName"));
            orphan.put("display_name", orphan.get("realName"));
        }

        Map<String, Object> resultBody = new HashMap<>();
        resultBody.put("roots", roots);
        resultBody.put("orphanNodes", orphanNodes);
        return ResponseEntity.ok(ApiResponse.success(resultBody));
    }

    /**
     * TASK-R18-004: RoleRank 等级常量（独立于被 UserRelationActivity 依赖的 PermissionLevel，不得改动 PermissionLevel）
     * ADMIN=1 > FOUNDER=2 > DEPT_HEAD=3 > TEAM_LEADER=4 > EMPLOYEE=5，未知角色=99
     */
    private int roleRankOf(String role) {
        if (role == null) return 99;
        switch (role) {
            case "ADMIN": return 1;
            case "FOUNDER": return 2;
            case "DEPT_HEAD": return 3;
            case "TEAM_LEADER": return 4;
            case "EMPLOYEE": return 5;
            default: return 99;
        }
    }

    /**
     * TASK-R18-004: 节点列表按 RoleRank 升序 + 用户 id 升序稳定排序
     */
    @SuppressWarnings("unchecked")
    private void sortNodesByRoleRank(List<Map<String, Object>> nodes) {
        nodes.sort((a, b) -> {
            Object roleA = a.get("role") != null ? a.get("role") : a.get("ROLE");
            Object roleB = b.get("role") != null ? b.get("role") : b.get("ROLE");
            int rankA = roleRankOf(roleA != null ? roleA.toString() : null);
            int rankB = roleRankOf(roleB != null ? roleB.toString() : null);
            if (rankA != rankB) return Integer.compare(rankA, rankB);
            // 同级按用户 id 升序稳定排序
            Long idA = extractId(a);
            Long idB = extractId(b);
            return Long.compare(idA, idB);
        });
    }

    private Long extractId(Map<String, Object> node) {
        Object id = node.get("id") != null ? node.get("id") : node.get("ID");
        if (id instanceof Number) return ((Number) id).longValue();
        return Long.MAX_VALUE;
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

                // TASK-R18-004: children 追加 snake_case 副字段（向后兼容，既有驼峰字段不删除）
                child.put("real_name", child.get("realName"));
                child.put("display_name", child.get("displayName"));

                child.put("children", buildChildren(subId, relations, visited));
                children.add(child);
            }
        }
        // TASK-R18-004: 同层子节点按 RoleRank 升序 + 用户 id 升序稳定排序
        sortNodesByRoleRank(children);
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

package com.pandora.backend.hierarchy;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

public class HierarchyServiceTest {
    private JdbcTemplate jdbc;
    private HierarchyService svc;

    @BeforeEach
    void setUp() {
        jdbc = Mockito.mock(JdbcTemplate.class);
        svc = new HierarchyService();
        ReflectionTestUtils.setField(svc, "jdbcTemplate", jdbc);
        ReflectionTestUtils.setField(svc, "maxDepth", 10);
        ReflectionTestUtils.setField(svc, "cycleDetect", true);
    }

    @Test
    void 获取下属_ADMIN获取所有用户_预期成功() {
        doReturn("ADMIN").when(jdbc).queryForObject(eq("SELECT role FROM users WHERE id = ? AND deleted = 0"), eq(String.class), any(Long.class));
        List<Map<String, Object>> result = List.of(
            Map.of("id", 2L, "username", "user2", "real_name", "乙", "role", "EMPLOYEE", "depth", 0),
            Map.of("id", 3L, "username", "user3", "real_name", "丙", "role", "TEAM_LEADER", "depth", 0)
        );
        doReturn(result).when(jdbc).queryForList(eq("SELECT id, username, real_name, role, 0 as depth FROM users WHERE deleted = 0 AND id != ? AND approval_status = 'APPROVED' ORDER BY id"), any(Long.class));
        assertEquals(2, svc.getSubordinates(1L).size());
    }

    @Test
    void 获取下属_FOUNDER获取所有非ADMIN用户_预期成功() {
        doReturn("FOUNDER").when(jdbc).queryForObject(eq("SELECT role FROM users WHERE id = ? AND deleted = 0"), eq(String.class), any(Long.class));
        List<Map<String, Object>> result = List.of(
            Map.of("id", 3L, "username", "user3", "real_name", "丙", "role", "DEPT_HEAD", "depth", 0),
            Map.of("id", 4L, "username", "user4", "real_name", "丁", "role", "EMPLOYEE", "depth", 0),
            Map.of("id", 5L, "username", "user5", "real_name", "戊", "role", "TEAM_LEADER", "depth", 0)
        );
        doReturn(result).when(jdbc).queryForList(eq("SELECT id, username, real_name, role, 0 as depth FROM users WHERE deleted = 0 AND id != ? AND role != 'ADMIN' AND approval_status = 'APPROVED' ORDER BY id"), any(Long.class));
        assertEquals(3, svc.getSubordinates(2L).size());
    }

    @Test
    void 获取下属_普通用户获取直属下属_预期成功() {
        doReturn("DEPT_HEAD").when(jdbc).queryForObject(eq("SELECT role FROM users WHERE id = ? AND deleted = 0"), eq(String.class), any(Long.class));
        List<Map<String, Object>> directSubs = List.of(
            Map.of("subordinate_id", 10L, "username", "user10", "real_name", "己", "role", "EMPLOYEE")
        );
        doReturn(directSubs).when(jdbc).queryForList(eq("SELECT h.subordinate_id, u.username, u.real_name, u.role FROM user_hierarchy h JOIN users u ON h.subordinate_id = u.id WHERE h.superior_id = ? AND u.deleted = 0 AND u.approval_status = 'APPROVED'"), any(Long.class));
        assertEquals(1, svc.getSubordinates(3L).size());
    }

    @Test
    void 获取下属_达到最大深度限制_预期截断() {
        // 设置 maxDepth 为 0 来测试深度限制
        ReflectionTestUtils.setField(svc, "maxDepth", 0);
        doReturn("EMPLOYEE").when(jdbc).queryForObject(eq("SELECT role FROM users WHERE id = ? AND deleted = 0"), eq(String.class), any(Long.class));
        // 当 maxDepth=0 时，即使有下属也应该返回空列表
        assertTrue(svc.getSubordinates(1L).isEmpty());
    }

    @Test
    void 获取下属_用户不存在_预期返回空() {
        doReturn(null).when(jdbc).queryForObject(eq("SELECT role FROM users WHERE id = ? AND deleted = 0"), eq(String.class), any(Long.class));
        assertTrue(svc.getSubordinates(99999L).isEmpty());
    }

    @Test
    void 判断下属_直接下属_预期true() {
        doReturn("ADMIN").when(jdbc).queryForObject(eq("SELECT role FROM users WHERE id = ? AND deleted = 0"), eq(String.class), any(Long.class));
        List<Map<String, Object>> result = List.of(
            Map.of("id", 2L, "username", "user2", "real_name", "乙", "role", "EMPLOYEE", "depth", 0)
        );
        doReturn(result).when(jdbc).queryForList(eq("SELECT id, username, real_name, role, 0 as depth FROM users WHERE deleted = 0 AND id != ? AND approval_status = 'APPROVED' ORDER BY id"), any(Long.class));
        assertTrue(svc.isSubordinate(1L, 2L));
    }

    @Test
    void 判断下属_间接下属_预期true() {
        doReturn("ADMIN").when(jdbc).queryForObject(eq("SELECT role FROM users WHERE id = ? AND deleted = 0"), eq(String.class), any(Long.class));
        List<Map<String, Object>> result = List.of(
            Map.of("id", 2L, "username", "user2", "real_name", "乙", "role", "EMPLOYEE", "depth", 0),
            Map.of("id", 3L, "username", "user3", "real_name", "丙", "role", "EMPLOYEE", "depth", 0)
        );
        doReturn(result).when(jdbc).queryForList(eq("SELECT id, username, real_name, role, 0 as depth FROM users WHERE deleted = 0 AND id != ? AND approval_status = 'APPROVED' ORDER BY id"), any(Long.class));
        assertTrue(svc.isSubordinate(1L, 3L));
    }

    @Test
    void 判断下属_自己不是自己的下属_预期false() {
        assertFalse(svc.isSubordinate(1L, 1L));
    }

    @Test
    void 判断下属_非下属关系_预期false() {
        doReturn("EMPLOYEE").when(jdbc).queryForObject(eq("SELECT role FROM users WHERE id = ? AND deleted = 0"), eq(String.class), any(Long.class));
        doReturn(List.of()).when(jdbc).queryForList(eq("SELECT h.subordinate_id, u.username, u.real_name, u.role FROM user_hierarchy h JOIN users u ON h.subordinate_id = u.id WHERE h.superior_id = ? AND u.deleted = 0 AND u.approval_status = 'APPROVED'"), any(Long.class));
        assertFalse(svc.isSubordinate(1L, 100L));
    }

    @Test
    void 添加上下级关系_正常添加_预期成功() {
        doReturn("EMPLOYEE").when(jdbc).queryForObject(eq("SELECT role FROM users WHERE id = ? AND deleted = 0"), eq(String.class), any(Long.class));
        doReturn(List.of()).when(jdbc).queryForList(eq("SELECT h.subordinate_id, u.username, u.real_name, u.role FROM user_hierarchy h JOIN users u ON h.subordinate_id = u.id WHERE h.superior_id = ? AND u.deleted = 0 AND u.approval_status = 'APPROVED'"), any(Long.class));
        doReturn(0).when(jdbc).queryForObject(eq("SELECT COUNT(*) FROM user_hierarchy WHERE subordinate_id = ?"), eq(Integer.class), any(Long.class));
        doReturn(1).when(jdbc).update(eq("INSERT INTO user_hierarchy (superior_id, subordinate_id) VALUES (?, ?)"), any(Long.class), any(Long.class));
        Map<String, Object> r = svc.addRelation(1L, 2L, false);
        assertEquals(Boolean.TRUE, r.get("success"));
    }

    @Test
    void 添加上下级关系_形成循环_预期拒绝() {
        doReturn("EMPLOYEE").when(jdbc).queryForObject(eq("SELECT role FROM users WHERE id = ? AND deleted = 0"), eq(String.class), any(Long.class));
        // 模拟 1 已经是 2 的上级（即 2 是 1 的下属）
        // 这样 2 再成为 1 的上级就会形成循环
        // checkNoCycle(2, 1) 会检查 1 是否在 2 的上级列表中
        List<Map<String, Object>> supsOf2 = List.of(
            Map.of("superior_id", 1L, "username", "user1", "real_name", "甲", "role", "EMPLOYEE")
        );
        doReturn(supsOf2).when(jdbc).queryForList(eq("SELECT h.superior_id, u.username, u.real_name, u.role FROM user_hierarchy h JOIN users u ON h.superior_id = u.id WHERE h.subordinate_id = ? AND u.deleted = 0"), any(Long.class));
        // 模拟 2 没有上级（checkSingleSuperior 返回 false）
        doReturn(0).when(jdbc).queryForObject(eq("SELECT COUNT(*) FROM user_hierarchy WHERE subordinate_id = ?"), eq(Integer.class), any(Long.class));
        Map<String, Object> r = svc.addRelation(2L, 1L, false);
        assertEquals(Boolean.FALSE, r.get("success"));
        assertTrue(String.valueOf(r.get("reason")).contains("循环"));
    }

    @Test
    void 添加上下级关系_已有上级且未强制替换_预期拒绝() {
        doReturn("EMPLOYEE").when(jdbc).queryForObject(eq("SELECT role FROM users WHERE id = ? AND deleted = 0"), eq(String.class), any(Long.class));
        doReturn(List.of()).when(jdbc).queryForList(eq("SELECT h.subordinate_id, u.username, u.real_name, u.role FROM user_hierarchy h JOIN users u ON h.subordinate_id = u.id WHERE h.superior_id = ? AND u.deleted = 0 AND u.approval_status = 'APPROVED'"), any(Long.class));
        doReturn(1).when(jdbc).queryForObject(eq("SELECT COUNT(*) FROM user_hierarchy WHERE subordinate_id = ?"), eq(Integer.class), any(Long.class));
        Map<String, Object> r = svc.addRelation(1L, 2L, false);
        assertEquals(Boolean.FALSE, r.get("success"));
        assertTrue(String.valueOf(r.get("reason")).contains("上级"));
    }

    @Test
    void 添加上下级关系_已有上级但强制替换_预期成功() {
        doReturn("EMPLOYEE").when(jdbc).queryForObject(eq("SELECT role FROM users WHERE id = ? AND deleted = 0"), eq(String.class), any(Long.class));
        doReturn(List.of()).when(jdbc).queryForList(eq("SELECT h.subordinate_id, u.username, u.real_name, u.role FROM user_hierarchy h JOIN users u ON h.subordinate_id = u.id WHERE h.superior_id = ? AND u.deleted = 0 AND u.approval_status = 'APPROVED'"), any(Long.class));
        doReturn(1).when(jdbc).queryForObject(eq("SELECT COUNT(*) FROM user_hierarchy WHERE subordinate_id = ?"), eq(Integer.class), any(Long.class));
        doReturn(1).when(jdbc).update(eq("DELETE FROM user_hierarchy WHERE subordinate_id = ?"), any(Long.class));
        doReturn(1).when(jdbc).update(eq("INSERT INTO user_hierarchy (superior_id, subordinate_id) VALUES (?, ?)"), any(Long.class), any(Long.class));
        Map<String, Object> r = svc.addRelation(1L, 2L, true);
        assertEquals(Boolean.TRUE, r.get("success"));
    }
}
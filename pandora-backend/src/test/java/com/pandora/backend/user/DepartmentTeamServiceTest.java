package com.pandora.backend.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import com.pandora.backend.common.ApiResponse;
import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.ErrorCode;

public class DepartmentTeamServiceTest {
    private JdbcTemplate jdbc;
    private PersonnelController ctrl;

    @BeforeEach
    void setUp() {
        jdbc = Mockito.mock(JdbcTemplate.class);
        ctrl = new PersonnelController(jdbc);
        setupDefaultMocks();
    }

    private void setupDefaultMocks() {
        // Mock GeneratedKeyHolder for insert operations
        doAnswer(invocation -> {
            GeneratedKeyHolder keyHolder = invocation.getArgument(1);
            var field = GeneratedKeyHolder.class.getDeclaredField("keyList");
            field.setAccessible(true);
            List<Map<String, Object>> keyList = List.of(Map.of("", 100L));
            field.set(keyHolder, keyList);
            return 1;
        }).when(jdbc).update(any(), any(GeneratedKeyHolder.class));
    }

    private Map<String,Object> body(Object... kv) { Map<String,Object> m=new HashMap<>(); for(int i=0;i<kv.length;i+=2) m.put((String)kv[i],kv[i+1]); return m; }
    private BusinessException biz(Runnable r) { return assertThrows(BusinessException.class, r::run); }

    @Test void 部门创建_正常名称_预期成功() {
        when(jdbc.queryForList(eq("SELECT id FROM departments WHERE name=? AND deleted=0"), eq(Long.class), any(String.class))).thenReturn(List.of());
        ApiResponse<Map<String,Object>> resp=ctrl.createDepartment(body("name","研发部"));
        Map<String,Object> out=resp.getData();
        assertEquals("研发部", out.get("name"));
        assertNotNull(out.get("id"));
    }

    @Test void 部门创建_名称最小长度1_预期成功() {
        when(jdbc.queryForList(eq("SELECT id FROM departments WHERE name=? AND deleted=0"), eq(Long.class), any(String.class))).thenReturn(List.of());
        ApiResponse<Map<String,Object>> resp=ctrl.createDepartment(body("name","A"));
        assertNotNull(resp.getData());
    }

    @Test void 部门创建_名称最大长度128_预期成功() {
        when(jdbc.queryForList(eq("SELECT id FROM departments WHERE name=? AND deleted=0"), eq(Long.class), any(String.class))).thenReturn(List.of());
        ApiResponse<Map<String,Object>> resp=ctrl.createDepartment(body("name","测".repeat(128)));
        assertNotNull(resp.getData());
    }

    @Test void 部门创建_名称为空_预期拒绝() {
        assertEquals(ErrorCode.BAD_REQUEST, biz(()->ctrl.createDepartment(body("name",""))).getErrorCode());
    }

    @Test void 部门创建_名称超长_预期拒绝() {
        assertEquals(ErrorCode.BAD_REQUEST, biz(()->ctrl.createDepartment(body("name","测".repeat(129)))).getErrorCode());
    }

    @Test void 部门创建_名称重复_预期拒绝() {
        when(jdbc.queryForList(eq("SELECT id FROM departments WHERE name=? AND deleted=0"), eq(Long.class), any(String.class))).thenReturn(List.of(1L));
        assertEquals(ErrorCode.BAD_REQUEST, biz(()->ctrl.createDepartment(body("name","研发部"))).getErrorCode());
    }

    @Test void 部门创建_非ADMIN操作_预期拒绝() {
        when(jdbc.queryForList(eq("SELECT id FROM departments WHERE name=? AND deleted=0"), eq(Long.class), any(String.class))).thenReturn(List.of());
        ApiResponse<Map<String,Object>> resp = ctrl.createDepartment(body("name","新部门"));
        assertNotNull(resp.getData());
    }

    @Test void 部门更新_修改名称_预期成功() {
        when(jdbc.queryForList(eq("SELECT id FROM departments WHERE id=? AND deleted=0"), eq(Long.class), anyLong())).thenReturn(List.of(1L));
        when(jdbc.queryForList(eq("SELECT id FROM departments WHERE name=? AND id<>? AND deleted=0"), eq(Long.class), any(String.class), anyLong())).thenReturn(List.of());
        assertDoesNotThrow(()->ctrl.updateDepartment(1L, body("name","新名称")));
    }

    @Test void 部门更新_部门不存在_预期拒绝() {
        when(jdbc.queryForList(eq("SELECT id FROM departments WHERE id=? AND deleted=0"), eq(Long.class), anyLong())).thenReturn(List.of());
        assertEquals(ErrorCode.NOT_FOUND, biz(()->ctrl.updateDepartment(99999L, body("name","新名称"))).getErrorCode());
    }

    @Test void 部门更新_名称重复_预期拒绝() {
        when(jdbc.queryForList(eq("SELECT id FROM departments WHERE id=? AND deleted=0"), eq(Long.class), anyLong())).thenReturn(List.of(1L));
        when(jdbc.queryForList(eq("SELECT id FROM departments WHERE name=? AND id<>? AND deleted=0"), eq(Long.class), any(String.class), anyLong())).thenReturn(List.of(2L));
        assertEquals(ErrorCode.BAD_REQUEST, biz(()->ctrl.updateDepartment(1L, body("name","市场部"))).getErrorCode());
    }

    @Test void 部门删除_空部门_预期成功() {
        when(jdbc.queryForList(eq("SELECT id FROM departments WHERE id=? AND deleted=0"), eq(Long.class), anyLong())).thenReturn(List.of(99L));
        when(jdbc.queryForList(eq("SELECT id FROM users WHERE department_id=? AND role NOT IN ('ADMIN','FOUNDER') AND deleted=0"), eq(Long.class), anyLong())).thenReturn(List.of());
        when(jdbc.queryForList(eq("SELECT id FROM teams WHERE department_id=? AND deleted=0"), eq(Long.class), anyLong())).thenReturn(List.of());
        assertDoesNotThrow(()->ctrl.deleteDepartment(99L));
    }

    @Test void 部门删除_部门不存在_预期拒绝() {
        when(jdbc.queryForList(eq("SELECT id FROM departments WHERE id=? AND deleted=0"), eq(Long.class), anyLong())).thenReturn(List.of());
        assertEquals(ErrorCode.NOT_FOUND, biz(()->ctrl.deleteDepartment(99999L)).getErrorCode());
    }

    @Test void 部门删除_部门仍有人员_预期拒绝() {
        when(jdbc.queryForList(eq("SELECT id FROM departments WHERE id=? AND deleted=0"), eq(Long.class), anyLong())).thenReturn(List.of(1L));
        when(jdbc.queryForList(eq("SELECT id FROM users WHERE department_id=? AND role NOT IN ('ADMIN','FOUNDER') AND deleted=0"), eq(Long.class), anyLong())).thenReturn(List.of(1L));
        assertEquals(ErrorCode.BAD_REQUEST, biz(()->ctrl.deleteDepartment(1L)).getErrorCode());
    }

    @Test void 部门删除_部门仍有团队_预期拒绝() {
        when(jdbc.queryForList(eq("SELECT id FROM departments WHERE id=? AND deleted=0"), eq(Long.class), anyLong())).thenReturn(List.of(1L));
        when(jdbc.queryForList(eq("SELECT id FROM users WHERE department_id=? AND role NOT IN ('ADMIN','FOUNDER') AND deleted=0"), eq(Long.class), anyLong())).thenReturn(List.of());
        when(jdbc.queryForList(eq("SELECT id FROM teams WHERE department_id=? AND deleted=0"), eq(Long.class), anyLong())).thenReturn(List.of(1L));
        assertEquals(ErrorCode.BAD_REQUEST, biz(()->ctrl.deleteDepartment(1L)).getErrorCode());
    }

    @Test void 团队创建_正常创建_预期成功() {
        when(jdbc.queryForList(eq("SELECT id FROM departments WHERE id=? AND deleted=0"), eq(Long.class), anyLong())).thenReturn(List.of(1L));
        when(jdbc.queryForList(eq("SELECT id FROM teams WHERE name=? AND department_id=? AND deleted=0"), eq(Long.class), any(String.class), anyLong())).thenReturn(List.of());
        ApiResponse<Map<String,Object>> resp=ctrl.createTeam(body("name","前端组","departmentId",1));
        Map<String,Object> out=resp.getData();
        assertEquals("前端组", out.get("name"));
    }

    @Test void 团队创建_部门不存在_预期拒绝() {
        when(jdbc.queryForList(eq("SELECT id FROM departments WHERE id=? AND deleted=0"), eq(Long.class), anyLong())).thenReturn(List.of());
        assertEquals(ErrorCode.BAD_REQUEST, biz(()->ctrl.createTeam(body("name","前端组","departmentId",999))).getErrorCode());
    }

    @Test void 团队创建_同部门同名团队已存在_预期拒绝() {
        when(jdbc.queryForList(eq("SELECT id FROM departments WHERE id=? AND deleted=0"), eq(Long.class), anyLong())).thenReturn(List.of(1L));
        when(jdbc.queryForList(eq("SELECT id FROM teams WHERE name=? AND department_id=? AND deleted=0"), eq(Long.class), any(String.class), anyLong())).thenReturn(List.of(1L));
        assertEquals(ErrorCode.BAD_REQUEST, biz(()->ctrl.createTeam(body("name","前端组","departmentId",1))).getErrorCode());
    }

    @Test void 团队删除_团队仍有成员_预期拒绝() {
        when(jdbc.queryForList(eq("SELECT id FROM teams WHERE id=? AND deleted=0"), eq(Long.class), anyLong())).thenReturn(List.of(1L));
        when(jdbc.queryForList(eq("SELECT id FROM users WHERE team_id=? AND role IN ('TEAM_LEADER','EMPLOYEE') AND deleted=0"), eq(Long.class), anyLong())).thenReturn(List.of(1L));
        assertEquals(ErrorCode.BAD_REQUEST, biz(()->ctrl.deleteTeam(1L)).getErrorCode());
    }
}
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import com.pandora.backend.common.ApiResponse;
import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.ErrorCode;
import com.pandora.backend.common.FileStorageService;

public class PersonnelOrgChangeTest {
    private JdbcTemplate jdbc;
    private PersonnelController personnelController;
    private UserController userController;
    private Authentication admin, leader;

    @BeforeEach
    void setUp() {
        jdbc = Mockito.mock(JdbcTemplate.class);
        personnelController = new PersonnelController(jdbc);
        
        // 初始化 UserController 用于 updateUserOrg 测试
        BCryptPasswordEncoder passwordEncoder = Mockito.mock(BCryptPasswordEncoder.class);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        AccountCreationService accountCreationService = new AccountCreationService(jdbc, passwordEncoder);
        FileStorageService fileStorageService = Mockito.mock(FileStorageService.class);
        com.pandora.backend.hierarchy.HierarchyService hierarchyService = Mockito.mock(com.pandora.backend.hierarchy.HierarchyService.class);
        com.pandora.backend.view.PersonalTop10Generator personalTop10Generator = Mockito.mock(com.pandora.backend.view.PersonalTop10Generator.class);
        
        userController = new UserController();
        ReflectionTestUtils.setField(userController, "jdbcTemplate", jdbc);
        ReflectionTestUtils.setField(userController, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(userController, "accountCreationService", accountCreationService);
        ReflectionTestUtils.setField(userController, "fileStorageService", fileStorageService);
        ReflectionTestUtils.setField(userController, "hierarchyService", hierarchyService);
        ReflectionTestUtils.setField(userController, "personalTop10Generator", personalTop10Generator);
        
        admin = new UsernamePasswordAuthenticationToken("a", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        leader = new UsernamePasswordAuthenticationToken("l", null, List.of(new SimpleGrantedAuthority("ROLE_DEPT_HEAD")));
        
        // 设置 SecurityContext 为 ADMIN
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
        SecurityContextHolder.getContext().setAuthentication(admin);
        
        when(jdbc.queryForObject(anyString(), eq(String.class))).thenReturn("ADMIN");
    }
    
    private Map<String,Object> body(Object... kv) { 
        Map<String,Object> m=new HashMap<>(); 
        for(int i=0;i<kv.length;i+=2) m.put((String)kv[i],kv[i+1]); 
        return m; 
    }

    // ============== 人员入职/状态变更测试 ==============

    @Test
    void 人员入职_员工入职分配部门团队_预期成功() {
        List<Map<String, Object>> target = List.of(Map.of("role", "EMPLOYEE"));
        when(jdbc.queryForList(eq("SELECT role FROM users WHERE id = ? AND deleted = 0 FOR UPDATE"), any(Object[].class))).thenReturn(target);
        when(jdbc.queryForList(eq("SELECT id FROM departments WHERE id = ? AND deleted = 0"), eq(Long.class), any(Long.class))).thenReturn(List.of(1L));
        when(jdbc.queryForList(eq("SELECT id FROM teams WHERE id = ? AND department_id = ? AND deleted = 0"), eq(Long.class), any(Long.class), any(Long.class))).thenReturn(List.of(1L));
        
        ApiResponse<Void> resp = personnelController.employment(admin, 200L, body("role","EMPLOYEE","departmentId",1,"teamId",1));
        assertEquals(200, resp.getCode());
    }

    @Test
    void 人员状态变更_员工晋升为团队负责人_预期成功() {
        List<Map<String, Object>> target = List.of(Map.of("role", "EMPLOYEE"));
        when(jdbc.queryForList(eq("SELECT role FROM users WHERE id = ? AND deleted = 0 FOR UPDATE"), any(Object[].class))).thenReturn(target);
        when(jdbc.queryForList(eq("SELECT id FROM departments WHERE id = ? AND deleted = 0"), eq(Long.class), any(Long.class))).thenReturn(List.of(1L));
        when(jdbc.queryForList(eq("SELECT id FROM teams WHERE id = ? AND department_id = ? AND deleted = 0"), eq(Long.class), any(Long.class), any(Long.class))).thenReturn(List.of(1L));
        
        ApiResponse<Void> resp = personnelController.employment(admin, 201L, body("role","TEAM_LEADER","departmentId",1,"teamId",2));
        assertEquals(200, resp.getCode());
    }

    @Test
    void 人员状态变更_团队负责人降为员工_预期成功() {
        List<Map<String, Object>> target = List.of(Map.of("role", "TEAM_LEADER"));
        when(jdbc.queryForList(eq("SELECT role FROM users WHERE id = ? AND deleted = 0 FOR UPDATE"), any(Object[].class))).thenReturn(target);
        when(jdbc.queryForList(eq("SELECT id FROM departments WHERE id = ? AND deleted = 0"), eq(Long.class), any(Long.class))).thenReturn(List.of(1L));
        when(jdbc.queryForList(eq("SELECT id FROM teams WHERE id = ? AND department_id = ? AND deleted = 0"), eq(Long.class), any(Long.class), any(Long.class))).thenReturn(List.of(1L));
        
        ApiResponse<Void> resp = personnelController.employment(admin, 202L, body("role","EMPLOYEE","departmentId",1,"teamId",2));
        assertEquals(200, resp.getCode());
    }

    @Test
    void 人员状态变更_员工跨部门调动_预期成功() {
        List<Map<String, Object>> target = List.of(Map.of("role", "EMPLOYEE"));
        when(jdbc.queryForList(eq("SELECT role FROM users WHERE id = ? AND deleted = 0 FOR UPDATE"), any(Object[].class))).thenReturn(target);
        when(jdbc.queryForList(eq("SELECT id FROM departments WHERE id = ? AND deleted = 0"), eq(Long.class), any(Long.class))).thenReturn(List.of(2L));
        when(jdbc.queryForList(eq("SELECT id FROM teams WHERE id = ? AND department_id = ? AND deleted = 0"), eq(Long.class), any(Long.class), any(Long.class))).thenReturn(List.of(3L));
        
        ApiResponse<Void> resp = personnelController.employment(admin, 203L, body("role","EMPLOYEE","departmentId",2,"teamId",3));
        assertEquals(200, resp.getCode());
    }

    @Test
    void 人员入职_非ADMIN操作_预期拒绝() {
        assertThrows(BusinessException.class, () -> personnelController.employment(leader, 200L, body("role","EMPLOYEE")));
    }

    @Test
    void 人员入职_用户不存在_预期拒绝() {
        when(jdbc.queryForList(eq("SELECT role FROM users WHERE id = ? AND deleted = 0 FOR UPDATE"), any(Object[].class))).thenReturn(List.of());
        assertThrows(BusinessException.class, () -> personnelController.employment(admin, 99999L, body("role","EMPLOYEE")));
    }

    @Test
    void 人员入职_管理员账号变更_预期拒绝() {
        List<Map<String, Object>> target = List.of(Map.of("role", "ADMIN"));
        when(jdbc.queryForList(eq("SELECT role FROM users WHERE id = ? AND deleted = 0 FOR UPDATE"), any(Object[].class))).thenReturn(target);
        assertThrows(BusinessException.class, () -> personnelController.employment(admin, 204L, body("role","EMPLOYEE")));
    }

    @Test
    void 人员入职_角色无效_预期拒绝() {
        List<Map<String, Object>> target = List.of(Map.of("role", "EMPLOYEE"));
        when(jdbc.queryForList(eq("SELECT role FROM users WHERE id = ? AND deleted = 0 FOR UPDATE"), any(Object[].class))).thenReturn(target);
        assertThrows(BusinessException.class, () -> personnelController.employment(admin, 200L, body("role","INVALID")));
    }

    @Test
    void 人员入职_缺少必填字段_预期拒绝() {
        List<Map<String, Object>> target = List.of(Map.of("role", "EMPLOYEE"));
        when(jdbc.queryForList(eq("SELECT role FROM users WHERE id = ? AND deleted = 0 FOR UPDATE"), any(Object[].class))).thenReturn(target);
        assertThrows(BusinessException.class, () -> personnelController.employment(admin, 200L, body("role","EMPLOYEE")));
    }

    @Test
    void 人员入职_缺少部门_预期拒绝() {
        List<Map<String, Object>> target = List.of(Map.of("role", "EMPLOYEE"));
        when(jdbc.queryForList(eq("SELECT role FROM users WHERE id = ? AND deleted = 0 FOR UPDATE"), any(Object[].class))).thenReturn(target);
        assertThrows(BusinessException.class, () -> personnelController.employment(admin, 200L, body("role","DEPT_HEAD")));
    }

    @Test
    void 人员入职_缺少团队_预期拒绝() {
        List<Map<String, Object>> target = List.of(Map.of("role", "EMPLOYEE"));
        when(jdbc.queryForList(eq("SELECT role FROM users WHERE id = ? AND deleted = 0 FOR UPDATE"), any(Object[].class))).thenReturn(target);
        when(jdbc.queryForList(eq("SELECT id FROM departments WHERE id = ? AND deleted = 0"), eq(Long.class), any(Long.class))).thenReturn(List.of(1L));
        assertThrows(BusinessException.class, () -> personnelController.employment(admin, 200L, body("role","EMPLOYEE","departmentId",1)));
    }

    @Test
    void 人员入职_部门不存在_预期拒绝() {
        List<Map<String, Object>> target = List.of(Map.of("role", "EMPLOYEE"));
        when(jdbc.queryForList(eq("SELECT role FROM users WHERE id = ? AND deleted = 0 FOR UPDATE"), any(Object[].class))).thenReturn(target);
        when(jdbc.queryForList(eq("SELECT id FROM departments WHERE id = ? AND deleted = 0"), eq(Long.class), any(Long.class))).thenReturn(List.of());
        assertThrows(BusinessException.class, () -> personnelController.employment(admin, 200L, body("role","EMPLOYEE","departmentId",999,"teamId",1)));
    }

    @Test
    void 人员入职_团队与部门不匹配_预期拒绝() {
        List<Map<String, Object>> target = List.of(Map.of("role", "EMPLOYEE"));
        when(jdbc.queryForList(eq("SELECT role FROM users WHERE id = ? AND deleted = 0 FOR UPDATE"), any(Object[].class))).thenReturn(target);
        when(jdbc.queryForList(eq("SELECT id FROM departments WHERE id = ? AND deleted = 0"), eq(Long.class), any(Long.class))).thenReturn(List.of(1L));
        when(jdbc.queryForList(eq("SELECT id FROM teams WHERE id = ? AND department_id = ? AND deleted = 0"), eq(Long.class), any(Long.class), any(Long.class))).thenReturn(List.of());
        assertThrows(BusinessException.class, () -> personnelController.employment(admin, 200L, body("role","EMPLOYEE","departmentId",1,"teamId",999)));
    }

    // ============== 人员调动测试 (updateUserOrg) - 补充9条 ==============

    @Test
    void 人员调动_员工换部门换团队_预期成功() {
        // 设置 ADMIN 认证上下文
        when(jdbc.queryForMap(eq("SELECT role FROM users WHERE id = ?"), eq(1L))).thenReturn(Map.of("role","ADMIN"));
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        
        // 查询目标用户角色
        List<String> targetRoles = List.of("EMPLOYEE");
        when(jdbc.queryForList(eq("SELECT role FROM users WHERE id=? AND deleted=0"), eq(String.class), anyLong())).thenReturn(targetRoles);
        when(jdbc.queryForList(eq("SELECT id FROM departments WHERE id=? AND deleted=0"), eq(Long.class), anyLong())).thenReturn(List.of(2L));
        when(jdbc.queryForList(eq("SELECT id FROM teams WHERE id=? AND department_id=? AND deleted=0"), eq(Long.class), anyLong(), anyLong())).thenReturn(List.of(3L));
        assertDoesNotThrow(() -> userController.updateUserOrg(100L, body("departmentId",2,"teamId",3)));
    }

    @Test
    void 人员调动_部门负责人只换部门_预期成功() {
        when(jdbc.queryForMap(eq("SELECT role FROM users WHERE id = ?"), eq(1L))).thenReturn(Map.of("role","ADMIN"));
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        
        List<String> targetRoles = List.of("DEPT_HEAD");
        when(jdbc.queryForList(eq("SELECT role FROM users WHERE id=? AND deleted=0"), eq(String.class), anyLong())).thenReturn(targetRoles);
        when(jdbc.queryForList(eq("SELECT id FROM departments WHERE id=? AND deleted=0"), eq(Long.class), anyLong())).thenReturn(List.of(2L));
        assertDoesNotThrow(() -> userController.updateUserOrg(101L, body("departmentId",2)));
    }

    @Test
    void 人员调动_创始人调动_预期成功不保留团队() {
        when(jdbc.queryForMap(eq("SELECT role FROM users WHERE id = ?"), eq(1L))).thenReturn(Map.of("role","ADMIN"));
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        
        List<String> targetRoles = List.of("FOUNDER");
        when(jdbc.queryForList(eq("SELECT role FROM users WHERE id=? AND deleted=0"), eq(String.class), anyLong())).thenReturn(targetRoles);
        assertDoesNotThrow(() -> userController.updateUserOrg(102L, body()));
    }

    @Test
    void 人员调动_非ADMIN操作_预期拒绝() {
        Authentication empAuth = new UsernamePasswordAuthenticationToken(2L, null, List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE")));
        // mock getCurrentUserRole() 查询 - 使用 anyLong() 匹配
        when(jdbc.queryForMap(eq("SELECT role FROM users WHERE id = ?"), anyLong())).thenReturn(Map.of("role","EMPLOYEE"));
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
        SecurityContextHolder.getContext().setAuthentication(empAuth);
        // 查询目标用户角色
        List<String> targetRoles = List.of("EMPLOYEE");
        when(jdbc.queryForList(eq("SELECT role FROM users WHERE id=? AND deleted=0"), eq(String.class), anyLong())).thenReturn(targetRoles);
        // 非ADMIN操作返回403
        var resp = userController.updateUserOrg(100L, body("departmentId",2));
        assertEquals(403, resp.getStatusCode().value());
    }

    @Test
    void 人员调动_用户不存在_预期拒绝() {
        when(jdbc.queryForMap(eq("SELECT role FROM users WHERE id = ?"), eq(1L))).thenReturn(Map.of("role","ADMIN"));
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        
        when(jdbc.queryForList(eq("SELECT role FROM users WHERE id=? AND deleted=0"), eq(String.class), anyLong())).thenReturn(List.of());
        assertThrows(BusinessException.class, () -> userController.updateUserOrg(99999L, body("departmentId",2)));
    }

    @Test
    void 人员调动_管理员账号调动_预期拒绝() {
        when(jdbc.queryForMap(eq("SELECT role FROM users WHERE id = ?"), eq(1L))).thenReturn(Map.of("role","ADMIN"));
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        
        List<String> targetRoles = List.of("ADMIN");
        when(jdbc.queryForList(eq("SELECT role FROM users WHERE id=? AND deleted=0"), eq(String.class), anyLong())).thenReturn(targetRoles);
        assertThrows(BusinessException.class, () -> userController.updateUserOrg(103L, body("departmentId",2)));
    }

    @Test
    void 人员调动_部门ID无效_预期拒绝() {
        when(jdbc.queryForMap(eq("SELECT role FROM users WHERE id = ?"), eq(1L))).thenReturn(Map.of("role","ADMIN"));
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        
        List<String> targetRoles = List.of("EMPLOYEE");
        when(jdbc.queryForList(eq("SELECT role FROM users WHERE id=? AND deleted=0"), eq(String.class), anyLong())).thenReturn(targetRoles);
        assertThrows(BusinessException.class, () -> userController.updateUserOrg(100L, body("departmentId",-1)));
    }

    @Test
    void 人员调动_团队与部门不匹配_预期拒绝() {
        when(jdbc.queryForMap(eq("SELECT role FROM users WHERE id = ?"), eq(1L))).thenReturn(Map.of("role","ADMIN"));
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        
        List<String> targetRoles = List.of("EMPLOYEE");
        when(jdbc.queryForList(eq("SELECT role FROM users WHERE id=? AND deleted=0"), eq(String.class), anyLong())).thenReturn(targetRoles);
        when(jdbc.queryForList(eq("SELECT id FROM departments WHERE id=? AND deleted=0"), eq(Long.class), anyLong())).thenReturn(List.of(1L));
        when(jdbc.queryForList(eq("SELECT id FROM teams WHERE id=? AND department_id=? AND deleted=0"), eq(Long.class), anyLong(), anyLong())).thenReturn(List.of());
        assertThrows(BusinessException.class, () -> userController.updateUserOrg(100L, body("departmentId",1,"teamId",999)));
    }

    @Test
    void 人员调动_员工缺少团队_预期拒绝() {
        when(jdbc.queryForMap(eq("SELECT role FROM users WHERE id = ?"), eq(1L))).thenReturn(Map.of("role","ADMIN"));
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        
        List<String> targetRoles = List.of("EMPLOYEE");
        when(jdbc.queryForList(eq("SELECT role FROM users WHERE id=? AND deleted=0"), eq(String.class), anyLong())).thenReturn(targetRoles);
        when(jdbc.queryForList(eq("SELECT id FROM departments WHERE id=? AND deleted=0"), eq(Long.class), anyLong())).thenReturn(List.of(1L));
        assertThrows(BusinessException.class, () -> userController.updateUserOrg(100L, body("departmentId",1)));
    }
}
package com.pandora.backend.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.FileStorageService;

public class UserQueryTest {
    private JdbcTemplate jdbc;
    private UserController ctrl;

    @BeforeEach
    void setUp() {
        jdbc = Mockito.mock(JdbcTemplate.class);
        BCryptPasswordEncoder passwordEncoder = Mockito.mock(BCryptPasswordEncoder.class);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        AccountCreationService accountCreationService = new AccountCreationService(jdbc, passwordEncoder);
        FileStorageService fileStorageService = Mockito.mock(FileStorageService.class);
        com.pandora.backend.hierarchy.HierarchyService hierarchyService = Mockito.mock(com.pandora.backend.hierarchy.HierarchyService.class);
        com.pandora.backend.view.PersonalTop10Generator personalTop10Generator = Mockito.mock(com.pandora.backend.view.PersonalTop10Generator.class);
        
        ctrl = new UserController();
        ReflectionTestUtils.setField(ctrl, "jdbcTemplate", jdbc);
        ReflectionTestUtils.setField(ctrl, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(ctrl, "accountCreationService", accountCreationService);
        ReflectionTestUtils.setField(ctrl, "fileStorageService", fileStorageService);
        ReflectionTestUtils.setField(ctrl, "hierarchyService", hierarchyService);
        ReflectionTestUtils.setField(ctrl, "personalTop10Generator", personalTop10Generator);
        
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(new ArrayList<Map<String,Object>>());
        when(jdbc.queryForObject(anyString(), eq(String.class))).thenReturn("ADMIN");
    }

    // 设置认证上下文（principal 为用户ID Long类型）
    private void setAuth(Long userId, String role) {
        Authentication auth = new UsernamePasswordAuthenticationToken(userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
        SecurityContextHolder.getContext().setAuthentication(auth);
        // mock getCurrentUserRole() 查询
        when(jdbc.queryForMap(eq("SELECT role FROM users WHERE id = ?"), eq(userId))).thenReturn(Map.of("role", role));
    }

    @Test void 获取当前用户_正常获取_预期成功() {
        setAuth(1L, "ADMIN");
        when(jdbc.queryForMap(anyString(), any(Object[].class))).thenReturn(Map.of("id",1,"username","admin","role","ADMIN"));
        assertEquals(HttpStatus.OK, ctrl.getCurrentUser().getStatusCode());
    }

    @Test void 获取当前用户_用户不存在_预期拒绝() {
        setAuth(99999L, "EMPLOYEE");
        when(jdbc.queryForMap(anyString(), any(Object[].class))).thenThrow(new RuntimeException("Not found"));
        assertThrows(BusinessException.class, ()->ctrl.getCurrentUser());
    }

    @Test void 获取用户列表_无过滤条件_预期成功() {
        setAuth(1L, "ADMIN");
        assertEquals(HttpStatus.OK, ctrl.listUsers(null,null,null).getStatusCode());
    }

    @Test void 获取用户列表_按角色过滤_预期成功() {
        setAuth(1L, "ADMIN");
        assertEquals(HttpStatus.OK, ctrl.listUsers("EMPLOYEE",null,null).getStatusCode());
    }

    @Test void 获取用户列表_按部门过滤_预期成功() {
        setAuth(1L, "ADMIN");
        assertEquals(HttpStatus.OK, ctrl.listUsers(null,1L,null).getStatusCode());
    }

    @Test void 获取用户列表_部门不存在返回空_预期成功() {
        setAuth(1L, "ADMIN");
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of());
        assertEquals(HttpStatus.OK, ctrl.listUsers(null,99999L,null).getStatusCode());
    }

    @Test void 获取用户设置_正常获取_预期成功() {
        setAuth(1L, "ADMIN");
        when(jdbc.queryForMap(anyString(), any(Object[].class))).thenReturn(Map.of("showCompletedTasks",1,"showYesterdayLog",0));
        assertEquals(HttpStatus.OK, ctrl.getSettings().getStatusCode());
    }

    @Test void 更新用户设置_更新显示设置_预期成功() {
        setAuth(1L, "ADMIN");
        assertEquals(HttpStatus.OK, ctrl.updateSettings(Map.of("showCompletedTasks",false,"showYesterdayLog",true)).getStatusCode());
    }

    @Test void 获取用户关系_正常获取_预期成功() {
        setAuth(1L, "ADMIN");
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of());
        assertEquals(HttpStatus.OK, ctrl.getUserRelations().getStatusCode());
    }
}
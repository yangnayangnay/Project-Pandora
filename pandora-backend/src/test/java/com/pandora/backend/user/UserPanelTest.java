package com.pandora.backend.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
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

public class UserPanelTest {
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

    @Test void 用户面板查询_正常查询_预期成功() {
        setAuth(1L, "ADMIN");
        when(jdbc.queryForMap(anyString(), any(Object[].class))).thenReturn(Map.of("id",1,"username","admin","role","ADMIN"));
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of());
        assertEquals(HttpStatus.OK, ctrl.getUserPanel(1L).getStatusCode());
    }

    @Test void 用户面板查询_用户不存在_预期拒绝() {
        setAuth(1L, "ADMIN");
        when(jdbc.queryForMap(anyString(), any(Object[].class))).thenThrow(new RuntimeException("Not found"));
        assertThrows(BusinessException.class, ()->ctrl.getUserPanel(99999L));
    }

    @Test void 用户面板查询_普通用户查看ADMIN面板_预期拒绝() {
        setAuth(2L, "EMPLOYEE");
        // 查询目标用户（ADMIN）的信息
        when(jdbc.queryForMap(eq("SELECT id, username, role, department_id, team_id, avatar FROM users WHERE id = ? AND deleted = 0"), eq(1L))).thenReturn(Map.of("id",1,"username","admin","role","ADMIN"));
        // 查询当前用户的角色
        when(jdbc.queryForMap(eq("SELECT role FROM users WHERE id = ?"), eq(2L))).thenReturn(Map.of("role","EMPLOYEE"));
        assertThrows(BusinessException.class, ()->ctrl.getUserPanel(1L));
    }
}
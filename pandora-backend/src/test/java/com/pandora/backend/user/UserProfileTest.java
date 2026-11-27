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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.jdbc.core.JdbcTemplate;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.ErrorCode;
import com.pandora.backend.common.FileStorageService;

public class UserProfileTest {
    private JdbcTemplate jdbc;
    private UserController ctrl;
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        jdbc = Mockito.mock(JdbcTemplate.class);
        passwordEncoder = Mockito.mock(BCryptPasswordEncoder.class);
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
    private Map<String,Object> body(Object... kv) { Map<String,Object> m=new HashMap<>(); for(int i=0;i<kv.length;i+=2) m.put((String)kv[i],kv[i+1]); return m; }
    private BusinessException biz(Runnable r) { return assertThrows(BusinessException.class, r::run); }

    // 设置认证上下文（principal 为用户ID Long类型）
    private void setAuth(Long userId, String role) {
        Authentication auth = new UsernamePasswordAuthenticationToken(userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
        SecurityContextHolder.getContext().setAuthentication(auth);
        // mock getCurrentUserRole() 查询
        when(jdbc.queryForMap(eq("SELECT role FROM users WHERE id = ?"), eq(userId))).thenReturn(Map.of("role", role));
    }

    @Test void 更新个人资料_普通用户修改头像_预期成功() {
        setAuth(5L, "EMPLOYEE");
        when(jdbc.queryForMap(eq("SELECT username, real_name FROM users WHERE id = ? AND deleted = 0"), any(Object[].class))).thenReturn(Map.of("username","EMP001","realName","张三"));
        assertDoesNotThrow(()->ctrl.updateProfile(body("avatar","http://example.com/a.jpg")));
    }

    @Test void 更新个人资料_普通用户修改签名_预期成功() {
        setAuth(5L, "EMPLOYEE");
        when(jdbc.queryForMap(eq("SELECT username, real_name FROM users WHERE id = ? AND deleted = 0"), any(Object[].class))).thenReturn(Map.of("username","EMP001","realName","张三"));
        assertDoesNotThrow(()->ctrl.updateProfile(body("signature","这是我的签名")));
    }

    @Test void 更新个人资料_签名最大长度20字_预期成功() {
        setAuth(5L, "EMPLOYEE");
        when(jdbc.queryForMap(eq("SELECT username, real_name FROM users WHERE id = ? AND deleted = 0"), any(Object[].class))).thenReturn(Map.of("username","EMP001","realName","张三"));
        assertDoesNotThrow(()->ctrl.updateProfile(body("signature","签".repeat(20))));
    }

    @Test void 更新个人资料_签名最大4行_预期成功() {
        setAuth(5L, "EMPLOYEE");
        when(jdbc.queryForMap(eq("SELECT username, real_name FROM users WHERE id = ? AND deleted = 0"), any(Object[].class))).thenReturn(Map.of("username","EMP001","realName","张三"));
        assertDoesNotThrow(()->ctrl.updateProfile(body("signature","第1行\n第2行\n第3行\n第4行")));
    }

    @Test void 更新个人资料_签名超长_预期拒绝() {
        setAuth(5L, "EMPLOYEE");
        when(jdbc.queryForMap(eq("SELECT username, real_name FROM users WHERE id = ? AND deleted = 0"), any(Object[].class))).thenReturn(Map.of("username","EMP001","realName","张三"));
        assertEquals(ErrorCode.BAD_REQUEST, biz(()->ctrl.updateProfile(body("signature","签".repeat(21)))).getErrorCode());
    }

    @Test void 更新个人资料_签名超4行_预期拒绝() {
        setAuth(5L, "EMPLOYEE");
        when(jdbc.queryForMap(eq("SELECT username, real_name FROM users WHERE id = ? AND deleted = 0"), any(Object[].class))).thenReturn(Map.of("username","EMP001","realName","张三"));
        assertEquals(ErrorCode.BAD_REQUEST, biz(()->ctrl.updateProfile(body("signature","1\n2\n3\n4\n5"))).getErrorCode());
    }

    @Test void 更新个人资料_普通用户修改邮箱_预期拒绝() {
        setAuth(5L, "EMPLOYEE");
        when(jdbc.queryForMap(eq("SELECT username, real_name FROM users WHERE id = ? AND deleted = 0"), any(Object[].class))).thenReturn(Map.of("username","EMP001","realName","张三"));
        assertEquals(ErrorCode.FORBIDDEN, biz(()->ctrl.updateProfile(body("email","new@test.com"))).getErrorCode());
    }

    @Test void 更新个人资料_普通用户修改realName_预期拒绝() {
        setAuth(5L, "EMPLOYEE");
        when(jdbc.queryForMap(eq("SELECT username, real_name FROM users WHERE id = ? AND deleted = 0"), any(Object[].class))).thenReturn(Map.of("username","EMP001","realName","张三"));
        assertEquals(ErrorCode.FORBIDDEN, biz(()->ctrl.updateProfile(body("realName","新名字"))).getErrorCode());
    }

    @Test void 更新个人资料_邮箱格式错误_预期拒绝() {
        setAuth(1L, "ADMIN");
        when(jdbc.queryForMap(eq("SELECT username, real_name FROM users WHERE id = ? AND deleted = 0"), any(Object[].class))).thenReturn(Map.of("username","admin","realName","管理员"));
        when(jdbc.queryForObject(eq("SELECT role FROM users WHERE id = ?"), eq(String.class), anyLong())).thenReturn("ADMIN");
        assertEquals(ErrorCode.INVALID_EMAIL_FORMAT, biz(()->ctrl.updateProfile(body("email","invalid-email"))).getErrorCode());
    }

    @Test void 更新个人资料_手机号格式错误_预期拒绝() {
        setAuth(1L, "ADMIN");
        when(jdbc.queryForMap(eq("SELECT username, real_name FROM users WHERE id = ? AND deleted = 0"), any(Object[].class))).thenReturn(Map.of("username","admin","realName","管理员"));
        when(jdbc.queryForObject(eq("SELECT role FROM users WHERE id = ?"), eq(String.class), anyLong())).thenReturn("ADMIN");
        assertEquals(ErrorCode.INVALID_PHONE_FORMAT, biz(()->ctrl.updateProfile(body("phone","12345"))).getErrorCode());
    }

    @Test void 密码修改_正常修改_预期成功() {
        setAuth(1L, "ADMIN");
        when(jdbc.queryForMap(eq("SELECT password_hash FROM users WHERE id = ? AND deleted = 0"), any(Object[].class))).thenReturn(Map.of("password_hash","hashed"));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        assertDoesNotThrow(()->ctrl.changePassword(Map.of("oldPassword","oldPass123","newPassword","newPass456")));
    }

    @Test void 密码修改_非ADMIN操作_预期拒绝() {
        setAuth(5L, "EMPLOYEE");
        assertEquals(ErrorCode.FORBIDDEN, biz(()->ctrl.changePassword(Map.of("oldPassword","oldPass123","newPassword","newPass456"))).getErrorCode());
    }

    @Test void 密码修改_旧密码错误_预期拒绝() {
        setAuth(1L, "ADMIN");
        when(jdbc.queryForMap(eq("SELECT password_hash FROM users WHERE id = ? AND deleted = 0"), any(Object[].class))).thenReturn(Map.of("password_hash","hashed"));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
        assertEquals(ErrorCode.INVALID_CREDENTIALS, biz(()->ctrl.changePassword(Map.of("oldPassword","wrongOld","newPassword","newPass456"))).getErrorCode());
    }

    @Test void 密码修改_新旧密码相同_预期拒绝() {
        setAuth(1L, "ADMIN");
        when(jdbc.queryForMap(eq("SELECT password_hash FROM users WHERE id = ? AND deleted = 0"), any(Object[].class))).thenReturn(Map.of("password_hash","hashed"));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        assertEquals(ErrorCode.BAD_REQUEST, biz(()->ctrl.changePassword(Map.of("oldPassword","samePass","newPassword","samePass"))).getErrorCode());
    }

    @Test void 密码修改_新密码过短_预期拒绝() {
        setAuth(1L, "ADMIN");
        assertEquals(ErrorCode.BAD_REQUEST, biz(()->ctrl.changePassword(Map.of("oldPassword","oldPass123","newPassword","123"))).getErrorCode());
    }

    @Test void 密码修改_新密码超长UTF8_预期拒绝() {
        setAuth(1L, "ADMIN");
        assertEquals(ErrorCode.BAD_REQUEST, biz(()->ctrl.changePassword(Map.of("oldPassword","oldPass123","newPassword","a".repeat(73)))).getErrorCode());
    }

    @Test void 头像上传_JPEG格式_预期成功() throws IOException {
        setAuth(5L, "EMPLOYEE");
        FileStorageService fileStorage = (FileStorageService) ReflectionTestUtils.getField(ctrl, "fileStorageService");
        when(fileStorage.storeAvatar(any(), any())).thenReturn("http://example.com/avatar.jpg");
        MockMultipartFile f=new MockMultipartFile("file","test.jpg","image/jpeg",new byte[1024*1024]);
        assertNotNull(ctrl.uploadAvatar(f,null));
    }

    @Test void 头像上传_PNG格式_预期成功() throws IOException {
        setAuth(5L, "EMPLOYEE");
        FileStorageService fileStorage = (FileStorageService) ReflectionTestUtils.getField(ctrl, "fileStorageService");
        when(fileStorage.storeAvatar(any(), any())).thenReturn("http://example.com/avatar.png");
        MockMultipartFile f=new MockMultipartFile("file","test.png","image/png",new byte[1024*1024]);
        assertNotNull(ctrl.uploadAvatar(f,null));
    }

    @Test void 头像上传_WEBP格式_预期成功() throws IOException {
        setAuth(5L, "EMPLOYEE");
        FileStorageService fileStorage = (FileStorageService) ReflectionTestUtils.getField(ctrl, "fileStorageService");
        when(fileStorage.storeAvatar(any(), any())).thenReturn("http://example.com/avatar.webp");
        MockMultipartFile f=new MockMultipartFile("file","test.webp","image/webp",new byte[1024*1024]);
        assertNotNull(ctrl.uploadAvatar(f,null));
    }

    @Test void 头像上传_最大5MB_预期成功() throws IOException {
        setAuth(5L, "EMPLOYEE");
        FileStorageService fileStorage = (FileStorageService) ReflectionTestUtils.getField(ctrl, "fileStorageService");
        when(fileStorage.storeAvatar(any(), any())).thenReturn("http://example.com/big.jpg");
        MockMultipartFile f=new MockMultipartFile("file","big.jpg","image/jpeg",new byte[5*1024*1024]);
        assertNotNull(ctrl.uploadAvatar(f,null));
    }

    @Test void 头像上传_空文件_预期拒绝() {
        setAuth(5L, "EMPLOYEE");
        MockMultipartFile f=new MockMultipartFile("file","empty.jpg","image/jpeg",new byte[0]);
        assertEquals(ErrorCode.MISSING_REQUIRED_FIELD, biz(()->ctrl.uploadAvatar(f,null)).getErrorCode());
    }

    @Test void 头像上传_文件过大_预期拒绝() {
        setAuth(5L, "EMPLOYEE");
        MockMultipartFile f=new MockMultipartFile("file","huge.jpg","image/jpeg",new byte[6*1024*1024]);
        assertEquals(ErrorCode.FILE_TOO_LARGE, biz(()->ctrl.uploadAvatar(f,null)).getErrorCode());
    }

    @Test void 头像上传_格式不支持_预期拒绝() {
        setAuth(5L, "EMPLOYEE");
        MockMultipartFile f=new MockMultipartFile("file","test.gif","image/gif",new byte[1024]);
        assertEquals(ErrorCode.UNSUPPORTED_IMAGE_FORMAT, biz(()->ctrl.uploadAvatar(f,null)).getErrorCode());
    }
}
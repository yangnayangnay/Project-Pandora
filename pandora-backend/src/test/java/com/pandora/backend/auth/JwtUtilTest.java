package com.pandora.backend.auth;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtil 单元测试——正常/边界/异常全覆盖。
 * 测试 JWT 令牌的生成、解析、校验功能。
 * 使用反射设置私有字段以初始化测试环境。
 */
@DisplayName("JwtUtil 令牌生成与校验")
class JwtUtilTest {

    private JwtUtil jwtUtil;

    /**
     * 测试初始化——创建 JwtUtil 实例并设置私有字段。
     * 使用反射设置 secret、expiration、refreshExpiration。
     */
    @BeforeEach
    void setUp() throws Exception {
        jwtUtil = new JwtUtil();
        setField("secret", "pandora-test-secret-0123456789abcdef0123456789abcdef");
        setField("expiration", 3600_000L);             // 1小时
        setField("refreshExpiration", 86400_000L);     // 24小时
    }

    /**
     * 通过反射设置私有字段值。
     */
    private void setField(String name, Object value) throws Exception {
        Field f = JwtUtil.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(jwtUtil, value);
    }

    /**
     * TC-AUTH-01[正常] 生成令牌后校验应通过。
     * 验证令牌生成和基本校验功能。
     */
    @Test
    @DisplayName("TC-AUTH-01[正常] 生成令牌后校验应通过")
    void tc01_generateThenValidate() {
        String token = jwtUtil.generateToken(1L, "alice", "ADMIN", 10L, 100L);
        assertNotNull(token);
        assertTrue(jwtUtil.validateToken(token));
    }

    /**
     * TC-AUTH-02[正常] 令牌可正确还原 userId/role。
     * 验证令牌解析功能。
     */
    @Test
    @DisplayName("TC-AUTH-02[正常] 令牌可正确还原userId/role")
    void tc02_roundTrip() {
        String token = jwtUtil.generateToken(42L, "bob", "EMPLOYEE", 2L, 3L);
        assertEquals(42L, jwtUtil.getUserIdFromToken(token));
        assertEquals("EMPLOYEE", jwtUtil.getRoleFromToken(token));
        assertEquals("bob", jwtUtil.getUsernameFromToken(token));
    }

    /**
     * TC-AUTH-03[边界] userId 取 Long.MAX_VALUE 可正常往返。
     * 验证边界值处理。
     */
    @Test
    @DisplayName("TC-AUTH-03[边界] userId取Long.MAX_VALUE可正常往返")
    void tc03_maxUserId() {
        String token = jwtUtil.generateToken(Long.MAX_VALUE, "max", "ADMIN", 1L, 1L);
        assertEquals(Long.MAX_VALUE, jwtUtil.getUserIdFromToken(token));
    }

    /**
     * TC-AUTH-04[异常] 非法 token 应判定无效。
     * 使用参数化测试覆盖多种非法输入。
     */
    @ParameterizedTest(name = "TC-AUTH-04[异常] 非法token(\"{0}\")应判定无效")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "garbage.token.here", "a.b.c.d"})
    void tc04_invalidToken(String bad) {
        assertFalse(jwtUtil.validateToken(bad));
    }

    /**
     * TC-AUTH-05[异常] 篡改签名的令牌应判定无效。
     * 验证签名校验功能。
     */
    @Test
    @DisplayName("TC-AUTH-05[异常] 篡改签名的令牌应判定无效")
    void tc05_tamperedToken() {
        String token = jwtUtil.generateToken(1L, "alice", "ADMIN", 1L, 1L);
        String tampered = token.substring(0, token.length() - 3) + "xxx";
        assertFalse(jwtUtil.validateToken(tampered));
    }

    /**
     * TC-AUTH-06[边界] 已过期令牌应判定无效。
     * 验证过期时间校验。
     */
    @Test
    @DisplayName("TC-AUTH-06[边界] 已过期令牌应判定无效")
    void tc06_expiredToken() throws Exception {
        setField("expiration", -1000L);
        String token = jwtUtil.generateToken(1L, "alice", "ADMIN", 1L, 1L);
        assertFalse(jwtUtil.validateToken(token));
    }

    /**
     * TC-AUTH-07[异常] parseToken 解析垃圾串应抛异常。
     */
    @Test
    @DisplayName("TC-AUTH-07[异常] parseToken解析垃圾串应抛异常")
    void tc07_parseGarbage() {
        assertThrows(Exception.class, () -> jwtUtil.parseToken("not-a-jwt"));
    }

    /**
     * TC-AUTH-08[正常] 刷新令牌生成与校验。
     */
    @Test
    @DisplayName("TC-AUTH-08[正常] 刷新令牌生成与校验")
    void tc08_refreshToken() throws Exception {
        setField("refreshExpiration", 86400_000L);
        String refreshToken = jwtUtil.generateRefreshToken(99L, "refresh_user");
        assertNotNull(refreshToken);
        assertTrue(jwtUtil.validateToken(refreshToken));
        assertEquals(99L, jwtUtil.getUserIdFromToken(refreshToken));
    }

    /**
     * TC-AUTH-09[边界] 空角色令牌生成。
     */
    @Test
    @DisplayName("TC-AUTH-09[边界] 空角色令牌生成")
    void tc09_emptyRole() {
        String token = jwtUtil.generateToken(100L, "norole", null, null, null);
        assertNotNull(token);
        assertTrue(jwtUtil.validateToken(token));
        assertEquals(100L, jwtUtil.getUserIdFromToken(token));
    }

    /**
     * TC-AUTH-10[正常] 多角色令牌解析。
     * 验证不同角色的令牌生成和解析。
     */
    @Test
    @DisplayName("TC-AUTH-10[正常] 多角色令牌解析")
    void tc10_multipleRoles() {
        String adminToken = jwtUtil.generateToken(1L, "admin", "ADMIN", 1L, 1L);
        String founderToken = jwtUtil.generateToken(2L, "founder", "FOUNDER", 1L, 1L);
        String employeeToken = jwtUtil.generateToken(3L, "emp", "EMPLOYEE", 2L, 3L);

        assertEquals("ADMIN", jwtUtil.getRoleFromToken(adminToken));
        assertEquals("FOUNDER", jwtUtil.getRoleFromToken(founderToken));
        assertEquals("EMPLOYEE", jwtUtil.getRoleFromToken(employeeToken));
    }

    // ============ V2.0 增量用例 ============

    /**
     * JWT-006[异常] validateToken 对格式错误令牌返回 false。
     */
    @Test
    @DisplayName("JWT-006[异常] validateToken对格式错误令牌(invalid.token.format)返回false")
    void jwt006_malformedFormat() {
        assertFalse(jwtUtil.validateToken("invalid.token.format"));
    }

    /**
     * JWT-RT[正常] generateRefreshToken 生成的刷新令牌可通过校验。
     */
    @Test
    @DisplayName("JWT-RT[正常] generateRefreshToken生成的刷新令牌可通过校验")
    void refreshTokenRoundTrip() {
        String rt = jwtUtil.generateRefreshToken(7L, "carol");
        assertNotNull(rt);
        assertTrue(jwtUtil.validateToken(rt));
    }
}
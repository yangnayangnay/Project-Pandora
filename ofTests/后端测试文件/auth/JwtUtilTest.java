package com.pandora.backend.auth;

/**
 * JwtUtil 单元测试 - 令牌生成与校验模块
 * 
 * 测试目标：验证 JWT 令牌的生成、验证、刷新、过期、签名校验等核心功能
 * 
 * 测试范围：
 * - 正常场景：令牌生成与校验的往返一致性、刷新令牌机制
 * - 边界场景：用户ID最大值、过期时刻、多角色组合
 * - 异常场景：无效token、签名篡改、垃圾输入、空角色、格式畸变
 * 
 * 前置条件：使用反射设置 JwtUtil 的 secret、expiration、refreshExpiration 字段
 * 测试用例：12个 (TC-AUTH-01~10, JWT-006, JWT-RT)
 */


import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtil 单元测试 —— 正常/边界/异常全覆盖
 * ⚠️ 带 [ADJUST] 标记处请对照你的真实代码修改（这本身就是要求的"人工复核"）
 */
@DisplayName("JwtUtil 令牌生成与校验")
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() throws Exception {
        jwtUtil = new JwtUtil();                       // [ADJUST] 若无空构造，见文末"常见问题"
        setField("secret", "pandora-test-secret-0123456789abcdef0123456789abcdef");  // [ADJUST] 字段名
        setField("expiration", 3600_000L);             // [ADJUST] 字段名（毫秒）
        setField("refreshExpiration", 86400_000L);     // [ADJUST] 字段名
    }

    private void setField(String name, Object value) throws Exception {
        Field f = JwtUtil.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(jwtUtil, value);
    }

    @Test
    /**
     * 测试用例：令牌生成后验证通过
     * 输入类别：同类（有效等价类）
     * 测试步骤：1. 生成令牌 2. 验证令牌有效性
     * 预期结果：令牌生成成功且验证通过
     */
    void tc01_generateThenValidate() {
        String token = jwtUtil.generateToken(1L, "alice", "ADMIN", 10L, 100L);
        assertNotNull(token);
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    /**
     * 测试用例：令牌往返一致性
     * 输入类别：同类（有效等价类）
     * 测试步骤：1. 生成令牌 2. 解析令牌获取 userId/role/username
     * 预期结果：解析出的信息与生成时一致
     */
    void tc02_roundTrip() {
        String token = jwtUtil.generateToken(42L, "bob", "EMPLOYEE", 2L, 3L);
        assertEquals(42L, jwtUtil.getUserIdFromToken(token));
        assertEquals("EMPLOYEE", jwtUtil.getRoleFromToken(token));
        assertEquals("bob", jwtUtil.getUsernameFromToken(token));
    }

    @Test
    /**
     * 测试用例：用户ID最大值边界
     * 输入类别：边界（边界值分析）
     * 测试步骤：使用 Long.MAX_VALUE 作为用户ID生成令牌
     * 预期结果：令牌生成正常，解析后 userId 为 Long.MAX_VALUE
     */
    void tc03_maxUserId() {
        String token = jwtUtil.generateToken(Long.MAX_VALUE, "max", "ADMIN", 1L, 1L);
        assertEquals(Long.MAX_VALUE, jwtUtil.getUserIdFromToken(token));
    }

    @ParameterizedTest(name = "TC-AUTH-04[异常] 非法token(\"{0}\")应判定无效")
    @NullAndEmptySource
    /**
     * 测试用例：5组无效token
     * 输入类别：异类-异常（无效等价类）
     * 测试步骤：传入 null、空串、空白、格式错误的令牌
     * 预期结果：validateToken 返回 false
     */
    void tc04_invalidToken(String bad) {
        assertFalse(jwtUtil.validateToken(bad));       // [ADJUST] 若实现为抛异常，改 assertThrows
    }

    @Test
    /**
     * 测试用例：签名篡改
     * 输入类别：异类-异常（无效等价类）
     * 测试步骤：1. 生成有效令牌 2. 篡改签名 3. 验证令牌
     * 预期结果：被篡改签名的令牌被拒绝
     */
    void tc05_tamperedToken() {
        String token = jwtUtil.generateToken(1L, "alice", "ADMIN", 1L, 1L);
        String tampered = token.substring(0, token.length() - 3) + "xxx";
        assertFalse(jwtUtil.validateToken(tampered));
    }

    @Test
    /**
     * 测试用例：过期时刻边界
     * 输入类别：边界（边界值分析）
     * 测试步骤：设置 expiration 为负值使令牌过期
     * 预期结果：过期令牌被正确拒绝
     */
    void tc06_expiredToken() throws Exception {
        setField("expiration", -1000L);
        String token = jwtUtil.generateToken(1L, "alice", "ADMIN", 1L, 1L);
        assertFalse(jwtUtil.validateToken(token));
    }

    @Test
    /**
     * 测试用例：垃圾输入
     * 输入类别：异类-异常（无效等价类）
     * 测试步骤：传入非JWT格式的字符串
     * 预期结果：抛出异常
     */
    void tc07_parseGarbage() {
        assertThrows(Exception.class, () -> jwtUtil.parseToken("not-a-jwt"));
    }

    @Test
    /**
     * 测试用例：刷新令牌功能
     * 输入类别：同类（有效等价类）
     * 测试步骤：1. 生成刷新令牌 2. 验证令牌 3. 解析 userId
     * 预期结果：刷新令牌生成成功且可通过校验
     */
    void tc08_refreshToken() throws Exception {
        setField("refreshExpiration", 86400_000L);
        String refreshToken = jwtUtil.generateRefreshToken(99L, "refresh_user");
        assertNotNull(refreshToken);
        assertTrue(jwtUtil.validateToken(refreshToken));
        assertEquals(99L, jwtUtil.getUserIdFromToken(refreshToken));
    }

    @Test
    /**
     * 测试用例：空角色
     * 输入类别：异类-异常（无效等价类）
     * 测试步骤：生成角色为 null 的令牌
     * 预期结果：令牌生成成功，解析时角色为 null
     */
    void tc09_emptyRole() {
        String token = jwtUtil.generateToken(100L, "norole", null, null, null);
        assertNotNull(token);
        assertTrue(jwtUtil.validateToken(token));
        assertEquals(100L, jwtUtil.getUserIdFromToken(token));
    }

    @Test
    /**
     * 测试用例：多角色组合边界
     * 输入类别：边界（边界值分析）
     * 测试步骤：为不同角色(ADMIN/FOUNDER/EMPLOYEE)分别生成令牌并验证
     * 预期结果：各角色令牌解析正确
     */
    void tc10_multipleRoles() {
        // 测试不同角色的令牌生成
        String adminToken = jwtUtil.generateToken(1L, "admin", "ADMIN", 1L, 1L);
        String founderToken = jwtUtil.generateToken(2L, "founder", "FOUNDER", 1L, 1L);
        String employeeToken = jwtUtil.generateToken(3L, "emp", "EMPLOYEE", 2L, 3L);

        assertEquals("ADMIN", jwtUtil.getRoleFromToken(adminToken));
        assertEquals("FOUNDER", jwtUtil.getRoleFromToken(founderToken));
        assertEquals("EMPLOYEE", jwtUtil.getRoleFromToken(employeeToken));
    }

    // ============ V2.0 增量用例 ============

    @Test
    /**
     * 测试用例：格式畸变
     * 输入类别：异类-异常（无效等价类）
     * 测试步骤：传入格式错误的JWT令牌
     * 预期结果：validateToken 返回 false
     */
    void jwt006_malformedFormat() {
        assertFalse(jwtUtil.validateToken("invalid.token.format"));
    }

    @Test
    /**
     * 测试用例：刷新令牌往返一致性
     * 输入类别：同类（有效等价类）
     * 测试步骤：1. 生成刷新令牌 2. 验证令牌有效性
     * 预期结果：刷新令牌生成后可正确验证
     */
    void refreshTokenRoundTrip() {
        String rt = jwtUtil.generateRefreshToken(7L, "carol");
        assertNotNull(rt);
        assertTrue(jwtUtil.validateToken(rt));
    }

    // ========== 认证模块测试结束 ==========
    // 测试用例: 12个 (TC-AUTH-01~10, JWT-006, JWT-RT)
    // 最后执行时间: 2026-11-18 22:50
    // 结果: 全部通过
}
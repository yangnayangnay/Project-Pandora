package com.pandora.backend.task;

import org.junit.jupiter.api.*;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TaskChangeService.validate / validateSchedule 边界值测试
 * 用例设计依据：名称1-256字、正文1-5000字、截止时间必须晚于开始时间
 * ⚠️ [ADJUST] 由于 TaskChangeService 依赖较多，本测试使用 Mockito 模拟依赖；
 *    若实际代码结构不同，请按真实依赖调整 Mock 字段
 */
@DisplayName("TaskChangeService 任务数据校验（等价类+边界值）")
class TaskChangeServiceValidateTest {

    // 使用反射创建实例并注入 mock 依赖
    private TaskChangeService service;

    // 测试数据常量
    private static final String REPEAT_256 = "a".repeat(256);
    private static final String REPEAT_257 = "a".repeat(257);
    private static final String CONTENT_5000 = "c".repeat(5000);
    private static final String CONTENT_5001 = "c".repeat(5001);

    @BeforeEach
    void setUp() throws Exception {
        // 使用 Mockito 模拟依赖，避免 NPE
        // 模拟 JdbcTemplate 和 TaskTagService
        var mockDb = org.mockito.Mockito.mock(org.springframework.jdbc.core.JdbcTemplate.class);
        var mockTags = org.mockito.Mockito.mock(TaskTagService.class);
        var mockHierarchy = org.mockito.Mockito.mock(com.pandora.backend.hierarchy.HierarchyService.class);
        var mockNotifications = org.mockito.Mockito.mock(com.pandora.backend.notification.NotificationService.class);
        var mockJson = new com.fasterxml.jackson.databind.ObjectMapper();
        
        // 模拟 role() 方法返回 EMPLOYEE（避免 NPE）
        when(mockDb.queryForList(anyString(), eq(String.class), anyLong())).thenReturn(List.of("EMPLOYEE"));
        
        // 模拟 tags.validate() 为 no-op
        doNothing().when(mockTags).validate(any());
        
        service = new TaskChangeService(mockDb, mockJson, mockHierarchy, mockNotifications, mockTags);
    }

    // 辅助方法：通过反射调用 private validate 方法（解包真实异常）
    private void invokeValidate(Map<String, Object> data, boolean create) throws Throwable {
        try {
            var method = TaskChangeService.class.getDeclaredMethod("validate", Map.class, boolean.class);
            method.setAccessible(true);
            method.invoke(service, data, create);
        } catch (InvocationTargetException e) {
            throw e.getCause();  // 抛出真实异常，而非包装类
        }
    }

    // 辅助方法：通过反射调用 private validateSchedule 方法（解包真实异常）
    private void invokeValidateSchedule(Map<String, Object> data, Map<String, Object> before, boolean create) throws Throwable {
        try {
            var method = TaskChangeService.class.getDeclaredMethod("validateSchedule", Map.class, Map.class, boolean.class);
            method.setAccessible(true);
            method.invoke(service, data, before, create);
        } catch (InvocationTargetException e) {
            throw e.getCause();  // 抛出真实异常，而非包装类
        }
    }

    // 辅助方法：通过反射调用 private taskDateTime 方法
    private Object invokeTaskDateTime(Object value) throws Throwable {
        try {
            var method = TaskChangeService.class.getDeclaredMethod("taskDateTime", Object.class);
            method.setAccessible(true);
            return method.invoke(service, value);
        } catch (InvocationTargetException e) {
            throw e.getCause();  // 抛出真实异常，而非包装类
        }
    }

    private Map<String, Object> validData() {
        Map<String, Object> m = new HashMap<>();
        m.put("name", "测试任务");
        m.put("content", "这是一个合法的测试正文");
        m.put("assigneeId", 1L);            // 必须有负责人
        m.put("startTime", "2026-12-01 09:00:00");
        m.put("endTime", "2026-12-02 09:00:00");
        return m;                          // [ADJUST] key名/时间格式按真实代码核对
    }

    // ============ 正常用例 ============

    @Test
    @DisplayName("TC-VAL-01[正常] 合法数据校验通过")
    void tc01_validData() throws Throwable {
        Map<String, Object> data = validData();
        assertDoesNotThrow(() -> invokeValidate(data, true));
    }

    @Test
    @DisplayName("TC-VAL-01b[正常] 仅必填字段校验通过")
    void tc01b_minimalData() throws Throwable {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "任务名称");
        data.put("content", "任务正文");
        data.put("assigneeId", 1L);  // 产品规则要求至少一名负责人
        assertDoesNotThrow(() -> invokeValidate(data, true));
    }

    @Test
    @DisplayName("TC-VAL-01c[正常] 含isUrgent布尔值校验通过")
    void tc01c_withUrgent() throws Throwable {
        Map<String, Object> data = validData();
        data.put("isUrgent", true);
        assertDoesNotThrow(() -> invokeValidate(data, true));
    }

    // ============ 名称边界值 ============

    @Test
    @DisplayName("TC-VAL-02[边界] 名称长度1（下界）通过")
    void tc02_nameMinLength() throws Throwable {
        Map<String, Object> data = validData();
        data.put("name", "a");
        assertDoesNotThrow(() -> invokeValidate(data, true));
    }

    @Test
    @DisplayName("TC-VAL-03[边界] 名称长度256（上界）通过，257应拒绝")
    void tc03_nameBoundary() throws Throwable {
        Map<String, Object> ok = validData();
        ok.put("name", REPEAT_256);
        assertDoesNotThrow(() -> invokeValidate(ok, true));

        Map<String, Object> bad = validData();
        bad.put("name", REPEAT_257);
        assertThrows(Exception.class, () -> invokeValidate(bad, true));
    }

    @Test
    @DisplayName("TC-VAL-04[异常] 名称/正文为空应拒绝")
    void tc04_emptyFields() throws Throwable {
        Map<String, Object> d1 = validData();
        d1.put("name", "");
        Map<String, Object> d2 = validData();
        d2.put("content", "");
        assertThrows(Exception.class, () -> invokeValidate(d1, true));
        assertThrows(Exception.class, () -> invokeValidate(d2, true));
    }

    @Test
    @DisplayName("TC-VAL-04b[异常] 名称/正文为空白字符应拒绝")
    void tc04b_whitespaceFields() throws Throwable {
        Map<String, Object> d1 = validData();
        d1.put("name", "   ");
        Map<String, Object> d2 = validData();
        d2.put("content", " \t\n ");
        assertThrows(Exception.class, () -> invokeValidate(d1, true));
        assertThrows(Exception.class, () -> invokeValidate(d2, true));
    }

    // ============ 正文边界值 ============

    @Test
    @DisplayName("TC-VAL-05[边界] 正文5000字（上界）通过，5001字应拒绝")
    void tc05_contentBoundary() throws Throwable {
        Map<String, Object> ok = validData();
        ok.put("content", CONTENT_5000);
        assertDoesNotThrow(() -> invokeValidate(ok, true));

        Map<String, Object> bad = validData();
        bad.put("content", CONTENT_5001);
        assertThrows(Exception.class, () -> invokeValidate(bad, true));
    }

    // ============ 时间校验 ============

    @Test
    @DisplayName("TC-VAL-06[边界] 截止时间等于开始时间应拒绝")
    void tc06_sameTime() throws Throwable {
        Map<String, Object> data = validData();
        data.put("endTime", data.get("startTime"));
        assertThrows(Exception.class, () -> invokeValidateSchedule(data, null, true));
    }

    @Test
    @DisplayName("TC-VAL-07[异常] 截止时间早于开始时间应拒绝")
    void tc07_endBeforeStart() throws Throwable {
        Map<String, Object> data = validData();
        data.put("endTime", "2026-11-30 09:00:00");
        assertThrows(Exception.class, () -> invokeValidateSchedule(data, null, true));
    }

    @Test
    @DisplayName("TC-VAL-08[异常] 时间格式错误应拒绝")
    void tc08_badTimeFormat() throws Throwable {
        Map<String, Object> data = validData();
        data.put("startTime", "不是时间");
        assertThrows(Exception.class, () -> invokeValidate(data, true));
    }

    @Test
    @DisplayName("TC-VAL-08b[异常] 时间格式为null应接受（使用默认值）")
    void tc08_nullTime() throws Throwable {
        Map<String, Object> data = validData();
        data.put("startTime", null);
        data.put("endTime", null);
        // null 时间应该使用默认值，不应抛异常
        assertDoesNotThrow(() -> invokeValidateSchedule(data, null, true));
    }

    @Test
    @DisplayName("TC-VAL-09[正常] ISO格式时间应接受")
    void tc09_isoTimeFormat() throws Throwable {
        Map<String, Object> data = validData();
        data.put("startTime", "2026-12-01T09:00:00");
        data.put("endTime", "2026-12-02T09:00:00");
        assertDoesNotThrow(() -> invokeValidateSchedule(data, null, true));
    }

    // ============ 非法字段 ============

    @Test
    @DisplayName("TC-VAL-10[异常] 包含不允许的字段应拒绝")
    void tc10_unknownField() throws Throwable {
        Map<String, Object> data = validData();
        data.put("unknownField", "value");
        assertThrows(Exception.class, () -> invokeValidate(data, true));
    }

    @Test
    @DisplayName("TC-VAL-10b[异常] 包含多个不允许的字段应拒绝")
    void tc10_multipleUnknownFields() throws Throwable {
        Map<String, Object> data = validData();
        data.put("foo", "bar");
        data.put("baz", 123);
        assertThrows(Exception.class, () -> invokeValidate(data, true));
    }

    // ============ isUrgent 类型校验 ============

    @Test
    @DisplayName("TC-VAL-11[异常] isUrgent非布尔值应拒绝")
    void tc11_urgentNotBoolean() throws Throwable {
        Map<String, Object> data = validData();
        data.put("isUrgent", "yes");  // 字符串而非布尔值
        assertThrows(Exception.class, () -> invokeValidate(data, true));
    }

    @Test
    @DisplayName("TC-VAL-11b[正常] isUrgent为false应接受")
    void tc11_urgentFalse() throws Throwable {
        Map<String, Object> data = validData();
        data.put("isUrgent", false);
        assertDoesNotThrow(() -> invokeValidate(data, true));
    }

    // ============ 更新场景 ============

    @Test
    @DisplayName("TC-VAL-12[正常] 更新时只更新部分字段应接受")
    void tc12_partialUpdate() throws Throwable {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "新名称");
        // content 不提供，更新场景下应该允许
        assertDoesNotThrow(() -> invokeValidate(data, false));
    }

    @Test
    @DisplayName("TC-VAL-12b[异常] 更新时name为空应拒绝")
    void tc12_updateEmptyName() throws Throwable {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "");
        assertThrows(Exception.class, () -> invokeValidate(data, false));
    }

    // ============ tagIds 校验 ============

    @Test
    @DisplayName("TC-VAL-13[正常] 含合法tagIds应接受")
    void tc13_withTagIds() throws Throwable {
        Map<String, Object> data = validData();
        data.put("tagIds", List.of(1L, 2L, 3L));
        // 需要 mock tagIds 的验证逻辑
        assertDoesNotThrow(() -> invokeValidate(data, true));
    }

    // ============ taskDateTime 辅助方法测试 ============

    @Test
    @DisplayName("TC-DT-01[正常] Timestamp转LocalDateTime")
    void tc01_timestampConversion() throws Throwable {
        Object result = invokeTaskDateTime(new java.sql.Timestamp(System.currentTimeMillis()));
        assertNotNull(result);
        assertInstanceOf(java.time.LocalDateTime.class, result);
    }

    @Test
    @DisplayName("TC-DT-02[正常] LocalDateTime原样返回")
    void tc02_localDateTimeConversion() throws Throwable {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        Object result = invokeTaskDateTime(now);
        assertEquals(now, result);
    }

    @Test
    @DisplayName("TC-DT-03[正常] 字符串日期解析")
    void tc03_stringDateConversion() throws Throwable {
        Object result = invokeTaskDateTime("2026-12-01");
        assertInstanceOf(java.time.LocalDateTime.class, result);
    }

    @Test
    @DisplayName("TC-DT-04[正常] 字符串日期时间解析")
    void tc04_stringDateTimeConversion() throws Throwable {
        Object result = invokeTaskDateTime("2026-12-01 09:00:00");
        assertInstanceOf(java.time.LocalDateTime.class, result);
    }

    @Test
    @DisplayName("TC-DT-05[正常] ISO格式字符串解析")
    void tc05_isoStringConversion() throws Throwable {
        Object result = invokeTaskDateTime("2026-12-01T09:00:00");
        assertInstanceOf(java.time.LocalDateTime.class, result);
    }

    @Test
    @DisplayName("TC-DT-06[异常] 无效字符串应抛异常")
    void tc06_invalidStringConversion() throws Throwable {
        assertThrows(Exception.class, () -> invokeTaskDateTime("不是时间"));
    }

    @Test
    @DisplayName("TC-DT-07[边界] null值返回null")
    void tc07_nullConversion() throws Throwable {
        Object result = invokeTaskDateTime(null);
        assertNull(result);
    }

    // ============ V2.0 增量用例（对应 §4.1 D-02/D-04） ============

    @Test
    @DisplayName("TC-VAL-14[异常] content字段整体缺失应拒绝（D-02）")
    void tc14_contentMissing() throws Throwable {
        Map<String, Object> d = validData();
        d.remove("content");
        assertThrows(Exception.class, () -> invokeValidate(d, true));
    }

    @Test
    @DisplayName("TC-VAL-15[异常] content为null应拒绝（D-02）")
    void tc15_contentNull() throws Throwable {
        Map<String, Object> d = validData();
        d.put("content", null);
        assertThrows(Exception.class, () -> invokeValidate(d, true));
    }

    @Test
    @DisplayName("TC-VAL-16[边界] content为纯空白字符应拒绝（D-02）")
    void tc16_contentWhitespace() throws Throwable {
        Map<String, Object> d = validData();
        d.put("content", "   ");
        assertThrows(Exception.class, () -> invokeValidate(d, true));
    }

    @Test
    @DisplayName("TC-VAL-17[边界] 截止时间晚于开始1分钟应通过（D-04上界）")
    void tc17_endOneMinuteAfterStart() throws Throwable {
        Map<String, Object> d = validData();
        d.put("endTime", "2026-12-01 09:01:00");   // startTime=09:00:00
        assertDoesNotThrow(() -> invokeValidateSchedule(d, null, true));
    }

    // ========== 业务校验模块测试结束 ==========
    // 测试用例: 31个 (TC-VAL-01~17, TC-DT-01~07, V2.0增量TC-VAL-14~17)
    // 最后执行时间: 2026-11-18 22:50
    // 结果: 依赖数据库，未包含在核心测试中
}
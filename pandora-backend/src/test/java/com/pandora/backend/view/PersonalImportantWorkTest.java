package com.pandora.backend.view;

import com.pandora.backend.common.ApiResponse;
import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.ErrorCode;
import com.pandora.backend.log.LogController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersonalImportantWorkTest {
    private static final long USER_1 = 1L;
    private static final long USER_2 = 2L;
    private static final AtomicInteger DB_SEQUENCE = new AtomicInteger();

    private JdbcTemplate db;
    private ViewController viewController;
    private LogController logController;
    private PersonalTop10Controller personalTop10Controller;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:personal-important-work-" + DB_SEQUENCE.incrementAndGet()
                        + ";MODE=MySQL;DB_CLOSE_DELAY=-1",
                "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("db/h2_schema.sql")).execute(dataSource);
        db = new JdbcTemplate(dataSource);
        db.update("DELETE FROM top10_works");
        db.update("DELETE FROM work_logs");
        db.update("DELETE FROM tasks");
        db.update("DELETE FROM users");
        seedUsers();

        PersonalImportantWorkService sharedService = new PersonalImportantWorkService(db);
        viewController = new ViewController();
        ReflectionTestUtils.setField(viewController, "jdbcTemplate", db);
        ReflectionTestUtils.setField(viewController, "taskTags", new com.pandora.backend.task.TaskTagService(db));
        ReflectionTestUtils.setField(viewController, "personalImportantWorks", sharedService);

        logController = new LogController();
        ReflectionTestUtils.setField(logController, "jdbcTemplate", db);
        ReflectionTestUtils.setField(logController, "personalImportantWorks", sharedService);

        personalTop10Controller = new PersonalTop10Controller();
        ReflectionTestUtils.setField(personalTop10Controller, "jdbcTemplate", db);

        loginAs(USER_1);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void profileAndBirthdayDoNotStandInForMissingSavedWorks() {
        java.sql.Date storedBirthday = db.queryForObject(
                "SELECT birthday FROM users WHERE id = ?", java.sql.Date.class, USER_1);
        assertNotNull(storedBirthday);
        LocalDate today = LocalDate.now();
        assertEquals(today.getMonthValue(), storedBirthday.toLocalDate().getMonthValue());
        assertEquals(today.getDayOfMonth(), storedBirthday.toLocalDate().getDayOfMonth());

        assertTrue(rows(mindmap().get("personalImportant")).isEmpty());
        assertTrue(editorPersonal().isEmpty());
    }

    @Test
    void bothViewsReturnOnlyCurrentUsersTwoSavedActiveRowsInRankOrder() {
        saveWork(501L, USER_1, "PERSONAL_IMPORTANT", 7, "第七个保存的个人事项", 0);
        saveWork(502L, USER_1, "PERSONAL_IMPORTANT", 2, "第二个保存的个人事项", 0);
        saveWork(503L, USER_2, "PERSONAL_IMPORTANT", 1, "其他用户的个人事项", 0);
        saveWork(504L, USER_1, "PERSONAL_IMPORTANT", 3, "当前用户已删除事项", 1);
        saveWork(505L, USER_1, "COMPANY_IMPORTANT", 1, "我的公司重要事项", 0);

        List<Map<String, Object>> expected = List.of(
                compact(502L, 2, "第二个保存的个人事项"),
                compact(501L, 7, "第七个保存的个人事项"));
        assertBothEndpoints(expected);

        Map<String, Object> mindMap = mindmap();
        assertTrue(mindMap.containsKey("companyDispatch"));
        assertTrue(mindMap.containsKey("personalImportant"));
        assertTrue(mindMap.containsKey("personalLog"));

        Map<String, Object> companyCard = rows(mindMap.get("companyImportant")).stream()
                .filter(card -> number(card, "id") == 505L)
                .findFirst()
                .orElseThrow();
        assertEquals(1L, number(companyCard, "rank_order"));
        assertEquals("我的公司重要事项", value(companyCard, "content"));
    }

    @Test
    void injectUpdateAndDeleteAreVisibleThroughBothViews() {
        String createdContent = "接口创建：季度复盘（第一版）。";
        ResponseEntity<ApiResponse<Map<String, Object>>> created = personalTop10Controller.inject(
                Map.of("content", createdContent, "rankOrder", 3));
        assertNotNull(created.getBody());
        assertEquals(200, created.getBody().getCode());
        long createdId = number(created.getBody().getData(), "id");
        assertTrue(createdId > 0);
        assertBothEndpoints(createdId, 3, createdContent);

        String updatedContent = "接口更新：季度复盘（终版）。";
        ResponseEntity<ApiResponse<Map<String, Object>>> updated = personalTop10Controller.update(
                createdId, Map.of("content", updatedContent));
        assertNotNull(updated.getBody());
        assertEquals(200, updated.getBody().getCode());
        assertEquals(createdId, number(updated.getBody().getData(), "id"));
        assertBothEndpoints(createdId, 3, updatedContent);

        ResponseEntity<ApiResponse<Void>> deleted = personalTop10Controller.delete(createdId);
        assertNotNull(deleted.getBody());
        assertEquals(200, deleted.getBody().getCode());
        assertTrue(rows(mindmap().get("personalImportant")).isEmpty());
        assertTrue(editorPersonal().isEmpty());
    }

    @Test
    void anotherUserCannotUpdateOrDeleteTheOwnersRow() {
        saveWork(601L, USER_1, "PERSONAL_IMPORTANT", 2, "用户一独有事项", 0);
        saveWork(602L, USER_2, "PERSONAL_IMPORTANT", 4, "用户二独有事项", 0);

        loginAs(USER_1);
        assertBothEndpoints(601L, 2, "用户一独有事项");

        loginAs(USER_2);
        assertBothEndpoints(602L, 4, "用户二独有事项");

        BusinessException updateError = assertThrows(BusinessException.class,
                () -> personalTop10Controller.update(601L, Map.of("content", "越权修改")));
        assertEquals(ErrorCode.PERSONAL_TOP10_INJECT_FORBIDDEN, updateError.getErrorCode());

        BusinessException deleteError = assertThrows(BusinessException.class,
                () -> personalTop10Controller.delete(601L));
        assertEquals(ErrorCode.PERSONAL_TOP10_INJECT_FORBIDDEN, deleteError.getErrorCode());

        assertEquals("用户一独有事项", db.queryForObject(
                "SELECT content FROM top10_works WHERE id = ?", String.class, 601L));
        Number deleted = db.queryForObject(
                "SELECT deleted FROM top10_works WHERE id = ?", Number.class, 601L);
        assertEquals(0, deleted.intValue());

        loginAs(USER_1);
        assertBothEndpoints(601L, 2, "用户一独有事项");
    }

    @Test
    void bothViewsLimitToTenOwnRowsAndKeepIdOrderForDuplicateRanks() {
        long[] ids = {701L, 702L, 703L, 704L, 705L, 706L, 707L, 708L, 709L, 710L, 711L, 712L};
        int[] ranks = {3, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 10};
        for (int i = 0; i < ids.length; i++) {
            saveWork(ids[i], USER_1, "PERSONAL_IMPORTANT", ranks[i], "个人事项-" + ids[i], 0);
        }
        saveWork(799L, USER_2, "PERSONAL_IMPORTANT", 1, "其他用户的个人事项", 0);

        List<Map<String, Object>> expected = List.of(
                compact(702L, 1, "个人事项-702"),
                compact(703L, 2, "个人事项-703"),
                compact(701L, 3, "个人事项-701"),
                compact(704L, 3, "个人事项-704"),
                compact(705L, 4, "个人事项-705"),
                compact(706L, 5, "个人事项-706"),
                compact(707L, 6, "个人事项-707"),
                compact(708L, 7, "个人事项-708"),
                compact(709L, 8, "个人事项-709"),
                compact(710L, 9, "个人事项-710"));
        assertBothEndpoints(expected);
    }

    @Test
    void legacyNullCategoryStillAppearsInBothViews() {
        saveWork(801L, USER_1, "PERSONAL_IMPORTANT", 4, "旧版无分类保存事项", 0);
        db.update("UPDATE top10_works SET category = NULL WHERE id = ?", 801L);
        saveWork(802L, USER_1, "PERSONAL_IMPORTANT", 6, "个人信息分类事项", 0);
        db.update("UPDATE top10_works SET category = 'PERSONAL_INFO' WHERE id = ?", 802L);

        List<Map<String, Object>> expected = List.of(
                compact(801L, 4, "旧版无分类保存事项"),
                compact(802L, 6, "个人信息分类事项"));
        assertBothEndpoints(expected);
    }

    private void seedUsers() {
        LocalDate today = LocalDate.now();
        java.sql.Date birthday = java.sql.Date.valueOf(today.withYear(2000));
        db.update("INSERT INTO users (id, username, password_hash, role, real_name, phone, email, "
                        + "birthday, approval_status, deleted) "
                        + "VALUES (?, ?, 'hash', 'EMPLOYEE', ?, ?, ?, ?, 'APPROVED', 0)",
                USER_1, "user1", "员工一", "13800000001", "user1@example.com", birthday);
        db.update("INSERT INTO users (id, username, password_hash, role, approval_status, deleted) "
                        + "VALUES (?, ?, 'hash', 'EMPLOYEE', 'APPROVED', 0)",
                USER_2, "user2");
    }

    private void saveWork(long id, long userId, String workType, int rank, String content, int deleted) {
        db.update("INSERT INTO top10_works (id, user_id, work_type, rank_order, content, deleted) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                id, userId, workType, rank, content, deleted);
    }

    private void loginAs(long userId) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))));
    }

    private Map<String, Object> mindmap() {
        ResponseEntity<ApiResponse<Map<String, Object>>> response = viewController.getMindMap(null);
        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().getCode());
        return response.getBody().getData();
    }

    private List<Map<String, Object>> editorPersonal() {
        ResponseEntity<ApiResponse<List<Map<String, Object>>>> response =
                logController.getTop10("PERSONAL_IMPORTANT");
        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().getCode());
        return response.getBody().getData();
    }

    private void assertBothEndpoints(long id, int rank, String content) {
        assertBothEndpoints(List.of(compact(id, rank, content)));
    }

    private void assertBothEndpoints(List<Map<String, Object>> expected) {
        assertEquals(expected, projection(rows(mindmap().get("personalImportant"))));
        assertEquals(expected, projection(editorPersonal()));
    }

    private static Map<String, Object> compact(long id, int rank, String content) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("rankOrder", rank);
        row.put("content", content);
        return row;
    }

    private static List<Map<String, Object>> projection(List<Map<String, Object>> rows) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            result.add(compact(number(row, "id"), (int) number(row, "rank_order"),
                    (String) value(row, "content")));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> rows(Object value) {
        return (List<Map<String, Object>>) value;
    }

    private static long number(Map<String, Object> row, String key) {
        return ((Number) value(row, key)).longValue();
    }

    private static Object value(Map<String, Object> row, String key) {
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (key.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }
}

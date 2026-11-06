package com.pandora.backend.daily;

import com.pandora.backend.common.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DailyItemServiceTest {
    private JdbcTemplate db;
    private DailyItemService service;
    private long id(Map<String, Object> row) { return ((Number) row.get("id")).longValue(); }
    @BeforeEach void setup() {
        var source = new DriverManagerDataSource("jdbc:h2:mem:daily-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("db/h2_schema.sql")).execute(source);
        db = new JdbcTemplate(source); service = new DailyItemService(db);
        db.update("DELETE FROM users");
        for (int i = 1; i <= 4; i++) db.update("INSERT INTO users(id,username,password_hash,role,approval_status) VALUES (?,?,'hash',?,'APPROVED')", i, "daily-user" + i, i == 1 ? "EMPLOYEE" : i == 2 ? "FOUNDER" : i == 3 ? "ADMIN" : "EMPLOYEE");
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    private Map<String, Object> create(long actor, String date, String content) { return service.create(actor, Map.of("content", content, "itemDate", date)); }

    @Test void persistsContentDateAndCompletionWithoutCreatingTasksOrLogs() {
        Map<String, Object> first = create(1, "2026-11-03", " 买菜 ");
        assertEquals("买菜", first.get("content")); assertEquals("2026-11-03", first.get("item_date")); assertEquals(false, first.get("completed"));
        assertEquals(true, service.update(1, id(first), Map.of("completed", true)).get("completed"));
        assertEquals(false, service.update(1, id(first), Map.of("completed", false)).get("completed"));
        DailyItemService reloaded = new DailyItemService(db);
        assertEquals("买菜", reloaded.list(1, "2026-11").get(0).get("content"));
        assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM tasks", Integer.class));
        assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM work_logs", Integer.class));
    }
    @ParameterizedTest @ValueSource(longs = {2, 3, 4})
    void allOtherRolesIncludingFounderAndAdminCannotReadOrWriteAnothersItems(long actor) {
        long id = id(create(1, "2026-11-03", "个人隐私事项"));
        assertTrue(service.list(actor, "2026-11").isEmpty());
        for (Map<String, Object> patch : List.<Map<String, Object>>of(Map.of("content", "越权编辑"), Map.of("completed", true), Map.of("itemDate", "2026-11-04"))) {
            BusinessException error = assertThrows(BusinessException.class, () -> service.update(actor, id, patch)); assertEquals(ErrorCode.NOT_FOUND, error.getErrorCode());
        }
        assertThrows(BusinessException.class, () -> service.delete(actor, id));
        assertEquals("个人隐私事项", service.list(1, "2026-11").get(0).get("content"));
    }
    @Test void monthBoundariesLeapDayAndMovingDatesAreAccurate() {
        create(1, "2026-10-31", "上个月"); create(1, "2026-11-01", "月初");
        long moved = id(create(1, "2026-11-30", "月底")); create(1, "2026-12-01", "下个月");
        assertEquals(2, service.list(1, "2026-11").size());
        service.update(1, moved, Map.of("itemDate", "2026-12-02", "content", "改期事项"));
        assertEquals(1, service.list(1, "2026-11").size()); assertEquals(2, service.list(1, "2026-12").size());
        create(1, "2028-02-29", "闰年事项"); assertEquals(1, service.list(1, "2028-02").size());
    }
    @Test void deletingItemDoesNotDeleteOtherItemsAndItCannotBeReopened() {
        long removed = id(create(1, "2026-11-03", "删除事项")); create(1, "2026-11-03", "保留事项");
        service.delete(1, removed); assertEquals(1, service.list(1, "2026-11").size());
        assertThrows(BusinessException.class, () -> service.update(1, removed, Map.of("completed", false)));
        assertThrows(BusinessException.class, () -> service.delete(1, removed));
    }
    @Test void allowsMultipleItemsOnOneDateAndKeepsOpenItemsFirst() {
        long first = id(create(1, "2026-11-03", "事项一")); long second = id(create(1, "2026-11-03", "事项二"));
        service.update(1, first, Map.of("completed", true));
        assertEquals(second, id(service.list(1, "2026-11").get(0)));
        assertEquals(2, service.list(1, "2026-11").size());
    }
    @Test void validatesContentDateStatusAndRejectsOwnershipInjection() {
        for (String bad : List.of("", "　", "x".repeat(501))) assertThrows(BusinessException.class, () -> create(1, "2026-11-03", bad));
        for (String bad : List.of("2026-02-29", "2026-13-01", "2026-1-01", "2026-11-03T00:00:00", "0000-01-01")) assertThrows(BusinessException.class, () -> create(1, bad, "事项"));
        for (String bad : List.of("2026-13", "2026-1", "2026-11-03", "0000-01")) assertThrows(BusinessException.class, () -> service.list(1, bad));
        assertThrows(BusinessException.class, () -> service.create(1, Map.of("content", "伪造", "itemDate", "2026-11-03", "userId", 2)));
        long item = id(create(1, "2026-11-03", "合法"));
        assertThrows(BusinessException.class, () -> service.update(1, item, Map.of("completed", "true")));
        assertThrows(BusinessException.class, () -> service.update(1, item, Map.of("user_id", 2)));
        assertThrows(BusinessException.class, () -> service.update(1, item, Map.of("taskId", 100)));
        assertThrows(BusinessException.class, () -> service.update(1, item, Map.of()));
    }
    @Test void httpUsesAuthenticatedOwnerAndStableLowercaseFields() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new DailyItemController(service)).setControllerAdvice(new GlobalExceptionHandler()).build();
        var json = new com.fasterxml.jackson.databind.ObjectMapper();
        login(1);
        var response = mvc.perform(post("/daily-items").contentType("application/json").content(json.writeValueAsBytes(Map.of("content", "私有待办", "itemDate", "2026-11-03"))))
                .andExpect(status().isOk()).andReturn();
        var created = json.readTree(response.getResponse().getContentAsByteArray()).path("data");
        long id = created.path("id").asLong(); assertTrue(id > 0); assertEquals("2026-11-03", created.path("item_date").asText()); assertFalse(created.path("completed").asBoolean());
        login(3);
        var hidden = mvc.perform(get("/daily-items").param("month", "2026-11").param("userId", "1")).andExpect(status().isOk()).andReturn();
        assertEquals(0, json.readTree(hidden.getResponse().getContentAsByteArray()).path("data").size());
        mvc.perform(patch("/daily-items/" + id).contentType("application/json").content("{\"completed\":true}")).andExpect(status().isNotFound());
        mvc.perform(delete("/daily-items/" + id)).andExpect(status().isNotFound());
        login(1);
        mvc.perform(patch("/daily-items/" + id).contentType("application/json").content("{\"completed\":true}")).andExpect(status().isOk());
        mvc.perform(delete("/daily-items/" + id)).andExpect(status().isOk());
        assertTrue(service.list(1, "2026-11").isEmpty());
    }
    private void login(long id) { SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(id, "", List.of())); }
}

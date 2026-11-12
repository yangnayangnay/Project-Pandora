package com.pandora.backend.ai;

import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DivinationRecordTest {
    private JdbcTemplate db;
    private AIController controller;
    private AIGatewayService gateway;

    @BeforeEach void setup() {
        db = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:divination-" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        db.execute("CREATE TABLE divination_records(id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, "
                + "divination_date DATE NOT NULL, original_name VARCHAR(16), mutual_name VARCHAR(16), changed_name VARCHAR(16), "
                + "moving_line INT, analysis VARCHAR(2000), source VARCHAR(32), UNIQUE(user_id,divination_date))");
        controller = new AIController(); gateway = mock(AIGatewayService.class);
        when(gateway.invoke(anyString(), eq("DIVINATION"), any(AiTransmitConfig.class)))
                .thenReturn(Map.of("result", "new interpretation", "source", "EXTERNAL"));
        ReflectionTestUtils.setField(controller, "jdbcTemplate", db);
        ReflectionTestUtils.setField(controller, "aiGatewayService", gateway);
        login(1L);
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    private void login(long user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, "", List.of()));
    }
    private void existing(long user) {
        db.update("INSERT INTO divination_records(user_id,divination_date,original_name,analysis,source) VALUES(?,CURRENT_DATE,?,?,?)",
                user, "old", "old interpretation", "LOCAL_FALLBACK");
    }
    private Map<String, Object> request(boolean reroll) {
        return Map.of("originalName", "new", "mutualName", "mutual", "changedName", "changed", "movingLine", 3, "reroll", reroll);
    }
    @Test void repeatReplacesTodaysResultAndReloadReturnsIt() {
        existing(1L); existing(2L);
        Map<String, Object> result = controller.recordDivination(request(true), new MockHttpServletRequest()).getBody().getData();
        assertEquals(false, result.get("alreadyToday"));
        Map<?, ?> record = (Map<?, ?>) controller.getDivinationToday().getBody().getData().get("record");
        assertEquals("new", record.get("originalName"));
        assertEquals("new interpretation", record.get("analysis"));
        assertEquals(3, record.get("movingLine"));
        assertEquals(1, db.queryForObject("SELECT COUNT(*) FROM divination_records WHERE user_id=1", Integer.class));
        assertEquals("old", db.queryForObject("SELECT original_name FROM divination_records WHERE user_id=2", String.class));
    }
    @Test void legacyClientStillReceivesStoredRecordWithoutGeneratingAgain() {
        existing(1L);
        Map<String, Object> result = controller.recordDivination(request(false), new MockHttpServletRequest()).getBody().getData();
        assertEquals(true, result.get("alreadyToday"));
        assertEquals("old", ((Map<?, ?>) result.get("record")).get("originalName"));
        verifyNoInteractions(gateway);
    }
    @Test void firstRepeatRequestCreatesTodaysRecordAndLeavesHistoryAlone() {
        db.update("INSERT INTO divination_records(user_id,divination_date,original_name,analysis,source) VALUES(1,CURRENT_DATE-1,'historic','old','LOCAL')");
        controller.recordDivination(request(true), new MockHttpServletRequest());
        assertEquals(2, db.queryForObject("SELECT COUNT(*) FROM divination_records WHERE user_id=1", Integer.class));
        assertEquals("historic", db.queryForObject("SELECT original_name FROM divination_records WHERE divination_date=CURRENT_DATE-1", String.class));
    }
}

package com.pandora.backend.log;

import com.pandora.backend.common.BusinessException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LogContentRulesTest {
    @Test void titleIsCanonicalAndUsesSelectedDay() {
        assertEquals("1月2日日志", LogContentRules.title("2026-01-02"));
        assertEquals("11月13日日志", LogContentRules.title("2026-11-13"));
    }
    @Test void bodyMustContainVisibleContentAndKeepsUserFormatting() {
        for (Object value : new Object[]{null, 1, "", " \n\t", "　", "\u00a0"})
            assertThrows(BusinessException.class, () -> LogContentRules.requireBody(value));
        assertEquals("\n  完成联调\n", LogContentRules.requireBody("\n  完成联调\n"));
    }
}

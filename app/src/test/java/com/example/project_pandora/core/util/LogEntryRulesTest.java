package com.example.project_pandora.core.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class LogEntryRulesTest {
    @Test public void titleUsesTheSelectedDayWithoutLeadingZeros() {
        assertEquals("1月2日日志", LogEntryRules.title("2026-01-02"));
        assertEquals("11月13日日志", LogEntryRules.title("2026-11-13"));
    }
    @Test public void bodyRejectsAllWhitespaceButPreservesMultilineContent() {
        assertFalse(LogEntryRules.hasBody(null)); assertFalse(LogEntryRules.hasBody(" \n\t　\u00a0"));
        assertTrue(LogEntryRules.hasBody("\n完成联调\n"));
    }
}

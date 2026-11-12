package com.example.project_pandora.core.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class SignatureRulesTest {
    @Test public void countsBlankAndTrailingLines() {
        assertEquals(1, SignatureRules.explicitLines(null));
        assertEquals(4, SignatureRules.explicitLines("一\n二\n三\n四"));
        assertEquals(4, SignatureRules.explicitLines("\n\n\n"));
        assertEquals(5, SignatureRules.explicitLines("一\n\n\n\n"));
    }
    @Test public void pastedWindowsAndUnicodeBreaksHaveConsistentMeaning() {
        assertEquals("一\n二\n三\n四", SignatureRules.normalize("一\r\n二\r三\u2028四"));
        assertEquals(5, SignatureRules.explicitLines("一\u2029二\u0085三\r四\n五"));
    }
}

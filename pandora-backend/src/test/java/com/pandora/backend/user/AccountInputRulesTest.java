package com.pandora.backend.user;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class AccountInputRulesTest {
    static String identity(String prefix) {
        int[] weights = {7,9,10,5,8,4,2,1,6,3,7,9,10,5,8,4,2};
        int sum = 0;
        for (int i = 0; i < 17; i++) sum += (prefix.charAt(i) - '0') * weights[i];
        return prefix + "10X98765432".charAt(sum % 11);
    }
    @Test void namesAndPasswordsHaveExactCharacterBoundaries() {
        for (String name : new String[]{"吴六", "汉".repeat(15), "𠮷野"}) assertTrue(AccountInputRules.realName(name));
        for (String name : new String[]{"", "吴 六", "Alice", "陈十1", "汉".repeat(16), "吴·六"}) assertFalse(AccountInputRules.realName(name));
        for (String value : new String[]{"123456", "a".repeat(16), "密".repeat(16), "😀".repeat(16)}) assertTrue(AccountInputRules.password(value));
        for (String value : new String[]{"", "12345", "a".repeat(17), "😀".repeat(17)}) assertFalse(AccountInputRules.password(value));
    }
    @Test void identityChecksEverySectionAndExactRetirementDay() {
        var today = LocalDate.of(2026, 11, 23);
        for (String prefix : new String[]{"11010119900101001", "11010319900101001", "11011519900101001", "11010119661124001", "11010120000229001"})
            assertTrue(AccountInputRules.idCard(identity(prefix), today), prefix);
        for (String prefix : new String[]{"11999919900101001", "11000019900101001", "11010119661123001", "11010119661122001", "11010120271123001", "11010120010229001", "11010119900101000"})
            assertFalse(AccountInputRules.idCard(identity(prefix), today), prefix);
        assertFalse(AccountInputRules.idCard("11010119900101001X", today));
        assertTrue(AccountInputRules.idCard(identity("11010119900203001").toLowerCase(), today));
    }
}

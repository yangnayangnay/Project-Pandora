package com.example.project_pandora.ui.mine;

import org.junit.Test;
import static org.junit.Assert.*;

public class AvatarInitialsTest {
    @Test public void keepsLastTwoNameCharactersAndSupportsShortNames() {
        assertEquals("小明", AvatarInitials.fromName("王小明"));
        assertEquals("娜娜", AvatarInitials.fromName(" 欧阳娜娜 "));
        assertEquals("王", AvatarInitials.fromName("王"));
        assertEquals("01", AvatarInitials.fromName("000001"));
    }
    @Test public void emptyNamesHaveAFallbackAndUnicodeCharactersAreNotSplit() {
        assertEquals("用户", AvatarInitials.fromName(null));
        assertEquals("用户", AvatarInitials.fromName("  "));
        assertEquals("𠮷明", AvatarInitials.fromName("王𠮷明"));
    }
}

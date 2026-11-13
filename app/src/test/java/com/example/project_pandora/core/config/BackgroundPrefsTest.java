package com.example.project_pandora.core.config;

import org.junit.Test;
import static org.junit.Assert.*;

public class BackgroundPrefsTest {
    @Test public void existingBackgroundChoicesKeepTheirStoredMeaningAndCuteIsAThirdChoice() {
        assertEquals(BackgroundPrefs.SOLID, BackgroundPrefs.normalizeStyle(0));
        assertEquals(BackgroundPrefs.COMPANY, BackgroundPrefs.normalizeStyle(1));
        assertEquals(BackgroundPrefs.CUTE, BackgroundPrefs.normalizeStyle(2));
        assertEquals(BackgroundPrefs.COMPANY, BackgroundPrefs.normalizeStyle(-1));
        assertEquals(BackgroundPrefs.COMPANY, BackgroundPrefs.normalizeStyle(99));
    }
}

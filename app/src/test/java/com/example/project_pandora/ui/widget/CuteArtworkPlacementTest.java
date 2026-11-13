package com.example.project_pandora.ui.widget;

import org.junit.Test;
import static org.junit.Assert.*;

public class CuteArtworkPlacementTest {
    @Test public void artworkCoversPhoneLandscapeTabletAndSplitWindowsWithoutSideOrBottomGaps() {
        for (int[] viewport : new int[][]{{1080, 2400}, {1080, 1920}, {2400, 1080}, {1200, 1200}, {360, 800}}) {
            float scale = CuteArtworkPlacement.scale(viewport[0], viewport[1], 720, 1600);
            assertTrue(scale > 0);
            assertTrue(720 * scale >= viewport[0] - .01f);
            assertTrue(1600 * scale >= viewport[1] - .01f);
            assertTrue(Math.abs(720 * scale - viewport[0]) < .01f || Math.abs(1600 * scale - viewport[1]) < .01f);
        }
    }
    @Test public void unmeasuredWindowsDoNotProduceInvalidTransforms() {
        assertEquals(0f, CuteArtworkPlacement.scale(0, 800, 720, 1600), 0f);
        assertEquals(0f, CuteArtworkPlacement.scale(360, 800, 0, 1600), 0f);
    }
}

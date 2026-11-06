package com.example.project_pandora.ui.widget;

import org.junit.Test;
import static org.junit.Assert.*;

public class PeriodSwipeGestureTest {
    @Test public void leftAdvancesAndRightGoesBack() {
        PeriodSwipeGesture swipe = new PeriodSwipeGesture(8, 48);
        swipe.start(150, 100); assertTrue(swipe.move(120, 102));
        assertEquals(1, swipe.finish(50, 105));
        swipe.start(50, 100); assertTrue(swipe.move(80, 102));
        assertEquals(-1, swipe.finish(150, 105));
    }

    @Test public void shortDragsAndTapsDoNotPage() {
        PeriodSwipeGesture swipe = new PeriodSwipeGesture(8, 48);
        swipe.start(100, 100); assertEquals(0, swipe.finish(100, 100));
        swipe.start(100, 100); swipe.move(80, 100);
        assertEquals(0, swipe.finish(60, 100));
    }

    @Test public void verticalScrollRemainsLockedEvenIfTheFingerMovesSidewaysLater() {
        PeriodSwipeGesture swipe = new PeriodSwipeGesture(8, 48);
        swipe.start(100, 100); assertFalse(swipe.move(102, 120));
        assertFalse(swipe.move(220, 125));
        assertEquals(0, swipe.finish(230, 125));
    }

    @Test public void diagonalScrollDoesNotPage() {
        PeriodSwipeGesture swipe = new PeriodSwipeGesture(8, 48);
        swipe.start(100, 100); swipe.move(130, 125);
        assertEquals(0, swipe.finish(200, 190));
    }

    @Test public void turningIntoVerticalMotionDoesNotCommitAPage() {
        PeriodSwipeGesture swipe = new PeriodSwipeGesture(8, 48);
        swipe.start(100, 100); assertTrue(swipe.move(130, 102));
        assertEquals(0, swipe.finish(160, 200));
    }

    @Test public void cancelIncludingMultiTouchDiscardsAPendingSwipe() {
        PeriodSwipeGesture swipe = new PeriodSwipeGesture(8, 48);
        swipe.start(200, 100); swipe.move(150, 100); swipe.cancel();
        assertEquals(0, swipe.finish(50, 100));
    }

    @Test public void aGestureCanCommitOnlyOnce() {
        PeriodSwipeGesture swipe = new PeriodSwipeGesture(8, 48);
        swipe.start(200, 100); swipe.move(150, 100); swipe.move(100, 100);
        assertEquals(1, swipe.finish(50, 100));
        assertEquals(0, swipe.finish(20, 100));
        assertFalse(swipe.move(0, 100));
    }

    @Test public void aNewGestureResetsTheDirectionLock() {
        PeriodSwipeGesture swipe = new PeriodSwipeGesture(8, 48);
        swipe.start(100, 100); swipe.move(100, 160); swipe.finish(100, 200);
        swipe.start(200, 100); assertTrue(swipe.move(150, 100));
        assertEquals(1, swipe.finish(100, 100));
    }
}

package com.h2play.canvas_magic.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SecretSelectionGestureTest {
    @Test public void screenEdgesAreClampedToValidCells() {
        assertEquals(1, SecretSelectionGesture.cellForCoordinates(0, 0, 900, 2100, 9));
        assertEquals(9, SecretSelectionGesture.cellForCoordinates(900, 2100, 900, 2100, 9));
        assertEquals(6, SecretSelectionGesture.cellForCoordinates(900, 2100, 900, 2100, 6));
    }

    @Test public void sameCellTwiceSelectsWithoutRecordingTheChoiceExternally() {
        SecretSelectionGesture gesture = new SecretSelectionGesture();
        assertEquals(-1, gesture.registerTap(8, 1_000));
        assertEquals(9, gesture.registerTap(8, 1_350));
    }

    @Test public void differentCellsDoNotSelect() {
        SecretSelectionGesture gesture = new SecretSelectionGesture();
        gesture.registerTap(1, 1_000);
        assertEquals(-1, gesture.registerTap(2, 1_200));
    }

    @Test public void twoFingerHoldAbortsAfterTwelveHundredMilliseconds() {
        SecretSelectionGesture gesture = new SecretSelectionGesture();
        gesture.startAbortHold(1_000, 10, 10, 40, 40);
        assertFalse(gesture.shouldAbort(2_199));
        assertTrue(gesture.shouldAbort(2_200));
    }

    @Test public void movingEitherFingerCancelsAbort() {
        SecretSelectionGesture gesture = new SecretSelectionGesture();
        gesture.startAbortHold(1_000, 10, 10, 40, 40);
        gesture.updateAbortHold(80, 10, 40, 40, 24);
        assertFalse(gesture.shouldAbort(3_000));
    }
}

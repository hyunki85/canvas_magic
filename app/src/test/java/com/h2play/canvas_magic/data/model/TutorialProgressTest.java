package com.h2play.canvas_magic.data.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TutorialProgressTest {
    @Test public void legacyGuideNeededStartsFromBeginning() {
        assertEquals(TutorialProgress.NOT_STARTED,
                TutorialProgress.migrateLegacy(false, 0));
    }

    @Test public void legacyCompletionWithoutPerformanceResumesPractice() {
        assertEquals(TutorialProgress.PRACTICE,
                TutorialProgress.migrateLegacy(true, 0));
    }

    @Test public void legacyCompletionWithPerformanceStaysComplete() {
        assertEquals(TutorialProgress.COMPLETED,
                TutorialProgress.migrateLegacy(true, 2));
    }

    @Test public void invalidStoredValueIsRejected() {
        assertNull(TutorialProgress.fromStored("DONE"));
    }
}

package com.h2play.canvas_magic.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class PromoImpressionTrackerTest {
    @Test public void backgroundRefreshDoesNotConsumeNextVisibleImpression() {
        PromoImpressionTracker tracker = new PromoImpressionTracker();
        assertFalse(tracker.recordIfVisible("8|ko|mind", false, false, 100, 100));
        assertFalse(tracker.recordIfVisible("8|ko|mind", true, false, 100, 100));
        assertTrue(tracker.recordIfVisible("8|ko|mind", true, true, 100, 100));
        assertFalse(tracker.recordIfVisible("8|ko|mind", true, true, 100, 100));
    }

    @Test public void scrollRequiresHalfTheCardAndDoesNotCountRepeatedPasses() {
        PromoImpressionTracker tracker = new PromoImpressionTracker();
        assertFalse(tracker.recordIfVisible("mind", true, true, 0, 0));
        assertFalse(tracker.recordIfVisible("mind", true, true, 49, 100));
        assertTrue(tracker.recordIfVisible("mind", true, true, 50, 100));
        assertFalse(tracker.recordIfVisible("mind", true, true, 100, 100));
        assertTrue(tracker.recordIfVisible("number", true, true, 100, 100));
    }

    @Test public void recreationPreservesCountsButANewCampaignCanBeMeasured() {
        PromoImpressionTracker before = new PromoImpressionTracker();
        assertTrue(before.recordIfVisible("8|ko|mind", true, true, 100, 100));
        PromoImpressionTracker recreated = new PromoImpressionTracker();
        recreated.restore(before.snapshot());
        assertFalse(recreated.recordIfVisible("8|ko|mind", true, true, 100, 100));
        assertTrue(recreated.recordIfVisible("9|ko|mind", true, true, 100, 100));
    }
}

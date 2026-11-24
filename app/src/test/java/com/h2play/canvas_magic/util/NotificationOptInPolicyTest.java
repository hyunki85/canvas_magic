package com.h2play.canvas_magic.util;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NotificationOptInPolicyTest {
    @Test public void onlyEligibleNonOptedInUsersArePrompted() {
        assertFalse(NotificationOptInPolicy.shouldPrompt(false, false, 0, 0, 100));
        assertFalse(NotificationOptInPolicy.shouldPrompt(true, true, 0, 0, 100));
        assertTrue(NotificationOptInPolicy.shouldPrompt(true, false, 0, 0, 100));
    }

    @Test public void promptRetriesOnceAfterFourteenDays() {
        long first = 1_000;
        assertFalse(NotificationOptInPolicy.shouldPrompt(true, false, 1, first,
                first + NotificationOptInPolicy.RETRY_INTERVAL_MS - 1));
        assertTrue(NotificationOptInPolicy.shouldPrompt(true, false, 1, first,
                first + NotificationOptInPolicy.RETRY_INTERVAL_MS));
        assertFalse(NotificationOptInPolicy.shouldPrompt(true, false, 2, first,
                first + NotificationOptInPolicy.RETRY_INTERVAL_MS));
    }

    @Test public void deliveriesAreLimitedToOncePerSevenDays() {
        assertFalse(NotificationOptInPolicy.canDeliver(1_000,
                1_000 + NotificationOptInPolicy.DELIVERY_INTERVAL_MS - 1));
        assertTrue(NotificationOptInPolicy.canDeliver(1_000,
                1_000 + NotificationOptInPolicy.DELIVERY_INTERVAL_MS));
    }
}

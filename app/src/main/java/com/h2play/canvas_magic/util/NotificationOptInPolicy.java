package com.h2play.canvas_magic.util;

import java.util.concurrent.TimeUnit;

/** Time/count policy kept independent from Android so it can be unit-tested. */
public final class NotificationOptInPolicy {
    public static final long RETRY_INTERVAL_MS = TimeUnit.DAYS.toMillis(14);
    public static final long DELIVERY_INTERVAL_MS = TimeUnit.DAYS.toMillis(7);
    public static final int MAX_PROMPT_COUNT = 2;

    private NotificationOptInPolicy() { }

    public static boolean shouldPrompt(boolean eligible, boolean optedIn, int promptCount,
                                       long lastPromptTime, long now) {
        if (!eligible || optedIn || promptCount >= MAX_PROMPT_COUNT) return false;
        return lastPromptTime <= 0 || now - lastPromptTime >= RETRY_INTERVAL_MS;
    }

    public static boolean canDeliver(long lastDeliveryTime, long now) {
        return lastDeliveryTime <= 0 || now - lastDeliveryTime >= DELIVERY_INTERVAL_MS;
    }
}

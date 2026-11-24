package com.h2play.canvas_magic.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/** One impression per home instance, config and card, including after activity recreation. */
public final class PromoImpressionTracker {
    private final Set<String> recorded = new HashSet<>();

    public boolean recordIfVisible(@NonNull String key, boolean foreground, boolean focused,
                                   long visibleArea, long totalArea) {
        if (!foreground || !focused || totalArea <= 0 || visibleArea < (totalArea + 1) / 2) {
            return false;
        }
        return recorded.add(key);
    }

    public void restore(@Nullable Collection<String> keys) {
        if (keys != null) recorded.addAll(keys);
    }

    @NonNull public ArrayList<String> snapshot() {
        return new ArrayList<>(recorded);
    }
}

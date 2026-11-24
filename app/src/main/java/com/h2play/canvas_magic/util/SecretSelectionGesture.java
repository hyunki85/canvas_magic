package com.h2play.canvas_magic.util;

/** Pure gesture state used by the discreet selection screen. */
public final class SecretSelectionGesture {
    public static final long DOUBLE_TAP_MS = 400L;
    public static final long ABORT_HOLD_MS = 1_200L;

    public static int cellForCoordinates(float x, float y, int width, int height, int count) {
        int columns = 3;
        int rows = Math.max(1, count / columns);
        int column = clamp((int) (x / Math.max(1, width / columns)), 0, columns - 1);
        int row = clamp((int) (y / Math.max(1, height / rows)), 0, rows - 1);
        return Math.min(count, row * columns + column + 1);
    }

    private long lastTapTime = Long.MIN_VALUE;
    private int lastTapIndex = -1;
    private boolean multiTouchSequence;
    private boolean holdActive;
    private long holdStartedAt;
    private float firstX;
    private float firstY;
    private float secondX;
    private float secondY;

    /** Returns the one-based selection, or -1 while the gesture is incomplete. */
    public int registerTap(int zeroBasedIndex, long eventTime) {
        if (multiTouchSequence) {
            multiTouchSequence = false;
            resetTap();
            return -1;
        }
        int selected = eventTime - lastTapTime < DOUBLE_TAP_MS && lastTapIndex == zeroBasedIndex
                ? zeroBasedIndex + 1 : -1;
        lastTapIndex = zeroBasedIndex;
        lastTapTime = eventTime;
        return selected;
    }

    public void startAbortHold(long eventTime, float x1, float y1, float x2, float y2) {
        multiTouchSequence = true;
        holdActive = true;
        holdStartedAt = eventTime;
        firstX = x1;
        firstY = y1;
        secondX = x2;
        secondY = y2;
        resetTap();
    }

    public void updateAbortHold(float x1, float y1, float x2, float y2, float tolerancePx) {
        if (!holdActive) return;
        if (distance(firstX, firstY, x1, y1) > tolerancePx
                || distance(secondX, secondY, x2, y2) > tolerancePx) {
            holdActive = false;
        }
    }

    public boolean shouldAbort(long eventTime) {
        return holdActive && eventTime - holdStartedAt >= ABORT_HOLD_MS;
    }

    public void endMultiTouch() {
        holdActive = false;
    }

    public boolean isMultiTouchSequence() {
        return multiTouchSequence;
    }

    public boolean isHoldActive() {
        return holdActive;
    }

    public void reset() {
        multiTouchSequence = false;
        holdActive = false;
        resetTap();
    }

    private void resetTap() {
        lastTapTime = Long.MIN_VALUE;
        lastTapIndex = -1;
    }

    private static float distance(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}

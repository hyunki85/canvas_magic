package com.h2play.canvas_magic.util.ads;

/** Consent-aware single cached banner. Navigation never waits for a request or retries. */
final class ExitAdState {
    private static final long MAX_AGE_MS = 55 * 60_000L;
    private boolean resumed;
    private boolean permitted;
    private boolean loading;
    private boolean loaded;
    private boolean destroyed;
    private int revision = -1;
    private int generation;
    private long loadedAt;
    private long retryAfter;

    void resume() { resumed = true; }
    void pause() { resumed = false; }

    boolean updateConsent(boolean allowed, int revision) {
        if (permitted == allowed && this.revision == revision) return false;
        permitted = allowed;
        this.revision = revision;
        invalidate();
        return true;
    }

    boolean expire(long now) {
        if (!loaded || (now >= loadedAt && now - loadedAt < MAX_AGE_MS)) return false;
        invalidate();
        return true;
    }

    boolean needsRequest(long now) {
        return !destroyed && resumed && permitted && !loading && !loaded && now >= retryAfter;
    }

    int request(long now) {
        if (!needsRequest(now)) throw new IllegalStateException("Exit ad request not allowed");
        loading = true;
        return ++generation;
    }

    boolean isCurrent(int token) { return !destroyed && permitted && generation == token; }

    void loaded(int token, long now) {
        if (!isCurrent(token)) return;
        loading = false;
        loaded = true;
        loadedAt = now;
    }

    void failed(int token, long now) {
        if (!isCurrent(token)) return;
        invalidate();
        retryAfter = now + 60_000L;
    }

    boolean canShow(long now) {
        return !destroyed && resumed && permitted && loaded
                && now >= loadedAt && now - loadedAt < MAX_AGE_MS;
    }

    private void invalidate() {
        generation++;
        loaded = false;
        loading = false;
        retryAfter = 0;
    }

    void destroy() { destroyed = true; invalidate(); }
}

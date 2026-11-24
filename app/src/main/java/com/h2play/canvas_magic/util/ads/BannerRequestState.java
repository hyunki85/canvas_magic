package com.h2play.canvas_magic.util.ads;

/** Prevents duplicate requests and rejects callbacks for resized or disposed banners. */
final class BannerRequestState {
    private boolean resumed;
    private boolean permitted;
    private boolean destroyed;
    private int width;
    private int generation;
    private int consentRevision = -1;

    void resume() { resumed = true; }
    void pause() { resumed = false; }

    boolean updateConsent(boolean allowed, int revision) {
        boolean changed = permitted != allowed || consentRevision != revision;
        permitted = allowed;
        consentRevision = revision;
        if (changed) invalidate();
        return changed;
    }

    boolean needsRequest(int availableWidth) {
        return !destroyed && resumed && permitted && availableWidth > 0 && width != availableWidth;
    }

    int request(int availableWidth) {
        if (!needsRequest(availableWidth)) throw new IllegalStateException("Banner request not allowed");
        width = availableWidth;
        return ++generation;
    }

    boolean isCurrent(int token) { return !destroyed && permitted && token == generation && width > 0; }

    private void invalidate() { width = 0; generation++; }

    void destroy() {
        destroyed = true;
        invalidate();
    }
}

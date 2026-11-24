package com.h2play.canvas_magic.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CommunityDeepLinkTest {

    @Test
    public void validFeaturedSetLinkReturnsDocumentId() {
        assertTrue(CommunityDeepLink.isValid("community:set_2026-09"));
        assertEquals("set_2026-09",
                CommunityDeepLink.parseDocumentId("community:set_2026-09"));
    }

    @Test
    public void malformedOrMissingSetLinkFallsBackToFeaturedTab() {
        assertFalse(CommunityDeepLink.isValid("community:"));
        assertFalse(CommunityDeepLink.isValid("community:set/../../other"));
        assertFalse(CommunityDeepLink.isValid("https://example.com/set"));
        assertNull(CommunityDeepLink.parseDocumentId(null));
    }
}

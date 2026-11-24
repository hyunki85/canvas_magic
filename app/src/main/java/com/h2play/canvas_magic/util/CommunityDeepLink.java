package com.h2play.canvas_magic.util;

import androidx.annotation.Nullable;

import java.util.regex.Pattern;

/** Parser for the deliberately small FCM deep-link contract used by Canvas Trick. */
public final class CommunityDeepLink {

    private static final String PREFIX = "community:";
    private static final Pattern DOCUMENT_ID = Pattern.compile("[A-Za-z0-9_-]{1,128}");

    private CommunityDeepLink() {
    }

    @Nullable
    public static String parseDocumentId(@Nullable String link) {
        if (link == null || !link.startsWith(PREFIX)) return null;
        String documentId = link.substring(PREFIX.length());
        return DOCUMENT_ID.matcher(documentId).matches() ? documentId : null;
    }

    public static boolean isValid(@Nullable String link) {
        return parseDocumentId(link) != null;
    }
}

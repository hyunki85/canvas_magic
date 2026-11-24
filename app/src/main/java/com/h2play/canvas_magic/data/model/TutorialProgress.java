package com.h2play.canvas_magic.data.model;

/** Persisted onboarding state. Completion means the practice drawing was rendered successfully. */
public enum TutorialProgress {
    NOT_STARTED,
    DRAWING_GUIDE,
    SECRET_SELECTION_GUIDE,
    PRACTICE,
    COMPLETED;

    public boolean isComplete() {
        return this == COMPLETED;
    }

    public static TutorialProgress fromStored(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static TutorialProgress migrateLegacy(boolean legacyCompleted, int trickCount) {
        if (!legacyCompleted) return NOT_STARTED;
        return trickCount > 0 ? COMPLETED : PRACTICE;
    }
}

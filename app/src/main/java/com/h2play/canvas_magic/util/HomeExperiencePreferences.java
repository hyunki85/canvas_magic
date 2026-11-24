package com.h2play.canvas_magic.util;

import android.content.Context;

/** Remember the first home introduction; old practice history is left untouched but unused. */
public final class HomeExperiencePreferences {
    private HomeExperiencePreferences() {}
    public static boolean hasSeenIntro(Context context) {
        return context.getSharedPreferences("canvas_home_experience", Context.MODE_PRIVATE)
                .getBoolean("intro_seen", false);
    }
    public static void markIntroSeen(Context context) {
        context.getSharedPreferences("canvas_home_experience", Context.MODE_PRIVATE)
                .edit().putBoolean("intro_seen", true).apply();
    }
}

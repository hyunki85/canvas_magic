package com.h2play.canvas_magic.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

/**
 * Stores the drawing tool settings independently of an Activity instance.
 */
public final class DrawingSettingsPreferences {

    private static final String PREFS_NAME = "drawing_settings_prefs";
    private static final String KEY_COLOR = "selected_color";
    private static final String KEY_THICKNESS = "selected_thickness";

    public static final int DEFAULT_COLOR = Color.RED;
    public static final float DEFAULT_THICKNESS = 10f;
    public static final float MIN_THICKNESS = 5f;
    public static final float MAX_THICKNESS = 50f;

    private DrawingSettingsPreferences() {
    }

    public static int getColor(Context context) {
        return getPreferences(context).getInt(KEY_COLOR, DEFAULT_COLOR);
    }

    public static float getThickness(Context context) {
        float thickness = getPreferences(context).getFloat(KEY_THICKNESS, DEFAULT_THICKNESS);
        return clampThickness(thickness);
    }

    public static void save(Context context, int color, float thickness) {
        getPreferences(context)
                .edit()
                .putInt(KEY_COLOR, color)
                .putFloat(KEY_THICKNESS, clampThickness(thickness))
                .apply();
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static float clampThickness(float thickness) {
        if (Float.isNaN(thickness)) {
            return DEFAULT_THICKNESS;
        }
        return Math.max(MIN_THICKNESS, Math.min(MAX_THICKNESS, thickness));
    }
}

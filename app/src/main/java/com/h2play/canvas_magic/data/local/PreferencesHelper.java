package com.h2play.canvas_magic.data.local;

import android.content.SharedPreferences;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PreferencesHelper {

    private static final String PREF_FILE_NAME = "mvpstarter_pref_file";

    private final SharedPreferences preferences;

    @Inject
    PreferencesHelper(SharedPreferences sharedPreferences) {
        preferences = sharedPreferences;
    }

    public boolean hasKey(@Nonnull String key) {
        return preferences.contains(key);
    }

    public void putString(@Nonnull String key, @Nonnull String value) {
        preferences.edit().putString(key, value).apply();
    }

    public String getString(@Nonnull String key) {
        return preferences.getString(key, "");
    }

    public void putBoolean(@Nonnull String key, @Nonnull boolean value) {
        preferences.edit().putBoolean(key, value).apply();
    }

    public boolean getBoolean(@Nonnull String key) {
        return preferences.getBoolean(key, false);
    }

    public void putInt(@Nonnull String key, @Nonnull int value) {
        preferences.edit().putInt(key, value).apply();
    }

    public int getInt(@Nonnull String key) {
        return preferences.getInt(key, -1);
    }

    public void clear() {
        preferences.edit().clear().apply();
    }
}

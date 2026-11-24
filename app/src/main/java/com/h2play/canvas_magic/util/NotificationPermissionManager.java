package com.h2play.canvas_magic.util;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.content.ContextCompat;

/**
 * 알림 권한을 앱 시작 직후가 아니라 첫 마술을 성공한 뒤에만 제안한다.
 * 거절한 사용자에게는 최대 한 번만, 14일 뒤 다시 제안할 수 있다.
 */
public final class NotificationPermissionManager {

    private static final String KEY_ELIGIBLE = "notification_permission_eligible";
    private static final String KEY_PROMPT_COUNT = "notification_permission_prompt_count";
    private static final String KEY_LAST_PROMPT_TIME = "notification_permission_last_prompt_time";
    private static final String KEY_OPTED_IN = "notification_new_drawings_opted_in";

    private NotificationPermissionManager() {
    }

    public static void markEligible(Context context) {
        preferences(context).edit().putBoolean(KEY_ELIGIBLE, true).apply();
    }

    public static boolean isEligible(Context context) {
        return preferences(context).getBoolean(KEY_ELIGIBLE, false);
    }

    public static boolean shouldShowOptIn(Context context) {
        SharedPreferences prefs = preferences(context);
        return NotificationOptInPolicy.shouldPrompt(
                prefs.getBoolean(KEY_ELIGIBLE, false),
                prefs.getBoolean(KEY_OPTED_IN, false),
                prefs.getInt(KEY_PROMPT_COUNT, 0),
                prefs.getLong(KEY_LAST_PROMPT_TIME, 0), System.currentTimeMillis());
    }

    public static void markPromptShown(Context context) {
        SharedPreferences prefs = preferences(context);
        int count = prefs.getInt(KEY_PROMPT_COUNT, 0);
        prefs.edit()
                .putInt(KEY_PROMPT_COUNT, count + 1)
                .putLong(KEY_LAST_PROMPT_TIME, System.currentTimeMillis())
                .apply();
    }

    public static void setOptedIn(Context context, boolean optedIn) {
        preferences(context).edit().putBoolean(KEY_OPTED_IN, optedIn).apply();
    }

    public static boolean isOptedIn(Context context) {
        return preferences(context).getBoolean(KEY_OPTED_IN, false);
    }

    public static boolean hasSystemPermission(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(
                ReminderWorker.PREF_NAME, Context.MODE_PRIVATE);
    }
}

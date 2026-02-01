package com.h2play.canvas_magic.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class ReminderScheduler {

    private static final String WORK_NAME = "canvas_reminder_work";

    public static void schedule(Context context) {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                ReminderWorker.class, 1, TimeUnit.DAYS)
                .setInitialDelay(1, TimeUnit.DAYS)
                .build();

        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME,
                        ExistingPeriodicWorkPolicy.KEEP,
                        request);
    }

    public static void recordVisit(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
                ReminderWorker.PREF_NAME, Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();

        if (!prefs.contains(ReminderWorker.KEY_INSTALL_TIME)) {
            prefs.edit().putLong(ReminderWorker.KEY_INSTALL_TIME, now).apply();
        }
        prefs.edit().putLong(ReminderWorker.KEY_LAST_VISIT, now).apply();
    }
}

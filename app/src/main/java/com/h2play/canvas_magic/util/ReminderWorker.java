package com.h2play.canvas_magic.util;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/** Compatibility tombstone for already queued work from older installs. Never posts a reminder. */
public class ReminderWorker extends Worker {
    public static final String PREF_NAME = "canvas_reminder_prefs";
    public static final String KEY_INSTALL_TIME = "install_time";
    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }
    @NonNull @Override public Result doWork() {
        ReminderScheduler.cancelRetiredReminders(getApplicationContext());
        return Result.success();
    }
}

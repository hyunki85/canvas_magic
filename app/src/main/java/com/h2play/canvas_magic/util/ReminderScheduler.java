package com.h2play.canvas_magic.util;

import android.app.NotificationManager;
import android.content.Context;
import androidx.work.WorkManager;

/** Upgrade cleanup only. Merely not opening a magic tool is not a reason to send a notification. */
public final class ReminderScheduler {
    private ReminderScheduler() {}
    public static void cancelRetiredReminders(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork("canvas_reminder_work");
        NotificationManager notifications =
                (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notifications == null) return;
        notifications.cancel(601);
        notifications.cancel(602);
        for (int id = 610; id <= 613; id++) notifications.cancel(id);
    }
}

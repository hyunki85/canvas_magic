package com.h2play.canvas_magic.util;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.h2play.canvas_magic.R;
import com.h2play.canvas_magic.features.menu.MenuActivity;

import java.util.concurrent.TimeUnit;

public class ReminderWorker extends Worker {

    public static final String PREF_NAME = "canvas_reminder_prefs";
    public static final String KEY_LAST_VISIT = "last_visit_time";
    public static final String KEY_INSTALL_TIME = "install_time";

    private static final String CHANNEL_ID = "canvas_reminder";
    private static final int NOTIFICATION_ID_3DAY = 601;
    private static final int NOTIFICATION_ID_7DAY = 602;

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return Result.success();
            }
        }

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        long lastVisit = prefs.getLong(KEY_LAST_VISIT, 0);
        if (lastVisit == 0) {
            return Result.success();
        }

        long now = System.currentTimeMillis();
        long elapsed = now - lastVisit;
        long days = TimeUnit.MILLISECONDS.toDays(elapsed);

        if (days >= 7) {
            sendNotification(context,
                    context.getString(R.string.reminder_title_7day),
                    context.getString(R.string.reminder_body_7day),
                    NOTIFICATION_ID_7DAY);
        } else if (days >= 3) {
            sendNotification(context,
                    context.getString(R.string.reminder_title_3day),
                    context.getString(R.string.reminder_body_3day),
                    NOTIFICATION_ID_3DAY);
        }

        return Result.success();
    }

    private void sendNotification(Context context, String title, String body, int notificationId) {
        Intent intent = new Intent(context, MenuActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, notificationId, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentIntent(pendingIntent);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    context.getString(R.string.reminder_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            nm.createNotificationChannel(channel);
        }

        nm.notify(notificationId, builder.build());
    }
}

/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.h2play.canvas_magic.util;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.util.concurrent.atomic.AtomicInteger;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.h2play.canvas_magic.R;
import com.h2play.canvas_magic.features.menu.MenuActivity;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final AtomicInteger notificationIdCounter = new AtomicInteger(521);
    private static final String CHANNEL_ID = "canvas_new_drawings";
    private static final String PREF_LAST_DELIVERY = "new_drawing_notification_last_delivery";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        if (!NotificationPermissionManager.isOptedIn(this)) return;
        String link = remoteMessage.getData().get("link");
        if (!isValidDrawingLink(link)) return;
        long now = System.currentTimeMillis();
        long lastDelivery = getSharedPreferences(ReminderWorker.PREF_NAME, MODE_PRIVATE)
                .getLong(PREF_LAST_DELIVERY, 0L);
        if (!NotificationOptInPolicy.canDeliver(lastDelivery, now)) return;

        String title = remoteMessage.getData().get("title");
        String body = remoteMessage.getData().get("body");
        if (remoteMessage.getNotification() != null) {
            if (TextUtils.isEmpty(title)) title = remoteMessage.getNotification().getTitle();
            if (TextUtils.isEmpty(body)) body = remoteMessage.getNotification().getBody();
        }
        if (TextUtils.isEmpty(title)) title = getString(R.string.new_drawing_notification_title);
        if (TextUtils.isEmpty(body)) body = getString(R.string.new_drawing_notification_body);
        if (sendNotification(title, body, link)) {
            getSharedPreferences(ReminderWorker.PREF_NAME, MODE_PRIVATE).edit()
                    .putLong(PREF_LAST_DELIVERY, now).apply();
        }
    }
    // [END receive_message]


    // [START on_new_token]

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    @Override
    public void onNewToken(String token) {

        saveRegistrationToServer(token);
        TopicManager.updateTopics(this);
    }
    // [END on_new_token]

    /**
     * Handle time allotted to BroadcastReceivers.
     */
    private void handleNow() {

    }

    private void saveRegistrationToServer(String token) {

    }

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param messageBody FCM message body received.
     */
    private boolean sendNotification(String title, String messageBody, String link) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        int notificationId = notificationIdCounter.getAndIncrement();
        Intent intent = new Intent(this, MenuActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (!TextUtils.isEmpty(link)) {
            intent.putExtra("link", link);
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, notificationId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification_magic)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    getString(R.string.reminder_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(notificationId, notificationBuilder.build());
        return true;
    }

    public static boolean isValidDrawingLink(String link) {
        return CommunityDeepLink.isValid(link);
    }
}
